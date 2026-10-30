package ru.validum.core.inn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.random.DataGenerator;

@DisplayName("InnCorruptor — намеренная порча ИНН")
class InnCorruptorTest {

  private static final int[] C11 = {7, 2, 4, 10, 3, 5, 9, 4, 6, 8};
  private static final int[] C12 = {3, 7, 2, 4, 10, 3, 5, 9, 4, 6, 8};

  @Test
  @DisplayName("После порчи контрольные разряды не сходятся")
  void corruptedInnFailsChecksum() {
    InnGenerator generator = new InnGenerator(DataGenerator.seededRandom(42L));
    for (int i = 0; i < 100; i++) {
      String valid = generator.generateIndividual();
      String broken = InnCorruptor.corruptIndividual(valid);

      assertThat(isValid(valid)).as("Валидный ИНН %s", valid).isTrue();
      assertThat(isValid(broken)).as("Битый ИНН %s", broken).isFalse();
      assertThat(broken).hasSize(12).containsOnlyDigits();
      assertThat(broken.substring(0, 11)).isEqualTo(valid.substring(0, 11));
    }
  }

  @Test
  @DisplayName("Неверная длина бросает исключение")
  void invalidLengthThrows() {
    assertThatThrownBy(() -> InnCorruptor.corruptIndividual("123"))
      .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> InnCorruptor.corruptIndividual(null))
      .isInstanceOf(IllegalArgumentException.class);
  }

  private static boolean isValid(String inn) {
    if (inn == null || inn.length() != 12) {
      return false;
    }
    int sum11 = 0;
    for (int i = 0; i < 10; i++) {
      sum11 += (inn.charAt(i) - '0') * C11[i];
    }
    int sum12 = 0;
    for (int i = 0; i < 11; i++) {
      sum12 += (inn.charAt(i) - '0') * C12[i];
    }
    return (sum11 % 11 % 10) == (inn.charAt(10) - '0')
      && (sum12 % 11 % 10) == (inn.charAt(11) - '0');
  }
}
