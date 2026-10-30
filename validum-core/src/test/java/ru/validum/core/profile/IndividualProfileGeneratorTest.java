package ru.validum.core.profile;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.account.AccountValidator;
import ru.validum.core.address.AddressStrategy;
import ru.validum.core.address.FiasMode;
import ru.validum.core.common.Country;
import ru.validum.core.name.Gender;
import ru.validum.core.random.DataGenerator;

class IndividualProfileGeneratorTest {

  @Test
  @DisplayName("Дефолтный профиль содержит все поля")
  void defaultProfileIsFullyPopulated() {
    IndividualProfile profile = IndividualProfileGenerator.defaults().generate();

    assertThat(profile.name()).isNotNull();
    assertThat(profile.birthDate()).isNotNull();
    assertThat(profile.birthPlace()).isNotNull();
    assertThat(profile.familyStatus()).isNotNull();
    assertThat(profile.residency()).isNotNull();
    assertThat(profile.passport()).isNotNull();
    assertThat(profile.inn()).isNotBlank();
    assertThat(profile.snils()).isNotBlank();
    assertThat(profile.registrationAddress()).isNotNull();
    assertThat(profile.postAddress()).isNotNull();
    assertThat(profile.currentAddress()).isNotNull();
    assertThat(profile.mobile()).isNotNull();
    assertThat(profile.email()).isNotNull();
    assertThat(profile.secretWord()).isNotBlank();
    assertThat(profile.categoryClient()).isEqualTo("КЛИЕНТ");
    assertThat(profile.accountPackage()).isEqualTo("БАЗОВЫЙ");
    assertThat(profile.personalManager()).isNotBlank();
    assertThat(profile.bank()).isNotNull();
    assertThat(profile.account()).isNotNull();
  }

  @Test
  @DisplayName("Дефолт: взрослый резидент РФ со счётом")
  void defaultProfileIsAdultResidentWithAccount() {
    IndividualProfile profile =
      IndividualProfileGenerator.builder().seed(42L).build().generate();

    assertThat(profile.age()).isEqualTo(19);
    assertThat(profile.residency().isResident()).isTrue();
    assertThat(profile.residency().residentCountry()).isEqualTo(Country.RU);
    assertThat(profile.residency().citizenship()).isEqualTo(Country.RU);
    assertThat(profile.residency().taxResidentCountry()).isEqualTo(Country.RU);
    assertThat(profile.hasAccount()).isTrue();
    assertThat(profile.account()).hasSize(20);
  }

  @Test
  @DisplayName("Счёт валиден по 809-П")
  void accountIsValidBy809P() {
    for (int i = 0; i < 50; i++) {
      IndividualProfile profile = IndividualProfileGenerator.defaults().generate();
      assertThat(
          AccountValidator.isValid(
            profile.account(),
            ru.validum.core.account.AccountType.CLAC,
            profile.bank() != null
              ? profile.bank().bik()
              : ru.validum.core.account.AccountGenerator.NO_BANK_BIK))
        .as("Счёт %s", profile.account())
        .isTrue();
    }
  }

  @Test
  @DisplayName("ИНН — 12 цифр, СНИЛС — 11 цифр")
  void innAndSnilsHaveCorrectLength() {
    IndividualProfile profile = IndividualProfileGenerator.defaults().generate();
    assertThat(profile.inn()).hasSize(12).containsOnlyDigits();
    assertThat(profile.snils()).hasSize(11).containsOnlyDigits();
  }

  @Test
  @DisplayName("Пол профиля согласован с ФИО")
  void profileGenderIsConsistent() {
    IndividualProfile male =
      IndividualProfileGenerator.builder().gender(Gender.MALE).build().generate();
    assertThat(male.gender()).isEqualTo(Gender.MALE);

    IndividualProfile female =
      IndividualProfileGenerator.builder().gender(Gender.FEMALE).build().generate();
    assertThat(female.gender()).isEqualTo(Gender.FEMALE);
    assertThat(female.fullName()).endsWith("на");
  }

  @Test
  @DisplayName("Возраст профиля в диапазоне 18–70 лет")
  void profileAgeIsInRange() {
    for (int i = 0; i < 50; i++) {
      IndividualProfile profile = IndividualProfileGenerator.defaults().generate();
      assertThat(profile.age()).isBetween(18, 70);
    }
  }

  @Test
  @DisplayName("Возраст можно задать явно")
  void profileAgeCanBeSet() {
    IndividualProfile profile =
      IndividualProfileGenerator.builder().age(45).build().generate();
    assertThat(profile.age()).isEqualTo(45);
  }

  @Test
  @DisplayName("Профиль без счёта: account = null")
  void profileWithoutAccountHasNullAccount() {
    IndividualProfile profile =
      IndividualProfileGenerator.builder().withAccount(false).build().generate();
    assertThat(profile.hasAccount()).isFalse();
    assertThat(profile.account()).isNull();
  }

