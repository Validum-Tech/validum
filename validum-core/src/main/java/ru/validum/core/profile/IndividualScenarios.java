package ru.validum.core.profile;

import ru.validum.core.abs.AbsDefaults;
import ru.validum.core.common.Country;
import ru.validum.core.document.DocumentType;
import ru.validum.core.name.Gender;
import ru.validum.core.passport.PassportType;

/**
 * Фасады типовых сценариев генерации профиля физлица.
 *
 * <p>Фасады фиксируют только то, что относится к сценарию: возраст, пол, категория, пакет,
 * наличие счёта, резидентство. Вся остальная логика — через {@link
 * IndividualProfileGenerator#builder()} и каскадные дефолты.
 */
public final class IndividualScenarios {

  // ── Возраста ─────────────────────────────────────────────────────────────

  public static final int ADULT_AGE = 19;
  public static final int SALARY_AGE = 35;
  public static final int PENSION_AGE_MALE = 65;
  public static final int PENSION_AGE_FEMALE = 60;

  // ── Алиасы на AbsDefaults ────────────────────────────────────────────────

  public static final String CATEGORY_CLIENT = AbsDefaults.CATEGORY_CLIENT;
  public static final String CATEGORY_PENSIONER = AbsDefaults.CATEGORY_PENSIONER;
  public static final String CATEGORY_SALARY = AbsDefaults.CATEGORY_SALARY;
  public static final String PACKAGE_SALARY = AbsDefaults.PACKAGE_SALARY;

  private IndividualScenarios() {}

  // ── Итерация 3: взрослый резидент ────────────────────────────────────────

  public static IndividualProfile createAdultResidentWithAccount() {
    return createAdultResidentWithAccount(IndividualProfileGenerator.builder());
  }

  public static IndividualProfile createAdultResidentWithAccount(long seed) {
    return createAdultResidentWithAccount(
      IndividualProfileGenerator.builder().seed(seed));
  }

  private static IndividualProfile createAdultResidentWithAccount(
    IndividualProfileGenerator.Builder b) {
    return b.age(ADULT_AGE).withAccount(true).build().generate();
  }

  public static IndividualProfile createAdultResidentWithoutAccount() {
    return createAdultResidentWithoutAccount(IndividualProfileGenerator.builder());
  }

  public static IndividualProfile createAdultResidentWithoutAccount(long seed) {
    return createAdultResidentWithoutAccount(
      IndividualProfileGenerator.builder().seed(seed));
  }

  private static IndividualProfile createAdultResidentWithoutAccount(
    IndividualProfileGenerator.Builder b) {
    return b.age(ADULT_AGE).withAccount(false).build().generate();
  }

  // ── Итерация 4: пенсионеры ───────────────────────────────────────────────

  public static IndividualProfile createPensionerMale() {
    return createPensionerMale(IndividualProfileGenerator.builder());
  }

  public static IndividualProfile createPensionerMale(long seed) {
    return createPensionerMale(IndividualProfileGenerator.builder().seed(seed));
  }

  private static IndividualProfile createPensionerMale(
    IndividualProfileGenerator.Builder b) {
    return b.gender(Gender.MALE)
      .age(PENSION_AGE_MALE)
      .withAccount(true)
      .categoryClient(CATEGORY_PENSIONER)
      .build()
      .generate();
  }

  public static IndividualProfile createPensionerFemale() {
    return createPensionerFemale(IndividualProfileGenerator.builder());
  }

  public static IndividualProfile createPensionerFemale(long seed) {
    return createPensionerFemale(IndividualProfileGenerator.builder().seed(seed));
  }

  private static IndividualProfile createPensionerFemale(
    IndividualProfileGenerator.Builder b) {
    return b.gender(Gender.FEMALE)
      .age(PENSION_AGE_FEMALE)
      .withAccount(true)
      .categoryClient(CATEGORY_PENSIONER)
      .build()
      .generate();
  }

  public static IndividualProfile createPensioner(Gender gender, long seed) {
    return switch (gender) {
      case MALE -> createPensionerMale(seed);
      case FEMALE -> createPensionerFemale(seed);
    };
  }

  // ── Итерация 4: зарплатник ───────────────────────────────────────────────

  public static IndividualProfile createSalaryClient() {
    return createSalaryClient(IndividualProfileGenerator.builder());
  }

  public static IndividualProfile createSalaryClient(long seed) {
    return createSalaryClient(IndividualProfileGenerator.builder().seed(seed));
  }

  private static IndividualProfile createSalaryClient(
    IndividualProfileGenerator.Builder b) {
    return b.age(SALARY_AGE)
      .withAccount(true)
      .categoryClient(CATEGORY_SALARY)
      .accountPackage(PACKAGE_SALARY)
      .build()
      .generate();
  }

  // ── Итерация 5: нерезидент ───────────────────────────────────────────────

  /**
   * Нерезидент указанной страны с иностранным паспортом.
   *
   * <p>Каскад связывает три поля: {@code residency.nonResident(country)}, {@code
   * passportType=FOREIGN}, {@code passportCountry=country}. Дополнительно страна попадает в {@code
   * citizenship}, {@code residentCountry}, {@code taxResidentCountry}.
   */
  public static IndividualProfile createNonResident(Country country) {
    return createNonResident(country, IndividualProfileGenerator.builder());
  }

