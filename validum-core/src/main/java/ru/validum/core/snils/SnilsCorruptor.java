package ru.validum.core.snils;

/**
 * Намеренная порча СНИЛС: контрольное число становится неверным.
 *
 * <p>Не путать с {@link SnilsGenerator}. Изменяется последняя цифра контрольного числа.
 */
public final class SnilsCorruptor {

  private SnilsCorruptor() {}

  /**
   * Порча 11-значного СНИЛС.
   *
   * @param validSnils валидный СНИЛС (11 цифр), сгенерированный {@link SnilsGenerator}
   * @return СНИЛС с битым контрольным числом
   */
  public static String corrupt(String validSnils) {
    if (validSnils == null || validSnils.length() != 11) {
      throw new IllegalArgumentException("СНИЛС должен содержать 11 цифр: " + validSnils);
    }
    char last = validSnils.charAt(10);
    int flipped = (last - '0' + 1) % 10;
    return validSnils.substring(0, 10) + flipped;
  }
}
