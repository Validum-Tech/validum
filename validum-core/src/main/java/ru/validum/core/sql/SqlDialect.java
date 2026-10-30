package ru.validum.core.sql;

/**
 * Диалект SQL для экспорта синтетических данных.
 *
 * <p>Различия, которые важны на практике: кавычки идентификаторов, логический тип и литералы,
 * строковый тип. Всё остальное (запятые, экранирование апострофов) одинаково.
 */
public enum SqlDialect {
  POSTGRESQL,
  ORACLE,
  MYSQL;

  /** Кавычки вокруг идентификатора: {@code "users"} или {@code `users`}. */
  public String quote(String identifier) {
    String escaped = identifier.replace("\"", "\"\"");
    return this == MYSQL ? "`" + identifier.replace("`", "``") + "`" : "\"" + escaped + "\"";
  }

  /** Тип для логических флагов: в Oracle до 23c своего BOOLEAN нет. */
  public String booleanType() {
    return switch (this) {
      case POSTGRESQL -> "BOOLEAN";
      case MYSQL -> "TINYINT(1)";
      case ORACLE -> "NUMBER(1)";
    };
  }

  /** Литерал логического значения. */
  public String booleanLiteral(boolean value) {
    return switch (this) {
      case POSTGRESQL -> value ? "TRUE" : "FALSE";
      case MYSQL, ORACLE -> value ? "1" : "0";
    };
  }

  /** Строковый тип достаточной длины. */
  public String textType() {
    return switch (this) {
      case POSTGRESQL -> "TEXT";
      case MYSQL -> "VARCHAR(400)";
      case ORACLE -> "VARCHAR2(400)";
    };
  }

  /** Целочисленный тип. */
  public String intType() {
    return this == ORACLE ? "NUMBER(10)" : "INTEGER";
  }

  public static SqlDialect fromString(String value) {
    if (value == null || value.isBlank()) {
      return POSTGRESQL;
    }
    return switch (value.toLowerCase().replace("-", "").trim()) {
      case "postgresql", "postgres", "pg" -> POSTGRESQL;
      case "oracle", "ora" -> ORACLE;
      case "mysql", "maria", "mariadb" -> MYSQL;
      default -> throw new IllegalArgumentException(
        "Неизвестный диалект: '" + value + "'. Допустимо: postgres, oracle, mysql");
    };
  }
}
