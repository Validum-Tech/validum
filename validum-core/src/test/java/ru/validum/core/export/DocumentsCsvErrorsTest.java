package ru.validum.core.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.document.DocumentType;
import ru.validum.core.document.IdentityDocument;

@DisplayName("Разбор документов: ошибочные ячейки")
class DocumentsCsvErrorsTest {

  @Test
  @DisplayName("Пустая ячейка — пустой список")
  void blankCell() {
    assertThat(DocumentsCsv.parse(null)).isEmpty();
    assertThat(DocumentsCsv.parse("   ")).isEmpty();
  }

  @Test
  @DisplayName("Минимальный JSON: достаточно вида и даты выдачи")
  void minimalJson() {
    List<IdentityDocument> docs =
      DocumentsCsv.parse("[{\"spdulCode\":\"21\",\"issueDate\":\"2020-01-01\"}]");

    assertThat(docs).hasSize(1);
    assertThat(docs.get(0).type()).isEqualTo(DocumentType.PASSPORT_RF);
    assertThat(docs.get(0).series()).isEmpty();
    assertThat(docs.get(0).number()).isEmpty();
    assertThat(docs.get(0).issueDate()).isEqualTo(LocalDate.of(2020, 1, 1));
    assertThat(docs.get(0).expirationDate()).isNull();
  }

  @Test
  @DisplayName("Обрезанный JSON — понятное исключение")
  void truncatedJson() {
    assertThatThrownBy(() -> DocumentsCsv.parse("["))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Некорректный JSON документов");
  }

  @Test
  @DisplayName("Объект без даты выдачи — исключение")
  void jsonWithoutIssueDate() {
    assertThatThrownBy(() -> DocumentsCsv.parse("[{}]"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("нет даты выдачи");
  }

  @Test
  @DisplayName("Дата не в ISO — исключение с исходным значением")
  void badDateInJson() {
    assertThatThrownBy(() -> DocumentsCsv.parse("[{\"spdulCode\":\"21\",\"issueDate\":\"31.12.2020\"}]"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Некорректная дата выдачи документа")
      .hasMessageContaining("31.12.2020");
  }

  @Test
  @DisplayName("Мусор после массива — исключение")
  void trailingGarbage() {
    assertThatThrownBy(() -> DocumentsCsv.parse("[{\"spdulCode\":\"21\",\"issueDate\":\"2020-01-01\"}] x"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Лишние символы после JSON-массива");
  }

  @Test
  @DisplayName("Неизвестная escape-последовательность в строке — исключение")
  void badEscape() {
    assertThatThrownBy(() -> DocumentsCsv.parse("[{\"spdulCode\":\"\\q\"}]"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Некорректный JSON документов");
  }

  @Test
  @DisplayName("Число вместо строки — исключение")
  void numberInsteadOfString() {
    assertThatThrownBy(() -> DocumentsCsv.parse("[{\"spdulCode\":21}]"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Некорректный JSON документов");
  }

  @Test
  @DisplayName("Мини-таблица с неверным числом полей — исключение")
  void inlineWrongFieldCount() {
    assertThatThrownBy(() -> DocumentsCsv.parseInline("21|45 10"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Ожидалось 7 полей");
  }

  @Test
  @DisplayName("Мини-таблица без даты выдачи — исключение")
  void inlineWithoutIssueDate() {
    assertThatThrownBy(() -> DocumentsCsv.parseInline("21|45 10|123456|ГУ МВД|||770-001"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("нет даты выдачи");
  }

  @Test
  @DisplayName("Неизвестный вид документа в мини-таблице — исключение")
  void inlineUnknownType() {
    assertThatThrownBy(() -> DocumentsCsv.parseInline("XX|1|2||2020-01-01||"))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("Пустой вид документа в мини-таблице — исключение")
  void inlineBlankType() {
    assertThatThrownBy(() -> DocumentsCsv.parseInline("|1|2||2020-01-01||"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Пустой вид документа");
  }
}
