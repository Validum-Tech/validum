package ru.validum.core.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Режим документов в CSV")
class DocumentsFormatTest {

  @Test
  @DisplayName("Пустое значение — inline по умолчанию")
  void blankMeansInline() {
    assertThat(DocumentsFormat.fromString(null)).isEqualTo(DocumentsFormat.INLINE);
    assertThat(DocumentsFormat.fromString("")).isEqualTo(DocumentsFormat.INLINE);
    assertThat(DocumentsFormat.fromString("   ")).isEqualTo(DocumentsFormat.INLINE);
  }

  @Test
  @DisplayName("Значения разбираются без учёта регистра и пробелов")
  void parsesKnownValues() {
    assertThat(DocumentsFormat.fromString("inline")).isEqualTo(DocumentsFormat.INLINE);
    assertThat(DocumentsFormat.fromString(" INLINE ")).isEqualTo(DocumentsFormat.INLINE);
    assertThat(DocumentsFormat.fromString("Json")).isEqualTo(DocumentsFormat.JSON);
    assertThat(DocumentsFormat.fromString("SPLIT")).isEqualTo(DocumentsFormat.SPLIT);
  }

  @Test
  @DisplayName("Неизвестный режим — исключение со списком допустимых")
  void unknownValueThrows() {
    assertThatThrownBy(() -> DocumentsFormat.fromString("xml"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Неизвестный режим документов")
      .hasMessageContaining("inline, json, split");
  }

  @Test
  @DisplayName("Всего три режима")
  void threeModes() {
    assertThat(DocumentsFormat.values()).hasSize(3);
  }
}
