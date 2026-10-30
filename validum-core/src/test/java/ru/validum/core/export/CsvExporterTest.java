package ru.validum.core.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CsvExporter — core-версия (P3.1a)")
class CsvExporterTest {

  @Test
  @DisplayName("Пустой список → только BOM")
  void emptyListReturnsOnlyBom() {
    assertThat(CsvExporter.toCsv(List.of())).isEqualTo("\uFEFF");
  }

  @Test
  @DisplayName("CSV начинается с UTF-8 BOM")
  void startsWithBom() {
    Map<String, String> row = new LinkedHashMap<>();
    row.put("a", "1");
    row.put("b", "2");
    assertThat(CsvExporter.toCsv(List.of(row))).startsWith("\uFEFF");
  }

  @Test
  @DisplayName("Заголовки берутся из ключей первой строки в порядке вставки")
  void headersFollowFirstRowKeyOrder() {
    Map<String, String> row = new LinkedHashMap<>();
    row.put("fullName", "Иванов Иван Иванович");
    row.put("inn", "500100732259");
    row.put("snils", "11223344595");

    String csv = CsvExporter.toCsv(List.of(row));
    String header = csv.lines().findFirst().orElse("");
    // \uFEFFfullName,inn,snils
    assertThat(header).endsWith("fullName,inn,snils");
  }

  @Test
  @DisplayName("Одна строка на одну map, заголовок + N строк данных")
  void oneLinePerRow() {
    Map<String, String> r1 = new LinkedHashMap<>();
    r1.put("x", "1");
    Map<String, String> r2 = new LinkedHashMap<>();
    r2.put("x", "2");

    String csv = CsvExporter.toCsv(List.of(r1, r2));
    // BOM + header + 2 data lines = 3 \r\n
    assertThat(csv.split("\r\n")).hasSize(3);
  }

  @Test
  @DisplayName("Экранирование: запятые, кавычки, переносы строк")
  void escapeSpecialCharacters() {
    Map<String, String> row = new LinkedHashMap<>();
    row.put("val", "Иванов, \"Иван\"\nвторая строка");
    String csv = CsvExporter.toCsv(List.of(row));
    assertThat(csv).contains("\"Иванов, \"\"Иван\"\"\nвторая строка\"");
  }

  @Test
  @DisplayName("escape() — запятая / кавычка / \\r / \\n → обёрнуто и удвоено")
  void escapeMethod() {
    assertThat(CsvExporter.escape("plain")).isEqualTo("plain");
    assertThat(CsvExporter.escape("a,b")).isEqualTo("\"a,b\"");
    assertThat(CsvExporter.escape("a\"b")).isEqualTo("\"a\"\"b\"");
    assertThat(CsvExporter.escape("a\nb")).isEqualTo("\"a\nb\"");
    assertThat(CsvExporter.escape("a\r\nb")).isEqualTo("\"a\r\nb\"");
  }

  @Test
  @DisplayName("escape() — пустая строка и null → пустая строка")
  void escapeNullOrEmpty() {
    assertThat(CsvExporter.escape("")).isEmpty();
    assertThat(CsvExporter.escape(null)).isEmpty();
  }

  @Test
  @DisplayName("Отсутствующая колонка в строке → пустое значение (не падает)")
  void missingColumnInRow() {
    Map<String, String> headerRow = new LinkedHashMap<>();
    headerRow.put("a", "1");
    headerRow.put("b", "2");

    Map<String, String> sparse = new LinkedHashMap<>();
    sparse.put("a", "1"); // b отсутствует

    String csv = CsvExporter.toCsv(List.of(headerRow, sparse));
    assertThat(csv).contains("1,2\r\n1,\r\n");
  }

  @Test
  @DisplayName("toBytes() — UTF-8 байты с BOM")
  void toBytesUtf8() {
    String csv = CsvExporter.toCsv(List.of(Map.of("k", "значение")));
    byte[] bytes = CsvExporter.toBytes(csv);

    // BOM = 0xEF 0xBB 0xBF
    assertThat(bytes[0] & 0xFF).isEqualTo(0xEF);
    assertThat(bytes[1] & 0xFF).isEqualTo(0xBB);
    assertThat(bytes[2] & 0xFF).isEqualTo(0xBF);
  }
}
