package ru.validum.core.validation;

/**
 * Валидатор ОКПО (Общероссийский классификатор предприятий и организаций).
 *
 * <p>Для ЮЛ — 8 цифр: 7 значащих + 1 контрольная. Для ИП — 10 цифр: 9 значащих + 1 контрольная.
 *
 * <p>Контрольная цифра = сумма значащих цифр по модулю 11. Если результат равен 10 — берётся 0.
 * Этот алгоритм совпадает с {@link ru.validum.core.organization.OkpoGenerator}, поэтому
 * сгенерированные ОКПО гарантированно проходят валидацию.
 */
public final class OkpoValidator {

  private OkpoValidator() {}

  /**
   * Автоопределение типа по длине: 8 цифр — ЮЛ, 10 цифр — ИП.
   *
   * @param okpo строка с ОКПО
   * @return {@code true}, если ОКПО валиден по контрольной цифре
   */
  public static boolean isValid(String okpo) {
    if (okpo == null) {
      return false;
    }
    return switch (okpo.length()) {
      case 8 -> isValidLegalEntity(okpo);
      case 10 -> isValidIndividualEntrepreneur(okpo);
      default -> false;
    };
  }

  /** ОКПО юридического лица — 8 цифр. */
  public static boolean isValidLegalEntity(String okpo) {
    return checkControl(okpo, 7);
  }

  /** ОКПО индивидуального предпринимателя — 10 цифр. */
  public static boolean isValidIndividualEntrepreneur(String okpo) {
    return checkControl(okpo, 9);
  }

  private static boolean checkControl(String okpo, int significantDigits) {
    if (okpo == null || okpo.length() != significantDigits + 1) {
      return false;
    }
    int sum = 0;
    for (int i = 0; i < significantDigits; i++) {
      char c = okpo.charAt(i);
      if (c < '0' || c > '9') {
        return false;
      }
      sum += c - '0';
    }
    char controlChar = okpo.charAt(significantDigits);
    if (controlChar < '0' || controlChar > '9') {
      return false;
    }
    int expected = sum % 11;
    if (expected == 10) {
      expected = 0;
    }
    return expected == (controlChar - '0');
  }
}