  /** Воспроизводимый вариант. */
  public static IndividualProfile createNonResident(Country country, long seed) {
    return createNonResident(country, IndividualProfileGenerator.builder().seed(seed));
  }

  private static IndividualProfile createNonResident(
    Country country, IndividualProfileGenerator.Builder b) {
    return b.residency(Residency.nonResident(country))
      .passportType(PassportType.FOREIGN)
      .passportCountry(country)
      .build()
      .generate();
  }

  // ── КУС: константы сценариев ────────────────────────────────────────────

  /** Возраст несовершеннолетнего клиента (14–17). */
  public static final int MINOR_AGE = 16;

  /** Возраст военнослужащего по призыву (18–27). */
  public static final int SERVICEMAN_AGE = 20;

  /** Возраст нерезидента с ВНЖ. */
  public static final int NON_RESIDENT_AGE = 32;

  /** Возраст иностранца с визой. */
  public static final int VISA_AGE = 28;

  /** Страна по умолчанию для нерезидентских сценариев (трудовые мигранты). */
  public static final Country DEFAULT_NON_RESIDENT_COUNTRY = Country.TJ;

  /** Страна по умолчанию для сценария «иностранец с визой». */
  public static final Country DEFAULT_VISA_COUNTRY = Country.US;

  // ── КУС: нерезидент с ВНЖ ───────────────────────────────────────────────

  /**
   * Нерезидент с видом на жительство: иностранный паспорт (10) + ВНЖ (12).
   *
   * <p>Самый частый нерезидентский кейс идентификации: человек живёт в РФ по ВНЖ.
   */
  public static IndividualProfile createNonResidentWithResidencePermit() {
    return createNonResidentWithResidencePermit(DEFAULT_NON_RESIDENT_COUNTRY);
  }

  public static IndividualProfile createNonResidentWithResidencePermit(long seed) {
    return createNonResidentWithResidencePermit(DEFAULT_NON_RESIDENT_COUNTRY, seed);
  }

  public static IndividualProfile createNonResidentWithResidencePermit(Country country) {
    return createNonResidentWithResidencePermit(country, IndividualProfileGenerator.builder());
  }

  public static IndividualProfile createNonResidentWithResidencePermit(
    Country country, long seed) {
    return createNonResidentWithResidencePermit(
      country, IndividualProfileGenerator.builder().seed(seed));
  }

  private static IndividualProfile createNonResidentWithResidencePermit(
    Country country, IndividualProfileGenerator.Builder b) {
    return b.residency(Residency.nonResident(country))
      .passportType(PassportType.FOREIGN)
      .passportCountry(country)
      .age(NON_RESIDENT_AGE)
      .withAccount(true)
      .documents(DocumentType.PASSPORT_FOREIGN, DocumentType.RESIDENCE_PERMIT)
      .build()
      .generate();
  }

  // ── КУС: иностранец с визой ─────────────────────────────────────────────

  /** Иностранец, въехавший по визе: иностранный паспорт (10) + виза. */
  public static IndividualProfile createForeignerWithVisa() {
    return createForeignerWithVisa(DEFAULT_VISA_COUNTRY);
  }

  public static IndividualProfile createForeignerWithVisa(long seed) {
    return createForeignerWithVisa(DEFAULT_VISA_COUNTRY, seed);
  }

  public static IndividualProfile createForeignerWithVisa(Country country) {
    return createForeignerWithVisa(country, IndividualProfileGenerator.builder());
  }

  public static IndividualProfile createForeignerWithVisa(Country country, long seed) {
    return createForeignerWithVisa(country, IndividualProfileGenerator.builder().seed(seed));
  }

  private static IndividualProfile createForeignerWithVisa(
    Country country, IndividualProfileGenerator.Builder b) {
    return b.residency(Residency.nonResident(country))
      .passportType(PassportType.FOREIGN)
      .passportCountry(country)
      .age(VISA_AGE)
      .withAccount(true)
      .documents(DocumentType.PASSPORT_FOREIGN, DocumentType.VISA)
      .build()
      .generate();
  }

  // ── КУС: военнослужащий ─────────────────────────────────────────────────

  /** Военнослужащий по призыву: мужчина 20 лет, паспорт РФ (21) + военный билет (07). */
  public static IndividualProfile createServiceman() {
    return createServiceman(IndividualProfileGenerator.builder());
  }

  public static IndividualProfile createServiceman(long seed) {
    return createServiceman(IndividualProfileGenerator.builder().seed(seed));
  }

  private static IndividualProfile createServiceman(IndividualProfileGenerator.Builder b) {
    return b.gender(Gender.MALE)
      .age(SERVICEMAN_AGE)
      .withAccount(true)
      .documents(DocumentType.PASSPORT_RF, DocumentType.MILITARY_ID)
      .build()
      .generate();
  }

  // ── КУС: несовершеннолетний ─────────────────────────────────────────────

  /** Несовершеннолетний клиент (16 лет) с паспортом РФ и счётом. */
  public static IndividualProfile createMinor() {
    return createMinor(IndividualProfileGenerator.builder());
  }

  public static IndividualProfile createMinor(long seed) {
    return createMinor(IndividualProfileGenerator.builder().seed(seed));
  }

  private static IndividualProfile createMinor(IndividualProfileGenerator.Builder b) {
    return b.age(MINOR_AGE)
      .withAccount(true)
      .documents(DocumentType.PASSPORT_RF)
      .build()
      .generate();
  }
}
