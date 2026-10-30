package ru.validum.core.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.common.Region;
import ru.validum.core.organization.KppGenerator;
import ru.validum.core.random.DataGenerator;

@DisplayName("KppValidator — расширенная проверка (P2.3)")
class KppValidatorTest {

  private final KppGenerator generator = new KppGenerator(DataGenerator.seededRandom(42L));

  // ── Валидные ─────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Сгенерированный КПП проходит валидацию (для всех 16 регионов)")
  void generatedKppIsValidForAllRegions() {
    for (Region region : Region.values()) {
      for (int i = 0; i < 10; i++) {
        String kpp = generator.generate(region);
        assertThat(KppValidator.isValid(kpp)).as("Регион %s, КПП %s", region, kpp).isTrue();
      }
    }
  }

  @Test
  @DisplayName("КПП с причиной 43 (филиал) проходит валидацию")
  void branchReasonIsValid() {
    String kpp = generator.generate(Region.MOSCOW, KppGenerator.REASON_BRANCH);
    assertThat(KppValidator.isValid(kpp)).isTrue();
  }

  @Test
  @DisplayName("Эталонный КПП 773601001 валиден")
  void referenceKppIsValid() {
    assertThat(KppValidator.isValid("773601001")).isTrue();
  }

  // ── Негативные: нулевые группы ───────────────────────────────────────────

  @Test
  @DisplayName("Код региона 00 → false")
  void zeroRegionFails() {
    assertThat(KppValidator.isValid("003601001")).isFalse();
  }

  @Test
  @DisplayName("Код ИФНС 00 → false")
  void zeroInspectionFails() {
    assertThat(KppValidator.isValid("770001001")).isFalse();
  }

  @Test
  @DisplayName("Причина постановки 00 → false")
  void zeroReasonFails() {
    assertThat(KppValidator.isValid("773600001")).isFalse();
  }

  @Test
  @DisplayName("Порядковый номер 000 → false")
  void zeroSerialFails() {
    assertThat(KppValidator.isValid("773601000")).isFalse();
  }

  // ── Негативные: формат ───────────────────────────────────────────────────

  @Test
  @DisplayName("Код региона 99 валиден (граница), 100 — нет (только 9 цифр)")
  void regionBoundaries() {
    assertThat(KppValidator.isValid("993601001")).isTrue();
    // Код 100 не влезает в 2 цифры — это уже не КПП
  }

  @Test
  @DisplayName("Неверная длина → false")
  void wrongLengthFails() {
    assertThat(KppValidator.isValid("77360100")).isFalse();     // 8
    assertThat(KppValidator.isValid("7736010010")).isFalse();   // 10
    assertThat(KppValidator.isValid("")).isFalse();
  }

  @Test
  @DisplayName("Нецифровые символы → false")
  void nonDigitsFail() {
    assertThat(KppValidator.isValid("77360100A")).isFalse();
    assertThat(KppValidator.isValid("ABCDEFGHI")).isFalse();
  }

  @Test
  @DisplayName("null → false")
  void nullFails() {
    assertThat(KppValidator.isValid(null)).isFalse();
  }
}
