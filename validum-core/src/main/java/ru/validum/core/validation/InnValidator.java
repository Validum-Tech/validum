package ru.validum.core.validation;

/**
 * Валидатор ИНН физических и юридических лиц.
 */
public final class InnValidator {

  private static final int[] C11 = {7, 2, 4, 10, 3, 5, 9, 4, 6, 8};
  private static final int[] C12 = {3, 7, 2, 4, 10, 3, 5, 9, 4, 6, 8};
  private static final int[] C10 = {2, 4, 10, 3, 5, 9, 4, 6, 8};

  private InnValidator() {}

  public static boolean isValid(String inn) {
    if (inn == null) return false;
    return isValidIndividual(inn) || isValidLegalEntity(inn);
  }

  public static boolean isValidIndividual(String inn) {
    if (inn == null || inn.length() != 12 || !inn.matches("\\d{12}")) return false;
    int sum11 = 0, sum12 = 0;
    for (int i = 0; i < 10; i++) sum11 += (inn.charAt(i) - '0') * C11[i];
    for (int i = 0; i < 11; i++) sum12 += (inn.charAt(i) - '0') * C12[i];
    return (sum11 % 11 % 10) == (inn.charAt(10) - '0')
      && (sum12 % 11 % 10) == (inn.charAt(11) - '0');
  }

  public static boolean isValidLegalEntity(String inn) {
    if (inn == null || inn.length() != 10 || !inn.matches("\\d{10}")) return false;
    int sum = 0;
    for (int i = 0; i < 9; i++) sum += (inn.charAt(i) - '0') * C10[i];
    return (sum % 11 % 10) == (inn.charAt(9) - '0');
  }
}
