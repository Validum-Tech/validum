package ru.validum.core.address;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Адрес: человекочитаемая строка")
class AddressHumanFormatTest {

  @Test
  @DisplayName("Пустые части не печатаются, тип стоит перед названием")
  void joinsPartsInRussianOrder() {
    String formatted =
      AddressBook.humanAddress(
        "196240", "Санкт-Петербург г", "Санкт-Петербург", "г", "Пушкин", "ул", "Госпитальная", "12", "5");

    assertThat(formatted)
      .isEqualTo("196240, Санкт-Петербург г, г. Пушкин, ул. Госпитальная, д. 12, кв. 5");
  }

  @Test
  @DisplayName("Город-субъект не дублируется: «Москва г, г. Москва» недопустимо")
  void subjectCityIsNotRepeated() {
    String formatted =
      AddressBook.humanAddress("125009", "Москва г", "Москва", "г", "Москва", "ул", "Тверская", "7", "");

    assertThat(formatted).isEqualTo("125009, Москва г, ул. Тверская, д. 7");
  }

  @Test
  @DisplayName("Нет индекса и квартиры — строка не начинается с запятой")
  void skipsAbsentParts() {
    String formatted =
      AddressBook.humanAddress("", "Омская обл", "Омская", "г", "Омск", "ул", "Ленина", "1", "");

    assertThat(formatted).isEqualTo("Омская обл, г. Омск, ул. Ленина, д. 1");
  }

  @Test
  @DisplayName("Тип с точкой («кп.») не даёт двойную точку")
  void typeWithDotIsNotDoubled() {
    String formatted =
      AddressBook.humanAddress("692086", "Приморский край", "Приморский",
        "кп.", "Горные Ключи", "ул", "Монастырская", "125", "83");

    assertThat(formatted).isEqualTo("692086, Приморский край, кп. Горные Ключи, ул. Монастырская, д. 125, кв. 83");
  }

  @Test
  @DisplayName("Реальный адрес из справочника читается человеком")
  void generatedAddressIsReadable() {
    Address address = AddressBook.address(new Random(42), "66", AddressType.REGISTRATION);

    assertThat(address.expectedAddress())
      .doesNotContain(",,", " ,")
      .contains(address.postalCode())
      .contains("д. ")
      .contains("кв. ");
  }
}
