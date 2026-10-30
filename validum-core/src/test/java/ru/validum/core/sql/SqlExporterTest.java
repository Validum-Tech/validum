package ru.validum.core.sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SQL-экспорт (SqlExporter)")
class SqlExporterTest {

  private static Map<String, String> row() {
    Map<String, String> row = new LinkedHashMap<>();
    row.put("inn", "7707083893");
    row.put("fullName", "Иванов' Олег");
    row.put("age", "42");
    row.put("terrorist", "false");
    row.put("blocked", "true");
    return row;
  }

  @Test
  @DisplayName("PostgreSQL: DDL с кавычками и типами")
  void postgresDdl() {
    String ddl = SqlExporter.createTable("clients", row(), SqlDialect.POSTGRESQL);

    assertThat(ddl)
      .startsWith("CREATE TABLE \"clients\" (")
      .contains("\"inn\" TEXT")
      .contains("\"age\" INTEGER")
      .contains("\"terrorist\" BOOLEAN")
      .contains("\"blocked\" BOOLEAN")
      .endsWith(");");
  }

  @Test
  @DisplayName("MySQL: обратные кавычки и TINYINT(1)")
  void mysqlDdl() {
    String ddl = SqlExporter.createTable("clients", row(), SqlDialect.MYSQL);

    assertThat(ddl)
      .contains("CREATE TABLE `clients`")
      .contains("`inn` VARCHAR(400)")
      .contains("`blocked` TINYINT(1)");
  }

  @Test
  @DisplayName("Oracle: VARCHAR2 и NUMBER")
  void oracleDdl() {
    String ddl = SqlExporter.createTable("clients", row(), SqlDialect.ORACLE);

    assertThat(ddl)
      .contains("\"inn\" VARCHAR2(400)")
      .contains("\"age\" NUMBER(10)")
      .contains("\"blocked\" NUMBER(1)");
  }

  @Test
  @DisplayName("Логические литералы зависят от диалекта")
  void booleanLiterals() {
    assertThat(SqlExporter.insert("t", row(), SqlDialect.POSTGRESQL))
      .contains("FALSE")
      .contains("TRUE");
    assertThat(SqlExporter.insert("t", row(), SqlDialect.MYSQL)).contains("0, 1)");
  }

  @Test
  @DisplayName("Апостроф в строке удваивается")
  void escapesQuote() {
    assertThat(SqlExporter.insert("t", row(), SqlDialect.POSTGRESQL))
      .contains("'Иванов'' Олег'");
  }

  @Test
  @DisplayName("Число возраста пишется без кавычек, ИНН — строкой")
  void numbersVsStrings() {
    String insert = SqlExporter.insert("t", row(), SqlDialect.POSTGRESQL);

    assertThat(insert).contains("'7707083893'").contains(", 42,");
  }

  @Test
  @DisplayName("null становится NULL, пустая строка — ''")
  void nulls() {
    Map<String, String> row = new LinkedHashMap<>();
    row.put("a", null);
    row.put("b", "");

    assertThat(SqlExporter.insert("t", row, SqlDialect.POSTGRESQL))
      .contains("NULL")
      .contains("''");
  }

  @Test
  @DisplayName("Скрипт: один DDL и по INSERT на строку")
  void scriptShape() {
    String script = SqlExporter.script("t", List.of(row(), row()), SqlDialect.POSTGRESQL, true);

    assertThat(script.split("CREATE TABLE")).hasSize(2);
    assertThat(script.split("INSERT INTO")).hasSize(3);
  }

  @Test
  @DisplayName("--no-ddl оставляет только INSERT")
  void withoutDdl() {
    String script = SqlExporter.script("t", List.of(row()), SqlDialect.POSTGRESQL, false);

    assertThat(script).doesNotContain("CREATE TABLE").contains("INSERT INTO \"t\"");
  }

  @Test
  @DisplayName("Пустой набор даёт пустой скрипт")
  void emptyRows() {
    assertThat(SqlExporter.script("t", List.of(), SqlDialect.POSTGRESQL, true)).isEmpty();
  }

  @Test
  @DisplayName("Диалект разбирается по алиасам")
  void dialectAliases() {
    assertThat(SqlDialect.fromString("postgres")).isEqualTo(SqlDialect.POSTGRESQL);
    assertThat(SqlDialect.fromString("PG")).isEqualTo(SqlDialect.POSTGRESQL);
    assertThat(SqlDialect.fromString("ora")).isEqualTo(SqlDialect.ORACLE);
    assertThat(SqlDialect.fromString("mariadb")).isEqualTo(SqlDialect.MYSQL);
    assertThat(SqlDialect.fromString(null)).isEqualTo(SqlDialect.POSTGRESQL);
    assertThatThrownBy(() -> SqlDialect.fromString("mssql"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Неизвестный диалект");
  }
}
