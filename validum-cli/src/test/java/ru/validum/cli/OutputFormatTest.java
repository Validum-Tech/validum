package ru.validum.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("OutputFormat — парсинг формата (P3.1b)")
class OutputFormatTest {

  @Test
  @DisplayName("json → JSON")
  void json() {
    assertThat(OutputFormat.fromString("json")).isEqualTo(OutputFormat.JSON);
  }

  @Test
  @DisplayName("json-array → JSON_ARRAY (дефис приводится к подчёркиванию)")
  void jsonArray() {
    assertThat(OutputFormat.fromString("json-array")).isEqualTo(OutputFormat.JSON_ARRAY);
  }

  @Test
  @DisplayName("csv → CSV")
  void csv() {
    assertThat(OutputFormat.fromString("csv")).isEqualTo(OutputFormat.CSV);
  }

  @ParameterizedTest
  @ValueSource(strings = {"csv-semicolon", "csv_semicolon", "csv-excel", "csv_excel", "CSV-SEMICOLON"})
  @DisplayName("csv-semicolon / csv-excel → CSV_SEMICOLON (алиасы для Excel)")
  void csvSemicolon(String input) {
    assertThat(OutputFormat.fromString(input)).isEqualTo(OutputFormat.CSV_SEMICOLON);
  }

  @ParameterizedTest
  @ValueSource(strings = {"JSON", "Json", "CSV", "Csv", "json-array", "JSON-ARRAY"})
  @DisplayName("Регистр не важен")
  void caseInsensitive(String input) {
    assertThat(OutputFormat.fromString(input)).isNotNull();
  }

  @Test
  @DisplayName("Неизвестный формат → IllegalArgumentException с понятным сообщением")
  void unknownFormat() {
    assertThatThrownBy(() -> OutputFormat.fromString("xml"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("xml")
      .hasMessageContaining("json, json-array, csv");
  }

  @Test
  @DisplayName("null / пустая строка → IllegalArgumentException")
  void nullOrEmpty() {
    assertThatThrownBy(() -> OutputFormat.fromString(null))
      .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> OutputFormat.fromString(""))
      .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> OutputFormat.fromString("  "))
      .isInstanceOf(IllegalArgumentException.class);
  }
}
