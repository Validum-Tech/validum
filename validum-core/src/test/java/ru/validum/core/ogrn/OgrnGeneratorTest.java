package ru.validum.core.ogrn;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.random.DataGenerator;

class OgrnGeneratorTest {

  private final OgrnGenerator generator = new OgrnGenerator();

  @Test
  @DisplayName("ОГРН юрлица — 13 цифр")
  void legalEntityOgrnHas13Digits() {
    assertThat(generator.generateLegalEntity()).hasSize(13).containsOnlyDigits();
  }

  @Test
  @DisplayName("ОГРНИП — 15 цифр")
  void individualEntrepreneurOgrnipHas15Digits() {
    assertThat(generator.generateIndividualEntrepreneur()).hasSize(15).containsOnlyDigits();
  }

  @Test
  @DisplayName("Контрольная цифра ОГРН корректна")
  void ogrnPassesChecksum() {
    for (int i = 0; i < 100; i++) {
      String ogrn = generator.generateLegalEntity();
      assertThat(isValidOgrn(ogrn)).as("ОГРН %s", ogrn).isTrue();
    }
  }

  @Test
  @DisplayName("Контрольная цифра ОГРНИП корректна")
  void ogrnipPassesChecksum() {
    for (int i = 0; i < 100; i++) {
      String ogrnip = generator.generateIndividualEntrepreneur();
      assertThat(isValidOgrnip(ogrnip)).as("ОГРНИП %s", ogrnip).isTrue();
    }
  }

  @Test
  @DisplayName("Одинаковый seed даёт одинаковый ОГРН")
  void sameSeedProducesSameOgrn() {
    OgrnGenerator first = new OgrnGenerator(DataGenerator.seededRandom(42L));
    OgrnGenerator second = new OgrnGenerator(DataGenerator.seededRandom(42L));
    assertThat(first.generateLegalEntity()).isEqualTo(second.generateLegalEntity());
  }

  private static boolean isValidOgrn(String ogrn) {
    long numeric = Long.parseLong(ogrn);
    int control = (int) ((numeric / 10) % 11 % 10);
    return control == (ogrn.charAt(12) - '0');
  }

  private static boolean isValidOgrnip(String ogrnip) {
    long numeric = Long.parseLong(ogrnip);
    int control = (int) ((numeric / 10) % 13 % 10);
    return control == (ogrnip.charAt(14) - '0');
  }
}
