package ru.validum.core.profile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import ru.validum.core.abs.AbsDefaults;
import ru.validum.core.account.AccountGenerator;
import ru.validum.core.account.AccountType;
import ru.validum.core.address.Address;
import ru.validum.core.address.AddressGenerator;
import ru.validum.core.address.AddressStrategy;
import ru.validum.core.address.FiasMode;
import ru.validum.core.bank.Bank;
import ru.validum.core.bank.BikGenerator;
import ru.validum.core.bank.BikSource;
import ru.validum.core.bank.BikSources;
import ru.validum.core.common.Country;
import ru.validum.core.common.Region;
import ru.validum.core.contact.Contact;
import ru.validum.core.contact.ContactGenerator;
import ru.validum.core.contact.ContactType;
import ru.validum.core.contact.EmailDomainMode;
import ru.validum.core.document.DocumentGenerator;
import ru.validum.core.document.DocumentRules;
import ru.validum.core.document.DocumentType;
import ru.validum.core.document.IdentityDocument;
import ru.validum.core.inn.InnCorruptor;
import ru.validum.core.inn.InnGenerator;
import ru.validum.core.name.FamilyStatus;
import ru.validum.core.name.FioScript;
import ru.validum.core.name.Gender;
import ru.validum.core.name.NameGenerator;
import ru.validum.core.name.PersonName;
import ru.validum.core.passport.Passport;
import ru.validum.core.passport.PassportGenerator;
import ru.validum.core.passport.PassportType;
import ru.validum.core.random.DataGenerator;
import ru.validum.core.snils.SnilsCorruptor;
import ru.validum.core.snils.SnilsGenerator;

/**
 * Генератор связанного профиля клиента-физлица.
 *
 * <p><b>Каскадные дефолты.</b> Пользователь задаёт только важные параметры, остальное
 * досчитывается автоматически (см. {@code README.md} и {@code docs/SCENARIOS.md}).
 *
 * <p><b>Банк (P3.3).</b> По умолчанию — случайный из справочника ({@link #bikSource}). Можно
 * задать явно:
 *
 * <ul>
 *   <li>{@code .bik("044525225")} — найти банк по БИК в справочнике. Если БИК не найден —
 *       {@link IllegalArgumentException};
 *   <li>{@code .bank(new Bank(...))} — использовать готовый объект {@link Bank} (например, из
 *       {@code .bank(new Bank(...))} — использовать готовый объект {@link Bank} (например, из
 *       {@link BikSource#findByBik(String)}). Приоритет выше, чем у {@code .bik}.
 * </ul>
 *
 * <p><b>Флаги {@code terrorist} и {@code blocked}</b> — для тестов клиентов, на которых АБС
 * должна реагировать особым образом. По умолчанию {@code false}, ставятся явно.
 */
public final class IndividualProfileGenerator implements DataGenerator {

  private static final int DEFAULT_AGE = 19;
  private static final int ADULT_AGE = 18;

  private static final List<String> SECRET_WORDS = loadLines("/secret_words.txt");
  private static final List<String> MANAGERS = loadLines("/managers.txt");

  private final Random random;
  private final BikSource bikSource;
  private final AddressStrategy addressStrategy;
  private final FiasMode fiasMode;
  private final Gender gender;
  private final Integer age;
  private final boolean withAccount;
  private final Boolean passportWasExchanged;
  private final PassportType passportType;
  private final Country passportCountry;
  private final FioScript fioScript;
  private final Set<CorruptField> corruptFields;
  private final Residency residency;
  private final String categoryClient;
  private final String accountPackage;
  private final FamilyStatus familyStatus;
  private final Region region;
  private final String secretWord;
  private final String personalManager;
  private final String email;
  private final EmailDomainMode emailDomainMode;
  private final boolean terrorist;
  private final boolean blocked;
  private final String bik;
  private final Bank explicitBank;
  private final List<DocumentType> documentTypes;
  private final LocalDate asOf;

  private final NameGenerator nameGen;
  private final PassportGenerator passportGen;
  private final AddressGenerator addressGen;
  private final ContactGenerator contactGen;
  private final InnGenerator innGen;
  private final SnilsGenerator snilsGen;
  private final BikGenerator bikGen;
  private final AccountGenerator accountGen;
  private final DocumentGenerator documentGen;

