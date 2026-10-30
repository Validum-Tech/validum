package ru.validum.core.profile;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import ru.validum.core.name.Gender;

import java.time.LocalDate;
import java.time.Period;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IndividualScenarios — пенсионеры (Итерация 4, 65/60)")
class IndividualScenariosPensionerTest {

  private static final long SEED = 42L;

  private static int ageOf(IndividualProfile p) {
    return Period.between(p.birthDate(), LocalDate.now()).getYears();
  }

  @Nested
  @DisplayName("createPensionerMale()")
  class Male {

    @Test
    @DisplayName("возраст = 65")
    void ageIsMalePensionAge() {
      var p = IndividualScenarios.createPensionerMale(SEED);
      assertThat(ageOf(p)).isEqualTo(IndividualScenarios.PENSION_AGE_MALE);
    }

    @Test
    @DisplayName("пол — мужской")
    void genderIsMale() {
      var p = IndividualScenarios.createPensionerMale(SEED);
      assertThat(p.name().gender()).isEqualTo(Gender.MALE);
    }

    @Test
    @DisplayName("категория «ПЕНСИОНЕР», счёт есть")
    void categoryAndAccount() {
      var p = IndividualScenarios.createPensionerMale(SEED);
      assertThat(p.categoryClient())
        .isEqualTo(IndividualScenarios.CATEGORY_PENSIONER);
      assertThat(p.account()).isNotBlank();
    }
  }

  @Nested
  @DisplayName("createPensionerFemale()")
  class Female {

    @Test
    @DisplayName("возраст = 60")
    void ageIsFemalePensionAge() {
      var p = IndividualScenarios.createPensionerFemale(SEED);
      assertThat(ageOf(p)).isEqualTo(IndividualScenarios.PENSION_AGE_FEMALE);
    }

    @Test
    @DisplayName("пол — женский")
    void genderIsFemale() {
      var p = IndividualScenarios.createPensionerFemale(SEED);
      assertThat(p.name().gender()).isEqualTo(Gender.FEMALE);
    }

    @Test
    @DisplayName("категория «ПЕНСИОНЕР», счёт есть")
    void categoryAndAccount() {
      var p = IndividualScenarios.createPensionerFemale(SEED);
      assertThat(p.categoryClient())
        .isEqualTo(IndividualScenarios.CATEGORY_PENSIONER);
      assertThat(p.account()).isNotBlank();
    }
  }

  @Nested
  @DisplayName("createPensioner(Gender, seed) — диспетчер")
  class Dispatcher {

    @Test
    @DisplayName("MALE → 65")
    void dispatchesMale() {
      var p = IndividualScenarios.createPensioner(Gender.MALE, SEED);
      assertThat(ageOf(p)).isEqualTo(IndividualScenarios.PENSION_AGE_MALE);
    }

    @Test
    @DisplayName("FEMALE → 60")
    void dispatchesFemale() {
      var p = IndividualScenarios.createPensioner(Gender.FEMALE, SEED);
      assertThat(ageOf(p)).isEqualTo(IndividualScenarios.PENSION_AGE_FEMALE);
    }

    @Test
    @DisplayName("одинаковый seed → равные профили (детерминизм)")
    void deterministic() {
      var a = IndividualScenarios.createPensioner(Gender.FEMALE, 7L);
      var b = IndividualScenarios.createPensioner(Gender.FEMALE, 7L);
      assertThat(a).isEqualTo(b);
    }
  }

  @Nested
  @DisplayName("Общие инварианты пенсионеров")
  class Common {

    @Test
    @DisplayName("паспорт действителен, ИНН и СНИЛС валидны")
    void documentsValid() {
      var p = IndividualScenarios.createPensionerMale(SEED);
      assertThat(p.passport().expirationDate()).isAfter(LocalDate.now());
      assertThat(p.inn()).hasSize(12);
      assertThat(p.snils()).hasSize(11);
    }

    @Test
    @DisplayName("женский пенсионный возраст меньше мужского")
    void femaleAgeLowerThanMale() {
      assertThat(IndividualScenarios.PENSION_AGE_FEMALE)
        .isLessThan(IndividualScenarios.PENSION_AGE_MALE);
    }

    @Test
    @DisplayName("все константы возраста согласованы: ADULT < FEMALE < MALE")
    void agesOrdered() {
      assertThat(IndividualScenarios.ADULT_AGE)
        .isLessThan(IndividualScenarios.PENSION_AGE_FEMALE);
      assertThat(IndividualScenarios.PENSION_AGE_FEMALE)
        .isLessThan(IndividualScenarios.PENSION_AGE_MALE);
    }
  }
}
