package ru.validum.core.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.organization.OkpoGenerator;
import ru.validum.core.random.DataGenerator;

@DisplayName("OkpoValidator — ОКПО ЮЛ и ИП (P2.3)")
class OkpoValidatorTest {

  private final OkpoGenerator generator = new OkpoGenerator(DataGenerator.seededRandom(42L));

  // ── Валидные сгенерированные ─────────────────────────────────────────────

  @Test
  @DisplayName("Сгенерированный ОКПО ЮЛ проходит валидацию")
  void generatedLegalEntityIsValid() {
    for (int i = 0; i < 100; i++) {
      String okpo = generator.generateLegalEntity();
      assertThat(OkpoValidator.isValid(okpo)).as("ОКПО %s", okpo).isTrue();
      assertThat(OkpoValidator.isValidLegalEntity(okpo)).as("ОКПО %s", okpo).isTrue();
    }
  }

  @Test
  @DisplayName("Сгенерированный ОКПО ИП проходит валидацию")
  void generatedIndividualEntrepreneurIsValid() {
    for (int i = 0; i < 100; i++) {
      String okpo = generator.generateIndividualEntrepreneur();
      assertThat(OkpoValidator.isValid(okpo)).as("ОКПО %s", okpo).isTrue();
      assertThat(OkpoValidator.isValidIndividualEntrepreneur(okpo)).as("ОКПО %s", okpo).isTrue();
    }
  }

  // ── Автоопределение по длине ─────────────────────────────────────────────

  @Test
  @DisplayName("isValid автоопределяет тип: 8 цифр → ЮЛ, 10 цифр → ИП")
  void autoDetectsByLength() {
    String legal = generator.generateLegalEntity();
    String ip = generator.generateIndividualEntrepreneur();

    assertThat(OkpoValidator.isValid(legal)).isTrue();
    assertThat(OkpoValidator.isValid(ip)).isTrue();
    // Перекрёстно: ЮЛ-ОКПО не пройдёт как ИП и наоборот
    assertThat(OkpoValidator.isValidIndividualEntrepreneur(legal)).isFalse();
    assertThat(OkpoValidator.isValidLegalEntity(ip)).isFalse();
  }

  // ── Негативные ───────────────────────────────────────────────────────────

  @Test
  @DisplayName("Битая контрольная цифра → false")
  void brokenControlDigitFails() {
    String valid = generator.generateLegalEntity();
    char last = valid.charAt(7);
    int flipped = (last - '0' + 1) % 10;
    String broken = valid.substring(0, 7) + flipped;

    assertThat(OkpoValidator.isValidLegalEntity(valid)).isTrue();
    assertThat(OkpoValidator.isValidLegalEntity(broken)).isFalse();
  }

  @Test
  @DisplayName("Неверная длина → false")
  void wrongLengthFails() {
    assertThat(OkpoValidator.isValid("1234567")).isFalse();    // 7 цифр
    assertThat(OkpoValidator.isValid("123456789")).isFalse();  // 9 цифр
    assertThat(OkpoValidator.isValid("12345678901")).isFalse(); // 11 цифр
    assertThat(OkpoValidator.isValid("")).isFalse();
  }

  @Test
  @DisplayName("Нецифровые символы → false")
  void nonDigitsFail() {
    assertThat(OkpoValidator.isValid("1234567A")).isFalse();
    assertThat(OkpoValidator.isValid("ABCDEFGH")).isFalse();
    assertThat(OkpoValidator.isValid("12345678 ")).isFalse();
  }

  @Test
  @DisplayName("null → false")
  void nullFails() {
    assertThat(OkpoValidator.isValid(null)).isFalse();
    assertThat(OkpoValidator.isValidLegalEntity(null)).isFalse();
    assertThat(OkpoValidator.isValidIndividualEntrepreneur(null)).isFalse();
  }
}