  @Test
  @DisplayName("Адрес-стратегия SAME_FOR_ALL: три адреса одинаковые")
  void sameForAllAddressStrategy() {
    IndividualProfile profile =
      IndividualProfileGenerator.builder()
        .addressStrategy(AddressStrategy.SAME_FOR_ALL)
        .build()
        .generate();
    assertThat(profile.registrationAddress()).isEqualTo(profile.postAddress());
    assertThat(profile.postAddress()).isEqualTo(profile.currentAddress());
  }

  @Test
  @DisplayName("FIAS-режим FULL: FIAS-части заполнены")
  void fiasModeFullFillsAllParts() {
    IndividualProfile profile =
      IndividualProfileGenerator.builder()
        .fiasMode(FiasMode.FULL)
        .seed(42L)
        .build()
        .generate();
    assertThat(profile.registrationAddress().region()).isNotBlank();
    assertThat(profile.registrationAddress().street()).isNotBlank();
    assertThat(profile.registrationAddress().streetType()).isNotBlank();
    assertThat(profile.registrationAddress().fiasIfnsfl()).isNotBlank();
  }

  @Test
  @DisplayName("FIAS-режим ADDRESS_ONLY: FIAS-части пустые, expectedAddress сохранён")
  void fiasModeAddressOnlyClearsFiasParts() {
    IndividualProfile profile =
      IndividualProfileGenerator.builder()
        .fiasMode(FiasMode.ADDRESS_ONLY)
        .seed(42L)
        .build()
        .generate();
    assertThat(profile.registrationAddress().region()).isNull();
    assertThat(profile.registrationAddress().street()).isNull();
    assertThat(profile.registrationAddress().fiasIfnsfl()).isNull();
    assertThat(profile.registrationAddress().expectedAddress()).isNotBlank();
  }

  @Test
  @DisplayName("Паспорт выдан по правилам РФ: 19 лет → выдан в 14")
  void passportIssuedByRussianRules() {
    IndividualProfile profile =
      IndividualProfileGenerator.builder().age(19).build().generate();
    assertThat(profile.passport().issueDate()).isEqualTo(profile.birthDate().plusYears(14));
    assertThat(profile.passport().status()).isEqualTo("Действительный");
  }

  @Test
  @DisplayName("Просроченный паспорт: age 25, passportWasExchanged=false")
  void expiredPassportWhenNotExchanged() {
    IndividualProfile profile =
      IndividualProfileGenerator.builder()
        .age(25)
        .passportWasExchanged(false)
        .build()
        .generate();
    assertThat(profile.passport().status()).isEqualTo("Просрочен");
  }

  @Test
  @DisplayName("Одинаковый seed даёт идентичный профиль")
  void sameSeedProducesSameProfile() {
    IndividualProfile first =
      IndividualProfileGenerator.builder().seed(42L).build().generate();
    IndividualProfile second =
      IndividualProfileGenerator.builder().seed(42L).build().generate();

    assertThat(first.fullName()).isEqualTo(second.fullName());
    assertThat(first.inn()).isEqualTo(second.inn());
    assertThat(first.snils()).isEqualTo(second.snils());
    assertThat(first.account()).isEqualTo(second.account());
    assertThat(first.bank().bik()).isEqualTo(second.bank().bik());
    assertThat(first.registrationAddress()).isEqualTo(second.registrationAddress());
  }

  @Test
  @DisplayName("Residency нерезидента: страна не RU")
  void nonResidentProfileHasNonRuCountry() {
    IndividualProfile profile =
      IndividualProfileGenerator.builder()
        .residency(Residency.nonResident(Country.TJ))
        .build()
        .generate();
    assertThat(profile.residency().isResident()).isFalse();
    assertThat(profile.residency().residentCountry()).isEqualTo(Country.TJ);
  }

  @Test
  @DisplayName("Кастомный пакет обслуживания")
  void customAccountPackage() {
    IndividualProfile profile =
      IndividualProfileGenerator.builder()
        .accountPackage("ОПТИМАЛЬНЫЙ")
        .build()
        .generate();
    assertThat(profile.accountPackage()).isEqualTo("ОПТИМАЛЬНЫЙ");
  }

  @Test
  @DisplayName("Работа со своим Random (не через seed)")
  void customRandomIsUsed() {
    IndividualProfile first =
      IndividualProfileGenerator.builder()
        .random(DataGenerator.seededRandom(1L))
        .build()
        .generate();
    IndividualProfile second =
      IndividualProfileGenerator.builder()
        .random(DataGenerator.seededRandom(1L))
        .build()
        .generate();
    assertThat(first.fullName()).isEqualTo(second.fullName());
  }
}
