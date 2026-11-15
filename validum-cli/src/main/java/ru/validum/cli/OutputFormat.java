package ru.validum.cli;

public enum OutputFormat {
  /** Машинный CSV: UTF-8 с BOM, «,», английские заголовки (стабильно для скриптов). */
  CSV,
  /** CSV для русского Excel: UTF-8 с BOM, «;», русские заголовки. */
  CSV_SEMICOLON,
  JSON,
  JSON_ARRAY,
  /** Вложенный JSON: структура профиля (passport, addresses, bank, cards) вместо плоской строки. */
  JSON_NESTED,
  SQL;

  public static OutputFormat fromString(String s) {
    if (s == null || s.isBlank()) {
      throw new IllegalArgumentException("Формат не задан");
    }
    return switch (s.toLowerCase().replace("-", "_").trim()) {
      case "json" -> JSON;
      case "json_array" -> JSON_ARRAY;
      case "csv" -> CSV;
      case "csv_semicolon", "csv_excel" -> CSV_SEMICOLON;
      case "json_nested", "nested" -> JSON_NESTED;
      case "sql", "ddl" -> SQL;
      default -> throw new IllegalArgumentException(
        "Неизвестный формат: '" + s + "'. Допустимо: json, json-array, csv, csv-semicolon, json-nested, sql");
    };
  }
}
