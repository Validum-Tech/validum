package ru.validum.core.validation;

/**
 * Валидатор СНИЛС.
 */
public final class SnilsValidator {

  private SnilsValidator() {}

  public static boolean isValid(String snils) {
    if (snils == null || snils.length() != 11 || !snils.matches("\\d{11}")) return false;
    int sum = 0;
    for (int i = 0; i < 9; i++) sum += (snils.charAt(i) - '0') * (9 - i);
    int control = sum % 101;
    if (control == 100 || control == 101) control = 0;
    return control == Integer.parseInt(snils.substring(9, 11));
  }
}
