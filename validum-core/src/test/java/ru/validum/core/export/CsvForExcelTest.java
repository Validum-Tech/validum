package ru.validum.core.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CSV для человека: «;», русские заголовки, BOM")
class CsvForExcelTest {

  private static Map<String, String> row() {
    Map<String, String> m = new LinkedHashMap<>();
    m.put("fullName", "Иванов Иван Иванович");
    m.put("inn", "500100732259");
    m.put("bankName", "ПАО Сбербанк");
    return m;
  }

  @Test
  @DisplayName("Разделитель «;» и русские заголовки")
  void semicolonAndRussianHeaders() {
    String csv = CsvExporter.toCsv(List.of(row()), ';', true);

    assertThat(csv).startsWith("\uFEFF");
    assertThat(csv).contains("ФИО;ИНН;Банк");
    assertThat(csv).contains("Иванов Иван Иванович;500100732259;ПАО Сбербанк");
  }

  @Test
  @DisplayName("Машинный CSV остаётся с «,» и английскими заголовками")
  void machineCsvUnchanged() {
    String csv = CsvExporter.toCsv(List.of(row()));

    assertThat(csv).contains("fullName,inn,bankName");
    assertThat(csv).doesNotContain("ФИО");
  }

  @Test
  @DisplayName("Запятая внутри значения не ломает «;»-CSV")
  void commaInsideValueIsNotQuoted() {
    Map<String, String> m = new LinkedHashMap<>();
    m.put("registrationFormatted", "г Москва, ул Тверская, д 1");

    String csv = CsvExporter.toCsv(List.of(m), ';', true);

    assertThat(csv).contains("г Москва, ул Тверская, д 1");
    assertThat(csv).doesNotContain("\"г Москва");
  }

  @Test
  @DisplayName("Точка с запятой внутри значения экранируется кавычками")
  void semicolonInsideValueIsQuoted() {
    Map<String, String> m = new LinkedHashMap<>();
    m.put("name", "ООО \"Ромашка\"; филиал");

    String csv = CsvExporter.toCsv(List.of(m), ';', false);

    assertThat(csv).contains("\"ООО \"\"Ромашка\"\"; филиал\"");
  }

  @Test
  @DisplayName("Неизвестная колонка остаётся как есть")
  void unknownColumnKept() {
    assertThat(CsvExporter.russianHeader("myOwnColumn")).isEqualTo("myOwnColumn");
    assertThat(CsvExporter.russianHeader("inn")).isEqualTo("ИНН");
  }
}
