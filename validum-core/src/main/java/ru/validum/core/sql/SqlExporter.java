package ru.validum.core.sql;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Экспорт плоских строк (например, CSV-строк профилей) в SQL-скрипт: {@code CREATE TABLE} + {@code
 * INSERT}.
 *
 * <p>Структура таблицы плоская — та же, что у CSV. Типы выводятся по имени колонки:
 *
 * <ul>
 *   <li>{@code terrorist}, {@code blocked} — логический тип диалекта;
 *   <li>{@code age} — целое;
 *   <li>всё остальное — строка (ИНН, СНИЛС, счёт и прочие сохраняют ведущие нули).
 * </ul>
 */
public final class SqlExporter {

  private static final Set<String> BOOLEAN_COLUMNS = Set.of("terrorist", "blocked");
  private static final Set<String> INT_COLUMNS = Set.of("age");

  private SqlExporter() {}

  /** Полный скрипт: DDL (если {@code withDdl}) и по одному INSERT на строку. */
  public static String script(
    String table, List<Map<String, String>> rows, SqlDialect dialect, boolean withDdl) {
    Objects.requireNonNull(table, "table");
    Objects.requireNonNull(rows, "rows");
    Objects.requireNonNull(dialect, "dialect");
    if (rows.isEmpty()) {
      return "";
    }

    StringBuilder out = new StringBuilder();
    if (withDdl) {
      out.append(createTable(table, rows.get(0), dialect)).append("\n\n");
    }
    for (Map<String, String> row : rows) {
      out.append(insert(table, row, dialect)).append('\n');
    }
    return out.toString();
  }

  /** {@code CREATE TABLE} по колонкам образцовой строки. */
  public static String createTable(String table, Map<String, String> sample, SqlDialect dialect) {
    StringBuilder columns = new StringBuilder();
    for (Map.Entry<String, String> entry : sample.entrySet()) {
      if (!columns.isEmpty()) {
        columns.append(",\n  ");
      }
      columns
        .append(dialect.quote(entry.getKey()))
        .append(' ')
        .append(columnType(entry.getKey(), dialect));
    }
    return "CREATE TABLE " + dialect.quote(table) + " (\n  " + columns + "\n);";
  }

  /** Один {@code INSERT} со всеми колонками строки. */
  public static String insert(String table, Map<String, String> row, SqlDialect dialect) {
    StringBuilder names = new StringBuilder();
    StringBuilder values = new StringBuilder();
    for (Map.Entry<String, String> entry : row.entrySet()) {
      if (!names.isEmpty()) {
        names.append(", ");
        values.append(", ");
      }
      names.append(dialect.quote(entry.getKey()));
      values.append(literal(entry.getKey(), entry.getValue(), dialect));
    }
    return "INSERT INTO " + dialect.quote(table) + " (" + names + ") VALUES (" + values + ");";
  }

  /** Колонки в порядке первой строки (для внешних потребителей). */
  public static Map<String, String> blankRow(List<Map<String, String>> rows) {
    return rows.isEmpty() ? new LinkedHashMap<>() : rows.get(0);
  }

  private static String columnType(String column, SqlDialect dialect) {
    if (BOOLEAN_COLUMNS.contains(column)) {
      return dialect.booleanType();
    }
    if (INT_COLUMNS.contains(column)) {
      return dialect.intType();
    }
    return dialect.textType();
  }

  private static String literal(String column, String value, SqlDialect dialect) {
    if (value == null) {
      return "NULL";
    }
    if (BOOLEAN_COLUMNS.contains(column)) {
      return dialect.booleanLiteral(Boolean.parseBoolean(value));
    }
    if (INT_COLUMNS.contains(column) && value.matches("-?\\d{1,9}")) {
      return value;
    }
    return "'" + value.replace("'", "''") + "'";
  }
}
