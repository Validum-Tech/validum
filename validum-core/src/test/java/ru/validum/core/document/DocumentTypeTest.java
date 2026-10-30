package ru.validum.core.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Виды документов (DocumentType)")
class DocumentTypeTest {

  @Test
  @DisplayName("Одиннадцать видов, коды СПДУЛ уникальны")
  void codesAreUnique() {
    List<String> codes = Arrays.stream(DocumentType.values())
      .map(DocumentType::spdulCode)
      .filter(code -> code != null)
      .toList();

    assertThat(DocumentType.values()).hasSize(11);
    assertThat(codes).doesNotHaveDuplicates().hasSize(9);
  }

  @Test
  @DisplayName("Код СПДУЛ находит тип")
  void fromCode() {
    assertThat(DocumentType.fromCode("21")).isEqualTo(DocumentType.PASSPORT_RF);
    assertThat(DocumentType.fromCode("07")).isEqualTo(DocumentType.MILITARY_ID);
    assertThat(DocumentType.fromCode("27")).isEqualTo(DocumentType.MILITARY_ID_RESERVE);
    assertThat(DocumentType.fromCode("10")).isEqualTo(DocumentType.PASSPORT_FOREIGN);
  }

  @Test
  @DisplayName("Неизвестный код СПДУЛ — исключение")
  void unknownCode() {
    assertThatThrownBy(() -> DocumentType.fromCode("99"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("99");
  }

  @Test
  @DisplayName("У визы и водительского удостоверения кода СПДУЛ нет")
  void noSpdulCode() {
    assertThat(DocumentType.VISA.spdulCode()).isNull();
    assertThat(DocumentType.DRIVER_LICENSE.spdulCode()).isNull();
  }

  @Test
  @DisplayName("Маски паспорта РФ и военного билета")
  void masks() {
    assertThat(DocumentType.PASSPORT_RF.seriesLength()).isEqualTo(4);
    assertThat(DocumentType.PASSPORT_RF.numberLength()).isEqualTo(6);
    assertThat(DocumentType.PASSPORT_RF.groupedSeries()).isTrue();
    assertThat(DocumentType.PASSPORT_RF.hasDepartmentCode()).isTrue();

    assertThat(DocumentType.MILITARY_ID.seriesLetters()).isTrue();
    assertThat(DocumentType.MILITARY_ID.seriesLength()).isEqualTo(2);
    assertThat(DocumentType.MILITARY_ID.numberLength()).isEqualTo(7);
    assertThat(DocumentType.MILITARY_ID.validityYears()).isNull();
  }
}
