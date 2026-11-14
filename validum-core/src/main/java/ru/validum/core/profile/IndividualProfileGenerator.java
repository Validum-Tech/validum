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
import ru.validum.core.address.AddressCorruptor;
import ru.validum.core.address.AddressGenerator;
import ru.validum.core.address.AddressStrategy;
import ru.validum.core.address.AddressType;
import ru.validum.core.address.FiasMode;
import ru.validum.core.bank.Bank;
import ru.validum.core.bank.BikGenerator;
import ru.validum.core.bank.BikSource;
import ru.validum.core.bank.BikSources;
import ru.validum.core.common.Country;
import ru.validum.core.common.PersonIds;
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
  private static final int MAX_AGE = 110;
  private static final int ADULT_AGE = 18;

  private static final List<String> SECRET_WORDS = loadLines("/secret_words.txt");
  private static final List<String> MANAGERS = loadLines("/managers.txt");

  private final Random random;
  private final BikSource bikSource;
  private final AddressStrategy addressStrategy;
  private final FiasMode fiasMode;
  private final Gender gender;
  private final Integer age;
  private final Integer ageMin;
  private final Integer ageMax;
  private final AgeDistribution ageDistribution;
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

  /**
   * Регион прописки на текущий вызов {@code generate()}: либо явный {@code --region}, либо
   * выбранный нами один раз. Паспорт выдаётся там же — иначе серия говорила бы об одном регионе,
   * а «кем выдан» и адрес о другом.
   */
  private Region homeRegion;
  private final String secretWord;
  private final String personalManager;
  private final String email;
  private final EmailDomainMode emailDomainMode;
  private final boolean terrorist;
  private final boolean blocked;
  private final String bik;
  private final Bank explicitBank;
  private final List<DocumentType> documentTypes;
  private final boolean autoDocuments;
  private final LocalDate asOf;
  private final Long seedValue;

  private final NameGenerator nameGen;
  private final PassportGenerator passportGen;
  private final AddressGenerator addressGen;
  private final ContactGenerator contactGen;
  private final InnGenerator innGen;
  private final SnilsGenerator snilsGen;
  private final BikGenerator bikGen;
  private final AccountGenerator accountGen;
  private final DocumentGenerator documentGen;

  /**
   * Виден пакету: билдер в том же пакете создаёт генератор через {@code build()}.
   * Наружу генератор собирается только через {@link #builder()}.
   */
  IndividualProfileGenerator(IndividualProfileBuilder b) {
    this.random = b.random;
    this.bikSource = b.bikSource;
    this.addressStrategy = b.addressStrategy;
    this.fiasMode = b.fiasMode;
    this.gender = b.gender;
    this.age = b.age;
    this.ageMin = b.ageMin;
    this.ageMax = b.ageMax;
    this.ageDistribution = b.ageDistribution;
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
    this.autoDocuments = b.autoDocuments;
    this.asOf = b.asOf != null ? b.asOf : LocalDate.now();
    this.seedValue = b.seedValue;

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

  public static IndividualProfileBuilder builder() {
    return new IndividualProfileBuilder();
  }

  public static IndividualProfileGenerator defaults() {
    return builder().build();
  }

  /** Связанный профиль с применением каскадных дефолтов. */
  public IndividualProfile generate() {
    Gender g = gender != null ? gender : (random.nextBoolean() ? Gender.MALE : Gender.FEMALE);
    int a = pickAge();
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

    // Нерезидент не может получить паспорт РФ: если тип не задан явно, берём иностранный.
    PassportType effectivePassportType =
      residency.isResident()
        ? (passportType != null ? passportType : PassportType.RF)
        : PassportType.FOREIGN;

    Country pCountry =
      passportCountry != null
        ? passportCountry
        : (effectivePassportType == PassportType.FOREIGN
            ? residency.residentCountry()
            : Country.RU);

    // Один регион на человека: паспорт РФ выдан там же, где прописка.
    homeRegion = region;
    if (homeRegion == null && effectivePassportType == PassportType.RF && !livesAbroad()) {
      homeRegion = Region.values()[random.nextInt(Region.values().length)];
    }
    String regionCode = homeRegion != null ? homeRegion.code() : null;

    Passport passport =
      passportGen.generate(
        birthDate, asOf, exchanged, effectivePassportType, pCountry, regionCode);

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
      post = applyFiasMode(addressFor(AddressType.POST));
      current = applyFiasMode(addressFor(AddressType.CURRENT));
    }

    if (corruptFields.contains(CorruptField.ADDRESS)) {
      registration = AddressCorruptor.corrupt(registration);
      post = AddressCorruptor.corrupt(post);
      current = AddressCorruptor.corrupt(current);
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

    List<IdentityDocument> documents = generateDocuments(g, birthDate, a, asOf, regionCode);

    // personId детерминирован seed: тот же seed → тот же идентификатор в CLI и REST.
    String personId = seedValue == null ? null : PersonIds.of(seedValue);

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
      asOf,
      personId);
  }

  /** Возраст профиля: явное значение, иначе диапазон с распределением, иначе каскадный дефолт. */
  private int pickAge() {
    if (age != null) {
      return age;
    }
    if (ageMin == null && ageDistribution != AgeDistribution.REALISTIC) {
      return DEFAULT_AGE;
    }
    int low = ageMin == null ? 0 : ageMin;
    int high = ageMax == null ? MAX_AGE : ageMax;
    return ageDistribution == AgeDistribution.REALISTIC ? realisticAge(low, high) : uniformAge(low, high);
  }

  private int uniformAge(int low, int high) {
    return low + random.nextInt(high - low + 1);
  }

  /** Когорты «реалистичного» распределения: {от, до, вес в процентах}. */
  private static final int[][] AGE_COHORTS = {
    {0, 6, 6}, {7, 13, 7}, {14, 17, 5}, {18, 24, 10}, {25, 34, 18},
    {35, 44, 17}, {45, 54, 14}, {55, 64, 13}, {65, 79, 8}, {80, 110, 2},
  };

  private int realisticAge(int floor, int ceiling) {
    int roll = random.nextInt(100);
    int accumulated = 0;
    for (int[] cohort : AGE_COHORTS) {
      accumulated += cohort[2];
      if (roll < accumulated) {
        int low = Math.max(cohort[0], floor);
        int high = Math.min(cohort[1], ceiling);
        if (low <= high) {
          return low + random.nextInt(high - low + 1);
        }
        break;
      }
    }
    return uniformAge(floor, ceiling);
  }

  /**
   * Дополнительные документы профиля с проверкой совместимости набора.
   *
   * @throws IllegalArgumentException если набор противоречит полу, возрасту или резидентству
   */
  private List<IdentityDocument> generateDocuments(
    Gender g, LocalDate birthDate, int ageYears, LocalDate at, String regionCode) {
    List<DocumentType> types = autoDocuments
      ? DocumentRules.autoSelect(ageYears, g, residency)
      : documentTypes;
    if (types.isEmpty()) {
      return List.of();
    }
    DocumentRules.validate(types, g, ageYears, residency);
    List<IdentityDocument> generated = new ArrayList<>(documentTypes.size());
    for (DocumentType type : types) {
      generated.add(documentGen.generate(type, birthDate, at, regionCode));
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
    if (livesAbroad()) {
      return addressGen.generateRegistrationOutsideRU(residency.residentCountry());
    }
    return homeRegion != null
      ? addressGen.generateRegistration(homeRegion)
      : addressGen.generateRegistration();
  }

  /**
   * Живёт ли клиент за границей: только нерезидент и без явно заданного региона РФ.
   *
   * <p>{@code --region} важнее гражданства: нерезидент из Таджикистана может жить в Москве, и
   * тогда адрес берётся из КЛАДР, а паспорт остаётся иностранным.
   */
  private boolean livesAbroad() {
    return !residency.isResident() && region == null;
  }

  /**
   * Почтовый и фактический адреса: у нерезидента они в стране проживания, у резидента — по КЛАДР.
   */
  private Address addressFor(AddressType type) {
    if (livesAbroad()) {
      return addressGen.generateOutsideRU(residency.residentCountry(), type);
    }
    return type == AddressType.POST
      ? (homeRegion != null ? addressGen.generatePost(homeRegion) : addressGen.generatePost())
      : (homeRegion != null ? addressGen.generateCurrent(homeRegion) : addressGen.generateCurrent());
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
}
