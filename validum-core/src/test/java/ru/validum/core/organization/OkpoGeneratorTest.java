package ru.validum.core.organization;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.random.DataGenerator;

@DisplayName("OkpoGenerator")
class OkpoGeneratorTest {

  private final OkpoGenerator generator = new OkpoGenerator(DataGenerator.seededRandom(42L));

  @Test
  @DisplayName("ЮЛ: 8 цифр, контрольная цифра верна")
  void legalEntityOkpo() {
    for (int i = 0; i < 100; i++) {
      String okpo = generator.generateLegalEntity();
      assertThat(okpo).hasSize(8).containsOnlyDigits();
      assertThat(isValid(okpo, 7)).as("ОКПО %s", okpo).isTrue();
    }
  }

  @Test
  @DisplayName("ИП: 10 цифр, контрольная цифра верна")
  void individualEntrepreneurOkpo() {
    for (int i = 0; i < 100; i++) {
      String okpo = generator.generateIndividualEntrepreneur();
      assertThat(okpo).hasSize(10).containsOnlyDigits();
      assertThat(isValid(okpo, 9)).as("ОКПО %s", okpo).isTrue();
    }
  }

  @Test
  @DisplayName("Детерминизм по seed")
  void determinism() {
    OkpoGenerator a = new OkpoGenerator(DataGenerator.seededRandom(42L));
    OkpoGenerator b = new OkpoGenerator(DataGenerator.seededRandom(42L));
    assertThat(a.generateLegalEntity()).isEqualTo(b.generateLegalEntity());
  }

  private static boolean isValid(String okpo, int significantDigits) {
    int sum = 0;
    for (int i = 0; i < significantDigits; i++) {
      sum += okpo.charAt(i) - '0';
    }
    int expected = sum % 11;
    if (expected == 10) {
      expected = 0;
    }
    return expected == (okpo.charAt(significantDigits) - '0');
  }
}
