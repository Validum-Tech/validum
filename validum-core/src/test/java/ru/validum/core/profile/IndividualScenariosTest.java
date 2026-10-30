package ru.validum.core.profile;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Period;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IndividualScenarios — фасады MVP-сценариев (Итерация 3)")
class IndividualScenariosTest {

  private static final long SEED = 42L;

  private static int ageOf(IndividualProfile p) {
    return Period.between(p.birthDate(), LocalDate.now()).getYears();
  }

  @Nested
  @DisplayName("createAdultResidentWithAccount()")
  class WithAccount {

    @Test
    @DisplayName("счёт и банк заполнены")
    void hasAccountAndBank() {
      var p = IndividualScenarios.createAdultResidentWithAccount(SEED);
      assertThat(p.account()).isNotBlank();
      assertThat(p.bank()).isNotNull();
    }

    @Test
    @DisplayName("возраст ровно 19 лет")
    void ageIs19() {
      var p = IndividualScenarios.createAdultResidentWithAccount(SEED);
      assertThat(ageOf(p)).isEqualTo(IndividualScenarios.ADULT_AGE);
    }

    @Test
    @DisplayName("три адреса совпадают (SAME_FOR_ALL)")
    void addressesAreSame() {
      var p = IndividualScenarios.createAdultResidentWithAccount(SEED);
      assertThat(p.registrationAddress()).isEqualTo(p.postAddress());
      assertThat(p.postAddress()).isEqualTo(p.currentAddress());
    }
  }

  @Nested
  @DisplayName("createAdultResidentWithoutAccount()")
  class WithoutAccount {

    @Test
    @DisplayName("счёт отсутствует, банк указан")
    void noAccountButBankPresent() {
      var p = IndividualScenarios.createAdultResidentWithoutAccount(SEED);
      assertThat(p.account()).isNull();
      assertThat(p.bank()).isNotNull();
    }

    @Test
    @DisplayName("возраст ровно 19 лет")
    void ageIs19() {
      var p = IndividualScenarios.createAdultResidentWithoutAccount(SEED);
      assertThat(ageOf(p)).isEqualTo(IndividualScenarios.ADULT_AGE);
    }
  }

  @Nested
  @DisplayName("Общие инварианты")
  class Common {

    @Test
    @DisplayName("ИНН, СНИЛС, паспорт валидны")
    void identifiersValid() {
      var p = IndividualScenarios.createAdultResidentWithAccount(SEED);
      assertThat(p.inn()).hasSize(12);
      assertThat(p.snils()).hasSize(11);
      assertThat(p.passport()).isNotNull();
      assertThat(p.passport().expirationDate()).isAfter(LocalDate.now());
    }

    @Test
    @DisplayName("одинаковый seed → равные профили")
    void sameSeedSameProfile() {
      var a = IndividualScenarios.createAdultResidentWithAccount(123L);
      var b = IndividualScenarios.createAdultResidentWithAccount(123L);
      assertThat(a).isEqualTo(b);
    }
  }
}
