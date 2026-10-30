package ru.validum.core.snils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.random.DataGenerator;

class SnilsGeneratorTest {

  private final SnilsGenerator generator = new SnilsGenerator();

  @Test
  @DisplayName("СНИЛС в сыром формате — 11 цифр")
  void rawSnilsHas11Digits() {
    assertThat(generator.generateRaw()).hasSize(11).containsOnlyDigits();
  }

  @Test
  @DisplayName("СНИЛС в форматированном виде — маска 123-456-789 00")
  void formattedSnilsMatchesMask() {
    assertThat(generator.generateFormatted()).matches("\\d{3}-\\d{3}-\\d{3} \\d{2}");
  }

  @Test
  @DisplayName("Контрольное число СНИЛС корректно по алгоритму ПФР")
  void snilsPassesPfrChecksum() {
    for (int i = 0; i < 100; i++) {
      String raw = generator.generateRaw();
      assertThat(isValidSnils(raw)).as("СНИЛС %s", raw).isTrue();
    }
  }

  @Test
  @DisplayName("Одинаковый seed даёт одинаковый СНИЛС")
  void sameSeedProducesSameSnils() {
    SnilsGenerator first = new SnilsGenerator(DataGenerator.seededRandom(42L));
    SnilsGenerator second = new SnilsGenerator(DataGenerator.seededRandom(42L));
    assertThat(first.generateRaw()).isEqualTo(second.generateRaw());
  }

  /**
   * Проверка СНИЛС по алгоритму ПФР: цифры слева направо на коэффициенты 9..1, mod 101, 100/101 →
   * 00.
   */
  private static boolean isValidSnils(String raw) {
    int sum = 0;
    for (int i = 0; i < 9; i++) {
      sum += (raw.charAt(i) - '0') * (9 - i);
    }
    int control = sum % 101;
    if (control == 100 || control == 101) {
      control = 0;
    }
    return control == Integer.parseInt(raw.substring(9, 11));
  }
}
