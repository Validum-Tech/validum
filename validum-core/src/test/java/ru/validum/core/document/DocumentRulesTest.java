package ru.validum.core.document;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.common.Country;
import ru.validum.core.name.Gender;
import ru.validum.core.profile.Residency;

@DisplayName("Правила совместимости документов (DocumentRules)")
class DocumentRulesTest {

  private static final Residency RESIDENT = Residency.residentRU();
  private static final Residency NON_RESIDENT = Residency.nonResident(Country.TJ);

  @Test
  @DisplayName("Пустой набор допустим")
  void emptySetAllowed() {
    assertThatCode(() -> DocumentRules.validate(List.of(), Gender.MALE, 30, RESIDENT))
      .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("Паспорт РФ и загранпаспорт вместе допустимы")
  void rfAndForeignTogether() {
    assertThatCode(
        () ->
          DocumentRules.validate(
            List.of(DocumentType.PASSPORT_RF, DocumentType.FOREIGN_PASSPORT),
            Gender.FEMALE,
            30,
            RESIDENT))
      .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("Загранпаспорт без паспорта РФ запрещён")
  void foreignWithoutRfRejected() {
    assertThatThrownBy(
        () ->
          DocumentRules.validate(List.of(DocumentType.FOREIGN_PASSPORT), Gender.MALE, 30, RESIDENT))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("требует паспорт РФ");
  }

  @Test
  @DisplayName("ВНЖ у резидента запрещён, у нерезидента допустим")
  void residencePermitNeedsNonResident() {
    assertThatThrownBy(
        () -> DocumentRules.validate(List.of(DocumentType.RESIDENCE_PERMIT), Gender.MALE, 30, RESIDENT))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("только для нерезидента");

    assertThatCode(
        () ->
          DocumentRules.validate(
            List.of(DocumentType.RESIDENCE_PERMIT), Gender.MALE, 30, NON_RESIDENT))
      .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("Военный билет — только мужчине")
  void militaryIdNeedsMale() {
    assertThatThrownBy(
        () -> DocumentRules.validate(List.of(DocumentType.MILITARY_ID), Gender.FEMALE, 25, RESIDENT))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("только для мужчин");
  }

  @Test
  @DisplayName("Военный билет рядового — только призывной возраст 18–27")
  void militaryIdAgeWindow() {
    assertThatCode(
        () -> DocumentRules.validate(List.of(DocumentType.MILITARY_ID), Gender.MALE, 25, RESIDENT))
      .doesNotThrowAnyException();

    assertThatThrownBy(
        () -> DocumentRules.validate(List.of(DocumentType.MILITARY_ID), Gender.MALE, 30, RESIDENT))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("призывной возраст");
  }

  @Test
  @DisplayName("Водительское удостоверение — с 18 лет")
  void driverLicenseNeedsAdult() {
    assertThatThrownBy(
        () ->
          DocumentRules.validate(List.of(DocumentType.DRIVER_LICENSE), Gender.MALE, 17, RESIDENT))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("с 18 лет");

    assertThatCode(
        () ->
          DocumentRules.validate(List.of(DocumentType.DRIVER_LICENSE), Gender.MALE, 18, RESIDENT))
      .doesNotThrowAnyException();
  }
}
