package ru.validum.core.address;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.random.DataGenerator;

class AddressGeneratorTest {

  private final AddressGenerator generator = new AddressGenerator();

  @Test
  @DisplayName("Адрес регистрации: тип REGISTRATION, формат FIAS, статус Действительный")
  void registrationAddressHasCorrectMetadata() {
    Address address = generator.generateRegistration();
    assertThat(address.type()).isEqualTo(AddressType.REGISTRATION);
    assertThat(address.format()).isEqualTo(AddressFormat.FIAS);
    assertThat(address.status()).isEqualTo("Действительный");
    assertThat(address.isDefault()).isTrue();
  }

  @Test
  @DisplayName("Индекс адреса регистрации — 6 цифр")
  void registrationAddressHasSixDigitPostalCode() {
    Address address = generator.generateRegistration();
    assertThat(address.postalCode()).hasSize(6).containsOnlyDigits();
  }

  @Test
  @DisplayName("Город адреса регистрации заполнен")
  void registrationAddressHasCity() {
    Address address = generator.generateRegistration();
    assertThat(address.city()).isNotBlank();
  }

  @Test
  @DisplayName("Улица и дом адреса регистрации заполнены")
  void registrationAddressHasStreetAndBuilding() {
    Address address = generator.generateRegistration();
    assertThat(address.street()).isNotBlank();
    assertThat(address.building()).isNotBlank();
  }

  @Test
  @DisplayName("expectedAddress и fiasAddress не пустые")
  void registrationAddressHasExpectedAndFiasAddress() {
    Address address = generator.generateRegistration();
    assertThat(address.expectedAddress()).isNotBlank();
    assertThat(address.fiasAddress()).isNotBlank();
  }

  @Test
  @DisplayName("Страна адреса — RU")
  void addressCountryIsRu() {
    Address address = generator.generateRegistration();
    assertThat(address.country()).isNotNull();
    assertThat(address.country().isoCode()).isEqualTo("RU");
  }

  @Test
  @DisplayName("FIAS-разбор: регион, тип улицы, тип дома, ИФНС заполнены")
  void fiasAddressHasFullStructure() {
    Address address = generator.generateRegistration();
    assertThat(address.region()).isNotBlank();
    assertThat(address.streetType()).isNotBlank();
    assertThat(address.buildingType()).isNotBlank();
    assertThat(address.fiasIfnsfl()).isNotBlank();
  }

  @Test
  @DisplayName("Почтовый адрес: тип POST")
  void postAddressHasCorrectType() {
    Address address = generator.generatePost();
    assertThat(address.type()).isEqualTo(AddressType.POST);
  }

  @Test
  @DisplayName("Адрес фактического проживания: тип CURRENT")
  void currentAddressHasCorrectType() {
    Address address = generator.generateCurrent();
    assertThat(address.type()).isEqualTo(AddressType.CURRENT);
  }

  @Test
  @DisplayName("Место рождения: тип BIRTH_PLACE")
  void birthPlaceHasCorrectType() {
    Address address = generator.generateBirthPlace();
    assertThat(address.type()).isEqualTo(AddressType.BIRTH_PLACE);
  }

  @Test
  @DisplayName("Свободный формат — только expectedAddress, FIAS-части пустые")
  void freeFormAddressHasOnlyExpectedAddress() {
    Address address = generator.generateFreeForm(AddressType.REGISTRATION);
    assertThat(address.format()).isEqualTo(AddressFormat.FREE_FORM);
    assertThat(address.postalCode()).isNull();
    assertThat(address.region()).isNull();
    assertThat(address.city()).isNull();
    assertThat(address.expectedAddress()).isNotBlank();
  }

  @Test
  @DisplayName("Одинаковый seed даёт одинаковый адрес")
  void sameSeedProducesSameAddress() {
    AddressGenerator first = new AddressGenerator(DataGenerator.seededRandom(42L));
    AddressGenerator second = new AddressGenerator(DataGenerator.seededRandom(42L));
    assertThat(first.generateRegistration()).isEqualTo(second.generateRegistration());
  }
}
