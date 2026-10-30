package ru.validum.core.snils;

import java.util.Random;
import ru.validum.core.random.DataGenerator;

/**
 * Генератор СНИЛС.
 *
 * <p>Гарантирует корректность контрольного числа по алгоритму ПФР: первые 9 цифр умножаются на
 * коэффициенты 9, 8, 7, ..., 1 слева направо. Сумма берётся по модулю 101. Значения 100 и 101
 * дают контрольное число 00.
 */
public final class SnilsGenerator implements DataGenerator {

  private final Random random;

  public SnilsGenerator() {
    this(DataGenerator.defaultRandom());
  }

  public SnilsGenerator(Random random) {
    this.random = random;
  }

  /** Генерирует СНИЛС в формате {@code 123-456-789 00}. */
  public String generateFormatted() {
    String raw = generateRaw();
    return String.format(
      "%s-%s-%s %s",
      raw.substring(0, 3), raw.substring(3, 6), raw.substring(6, 9), raw.substring(9, 11));
  }

  /** Генерирует СНИЛС в сыром формате {@code 12345678900}. */
  public String generateRaw() {
    int base = 100_000_000 + random.nextInt(900_000_000);
    int control = calculateControlNumber(base);
    return String.format("%09d%02d", base, control);
  }

  /**
   * Контрольное число СНИЛС по алгоритму ПФР.
   *
   * <p>Цифры извлекаются слева направо и умножаются на коэффициенты 9..1. Сумма по модулю 101 даёт
   * контрольное число. Значения 100 и 101 превращаются в 00.
   */
  private int calculateControlNumber(int base) {
    int[] digits = new int[9];
    int temp = base;
    for (int i = 8; i >= 0; i--) {
      digits[i] = temp % 10;
      temp /= 10;
    }

    int sum = 0;
    for (int i = 0; i < 9; i++) {
      sum += digits[i] * (9 - i);
    }

    int control = sum % 101;
    if (control == 100 || control == 101) {
      return 0;
    }
    return control;
  }
}
