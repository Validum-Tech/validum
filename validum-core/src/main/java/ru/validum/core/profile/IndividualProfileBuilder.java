package ru.validum.core.profile;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import ru.validum.core.address.AddressStrategy;
import ru.validum.core.address.FiasMode;
import ru.validum.core.bank.Bank;
import ru.validum.core.bank.BikSource;
import ru.validum.core.bank.BikSources;
import ru.validum.core.abs.AbsDefaults;
import ru.validum.core.common.Country;
import ru.validum.core.contact.EmailDomainMode;
import ru.validum.core.name.FamilyStatus;
import ru.validum.core.name.FioScript;
import ru.validum.core.name.Gender;
import ru.validum.core.common.Region;
import ru.validum.core.random.DataGenerator;
import ru.validum.core.document.DocumentType;
import ru.validum.core.document.DocumentRules;
import ru.validum.core.passport.PassportType;

/** Строитель профиля физлица: вынесен из {@link IndividualProfileGenerator}. */
public final class IndividualProfileBuilder {

  Random random = DataGenerator.defaultRandom();
  Long seedValue;
  BikSource bikSource = BikSources.defaultSource();
  AddressStrategy addressStrategy = AddressStrategy.SAME_FOR_ALL;
  FiasMode fiasMode = FiasMode.FULL;
  Gender gender;
  Integer age;
  Integer ageMin;
  Integer ageMax;
  AgeDistribution ageDistribution = AgeDistribution.UNIFORM;
  boolean withAccount = true;
  Boolean passportWasExchanged;
  PassportType passportType = PassportType.RF;
  Country passportCountry;
  FioScript fioScript = FioScript.CYRILLIC;
  Set<CorruptField> corruptFields = Set.of();
  Residency residency = Residency.residentRU();
  String categoryClient = AbsDefaults.CATEGORY_CLIENT;
  String accountPackage;
  FamilyStatus familyStatus;
  Region region;
  String secretWord;
  String personalManager;
  String email;
  EmailDomainMode emailDomainMode = EmailDomainMode.TEST;
  boolean terrorist = false;
  boolean blocked = false;
  String bik;
  Bank explicitBank;
  List<DocumentType> documentTypes = List.of();
  boolean autoDocuments;
  LocalDate asOf;

  IndividualProfileBuilder() {}

  public IndividualProfileBuilder seed(long seed) {
    this.random = DataGenerator.seededRandom(seed);
    this.seedValue = seed;
    return this;
  }

  public IndividualProfileBuilder random(Random random) {
    this.random = random;
    return this;
  }

  /**
   * Дата расчёта профиля: возраст, даты выдачи паспорта и документов считаются от неё.
   *
   * <p>Нужна для воспроизводимости: при одном и том же seed и {@code asOf} профиль одинаков в
   * любой день. Не задана — используется сегодняшняя дата.
   */
  public IndividualProfileBuilder asOf(LocalDate date) {
    this.asOf = date;
    return this;
  }

  public IndividualProfileBuilder bikSource(BikSource source) {
    this.bikSource = source;
    return this;
  }

  /**
   * Явный БИК (9 цифр). Банк ищется в текущем {@link BikSource}.
   *
   * @throws IllegalArgumentException при генерации, если БИК не найден
   */
  public IndividualProfileBuilder bik(String bik) {
    this.bik = bik;
    return this;
  }

  /**
   * Готовый объект {@link Bank} (например, из {@link BikSource#findByBik(String)}).
   *
   * <p>Приоритет выше, чем у {@link #bik(String)} — если заданы оба, используется этот.
   */
  public IndividualProfileBuilder bank(Bank bank) {
    this.explicitBank = bank;
    return this;
  }

  public IndividualProfileBuilder addressStrategy(AddressStrategy strategy) {
    this.addressStrategy = strategy;
    return this;
  }

  public IndividualProfileBuilder fiasMode(FiasMode mode) {
    this.fiasMode = mode;
    return this;
  }

  public IndividualProfileBuilder gender(Gender gender) {
    this.gender = gender;
    return this;
  }

