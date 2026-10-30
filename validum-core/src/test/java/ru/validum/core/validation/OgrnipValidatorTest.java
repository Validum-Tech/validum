package ru.validum.core.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.ogrn.OgrnGenerator;
import ru.validum.core.random.DataGenerator;

@DisplayName("OgrnipValidator — ОГРНИП (P2.3)")
class OgrnipValidatorTest {

  private final OgrnGenerator generator = new OgrnGenerator(DataGenerator.seededRandom(42L));

  @Test
  @DisplayName("Сгенерированный ОГРНИП проходит валидацию")
  void generatedOgrnipIsValid() {
    for (int i = 0; i < 100; i++) {
      String ogrnip = generator.generateIndividualEntrepreneur();
      assertThat(OgrnipValidator.isValid(ogrnip)).as("ОГРНИП %s", ogrnip).isTrue();
    }
  }

  @Test
  @DisplayName("Сгенерированный ОГРН ЮЛ (13) не проходит как ОГРНИП")
  void legalEntityOgrnFailsAsOgrnip() {
    String ogrn = generator.generateLegalEntity();
    assertThat(OgrnipValidator.isValid(ogrn)).isFalse();
  }

  @Test
  @DisplayName("Битая контрольная цифра → false")
  void brokenControlDigitFails() {
    String valid = generator.generateIndividualEntrepreneur();
    char last = valid.charAt(14);
    int flipped = (last - '0' + 1) % 10;
    String broken = valid.substring(0, 14) + flipped;

    assertThat(OgrnipValidator.isValid(valid)).isTrue();
    assertThat(OgrnipValidator.isValid(broken)).isFalse();
  }

  @Test
  @DisplayName("Неверная длина → false")
  void wrongLengthFails() {
    assertThat(OgrnipValidator.isValid("12345678901234")).isFalse();    // 14
    assertThat(OgrnipValidator.isValid("1234567890123456")).isFalse();  // 16
    assertThat(OgrnipValidator.isValid("")).isFalse();
  }

  @Test
  @DisplayName("Нецифровые символы → false")
  void nonDigitsFail() {
    String valid = generator.generateIndividualEntrepreneur();
    String withLetter = valid.substring(0, 14) + "X";
    assertThat(OgrnipValidator.isValid(withLetter)).isFalse();
  }

  @Test
  @DisplayName("null → false")
  void nullFails() {
    assertThat(OgrnipValidator.isValid(null)).isFalse();
  }
}
