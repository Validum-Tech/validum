package ru.validum.core.snils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.random.DataGenerator;

@DisplayName("SnilsCorruptor — намеренная порча СНИЛС")
class SnilsCorruptorTest {

  @Test
  @DisplayName("После порчи контрольное число не сходится")
  void corruptedSnilsFailsChecksum() {
    SnilsGenerator generator = new SnilsGenerator(DataGenerator.seededRandom(42L));
    for (int i = 0; i < 100; i++) {
      String valid = generator.generateRaw();
      String broken = SnilsCorruptor.corrupt(valid);

      assertThat(isValid(valid)).as("Валидный СНИЛС %s", valid).isTrue();
      assertThat(isValid(broken)).as("Битый СНИЛС %s", broken).isFalse();
      assertThat(broken).hasSize(11).containsOnlyDigits();
    }
  }

  @Test
  @DisplayName("Неверная длина бросает исключение")
  void invalidLengthThrows() {
    assertThatThrownBy(() -> SnilsCorruptor.corrupt("123"))
      .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> SnilsCorruptor.corrupt(null))
      .isInstanceOf(IllegalArgumentException.class);
  }

  private static boolean isValid(String raw) {
    if (raw == null || raw.length() != 11) {
      return false;
    }
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
