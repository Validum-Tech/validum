package ru.validum.core.validation;

/**
 * Валидатор ОГРНИП (15 цифр) — отдельный фасад для явного контракта.
 *
 * <p>Алгоритм полностью совпадает с {@link OgrnValidator#isValidIndividualEntrepreneur(String)},
 * но вынесен в отдельный класс, чтобы REST-эндпоинт {@code /validate/ogrnip} не путал
 * ОГРНИП с ОГРН ЮЛ и не давал ложных срабатываний по длине.
 *
 * <p>Контрольная цифра: остаток от деления числа без последней цифры на 13, взятый по модулю 10.
 */
public final class OgrnipValidator {

  private OgrnipValidator() {}

  /**
   * Проверяет ОГРНИП (15 цифр).
   *
   * @param ogrnip строка с ОГРНИП
   * @return {@code true}, если длина 15, все символы цифровые и контрольная цифра сходится
   */
  public static boolean isValid(String ogrnip) {
    if (ogrnip == null || ogrnip.length() != 15) {
      return false;
    }
    for (int i = 0; i < 15; i++) {
      char c = ogrnip.charAt(i);
      if (c < '0' || c > '9') {
        return false;
      }
    }
    long numeric = Long.parseLong(ogrnip);
    int control = (int) ((numeric / 10) % 13 % 10);
    return control == (ogrnip.charAt(14) - '0');
  }
}
