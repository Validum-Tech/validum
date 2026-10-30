package ru.validum.core.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.random.DataGenerator;

@DisplayName("Генератор счетов")
class AccountGeneratorTest {

  private static final String BANK_BIK = "044525225";
  private static final String INDIVIDUAL_BALANCE = "40817";
  private static final String CBR_BIK = "044525000";

  private final AccountGenerator generator = new AccountGenerator();

  @Test
  @DisplayName("Все 12 типов счетов проходят проверку своим правилом")
  void allAccountTypesAreValid() {
    for (AccountType type : AccountType.values()) {
      String bik = type.isClientAccount() ? BANK_BIK : CBR_BIK;
      for (int i = 0; i < 20; i++) {
        String account = generator.generate("40817", type, bik);
        assertThat(account).as("%s: %s", type.code(), account).hasSize(20).containsOnlyDigits();
        assertThat(account.substring(5, 8)).as("Код валюты в %s", account).isEqualTo("810");
        assertThat(AccountValidator.isValid(account, type, bik))
          .as("%s %s", type.code(), account)
          .isTrue();
      }
    }
  }

  @Test
  @DisplayName("Валюта пишется в разряды 6-8 и участвует в ключе")
  void currencyIsWrittenAndValidated() {
    String account = generator.generate("40817", AccountType.CLAC, BANK_BIK, "840");

    assertThat(account.substring(5, 8)).isEqualTo("840");
    assertThat(AccountValidator.isValid(account, AccountType.CLAC, BANK_BIK)).isTrue();
  }

  @Test
  @DisplayName("Ключ считается по правилу своего типа: клиентский и корреспондентский различаются")
  void keyDependsOnTypeRule() {
    AccountGenerator first = new AccountGenerator(DataGenerator.seededRandom(7L));
    AccountGenerator second = new AccountGenerator(DataGenerator.seededRandom(7L));
    String client = first.generate("30102", AccountType.CLAC, BANK_BIK);
    String correspondent = second.generate("30102", AccountType.CRSA, CBR_BIK);

    assertThat(client.substring(0, 8)).isEqualTo(correspondent.substring(0, 8));
    assertThat(client.charAt(8) - '0')
      .isEqualTo(
        AccountValidator.calculateKey(client, AccountValidator.prefixFor(AccountType.CLAC, BANK_BIK)));
    assertThat(correspondent.charAt(8) - '0')
      .isEqualTo(
        AccountValidator.calculateKey(correspondent, AccountValidator.prefixFor(AccountType.CRSA, CBR_BIK)));
  }

  @Test
  @DisplayName("Одинаковый seed даёт одинаковый счёт")
  void sameSeedProducesSameAccount() {
    AccountGenerator first = new AccountGenerator(DataGenerator.seededRandom(42L));
    AccountGenerator second = new AccountGenerator(DataGenerator.seededRandom(42L));

    assertThat(first.generate(INDIVIDUAL_BALANCE, AccountType.CLAC, BANK_BIK))
      .isEqualTo(second.generate(INDIVIDUAL_BALANCE, AccountType.CLAC, BANK_BIK));
  }

  @Test
  @DisplayName("Разные вызовы дают разные счета (уникальность на 1000 значений)")
  void accountsAreUnique() {
    Set<String> accounts = new HashSet<>();
    for (int i = 0; i < 1000; i++) {
      accounts.add(generator.generate(INDIVIDUAL_BALANCE, AccountType.CLAC, BANK_BIK));
    }
    assertThat(accounts).hasSizeGreaterThan(990);
  }

  @Test
  @DisplayName("Неверные входные данные бросают исключение")
  void invalidInputsThrow() {
    assertThatThrownBy(() -> generator.generate("4081", AccountType.CLAC, BANK_BIK))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("5 цифр");
    assertThatThrownBy(() -> generator.generate(null, AccountType.CLAC, BANK_BIK))
      .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> generator.generate("40817", null, BANK_BIK))
      .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> generator.generate("40817", AccountType.CLAC, "04452522"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("БИК");
    assertThatThrownBy(() -> generator.generate("40817", AccountType.CLAC, BANK_BIK, "81"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("валюты");
  }
}