  private IndividualProfileGenerator(Builder b) {
    this.random = b.random;
    this.bikSource = b.bikSource;
    this.addressStrategy = b.addressStrategy;
    this.fiasMode = b.fiasMode;
    this.gender = b.gender;
    this.age = b.age;
    this.withAccount = b.withAccount;
    this.passportWasExchanged = b.passportWasExchanged;
    this.passportType = b.passportType;
    this.passportCountry = b.passportCountry;
    this.fioScript = b.fioScript;
    this.corruptFields = b.corruptFields;
    this.residency = b.residency;
    this.categoryClient = b.categoryClient;
    this.accountPackage = b.accountPackage;
    this.familyStatus = b.familyStatus;
    this.region = b.region;
    this.secretWord = b.secretWord;
    this.personalManager = b.personalManager;
    this.email = b.email;
    this.emailDomainMode = b.emailDomainMode;
    this.terrorist = b.terrorist;
    this.blocked = b.blocked;
    this.bik = b.bik;
    this.explicitBank = b.explicitBank;
    this.documentTypes = b.documentTypes;
    this.asOf = b.asOf != null ? b.asOf : LocalDate.now();

    this.nameGen = new NameGenerator(random);
    this.passportGen = new PassportGenerator(random);
    this.addressGen = new AddressGenerator(random);
    this.contactGen = new ContactGenerator(random);
    this.innGen = new InnGenerator(random);
    this.snilsGen = new SnilsGenerator(random);
    this.bikGen = new BikGenerator(random, bikSource);
    this.accountGen = new AccountGenerator(random);
    this.documentGen = new DocumentGenerator(random);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static IndividualProfileGenerator defaults() {
    return builder().build();
  }

  /** Связанный профиль с применением каскадных дефолтов. */
  public IndividualProfile generate() {
    Gender g = gender != null ? gender : (random.nextBoolean() ? Gender.MALE : Gender.FEMALE);
    int a = age != null ? age : DEFAULT_AGE;
    LocalDate birthDate = randomBirthDate(a);

    FamilyStatus fs = familyStatus != null ? familyStatus : familyStatusForAge(a);
    String pkg = accountPackage != null ? accountPackage : packageForCategory(categoryClient);

    String actualSecretWord = secretWord != null ? secretWord : SECRET_WORDS.get(random.nextInt(SECRET_WORDS.size()));
    String actualManager = personalManager != null ? personalManager : MANAGERS.get(random.nextInt(MANAGERS.size()));

    PersonName name = nameGen.generate(g, fioScript);

    Address birthPlace =
      region != null
        ? addressGen.generateBirthPlace(region)
        : addressGen.generateBirthPlace();

    boolean exchanged =
      passportWasExchanged != null
        ? passportWasExchanged
        : (a >= PassportGenerator.AGE_FIRST_EXCHANGE);

    Country pCountry =
      passportCountry != null
        ? passportCountry
        : (passportType == PassportType.FOREIGN ? residency.residentCountry() : Country.RU);

    String regionCode = region != null ? region.code() : null;

    Passport passport =
      passportGen.generate(
        birthDate, asOf, exchanged, passportType, pCountry, regionCode);

    String inn = innGen.generateIndividual();
    String snils = snilsGen.generateRaw();

    if (corruptFields.contains(CorruptField.INN)) {
      inn = InnCorruptor.corruptIndividual(inn);
    }
    if (corruptFields.contains(CorruptField.SNILS)) {
      snils = SnilsCorruptor.corrupt(snils);
    }

    Address registration;
    Address post;
    Address current;
    if (addressStrategy == AddressStrategy.SAME_FOR_ALL) {
      Address single = applyFiasMode(registrationFor());
      registration = single;
      post = single;
      current = single;
    } else {
      registration = applyFiasMode(registrationFor());
      post = applyFiasMode(addressGen.generatePost());
      current = applyFiasMode(addressGen.generateCurrent());
    }

    Contact mobile = contactGen.mobile();
    Contact emailContact = email != null
      ? new Contact(ContactType.EMAIL, email, "Описание для email", "Действительный", true)
      : contactGen.email(name, emailDomainMode);

    Bank bank = resolveBank();
    String account =
      withAccount
        ? accountGen.generate(
            AccountGenerator.DEFAULT_BALANCE_ACCOUNT,
            AccountType.CLAC,
            bank != null ? bank.bik() : AccountGenerator.NO_BANK_BIK)
        : null;

    List<IdentityDocument> documents = generateDocuments(g, a, asOf);

    return new IndividualProfile(
      name,
      birthDate,
      birthPlace,
      fs,
      residency,
      passport,
      inn,
      snils,
      registration,
      post,
      current,
      mobile,
      emailContact,
      actualSecretWord,
      categoryClient,
      pkg,
      actualManager,
      terrorist,
      blocked,
      bank,
      account,
      documents,
      asOf);
  }

  /**
   * Дополнительные документы профиля с проверкой совместимости набора.
   *
   * @throws IllegalArgumentException если набор противоречит полу, возрасту или резидентству
   */
  private List<IdentityDocument> generateDocuments(Gender g, int ageYears, LocalDate at) {
    if (documentTypes.isEmpty()) {
      return List.of();
    }
    DocumentRules.validate(documentTypes, g, ageYears, residency);
    List<IdentityDocument> generated = new ArrayList<>(documentTypes.size());
    for (DocumentType type : documentTypes) {
      generated.add(documentGen.generate(type, at));
    }
    return List.copyOf(generated);
  }

  /**
   * Резолвит банк с учётом P3.3: явный {@code .bank(...)} → поиск по БИК → случайный.
   *
   * @throws IllegalArgumentException если задан {@code .bik(...)}, но банк не найден
   */
  private Bank resolveBank() {
    if (explicitBank != null) {
      return explicitBank;
    }
    if (bik != null && !bik.isBlank()) {
      return bikSource
        .findByBik(bik)
        .orElseThrow(
          () ->
            new IllegalArgumentException(
              "БИК не найден в справочнике: " + bik
                + ". Проверьте, что БИК существует и справочник загружен."));
    }
    return bikGen.generateBank();
  }

  private Address registrationFor() {
    return region != null
      ? addressGen.generateRegistration(region)
      : addressGen.generateRegistration();
  }

  private FamilyStatus familyStatusForAge(int ageYears) {
    if (ageYears < ADULT_AGE) {
      return FamilyStatus.SINGLE;
    }
    FamilyStatus[] values = FamilyStatus.values();
    return values[random.nextInt(values.length)];
  }

  private String packageForCategory(String category) {
    if (AbsDefaults.CATEGORY_SALARY.equals(category)) {
      return AbsDefaults.PACKAGE_SALARY;
    }
    if (AbsDefaults.CATEGORY_PENSIONER.equals(category)) {
      return AbsDefaults.PACKAGE_PENSIONER;
    }
    return AbsDefaults.PACKAGE_BASE;
  }

  private Address applyFiasMode(Address source) {
    if (fiasMode == FiasMode.FULL) {
      return source;
    }
    return new Address(
      source.type(),
      source.format(),
      source.status(),
      source.isDefault(),
      source.country(),
      null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
      null, null,
      source.expectedAddress(),
      source.fiasAddress(),
      null,
      null);
  }

  /**
   * Дата рождения — только от seed и {@link #asOf}, без привязки к календарю запуска.
   *
   * <p>Раньше смещение бралось как {@code today.getDayOfYear()}, из-за чего один и тот же seed
   * давал разные профили в разные дни. Теперь смещение всегда в пределах года.
   */
  private LocalDate randomBirthDate(int ageYears) {
    // Возраст на дату расчёта ровно ageYears: дата рождения лежит в интервале
    // (asOf-(age+1) лет, asOf-age лет), поэтому ageAt(asOf) == ageYears для любого смещения.
    int dayOffset = 1 + random.nextInt(364);
    return asOf.minusYears(ageYears + 1L).plusDays(dayOffset);
  }

  private static List<String> loadLines(String resource) {
    try (InputStream in = IndividualProfileGenerator.class.getResourceAsStream(resource)) {
      if (in == null) {
        throw new IllegalStateException("Resource not found: " + resource);
      }
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
        return reader.lines()
          .map(String::trim)
          .filter(s -> !s.isEmpty())
          .toList();
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to load " + resource, e);
    }
  }

  /** Builder с дефолтами и каскадной логикой. */
  public static final class Builder {

    private Random random = DataGenerator.defaultRandom();
    private BikSource bikSource = BikSources.defaultSource();
    private AddressStrategy addressStrategy = AddressStrategy.SAME_FOR_ALL;
    private FiasMode fiasMode = FiasMode.FULL;
    private Gender gender;
    private Integer age;
    private boolean withAccount = true;
    private Boolean passportWasExchanged;
    private PassportType passportType = PassportType.RF;
    private Country passportCountry;
    private FioScript fioScript = FioScript.CYRILLIC;
    private Set<CorruptField> corruptFields = Set.of();
    private Residency residency = Residency.residentRU();
    private String categoryClient = AbsDefaults.CATEGORY_CLIENT;
    private String accountPackage;
    private FamilyStatus familyStatus;
    private Region region;
    private String secretWord;
    private String personalManager;
    private String email;
    private EmailDomainMode emailDomainMode = EmailDomainMode.TEST;
    private boolean terrorist = false;
    private boolean blocked = false;
    private String bik;
    private Bank explicitBank;
    private List<DocumentType> documentTypes = List.of();
    private LocalDate asOf;

    private Builder() {}

    public Builder seed(long seed) {
      this.random = DataGenerator.seededRandom(seed);
      return this;
    }

    public Builder random(Random random) {
      this.random = random;
      return this;
    }

    /**
     * Дата расчёта профиля: возраст, даты выдачи паспорта и документов считаются от неё.
     *
     * <p>Нужна для воспроизводимости: при одном и том же seed и {@code asOf} профиль одинаков в
     * любой день. Не задана — используется сегодняшняя дата.
     */
    public Builder asOf(LocalDate date) {
      this.asOf = date;
      return this;
    }

    public Builder bikSource(BikSource source) {
      this.bikSource = source;
      return this;
    }

    /**
     * Явный БИК (9 цифр). Банк ищется в текущем {@link BikSource}.
     *
     * @throws IllegalArgumentException при генерации, если БИК не найден
     */
    public Builder bik(String bik) {
      this.bik = bik;
      return this;
    }

    /**
     * Готовый объект {@link Bank} (например, из {@link BikSource#findByBik(String)}).
     *
     * <p>Приоритет выше, чем у {@link #bik(String)} — если заданы оба, используется этот.
     */
    public Builder bank(Bank bank) {
      this.explicitBank = bank;
      return this;
    }

    public Builder addressStrategy(AddressStrategy strategy) {
      this.addressStrategy = strategy;
      return this;
    }

    public Builder fiasMode(FiasMode mode) {
      this.fiasMode = mode;
      return this;
    }

    public Builder gender(Gender gender) {
      this.gender = gender;
      return this;
    }

    public Builder age(int age) {
      this.age = age;
      return this;
    }

    public Builder withAccount(boolean withAccount) {
      this.withAccount = withAccount;
      return this;
    }

    public Builder passportWasExchanged(boolean exchanged) {
      this.passportWasExchanged = exchanged;
      return this;
    }

    public Builder passportType(PassportType type) {
      this.passportType = type;
      return this;
    }

    public Builder passportCountry(Country country) {
      this.passportCountry = country;
      return this;
    }

    public Builder fioScript(FioScript script) {
      this.fioScript = script;
      return this;
    }

    public Builder corrupt(CorruptField... fields) {
      this.corruptFields =
        (fields == null || fields.length == 0) ? Set.of() : Set.copyOf(Arrays.asList(fields));
      return this;
    }

    public Builder residency(Residency residency) {
      this.residency = residency;
      return this;
    }

    public Builder categoryClient(String category) {
      this.categoryClient = category;
      return this;
    }

    public Builder accountPackage(String pack) {
      this.accountPackage = pack;
      return this;
    }

    public Builder familyStatus(FamilyStatus status) {
      this.familyStatus = status;
      return this;
    }

    public Builder region(Region region) {
      this.region = region;
      return this;
    }

    public Builder secretWord(String word) {
      this.secretWord = word;
      return this;
    }

    public Builder personalManager(String manager) {
      this.personalManager = manager;
      return this;
    }

    /** Явный email. Переопределяет генерацию по ФИО и режиму домена. */
    public Builder email(String email) {
      this.email = email;
      return this;
    }

    /** Режим домена для сгенерированного email (TEST / REALISTIC / MIXED). */
    public Builder emailDomainMode(EmailDomainMode mode) {
      this.emailDomainMode = mode;
      return this;
    }

    /** Флаг «террорист / экстремист» — для тестов, на которых АБС должна реагировать особым образом. */
    public Builder terrorist(boolean terrorist) {
      this.terrorist = terrorist;
      return this;
    }

    /** Флаг «заблокирован банком» — для тестов операций, которые должны быть запрещены. */
    public Builder blocked(boolean blocked) {
      this.blocked = blocked;
      return this;
    }

    /**
     * Дополнительные документы, удостоверяющие личность (СПДУЛ ФНС).
     *
     * <p>Набор проверяется по правилам {@link DocumentRules}: загранпаспорт требует паспорт РФ,
     * ВНЖ/РВП/беженец — только нерезиденту, военный билет — мужчине призывного возраста.
     *
     * @throws IllegalArgumentException при генерации, если набор противоречив
     */
    public Builder documents(DocumentType... types) {
      this.documentTypes = (types == null || types.length == 0) ? List.of() : List.of(types);
      return this;
    }

    /** Дополнительные документы из готового списка. */
    public Builder documents(List<DocumentType> types) {
      this.documentTypes = (types == null) ? List.of() : List.copyOf(types);
      return this;
    }

    public IndividualProfileGenerator build() {
      return new IndividualProfileGenerator(this);
    }
  }
}
