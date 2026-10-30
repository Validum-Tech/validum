package ru.validum.core.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.document.DocumentType;
import ru.validum.core.document.IdentityDocument;

@DisplayName("ДУЛ в плоском CSV: три представления (DocumentsCsv)")
class DocumentsCsvTest {

  private static IdentityDocument passport() {
    return new IdentityDocument(
      DocumentType.PASSPORT_RF,
      "45 10",
      "123456",
      "ГУ МВД России по г. Москве",
      LocalDate.of(2015, 6, 1),
      null,
      "770-001");
  }

  private static IdentityDocument visa() {
    return new IdentityDocument(
      DocumentType.VISA,
      "10",
      "1234567",
      "МИД России",
      LocalDate.of(2023, 3, 15),
      LocalDate.of(2024, 3, 15),
      null);
  }

  @Test
  @DisplayName("inline: два документа переживают запись и чтение")
  void inlineRoundTrip() {
    List<IdentityDocument> documents = List.of(passport(), visa());

    String cell = DocumentsCsv.inline(documents);

    assertThat(cell).contains("\n");
    assertThat(DocumentsCsv.parseInline(cell)).isEqualTo(documents);
  }

  @Test
  @DisplayName("inline: пустой список даёт пустую ячейку")
  void inlineEmpty() {
    assertThat(DocumentsCsv.inline(List.of())).isEmpty();
    assertThat(DocumentsCsv.parseInline("")).isEmpty();
  }

  @Test
  @DisplayName("inline: разделитель внутри поля экранируется")
  void inlineEscapesSeparator() {
    IdentityDocument tricky = new IdentityDocument(
      DocumentType.PASSPORT_RF,
      "45 10",
      "123456",
      "ГУ МВД \\ отдел | 2",
      LocalDate.of(2015, 6, 1),
      null,
      "770-001");

    String cell = DocumentsCsv.inline(List.of(tricky));

    assertThat(DocumentsCsv.parseInline(cell)).containsExactly(tricky);
  }

  @Test
  @DisplayName("json: два документа переживают запись и чтение")
  void jsonRoundTrip() {
    List<IdentityDocument> documents = List.of(passport(), visa());

    String cell = DocumentsCsv.json(documents);

    assertThat(cell).startsWith("[{").endsWith("}]");
    assertThat(DocumentsCsv.parseJson(cell)).isEqualTo(documents);
  }

  @Test
  @DisplayName("json: отсутствующий срок действия пишется как null")
  void jsonNullExpiration() {
    String cell = DocumentsCsv.json(List.of(passport()));

    assertThat(cell).contains("\"expirationDate\":null");
    assertThat(DocumentsCsv.parseJson(cell).get(0).expirationDate()).isNull();
  }

  @Test
  @DisplayName("json: кавычки в тексте экранируются")
  void jsonEscapesQuotes() {
    IdentityDocument tricky = new IdentityDocument(
      DocumentType.PASSPORT_RF,
      "45 10",
      "123456",
      "ГУ \"МВД\"",
      LocalDate.of(2015, 6, 1),
      null,
      "770-001");

    assertThat(DocumentsCsv.parseJson(DocumentsCsv.json(List.of(tricky))))
      .containsExactly(tricky);
  }

  @Test
  @DisplayName("parse: JSON распознаётся по квадратной скобке")
  void parseDetectsJson() {
    List<IdentityDocument> documents = List.of(passport());

    assertThat(DocumentsCsv.parse(DocumentsCsv.json(documents))).isEqualTo(documents);
  }

  @Test
  @DisplayName("parse: мини-таблица распознаётся по отсутствию скобки")
  void parseDetectsInline() {
    List<IdentityDocument> documents = List.of(passport());

    assertThat(DocumentsCsv.parse(DocumentsCsv.inline(documents))).isEqualTo(documents);
  }

  @Test
  @DisplayName("parse: пустая ячейка — пустой список")
  void parseBlank() {
    assertThat(DocumentsCsv.parse(null)).isEmpty();
    assertThat(DocumentsCsv.parse("  ")).isEmpty();
  }

  @Test
  @DisplayName("Неизвестный вид документа отклоняется")
  void unknownType() {
    assertThatThrownBy(() -> DocumentsCsv.parseInline("XX|45 10|123456|ГУ МВД|2015-06-01||770-001"))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("Неверное число полей отклоняется с понятным текстом")
  void wrongFieldCount() {
    assertThatThrownBy(() -> DocumentsCsv.parseInline("21|45 10|123456"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Ожидалось 7 полей");
  }

  @Test
  @DisplayName("split: таблица документов собирается обратно по profileKey")
  void splitTableRoundTrip() {
    String key = "500100732259";
    List<IdentityDocument> documents = List.of(passport(), visa());

    String csv = CsvExporter.toCsv(DocumentsCsv.toRows(key, documents));
    Map<String, List<IdentityDocument>> grouped = DocumentsCsv.groupRows(CsvImporter.parse(csv));

    assertThat(grouped).containsOnlyKeys(key);
    assertThat(grouped.get(key)).isEqualTo(documents);
  }

  @Test
  @DisplayName("split: нет profileKey — понятная ошибка")
  void splitWithoutKey() {
    Map<String, String> row = Map.of("spdulCode", "21", "issueDate", "2015-06-01");

    assertThatThrownBy(() -> DocumentsCsv.groupRows(List.of(row)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("profileKey");
  }
}
