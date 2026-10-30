package ru.validum.core.profile;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Period;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("IndividualScenarios — зарплатник (Итерация 4)")
class IndividualScenariosSalaryTest {

  private static final long SEED = 42L;

  private static int ageOf(IndividualProfile p) {
    return Period.between(p.birthDate(), LocalDate.now()).getYears();
  }

  @Test
  @DisplayName("возраст = SALARY_AGE")
  void ageIsSalaryAge() {
    var p = IndividualScenarios.createSalaryClient(SEED);
    assertThat(ageOf(p)).isEqualTo(IndividualScenarios.SALARY_AGE);
  }

  @Test
  @DisplayName("категория «ЗАРПЛАТНИК», пакет «ЗАРПЛАТНЫЙ»")
  void categoryAndPackage() {
    var p = IndividualScenarios.createSalaryClient(SEED);
    assertThat(p.categoryClient()).isEqualTo(IndividualScenarios.CATEGORY_SALARY);
    assertThat(p.accountPackage()).isEqualTo(IndividualScenarios.PACKAGE_SALARY);
  }

  @Test
  @DisplayName("счёт и банк заполнены")
  void hasAccountAndBank() {
    var p = IndividualScenarios.createSalaryClient(SEED);
    assertThat(p.account()).isNotBlank();
    assertThat(p.bank()).isNotNull();
  }

  @Test
  @DisplayName("одинаковый seed → равные профили (детерминизм)")
  void deterministic() {
    var a = IndividualScenarios.createSalaryClient(7L);
    var b = IndividualScenarios.createSalaryClient(7L);
    assertThat(a).isEqualTo(b);
  }

  @Test
  @DisplayName("возраста упорядочены: ADULT < SALARY < PENSION_FEMALE < PENSION_MALE")
  void agesOrdered() {
    assertThat(IndividualScenarios.ADULT_AGE)
      .isLessThan(IndividualScenarios.SALARY_AGE);
    assertThat(IndividualScenarios.SALARY_AGE)
      .isLessThan(IndividualScenarios.PENSION_AGE_FEMALE);
    assertThat(IndividualScenarios.PENSION_AGE_FEMALE)
      .isLessThan(IndividualScenarios.PENSION_AGE_MALE);
  }
}
