package ru.validum.core.profile;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Period;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import ru.validum.core.abs.AbsDefaults;
import ru.validum.core.common.Region;
import ru.validum.core.name.FamilyStatus;

@DisplayName("Каскадные дефолты — регион / категория / семейное положение")
class IndividualProfileExtendedCascadeTest {

  private static final long SEED = 42L;

  private static int ageOf(IndividualProfile p) {
    return Period.between(p.birthDate(), java.time.LocalDate.now()).getYears();
  }

  @Nested
  @DisplayName("Каскад #1 — возраст → семейное положение")
  class FamilyStatusCascade {

    @Test
    @DisplayName("age=16 → familyStatus=SINGLE (принудительно)")
    void minorIsSingle() {
      var p = IndividualProfileGenerator.builder().age(16).seed(SEED).build().generate();
      assertThat(ageOf(p)).isEqualTo(16);
      assertThat(p.familyStatus()).isEqualTo(FamilyStatus.SINGLE);
    }

    @Test
    @DisplayName("age=14 → тоже SINGLE")
    void age14IsSingle() {
      var p = IndividualProfileGenerator.builder().age(14).seed(SEED).build().generate();
      assertThat(p.familyStatus()).isEqualTo(FamilyStatus.SINGLE);
    }

    @Test
    @DisplayName("age=16 + явный familyStatus=MARRIED → MARRIED (мягкий каскад)")
    void minorCanBeMarried() {
      var p =
        IndividualProfileGenerator.builder()
          .age(16)
          .familyStatus(FamilyStatus.MARRIED)
          .seed(SEED)
          .build()
          .generate();
      assertThat(p.familyStatus()).isEqualTo(FamilyStatus.MARRIED);
    }

    @Test
    @DisplayName("age=25 без familyStatus → из enum (детерминизм по seed)")
    void adultRandomFromEnum() {
      var a = IndividualProfileGenerator.builder().age(25).seed(SEED).build().generate();
      var b = IndividualProfileGenerator.builder().age(25).seed(SEED).build().generate();
      assertThat(a.familyStatus()).isEqualTo(b.familyStatus());
    }
  }

  @Nested
  @DisplayName("Каскад #2 — категория → пакет")
  class PackageCascade {

    @Test
    @DisplayName("categoryClient=КЛИЕНТ без пакета → БАЗОВЫЙ")
    void clientGetsBase() {
      var p = IndividualProfileGenerator.builder().seed(SEED).build().generate();
      assertThat(p.categoryClient()).isEqualTo(AbsDefaults.CATEGORY_CLIENT);
      assertThat(p.accountPackage()).isEqualTo(AbsDefaults.PACKAGE_BASE);
    }

    @Test
    @DisplayName("categoryClient=ЗАРПЛАТНИК → ЗАРПЛАТНЫЙ")
    void salaryGetsSalaryPackage() {
      var p =
        IndividualProfileGenerator.builder()
          .categoryClient(AbsDefaults.CATEGORY_SALARY)
          .seed(SEED)
          .build()
          .generate();
      assertThat(p.accountPackage()).isEqualTo(AbsDefaults.PACKAGE_SALARY);
    }

    @Test
    @DisplayName("categoryClient=ПЕНСИОНЕР → ПЕНСИОННЫЙ")
    void pensionerGetsPensionPackage() {
      var p =
        IndividualProfileGenerator.builder()
          .categoryClient(AbsDefaults.CATEGORY_PENSIONER)
          .age(65)
          .seed(SEED)
          .build()
          .generate();
      assertThat(p.accountPackage()).isEqualTo(AbsDefaults.PACKAGE_PENSIONER);
    }

    @Test
    @DisplayName("явный accountPackage переопределяет каскад")
    void explicitPackageWins() {
      var p =
        IndividualProfileGenerator.builder()
          .categoryClient(AbsDefaults.CATEGORY_SALARY)
          .accountPackage("VIP")
          .seed(SEED)
          .build()
          .generate();
      assertThat(p.accountPackage()).isEqualTo("VIP");
    }
  }

  @Nested
  @DisplayName("Каскад #3 — регион → адреса + серия паспорта")
  class RegionCascade {

