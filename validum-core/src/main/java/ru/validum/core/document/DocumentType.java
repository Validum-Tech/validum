package ru.validum.core.document;

import java.util.Arrays;

/**
 * Виды документов, удостоверяющих личность (ДУЛ).
 *
 * <p>Коды — из классификатора СПДУЛ ФНС (приказ от 29.10.2020 № ЕД-7-11/783@). У визы и
 * водительского удостоверения кода в СПДУЛ нет — {@link #spdulCode()} возвращает {@code null}.
 *
 * <p>Маски взяты из нормативных актов о бланках (ПП РФ № 2267, № 298, № 789, № 356;
 * приказы МВД № 365 и МО № 700):
 *
 * <ul>
 *   <li>паспорт РФ — серия 4 цифры двумя группами, номер 6 цифр, код подразделения;
 *   <li>загранпаспорт — 2 + 7 (биометрические серии 71–79 живут 10 лет, остальные 5);
 *   <li>вид на жительство — 2 + 7, бессрочный;
 *   <li>разрешение на временное проживание — 2 + 6, 3 года;
 *   <li>удостоверение беженца — 2 + 7, до 3 лет;
 *   <li>военный билет и билет офицера запаса — 2 буквы + 7 цифр;
 *   <li>дипломатический паспорт — 2 + 7, 5 лет;
 *   <li>виза — 2 + 7;
 *   <li>водительское удостоверение — 4 + 6, 10 лет.
 * </ul>
 *
 * <p>Сознательно НЕ реализованы (формат не подтверждён первоисточником): миграционная карта,
 * свидетельство о рождении, свидетельство о смерти, паспорт моряка.
 */
public enum DocumentType {
  /** Паспорт гражданина РФ, СПДУЛ 21. */
  PASSPORT_RF("21", "Паспорт гражданина РФ", 4, 6, false, true, true, null, 14, null),

  /** Дипломатический паспорт, СПДУЛ 09. */
  DIPLOMATIC_PASSPORT("09", "Дипломатический паспорт", 2, 7, false, false, false, 5, 0, null),

  /** Загранпаспорт гражданина РФ, СПДУЛ 22. */
  FOREIGN_PASSPORT("22", "Загранпаспорт гражданина РФ", 2, 7, false, false, false, 10, 0, null),

  /** Паспорт иностранного гражданина, СПДУЛ 10 (формат страны выдачи). */
  PASSPORT_FOREIGN("10", "Паспорт иностранного гражданина", 2, 7, false, false, false, 5, 0, null),

  /** Вид на жительство, СПДУЛ 12. */
  RESIDENCE_PERMIT("12", "Вид на жительство", 2, 7, false, false, false, null, 0, null),

  /** Разрешение на временное проживание, СПДУЛ 15. */
  TEMPORARY_RESIDENCE("15", "Разрешение на временное проживание", 2, 6, false, false, false, 3, 0, null),

  /** Удостоверение беженца, СПДУЛ 13. */
  REFUGEE_CERTIFICATE("13", "Удостоверение беженца", 2, 7, false, false, false, 3, 0, null),

  /** Военный билет, СПДУЛ 07. */
  MILITARY_ID("07", "Военный билет", 2, 7, true, false, false, null, 18, 27),

  /** Военный билет офицера запаса, СПДУЛ 27. */
  MILITARY_ID_RESERVE("27", "Военный билет офицера запаса", 2, 7, true, false, false, null, 18, null),

  /** Виза (в СПДУЛ отсутствует). */
  VISA(null, "Виза", 2, 7, false, false, false, 1, 0, null),

  /** Водительское удостоверение (в СПДУЛ отсутствует). */
  DRIVER_LICENSE(null, "Водительское удостоверение", 4, 6, false, false, false, 10, 18, null);

  private final String spdulCode;
  private final String title;
  private final int seriesLength;
  private final int numberLength;
  private final boolean seriesLetters;
  private final boolean groupedSeries;
  private final boolean hasDepartmentCode;
  private final Integer validityYears;
  private final int minAge;
  private final Integer maxAge;

  DocumentType(
    String spdulCode,
    String title,
    int seriesLength,
    int numberLength,
    boolean seriesLetters,
    boolean groupedSeries,
    boolean hasDepartmentCode,
    Integer validityYears,
    int minAge,
    Integer maxAge) {
    this.spdulCode = spdulCode;
    this.title = title;
    this.seriesLength = seriesLength;
    this.numberLength = numberLength;
    this.seriesLetters = seriesLetters;
    this.groupedSeries = groupedSeries;
    this.hasDepartmentCode = hasDepartmentCode;
    this.validityYears = validityYears;
    this.minAge = minAge;
    this.maxAge = maxAge;
  }

  /** Код СПДУЛ или {@code null}, если документа нет в классификаторе. */
  public String spdulCode() {
    return spdulCode;
  }

  /** Человеческое название вида документа. */
  public String title() {
    return title;
  }

  /** Сколько знаков в серии (без учёта разделителя). */
  public int seriesLength() {
    return seriesLength;
  }

  /** Сколько знаков в номере. */
  public int numberLength() {
    return numberLength;
  }

  /** Серия состоит из заглавных букв (военные билеты). */
  public boolean seriesLetters() {
    return seriesLetters;
  }

  /** Серия печатается двумя группами: {@code 99 99} (паспорт РФ). */
  public boolean groupedSeries() {
    return groupedSeries;
  }

  /** У документа есть код подразделения (паспорт РФ). */
  public boolean hasDepartmentCode() {
    return hasDepartmentCode;
  }

  /** Минимальный возраст, с которого документ может быть выдан. */
  public int minAge() {
    return minAge;
  }

  /** Максимальный возраст держателя; {@code null} — без ограничения. */
  public Integer maxAge() {
    return maxAge;
  }

  /** Срок действия в годах; {@code null} — бессрочно или срок задаётся возрастом. */
  public Integer validityYears() {
    return validityYears;
  }

  /**
   * Находит тип по коду СПДУЛ.
   *
   * @throws IllegalArgumentException если код неизвестен
   */
  public static DocumentType fromCode(String code) {
    String normalized = normalizeCode(code);
    return Arrays.stream(values())
      .filter(type -> normalized != null && normalized.equals(type.spdulCode))
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("Неизвестный код СПДУЛ: " + code));
  }

  /**
   * Коды СПДУЛ двузначные, но из PowerShell аргумент {@code 07} приезжает как число {@code 7}
   * (оболочка сама превращает «21,07» в массив чисел). Одноразрядный код дополняем нулём,
   * чтобы {@code --documents 21,7} не падал на ровном месте.
   */
  private static String normalizeCode(String code) {
    if (code == null) {
      return null;
    }
    String trimmed = code.trim();
    if (trimmed.length() == 1 && Character.isDigit(trimmed.charAt(0))) {
      return "0" + trimmed;
    }
    return trimmed;
  }
}
