package ru.validum.core.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.bank.Bank;
import ru.validum.core.bank.BikSource;
import ru.validum.core.bank.BikSources;

/**
 * P3.3 — явный выбор банка в {@link IndividualProfileGenerator}.
 *
 * <p>Приоритет:
 *
 * <ol>
 *   <li>{@code .bank(Bank)} — если задан, используется он;
 *   <li>{@code .bik(String)} — если задан, банк ищется в справочнике (не найден → {@link
 *       IllegalArgumentException});
 *   <li>ничего не задано — случайный из справочника.
 * </ol>
 */
@DisplayName("IndividualProfile — .bik / .bank (P3.3)")
class IndividualProfileWithBikTest {

  private static final long SEED = 42L;
  private static final String SBER_BIK = "044525225";
  private static final String VTB_BIK = "044525187";
  private static final BikSource BANKS =
    BikSources.resolve(Path.of("src", "test", "resources", "bik-sample.xml").toString());

  // ── .bik(String) ─────────────────────────────────────────────────────────

  @Test
  @DisplayName(".bik(SBER_BIK) → банк ПАО Сбербанк")
  void bikResolvesSberbank() {
    IndividualProfile p =
      IndividualProfileGenerator.builder().seed(SEED).bik(SBER_BIK).build().generate();

    assertThat(p.bank()).isNotNull();
    assertThat(p.bank().bik()).isEqualTo(SBER_BIK);
    assertThat(p.bank().name()).isEqualTo("ПАО Сбербанк");
    assertThat(p.bank().corrAccount()).isEqualTo("30101810400000000225");
  }

  @Test
  @DisplayName(".bik(VTB_BIK) → банк ВТБ")
  void bikResolvesVtb() {
    IndividualProfile p =
      IndividualProfileGenerator.builder().seed(SEED).bik(VTB_BIK).build().generate();

    assertThat(p.bank().bik()).isEqualTo(VTB_BIK);
    assertThat(p.bank().name()).isEqualTo("Банк ВТБ (ПАО)");
  }

  @Test
  @DisplayName(".bik(несуществующий) → IllegalArgumentException с понятным сообщением")
  void bikNotFoundThrows() {
    assertThatThrownBy(
      () ->
        IndividualProfileGenerator.builder()
          .seed(SEED)
          .bik("000000000")
          .build()
          .generate())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("000000000")
      .hasMessageContaining("не найден");
  }

  @Test
  @DisplayName(".bik(неверная длина) → IllegalArgumentException (не найден)")
  void bikWrongLengthThrows() {
    assertThatThrownBy(
      () ->
        IndividualProfileGenerator.builder()
          .seed(SEED)
          .bik("12345")
          .build()
          .generate())
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName(".bik(blank) — игнорируется, случайный банк")
  void blankBikFallsBackToRandom() {
    IndividualProfile p =
      IndividualProfileGenerator.builder().seed(SEED).bik("  ").build().generate();

    assertThat(p.bank()).isNotNull();
    assertThat(p.bank().bik()).hasSize(9);
  }

  // ── .bank(Bank) ──────────────────────────────────────────────────────────

  @Test
  @DisplayName(".bank(explicit) → используется явный объект")
  void explicitBankIsUsed() {
    Bank custom = BANKS.findByBik(SBER_BIK).orElseThrow();
    IndividualProfile p =
      IndividualProfileGenerator.builder().seed(SEED).bank(custom).build().generate();

    assertThat(p.bank()).isEqualTo(custom);
  }

  @Test
  @DisplayName(".bank(Bank) приоритетнее .bik(String)")
  void bankOverridesBik() {
    Bank vtb = BANKS.findByBik(VTB_BIK).orElseThrow();
    IndividualProfile p =
      IndividualProfileGenerator.builder()
        .seed(SEED)
        .bik(SBER_BIK) // будет проигнорирован
        .bank(vtb)
        .build()
        .generate();

    assertThat(p.bank().bik()).isEqualTo(VTB_BIK);
  }

  // ── Совместимость и детерминизм ──────────────────────────────────────────

  @Test
  @DisplayName("Без .bik / .bank — случайный банк (как раньше)")
  void noBikNoBankRandom() {
    IndividualProfile p =
      IndividualProfileGenerator.builder().seed(SEED).build().generate();

    assertThat(p.bank()).isNotNull();
    assertThat(p.bank().bik()).hasSize(9);
  }

  @Test
  @DisplayName(".bik не ломает детерминизм остальных полей")
  void bikDoesNotBreakDeterminism() {
    IndividualProfile a =
      IndividualProfileGenerator.builder().seed(SEED).bik(SBER_BIK).build().generate();
    IndividualProfile b =
      IndividualProfileGenerator.builder().seed(SEED).bik(SBER_BIK).build().generate();

    assertThat(a.fullName()).isEqualTo(b.fullName());
    assertThat(a.inn()).isEqualTo(b.inn());
    assertThat(a.snils()).isEqualTo(b.snils());
    assertThat(a.account()).isEqualTo(b.account());
    assertThat(a.bank()).isEqualTo(b.bank());
  }

  @Test
  @DisplayName(".bik не влияет на счёт (счёт валиден по 809-П)")
  void accountStillValid() {
    IndividualProfile p =
      IndividualProfileGenerator.builder().seed(SEED).bik(SBER_BIK).build().generate();

    assertThat(p.account()).hasSize(20);
    assertThat(
        ru.validum.core.account.AccountValidator.isValid(
          p.account(), ru.validum.core.account.AccountType.CLAC, SBER_BIK))
      .isTrue();
  }
}
