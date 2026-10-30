package ru.validum.core.account;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Контрольный ключ счёта (письмо ЦБ № 515 / 809-П)")
class AccountValidatorTest {

  /** Реальные счета из ED807 (2026.09.0) и БИК подразделения Банка России (AccountCBRBIC). */
  private static final String[][] ED807_PAIRS = {
    {"40116810900000010001", "040397002"},
    {"40116810903970010002", "040397002"},
    {"30103810345374525078", "044525000"},
    {"30101810145250000440", "044525000"},
    {"30101810600000000772", "046311001"},
    {"40102810445370000099", "043510001"},
  };

  @Test
  @DisplayName("Все реальные счета ED807 проходят проверку по условному номеру подразделения")
  void ed807AccountsAreValid() {
    for (String[] pair : ED807_PAIRS) {
      assertThat(AccountValidator.isValidAuto(pair[0], pair[1]))
        .as("Счёт %s, БИК %s", pair[0], pair[1])
        .isTrue();
      assertThat(AccountValidator.isValid(pair[0], AccountType.CRSA, pair[1]))
        .as("Корреспондентское правило для %s", pair[0])
        .isTrue();
    }
  }

  @Test
  @DisplayName("Клиентское правило не принимает корреспондентский счёт")
  void clientRuleRejectsCorrespondentAccount() {
    assertThat(AccountValidator.isValid("30103810345374525078", AccountType.CLAC, "044525000"))
      .isFalse();
  }

  @Test
  @DisplayName("calculateKey совпадает с 9-м разрядом реального счёта")
  void calculateKeyMatchesRealAccount() {
    String account = "30103810345374525078";
    int key = AccountValidator.calculateKey(account, "025");

    assertThat(key).isEqualTo(account.charAt(8) - '0');
  }

  @Test
  @DisplayName("Изменённый контрольный ключ не проходит проверку")
  void wrongKeyIsInvalid() {
    assertThat(AccountValidator.isValidAuto("30103810445374525078", "044525000")).isFalse();
  }

  @Test
  @DisplayName("Неверная длина и нецифровые символы не проходят проверку")
  void malformedAccountIsInvalid() {
    assertThat(AccountValidator.isValidAuto("3010381034537452507", "044525000")).isFalse();
    assertThat(AccountValidator.isValidAuto("301038103453745250781", "044525000")).isFalse();
    assertThat(AccountValidator.isValidAuto("3010381034537452507X", "044525000")).isFalse();
    assertThat(AccountValidator.isValidAuto(null, "044525000")).isFalse();
    assertThat(AccountValidator.isValidAuto("30103810345374525078", null)).isFalse();
  }

  @Test
  @DisplayName("Буква клиринговой валюты в 6-м разряде допустима")
  void letterInCurrencyDigitIsAllowed() {
    String draft = "40817A00000000000953";
    int key = AccountValidator.calculateKey(draft, AccountValidator.NO_PREFIX);
    String account = draft.substring(0, 8) + key + draft.substring(9);

    assertThat(AccountValidator.isWellFormed(account)).isTrue();
    assertThat(AccountValidator.isValid(account, AccountValidator.NO_PREFIX)).isTrue();
  }

  @Test
  @DisplayName("prefixFor: клиентский счёт — разряды 7-9 БИК, корреспондентский — «0» + 5-6")
  void prefixForRules() {
    assertThat(AccountValidator.prefixFor(AccountType.CLAC, "044525225")).isEqualTo("225");
    assertThat(AccountValidator.prefixFor(AccountType.CRSA, "044525000")).isEqualTo("025");
    assertThat(AccountValidator.prefixFor(AccountType.CBRA, "044525000")).isEqualTo("025");
  }

}
