package ru.validum.core.address;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AddressBook — реальный справочник КЛАДР")
class AddressBookTest {

  @Test
  @DisplayName("Срез содержит ~90 субъектов, включая Москву, Петербург и регионы-миллионники")
  void subjectsCoverRussia() {
    assertThat(AddressBook.subjects()).hasSizeGreaterThan(80);
    assertThat(AddressBook.subjects())
      .extracting(AddressBook.Subject::code)
      .contains("77", "78", "50", "02", "63", "16", "66");
    assertThat(AddressBook.subject("77").name()).isEqualTo("Москва");
    assertThat(AddressBook.subject("77").display()).isEqualTo("Москва г");
  }

  @Test
  @DisplayName("В Москве реальные населённые пункты и больше 20 тысяч улиц")
  void moscowPlacesAndStreets() {
    var places = AddressBook.places("77");

    assertThat(places).isNotEmpty();
    int streets = places.stream().mapToInt(p -> p.streets().size()).sum();
    assertThat(streets).isGreaterThan(20_000);
    assertThat(places).allSatisfy(p -> assertThat(p.streets()).isNotEmpty());
  }

  @Test
  @DisplayName("Адрес по коду субъекта: регион, индекс, улица, дом и квартира заполнены")
  void addressForSubject() {
    Address address = AddressBook.address(new Random(42L), "63", AddressType.REGISTRATION);

    assertThat(address.region()).isEqualTo("Самарская");
    assertThat(address.regionType()).isEqualTo("обл");
    assertThat(address.postalCode()).hasSize(6).containsOnlyDigits();
    assertThat(address.city()).isNotBlank();
    assertThat(address.street()).isNotBlank();
    assertThat(address.building()).containsOnlyDigits();
    assertThat(address.apartment()).containsOnlyDigits();
    assertThat(address.format()).isEqualTo(AddressFormat.FIAS);
    assertThat(address.formatted()).contains("Самарская обл").contains(address.street());
  }

  @Test
  @DisplayName("Один и тот же seed даёт одинаковый адрес")
  void deterministicBySeed() {
    Address a = AddressBook.address(new Random(7L), "16", AddressType.REGISTRATION);
    Address b = AddressBook.address(new Random(7L), "16", AddressType.REGISTRATION);

    assertThat(a).isEqualTo(b);
  }

  @Test
  @DisplayName("Разные seed дают разные адреса")
  void differentSeeds() {
    Address a = AddressBook.address(new Random(1L), "02", AddressType.REGISTRATION);
    Address b = AddressBook.address(new Random(2L), "02", AddressType.REGISTRATION);

    assertThat(a.formatted()).isNotEqualTo(b.formatted());
  }

  @Test
  @DisplayName("Без кода субъекта выбирается случайный субъект")
  void randomSubjectWhenCodeNull() {
    Address address = AddressBook.address(new Random(11L), null, AddressType.POST);

    assertThat(address.region()).isNotBlank();
    assertThat(address.type()).isEqualTo(AddressType.POST);
  }

  @Test
  @DisplayName("Неизвестный код субъекта — исключение")
  void unknownSubject() {
    assertThatThrownBy(() -> AddressBook.subject("ZZ"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Субъект РФ не найден");
  }
}
