package ru.validum.core.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Чтение CSV (CsvImporter)")
class CsvImporterTest {

  @Test
  @DisplayName("Простейший CSV с заголовком")
  void simpleCsv() {
    List<Map<String, String>> rows =
      CsvImporter.parse("inn,fullName\n1234567890,Иванов Иван Иванович\n");

    assertThat(rows).hasSize(1);
    assertThat(rows.get(0))
      .containsEntry("inn", "1234567890")
      .containsEntry("fullName", "Иванов Иван Иванович");
  }

  @Test
  @DisplayName("Несколько строк сохраняют порядок")
  void multipleRows() {
    List<Map<String, String>> rows = CsvImporter.parse("a,b\n1,2\n3,4\n");

    assertThat(rows).hasSize(2);
    assertThat(rows.get(0)).containsEntry("a", "1").containsEntry("b", "2");
    assertThat(rows.get(1)).containsEntry("a", "3").containsEntry("b", "4");
  }

  @Test
  @DisplayName("Поле в кавычках может содержать запятую")
  void quotedComma() {
    List<Map<String, String>> rows = CsvImporter.parse("a,b\n\"Иванов, Иван\",2\n");

    assertThat(rows.get(0)).containsEntry("a", "Иванов, Иван").containsEntry("b", "2");
  }

  @Test
  @DisplayName("Удвоенная кавычка внутри поля читается как одна")
  void quotedEscapedQuotes() {
    List<Map<String, String>> rows = CsvImporter.parse("a\n\"ООО \"\"Ромашка\"\"\"\n");

    assertThat(rows.get(0)).containsEntry("a", "ООО \"Ромашка\"");
  }

  @Test
  @DisplayName("Перенос строки внутри поля в кавычках не режет строку")
  void quotedNewline() {
    List<Map<String, String>> rows = CsvImporter.parse("a,b\n\"строка1\nстрока2\",2\n");

    assertThat(rows).hasSize(1);
    assertThat(rows.get(0)).containsEntry("a", "строка1\nстрока2").containsEntry("b", "2");
  }

  @Test
  @DisplayName("BOM в начале файла отбрасывается")
  void bomRemoved() {
    List<Map<String, String>> rows = CsvImporter.parse("\uFEFFa,b\n1,2\n");

    assertThat(rows.get(0)).containsEntry("a", "1").containsEntry("b", "2");
  }

  @Test
  @DisplayName("CRLF и одиночный LF равнозначны")
  void crlfAndLf() {
    List<Map<String, String>> crlf = CsvImporter.parse("a,b\r\n1,2\r\n");
    List<Map<String, String>> lf = CsvImporter.parse("a,b\n1,2\n");

    assertThat(crlf).isEqualTo(lf);
  }

  @Test
  @DisplayName("Пустые строки пропускаются")
  void blankLinesSkipped() {
    List<Map<String, String>> rows = CsvImporter.parse("a,b\n1,2\n\n\n3,4\n");

    assertThat(rows).hasSize(2);
  }

  @Test
  @DisplayName("Недостающие ячейки считаются пустыми")
  void missingCells() {
    List<Map<String, String>> rows = CsvImporter.parse("a,b,c\n1\n");

    assertThat(rows.get(0))
      .containsEntry("a", "1")
      .containsEntry("b", "")
      .containsEntry("c", "");
  }

  @Test
  @DisplayName("Пустой вход даёт пустой список")
  void emptyInput() {
    assertThat(CsvImporter.parse("")).isEmpty();
  }

  @Test
  @DisplayName("readRows видит ячейки до отображения на заголовки")
  void readRowsDirect() {
    List<List<String>> rows = CsvImporter.readRows("a,b\n\"1,5\",2\n");

    assertThat(rows).hasSize(2);
    assertThat(rows.get(1)).containsExactly("1,5", "2");
  }

  @Test
  @DisplayName("Точка с запятой как разделитель: «дружелюбный» CSV")
  void semicolonDelimiter() {
    List<Map<String, String>> rows =
      CsvImporter.parse("ИНН;ФИО\n1234567890;Иванов Иван\n", ';');

    assertThat(rows).hasSize(1);
    assertThat(rows.get(0))
      .containsEntry("ИНН", "1234567890")
      .containsEntry("ФИО", "Иванов Иван");
  }

  @Test
  @DisplayName("Запятая внутри кавычек не считается разделителем")
  void semicolonKeepsCommasInQuotes() {
    List<Map<String, String>> rows =
      CsvImporter.parse("inn;address\n500100732259;\"Москва, ул. Тверская\"\n", ';');

    assertThat(rows.get(0)).containsEntry("address", "Москва, ул. Тверская");
  }

  @Test
  @DisplayName("Разделитель определяется по заголовку, кавычки не путают")
  void detectsDelimiter() {
    assertThat(CsvImporter.detectDelimiter("inn;fullName;city")).isEqualTo(';');
    assertThat(CsvImporter.detectDelimiter("inn,fullName,city")).isEqualTo(',');
    assertThat(CsvImporter.detectDelimiter("\"a;b\",c")).isEqualTo(',');
  }

  @Test
  @DisplayName("Обёртка =\"…\" снимается, обычное значение не трогается")
  void unquotesFormula() {
    assertThat(CsvImporter.unquoteFormula("=\"500100732259\"")).isEqualTo("500100732259");
    assertThat(CsvImporter.unquoteFormula("500100732259")).isEqualTo("500100732259");
    assertThat(CsvImporter.unquoteFormula("")).isEmpty();
  }
}
