package ru.validum.core.bank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Банк: реквизиты и проверки конструктора")
class BankTest {

  @Test
  @DisplayName("Корректные реквизиты сохраняются как есть")
  void keepsRequisites() {
    Bank bank = new Bank("044525225", "ПАО Сбербанк", "30101810400000000225", "Москва", "Москва");

    assertThat(bank.bik()).isEqualTo("044525225");
    assertThat(bank.name()).isEqualTo("ПАО Сбербанк");
    assertThat(bank.corrAccount()).isEqualTo("30101810400000000225");
    assertThat(bank.city()).isEqualTo("Москва");
  }

  @Test
  @DisplayName("БИК не из 9 символов — исключение")
  void rejectsBadBik() {
    assertThatThrownBy(() -> new Bank(null, "Банк", "30101810400000000225", "Москва", "Москва"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("БИК должен содержать 9 цифр");
    assertThatThrownBy(() -> new Bank("04452522", "Банк", "30101810400000000225", "Москва", "Москва"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("БИК должен содержать 9 цифр");
  }

  @Test
  @DisplayName("Корсчёт не из 20 символов — исключение")
  void rejectsBadCorrAccount() {
    assertThatThrownBy(() -> new Bank("044525225", "Банк", null, "Москва", "Москва"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Корсчёт должен содержать 20 цифр");
    assertThatThrownBy(() -> new Bank("044525225", "Банк", "3010181040000000022", "Москва", "Москва"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Корсчёт должен содержать 20 цифр");
  }
}
