package ru.validum.core.inn;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.random.DataGenerator;

class InnGeneratorTest {

  private final InnGenerator generator = new InnGenerator();

  @Test
  @DisplayName("ИНН физлица — 12 цифр")
  void individualInnHas12Digits() {
    assertThat(generator.generateIndividual()).hasSize(12).containsOnlyDigits();
  }

  @Test
  @DisplayName("ИНН юрлица — 10 цифр")
  void legalEntityInnHas10Digits() {
    assertThat(generator.generateLegalEntity()).hasSize(10).containsOnlyDigits();
  }

  @Test
  @DisplayName("Контрольные разряды ИНН физлица корректны по алгоритму ФНС")
  void individualInnPassesFnsChecksum() {
    for (int i = 0; i < 100; i++) {
      String inn = generator.generateIndividual();
      assertThat(isValidIndividualInn(inn)).as("ИНН %s", inn).isTrue();
    }
  }

  @Test
  @DisplayName("Контрольный разряд ИНН юрлица корректен по алгоритму ФНС")
  void legalEntityInnPassesFnsChecksum() {
    for (int i = 0; i < 100; i++) {
      String inn = generator.generateLegalEntity();
      assertThat(isValidLegalEntityInn(inn)).as("ИНН %s", inn).isTrue();
    }
  }

  @Test
  @DisplayName("Одинаковый seed даёт одинаковый ИНН")
  void sameSeedProducesSameInn() {
    InnGenerator first = new InnGenerator(DataGenerator.seededRandom(42L));
    InnGenerator second = new InnGenerator(DataGenerator.seededRandom(42L));
    assertThat(first.generateIndividual()).isEqualTo(second.generateIndividual());
  }

  @Test
  @DisplayName("Разные seed дают разные ИНН (уникальность на 1000 значений)")
  void differentSeedsProduceDifferentInns() {
    Set<String> inns = new HashSet<>();
    for (int i = 0; i < 1000; i++) {
      inns.add(generator.generateIndividual());
    }
    assertThat(inns).hasSizeGreaterThan(990);
  }

  /**
   * Проверка ИНН физлица по алгоритму ФНС.
   *
   * <p>11-я контрольная цифра: сумма первых 10 цифр на коэффициенты c11, mod 11, mod 10.
   *
   * <p>12-я контрольная цифра: сумма первых 11 цифр (10 исходных + 11-я контрольная) на
   * коэффициенты c12, mod 11, mod 10.
   */
  private static boolean isValidIndividualInn(String inn) {
    int[] c11 = {7, 2, 4, 10, 3, 5, 9, 4, 6, 8};
    int[] c12 = {3, 7, 2, 4, 10, 3, 5, 9, 4, 6, 8};

    int sum11 = 0;
    for (int i = 0; i < 10; i++) {
      sum11 += (inn.charAt(i) - '0') * c11[i];
    }

    int sum12 = 0;
    for (int i = 0; i < 11; i++) {
      sum12 += (inn.charAt(i) - '0') * c12[i];
    }

    int d11 = sum11 % 11 % 10;
    int d12 = sum12 % 11 % 10;
    return d11 == (inn.charAt(10) - '0') && d12 == (inn.charAt(11) - '0');
  }

  private static boolean isValidLegalEntityInn(String inn) {
    int[] c10 = {2, 4, 10, 3, 5, 9, 4, 6, 8};
    int sum = 0;
    for (int i = 0; i < 9; i++) {
      sum += (inn.charAt(i) - '0') * c10[i];
    }
    return (sum % 11 % 10) == (inn.charAt(9) - '0');
  }
}
