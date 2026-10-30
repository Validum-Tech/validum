package ru.validum.core.validation;

/**
 * Валидатор КПП (код причины постановки на учёт).
 *
 * <p>Формат: {@code NNNNPPZZZ} — ровно 9 цифр.
 *
 * <ul>
 *   <li>{@code NN} (позиции 1–2) — код субъекта РФ (01–99);
 *   <li>{@code NN} (позиции 3–4) — код ИФНС в регионе (01–99);
 *   <li>{@code PP} (позиции 5–6) — код причины постановки (01–99);
 *   <li>{@code ZZZ} (позиции 7–9) — порядковый номер постановки (001–999).
 * </ul>
 *
 * <p>Все четыре группы не могут быть нулевыми: код региона 00, код ИФНС 00, причина 00 или
 * порядковый 000 — невалидные значения.
 *
 * <p>Расширенная проверка (P2.3) добавлена к базовой (только код региона 01–99). Совместима с
 * {@link ru.validum.core.organization.KppGenerator}, который генерирует код ИФНС в диапазоне
 * 01–99, причину 01 или 43, порядковый 001–999.
 */
public final class KppValidator {

  private KppValidator() {}

  /**
   * Проверяет КПП.
   *
   * @param kpp строка с КПП
   * @return {@code true}, если формат верный и все группы не нулевые
   */
  public static boolean isValid(String kpp) {
    if (kpp == null || kpp.length() != 9) {
      return false;
    }
    for (int i = 0; i < 9; i++) {
      char c = kpp.charAt(i);
      if (c < '0' || c > '9') {
        return false;
      }
    }
    int regionCode = Integer.parseInt(kpp.substring(0, 2));
    if (regionCode < 1 || regionCode > 99) {
      return false;
    }
    int inspectionCode = Integer.parseInt(kpp.substring(2, 4));
    if (inspectionCode < 1) {
      return false;
    }
    int reasonCode = Integer.parseInt(kpp.substring(4, 6));
    if (reasonCode < 1) {
      return false;
    }
    int serial = Integer.parseInt(kpp.substring(6, 9));
    return serial >= 1;
  }
}