  public IndividualProfileBuilder age(int age) {
    this.age = age;
    return this;
  }

  /**
   * Диапазон возраста {@code MIN-MAX}; при {@code --unique} каждый профиль берёт своё значение.
   * С {@link #age(int)} явный возраст важнее диапазона.
   */
  public IndividualProfileBuilder ageRange(int min, int max) {
    if (min < 0 || max > 110 || min > max) {
      throw new IllegalArgumentException(
        "Возрастной диапазон должен быть 0-110 и не перевёрнут, получено: " + min + "-" + max);
    }
    this.ageMin = min;
    this.ageMax = max;
    return this;
  }

  /** Распределение внутри {@link #ageRange(int, int)}; по умолчанию {@link AgeDistribution#UNIFORM}. */
  public IndividualProfileBuilder ageDistribution(AgeDistribution distribution) {
    this.ageDistribution = distribution == null ? AgeDistribution.UNIFORM : distribution;
    return this;
  }

  public IndividualProfileBuilder withAccount(boolean withAccount) {
    this.withAccount = withAccount;
    return this;
  }

  public IndividualProfileBuilder passportWasExchanged(boolean exchanged) {
    this.passportWasExchanged = exchanged;
    return this;
  }

  public IndividualProfileBuilder passportType(PassportType type) {
    this.passportType = type;
    return this;
  }

  public IndividualProfileBuilder passportCountry(Country country) {
    this.passportCountry = country;
    return this;
  }

  public IndividualProfileBuilder fioScript(FioScript script) {
    this.fioScript = script;
    return this;
  }

  public IndividualProfileBuilder corrupt(CorruptField... fields) {
    this.corruptFields =
      (fields == null || fields.length == 0) ? Set.of() : Set.copyOf(Arrays.asList(fields));
    return this;
  }

  public IndividualProfileBuilder residency(Residency residency) {
    this.residency = residency;
    return this;
  }

  public IndividualProfileBuilder categoryClient(String category) {
    this.categoryClient = category;
    return this;
  }

  public IndividualProfileBuilder accountPackage(String pack) {
    this.accountPackage = pack;
    return this;
  }

  public IndividualProfileBuilder familyStatus(FamilyStatus status) {
    this.familyStatus = status;
    return this;
  }

  public IndividualProfileBuilder region(Region region) {
    this.region = region;
    return this;
  }

  public IndividualProfileBuilder secretWord(String word) {
    this.secretWord = word;
    return this;
  }

  public IndividualProfileBuilder personalManager(String manager) {
    this.personalManager = manager;
    return this;
  }

  /** Явный email. Переопределяет генерацию по ФИО и режиму домена. */
  public IndividualProfileBuilder email(String email) {
    this.email = email;
    return this;
  }

  /** Режим домена для сгенерированного email (TEST / REALISTIC / MIXED). */
  public IndividualProfileBuilder emailDomainMode(EmailDomainMode mode) {
    this.emailDomainMode = mode;
    return this;
  }

  /** Флаг «террорист / экстремист» — для тестов, на которых АБС должна реагировать особым образом. */
  public IndividualProfileBuilder terrorist(boolean terrorist) {
    this.terrorist = terrorist;
    return this;
  }

  /** Флаг «заблокирован банком» — для тестов операций, которые должны быть запрещены. */
  public IndividualProfileBuilder blocked(boolean blocked) {
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
  public IndividualProfileBuilder documents(DocumentType... types) {
    this.documentTypes = (types == null || types.length == 0) ? List.of() : List.of(types);
    return this;
  }

  /** Дополнительные документы из готового списка. */
  /** Набор ДУЛ подобрать автоматически по возрасту, полу и резидентству ({@code --documents auto}). */
  public IndividualProfileBuilder documentsAuto() {
    this.autoDocuments = true;
    return this;
  }

  public IndividualProfileBuilder documents(List<DocumentType> types) {
    this.documentTypes = (types == null) ? List.of() : List.copyOf(types);
    return this;
  }

  public IndividualProfileGenerator build() {
    return new IndividualProfileGenerator(this);
  }
}

