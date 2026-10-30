package ru.validum.core.bank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ParticipantRegistryTest {

  private ParticipantRegistry loadRegistry() throws Exception {
    try (InputStream in = getClass().getResourceAsStream("/bik-sample.xml")) {
      assertThat(in).as("Тестовый файл bik-sample.xml не найден").isNotNull();
      return BikRegistryLoader.loadRegistry(in);
    }
  }

  @Test
  @DisplayName("Реестр содержит все 6 участников (не только банки)")
  void loadsAllSixParticipants() throws Exception {
    ParticipantRegistry registry = loadRegistry();
    assertThat(registry.size()).isEqualTo(6);
    assertThat(registry.all()).hasSize(6);
  }

  @Test
  @DisplayName("byType(BANK) = 4: 3 активных + 1 закрытый")
  void bankTypeIncludesClosed() throws Exception {
    ParticipantRegistry registry = loadRegistry();
    List<Participant> banks = registry.byType(ParticipantType.BANK);
    assertThat(banks).hasSize(4);
    assertThat(banks).allMatch(p -> p.type() == ParticipantType.BANK);
  }

  @Test
  @DisplayName("banks() = 3 активных банка (обратная совместимость с BikSource)")
  void activeBanksExcludeClosedAndNonBanks() throws Exception {
    ParticipantRegistry registry = loadRegistry();
    assertThat(registry.banks())
      .extracting(Bank::bik)
      .containsExactlyInAnyOrder("044525225", "044525187", "044525974");
  }

  @Test
  @DisplayName("Казначейство (PtType=52) — участник без корсчёта")
  void treasuryIsParticipantWithoutCorrAccount() throws Exception {
    ParticipantRegistry registry = loadRegistry();
    Participant treasury = registry.findByBik("016311121").orElseThrow();

    assertThat(treasury.type()).isEqualTo(ParticipantType.TREASURY_REGIONAL);
    assertThat(treasury.corrAccount()).isNull();
    assertThat(treasury.isActiveBank()).isFalse();
    assertThatThrownBy(treasury::toBank).isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("Главное управление БР (PtType=00) — участник без корсчёта")
  void headOfficeIsParticipantWithoutCorrAccount() throws Exception {
    ParticipantRegistry registry = loadRegistry();
    Participant head = registry.findByBik("044525000").orElseThrow();

    assertThat(head.type()).isEqualTo(ParticipantType.BR_HEAD_OFFICE);
    assertThat(head.corrAccount()).isNull();
    assertThat(head.isActiveBank()).isFalse();
  }

  @Test
  @DisplayName("Закрытый банк (CLOSED) есть в реестре, но не активный")
  void closedBankIsPresentButInactive() throws Exception {
    ParticipantRegistry registry = loadRegistry();
    Participant closed = registry.findByBik("999999999").orElseThrow();

    assertThat(closed.type()).isEqualTo(ParticipantType.BANK);
    assertThat(closed.status()).isEqualTo("CLOSED");
    assertThat(closed.corrAccount()).isEqualTo("30101810400000000225");
    assertThat(closed.isActiveBank()).isFalse();
  }

  @Test
  @DisplayName("Активный банк конвертируется в Bank с полями")
  void activeBankMapsToBank() throws Exception {
    ParticipantRegistry registry = loadRegistry();
    Participant sber = registry.findByBik("044525225").orElseThrow();

    assertThat(sber.isActiveBank()).isTrue();
    Bank bank = sber.toBank();
    assertThat(bank.name()).isEqualTo("ПАО Сбербанк");
    assertThat(bank.corrAccount()).isEqualTo("30101810400000000225");
    assertThat(bank.city()).isEqualTo("Москва");
    assertThat(bank.region()).isEqualTo("45");
  }
}
