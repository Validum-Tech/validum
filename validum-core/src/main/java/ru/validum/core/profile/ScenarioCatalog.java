package ru.validum.core.profile;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.LongFunction;
import java.util.function.Supplier;

/**
 * Единый каталог именованных сценариев генерации профиля физлица.
 *
 * <p>Slug'и — часть публичного контракта: они отдаются в {@code GET /api/v1/scenarios},
 * принимаются в {@code POST /api/v1/scenarios/{name}} и печатаются командой CLI {@code scenarios}.
 * Порядок объявления фиксирован и не меняется без мажорной версии — API и CLI читают его отсюда,
 * поэтому расхождение между ними невозможно.
 */
public enum ScenarioCatalog {

  ADULT_RESIDENT_WITH_ACCOUNT(
    "adult-resident-with-account",
    IndividualScenarios::createAdultResidentWithAccount,
    IndividualScenarios::createAdultResidentWithAccount),

  ADULT_RESIDENT_WITHOUT_ACCOUNT(
    "adult-resident-without-account",
    IndividualScenarios::createAdultResidentWithoutAccount,
    IndividualScenarios::createAdultResidentWithoutAccount),

  PENSIONER_MALE(
    "pensioner-male",
    IndividualScenarios::createPensionerMale,
    IndividualScenarios::createPensionerMale),

  PENSIONER_FEMALE(
    "pensioner-female",
    IndividualScenarios::createPensionerFemale,
    IndividualScenarios::createPensionerFemale),

  SALARY_CLIENT(
    "salary-client",
    IndividualScenarios::createSalaryClient,
    IndividualScenarios::createSalaryClient),

  NON_RESIDENT_WITH_RESIDENCE_PERMIT(
    "non-resident-with-residence-permit",
    IndividualScenarios::createNonResidentWithResidencePermit,
    IndividualScenarios::createNonResidentWithResidencePermit),

  FOREIGNER_WITH_VISA(
    "foreigner-with-visa",
    IndividualScenarios::createForeignerWithVisa,
    IndividualScenarios::createForeignerWithVisa),

  SERVICEMAN("serviceman", IndividualScenarios::createServiceman, IndividualScenarios::createServiceman),

  MINOR("minor", IndividualScenarios::createMinor, IndividualScenarios::createMinor);

  private final String slug;
  private final Supplier<IndividualProfile> noSeed;
  private final LongFunction<IndividualProfile> withSeed;

  ScenarioCatalog(
    String slug, Supplier<IndividualProfile> noSeed, LongFunction<IndividualProfile> withSeed) {
    this.slug = slug;
    this.noSeed = noSeed;
    this.withSeed = withSeed;
  }

  /** Slug сценария (kebab-case, латиница). */
  public String slug() {
    return slug;
  }

  /** Сгенерировать профиль со случайным seed. */
  public IndividualProfile generate() {
    return noSeed.get();
  }

  /** Сгенерировать воспроизводимый профиль. */
  public IndividualProfile generate(long seed) {
    return withSeed.apply(seed);
  }

  /** Найти сценарий по slug (без учёта регистра не ищем — slug'и публичные и точные). */
  public static Optional<ScenarioCatalog> bySlug(String slug) {
    if (slug == null) {
      return Optional.empty();
    }
    return Arrays.stream(values()).filter(s -> s.slug.equals(slug)).findFirst();
  }

  /** Все slug'и в порядке объявления. */
  public static List<String> allSlugs() {
    return Arrays.stream(values()).map(ScenarioCatalog::slug).toList();
  }
}
