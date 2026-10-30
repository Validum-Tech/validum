package ru.validum.core.document;

import java.util.Arrays;

/**
 * Виды документов, удостоверяющих личность (ДУЛ).
 *
 * <p>Коды — из классификатора СПДУЛ ФНС (приказ от 29.10.2020 № ЕД-7-11/783@). У визы и
 * водительского удостоверения кода в СПДУЛ нет — {@link #spdulCode()} возвращает {@code null}.
 *
 * <p>Маски взяты из нормативных актов о бланках (ПП РФ, приказы МВД/МО/ФМС):
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
  PASSPORT_RF("21", "Паспорт гражданина РФ", 4, 6, false, true, true, null),

  /** Дипломатический паспорт, СПДУЛ 09. */
  DIPLOMATIC_PASSPORT("09", "Дипломатический паспорт", 2, 7, false, false, false, 5),

  /** Загранпаспорт гражданина РФ, СПДУЛ 22. */
  FOREIGN_PASSPORT("22", "Загранпаспорт гражданина РФ", 2, 7, false, false, false, 10),

  /** Паспорт иностранного гражданина, СПДУЛ 10 (формат страны выдачи). */
  PASSPORT_FOREIGN("10", "Паспорт иностранного гражданина", 2, 7, false, false, false, 5),

  /** Вид на жительство, СПДУЛ 12. */
  RESIDENCE_PERMIT("12", "Вид на жительство", 2, 7, false, false, false, null),

  /** Разрешение на временное проживание, СПДУЛ 15. */
  TEMPORARY_RESIDENCE("15", "Разрешение на временное проживание", 2, 6, false, false, false, 3),

  /** Удостоверение беженца, СПДУЛ 13. */
  REFUGEE_CERTIFICATE("13", "Удостоверение беженца", 2, 7, false, false, false, 3),

  /** Военный билет, СПДУЛ 07. */
  MILITARY_ID("07", "Военный билет", 2, 7, true, false, false, null),

  /** Военный билет офицера запаса, СПДУЛ 27. */
  MILITARY_ID_RESERVE("27", "Военный билет офицера запаса", 2, 7, true, false, false, null),

  /** Виза (в СПДУЛ отсутствует). */
  VISA(null, "Виза", 2, 7, false, false, false, 1),

  /** Водительское удостоверение (в СПДУЛ отсутствует). */
  DRIVER_LICENSE(null, "Водительское удостоверение", 4, 6, false, false, false, 10);

  private final String spdulCode;
  private final String title;
  private final int seriesLength;
  private final int numberLength;
  private final boolean seriesLetters;
  private final boolean groupedSeries;
  private final boolean hasDepartmentCode;
  private final Integer validityYears;

  DocumentType(
    String spdulCode,
    String title,
    int seriesLength,
    int numberLength,
    boolean seriesLetters,
    boolean groupedSeries,
    boolean hasDepartmentCode,
    Integer validityYears) {
    this.spdulCode = spdulCode;
    this.title = title;
    this.seriesLength = seriesLength;
    this.numberLength = numberLength;
    this.seriesLetters = seriesLetters;
    this.groupedSeries = groupedSeries;
    this.hasDepartmentCode = hasDepartmentCode;
    this.validityYears = validityYears;
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
    return Arrays.stream(values())
      .filter(type -> code != null && code.equals(type.spdulCode))
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("Неизвестный код СПДУЛ: " + code));
  }
}
