package ru.validum.cli;

public enum OutputFormat {
  JSON,
  JSON_ARRAY,
  CSV,
  SQL;

  public static OutputFormat fromString(String s) {
    if (s == null || s.isBlank()) {
      throw new IllegalArgumentException("Формат не задан");
    }
    return switch (s.toLowerCase().replace("-", "_").trim()) {
      case "json" -> JSON;
      case "json_array" -> JSON_ARRAY;
      case "csv" -> CSV;
      case "sql", "ddl" -> SQL;
      default -> throw new IllegalArgumentException(
        "Неизвестный формат: '" + s + "'. Допустимо: json, json-array, csv, sql");
    };
  }
}
