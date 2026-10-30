package ru.validum.core.export;

import java.util.Locale;

/**
 * Способ, которым список ДУЛ попадает в плоский CSV.
 *
 * <ul>
 *   <li>{@link #INLINE} — одна ячейка {@code documents}, внутри строки вида {@code
 *       21|45 10|123456|ГУ МВД|2015-06-01||770-001}, разделённые переводом строки. Открывается в
 *       Excel одной строкой и остаётся человекочитаемым;
 *   <li>{@link #JSON} — одна ячейка {@code documents} с компактным JSON-массивом объектов. Удобно
 *       для скриптов и как замена JSON-профилю в плоской таблице;
 *   <li>{@link #SPLIT} — колонка {@code documents} пустая, документы пишутся отдельной таблицей
 *       {@code documents.csv} с ключом {@code profileKey} (см. {@link DocumentsCsv#toRows}). Чистая
 *       нормализация: грузится в БД через {@code COPY}.
 * </ul>
 */
public enum DocumentsFormat {
  INLINE,
  JSON,
  SPLIT;

  /** Разбирает значение опции CLI ({@code inline}, {@code json}, {@code split}). */
  public static DocumentsFormat fromString(String value) {
    if (value == null || value.isBlank()) {
      return INLINE;
    }
    return switch (value.trim().toLowerCase(Locale.ROOT)) {
      case "inline" -> INLINE;
      case "json" -> JSON;
      case "split" -> SPLIT;
      default -> throw new IllegalArgumentException(
        "Неизвестный режим документов: '" + value + "'. Допустимо: inline, json, split");
    };
  }
}
