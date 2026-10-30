package ru.validum.core.bank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ParticipantTypeTest {

  @Test
  @DisplayName("Ровно 17 кодов PtType, без дублей кодов")
  void has17UniqueCodes() {
    assertThat(ParticipantType.values()).hasSize(17);
    Set<String> codes = new HashSet<>();
    for (ParticipantType t : ParticipantType.values()) {
      assertThat(codes.add(t.code()))
        .as("Дублирующийся код PtType: %s", t.code())
        .isTrue();
    }
  }

  @Test
  @DisplayName("90 — конкурсный управляющий, не РКЦ; 10 — РКЦ")
  void resolvesTrickyCodes() {
    assertThat(ParticipantType.fromCode("90")).isEqualTo(ParticipantType.LIQUIDATOR);
    assertThat(ParticipantType.fromCode("10")).isEqualTo(ParticipantType.RKC);
    assertThat(ParticipantType.LIQUIDATOR.isBank()).isFalse();
    assertThat(ParticipantType.RKC.isBank()).isFalse();
  }

  @Test
  @DisplayName("20 — единственный тип, который является банком")
  void only20IsBank() {
    assertThat(ParticipantType.BANK.isBank()).isTrue();
    long bankCount = Arrays.stream(ParticipantType.values()).filter(ParticipantType::isBank).count();
    assertThat(bankCount).isEqualTo(1);
  }

  @Test
  @DisplayName("Коды филиала/казначейства читаются корректно")
  void resolvesCommonCodes() {
    assertThat(ParticipantType.fromCode("30")).isEqualTo(ParticipantType.BANK_BRANCH);
    assertThat(ParticipantType.fromCode("51")).isEqualTo(ParticipantType.TREASURY);
    assertThat(ParticipantType.fromCode("52")).isEqualTo(ParticipantType.TREASURY_REGIONAL);
    assertThat(ParticipantType.fromCode("00")).isEqualTo(ParticipantType.BR_HEAD_OFFICE);
  }

  @Test
  @DisplayName("Неизвестный код кидает исключение")
  void unknownCodeThrows() {
    assertThatThrownBy(() -> ParticipantType.fromCode("XX"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("XX");
  }

  @Test
  @DisplayName("fromCodeOrName принимает и код, и имя константы")
  void fromCodeOrNameAcceptsBoth() {
    assertThat(ParticipantType.fromCodeOrName("rkc")).isEqualTo(ParticipantType.RKC);
    assertThat(ParticipantType.fromCodeOrName("RKC")).isEqualTo(ParticipantType.RKC);
    assertThat(ParticipantType.fromCodeOrName("52")).isEqualTo(ParticipantType.TREASURY_REGIONAL);
  }
}