    @Test
    @DisplayName("region=SPB → регистрация СПб, серия паспорта на 78")
    void spbRegion() {
      var p =
        IndividualProfileGenerator.builder()
          .region(Region.SPB)
          .seed(SEED)
          .build()
          .generate();
      assertThat(p.registrationAddress().region()).isEqualTo(Region.SPB.regionName());
      assertThat(p.registrationAddress().city()).isNotBlank();
      assertThat(p.passport().series()).startsWith("78");
      assertThat(p.birthPlace().region()).isEqualTo(Region.SPB.regionName());
      assertThat(p.birthPlace().city()).isNotBlank();
    }

    @Test
    @DisplayName("region=MOSCOW → регистрация Москва, серия 77")
    void moscowRegion() {
      var p =
        IndividualProfileGenerator.builder()
          .region(Region.MOSCOW)
          .seed(SEED)
          .build()
          .generate();
      assertThat(p.registrationAddress().region()).isEqualTo(Region.MOSCOW.regionName());
      assertThat(p.registrationAddress().city()).isNotBlank();
      assertThat(p.passport().series()).startsWith("77");
      assertThat(p.birthPlace().region()).isEqualTo(Region.MOSCOW.regionName());
      assertThat(p.birthPlace().city()).isNotBlank();
    }

    @Test
    @DisplayName("region=SVERDLOVSK → регистрация Екатеринбург, серия 66")
    void sverdlovskRegion() {
      var p =
        IndividualProfileGenerator.builder()
          .region(Region.SVERDLOVSK)
          .seed(SEED)
          .build()
          .generate();
      assertThat(p.registrationAddress().region()).isEqualTo(Region.SVERDLOVSK.regionName());
      assertThat(p.registrationAddress().city()).isNotBlank();
      assertThat(p.passport().series()).startsWith("66");
    }

    @Test
    @DisplayName("region не задан → случайный (серия не из фиксированного набора не проверить, но не падает)")
    void noRegion() {
      var p = IndividualProfileGenerator.builder().seed(SEED).build().generate();
      assertThat(p.registrationAddress()).isNotNull();
      assertThat(p.passport().series()).hasSize(4);
    }

    @Test
    @DisplayName("region + FOREIGN паспорт → series не привязан к региону")
    void regionForeignPassport() {
      var p =
        IndividualProfileGenerator.builder()
          .region(Region.MOSCOW)
          .passportType(ru.validum.core.passport.PassportType.FOREIGN)
          .residency(Residency.nonResident(ru.validum.core.common.Country.TJ))
          .seed(SEED)
          .build()
          .generate();
      assertThat(p.registrationAddress().region()).isEqualTo(Region.MOSCOW.regionName());
      assertThat(p.registrationAddress().city()).isNotBlank();
      assertThat(p.passport().type())
        .isEqualTo(ru.validum.core.passport.PassportType.FOREIGN);
    }
  }

  @Nested
  @DisplayName("Композиция всех каскадов")
  class FullComposition {

    @Test
    @DisplayName("age=16 + region=MOSCOW + категория ЗАРПЛАТНИК → всё применяется согласованно")
    void everythingTogether() {
      var p =
        IndividualProfileGenerator.builder()
          .age(16)
          .region(Region.MOSCOW)
          .categoryClient(AbsDefaults.CATEGORY_SALARY)
          .seed(SEED)
          .build()
          .generate();

      assertThat(ageOf(p)).isEqualTo(16);
      assertThat(p.familyStatus()).isEqualTo(FamilyStatus.SINGLE);
      assertThat(p.registrationAddress().region()).isEqualTo(Region.MOSCOW.regionName());
      assertThat(p.registrationAddress().city()).isNotBlank();
      assertThat(p.passport().series()).startsWith("77");
      assertThat(p.accountPackage()).isEqualTo(AbsDefaults.PACKAGE_SALARY);
    }

    @Test
    @DisplayName("Детерминизм: одинаковый seed + одинаковые параметры → одинаковый профиль")
    void deterministic() {
      var a =
        IndividualProfileGenerator.builder()
          .age(30)
          .region(Region.KRASNODAR)
          .categoryClient(AbsDefaults.CATEGORY_PENSIONER)
          .seed(SEED)
          .build()
          .generate();
      var b =
        IndividualProfileGenerator.builder()
          .age(30)
          .region(Region.KRASNODAR)
          .categoryClient(AbsDefaults.CATEGORY_PENSIONER)
          .seed(SEED)
          .build()
          .generate();
      assertThat(a).isEqualTo(b);
    }
  }
}
