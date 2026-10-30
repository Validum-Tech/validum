package ru.validum.core.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Типы счетов УФЭБС")
class AccountTypeTest {

  @Test
  @DisplayName("Все 12 типов имеют уникальные коды")
  void codesAreUnique() {
    Set<String> codes = new HashSet<>();
    for (AccountType type : AccountType.values()) {
      assertThat(codes.add(type.code())).as("Дубль кода %s", type.code()).isTrue();
    }
    assertThat(codes).hasSize(12);
  }

  @Test
  @DisplayName("Клиентские и корреспондентские счета различаются областью")
  void scopesAreCorrect() {
    assertThat(AccountType.CLAC.isClientAccount()).isTrue();
    assertThat(AccountType.TRSA.isClientAccount()).isTrue();
    assertThat(AccountType.CRSA.isClientAccount()).isFalse();
    assertThat(AccountType.CRSA.scope()).isEqualTo(AccountType.Scope.CORRESPONDENT);
    assertThat(AccountType.CBRA.scope()).isEqualTo(AccountType.Scope.BANK_OF_RUSSIA);
  }

  @Test
  @DisplayName("fromCode принимает код в любом регистре")
  void fromCodeIsCaseInsensitive() {
    assertThat(AccountType.fromCode("CRSA")).isEqualTo(AccountType.CRSA);
    assertThat(AccountType.fromCode("crsa")).isEqualTo(AccountType.CRSA);
    assertThat(AccountType.fromCode(" CLAC ")).isEqualTo(AccountType.CLAC);
  }

  @Test
  @DisplayName("Неизвестный код бросает исключение")
  void unknownCodeThrows() {
    assertThatThrownBy(() -> AccountType.fromCode("XXXX"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("XXXX");
    assertThatThrownBy(() -> AccountType.fromCode(null))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("Названия типов заполнены по-русски")
  void titlesAreFilled() {
    for (AccountType type : AccountType.values()) {
      assertThat(type.title()).as("%s", type.code()).isNotBlank();
    }
  }
}
