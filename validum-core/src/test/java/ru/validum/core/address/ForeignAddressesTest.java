package ru.validum.core.address;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.common.Country;

class ForeignAddressesTest {

  @Test
  @DisplayName("Казахстан: город, улица и индекс в формате страны")
  void kazakhstan() {
    Address address = new AddressGenerator(new Random(1))
      .generateRegistrationOutsideRU(Country.KZ);

    assertThat(address.country()).isEqualTo(Country.KZ);
    assertThat(address.city()).isEqualTo("Алматы");
    assertThat(address.street()).isNotBlank();
    assertThat(address.postalCode()).startsWith("05").hasSize(6);
    assertThat(address.expectedAddress()).contains("Казахстан", "Алматы", "05");
    assertThat(address.format()).isEqualTo(AddressFormat.FREE_FORM);
    assertThat(address.type()).isEqualTo(AddressType.REGISTRATION);
  }

  @Test
  @DisplayName("Германия и США: пятизначный индекс, английский адрес")
  void westernFormats() {
    Address de = new AddressGenerator(new Random(2)).generateOutsideRU(Country.DE, AddressType.POST);
    Address us = new AddressGenerator(new Random(3)).generateOutsideRU(Country.US, AddressType.CURRENT);

    assertThat(de.postalCode()).startsWith("10").hasSize(5);
    assertThat(de.city()).isEqualTo("Берлин");
    assertThat(us.postalCode()).hasSize(5);
    assertThat(us.type()).isEqualTo(AddressType.CURRENT);
  }

  @Test
  @DisplayName("Страна без своих данных получает нейтральный набор, Россия — ошибку")
  void fallbacks() {
    Address tj = new AddressGenerator(new Random(4)).generateOutsideRU(Country.TJ, AddressType.REGISTRATION);
    assertThat(tj.city()).isEqualTo("Столица");
    assertThat(tj.postalCode()).hasSize(6);

    assertThatThrownBy(
        () -> new AddressGenerator(new Random(5)).generateOutsideRU(Country.RU, AddressType.POST))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("КЛАДР");
  }

  @Test
  @DisplayName("Один и тот же seed даёт один и тот же зарубежный адрес")
  void deterministic() {
    Address first = new AddressGenerator(new Random(42)).generateRegistrationOutsideRU(Country.BY);
    Address second = new AddressGenerator(new Random(42)).generateRegistrationOutsideRU(Country.BY);
    assertThat(first).isEqualTo(second);
  }
}
