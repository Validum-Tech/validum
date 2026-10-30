package ru.validum.core.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.common.Country;
import ru.validum.core.document.DocumentType;
import ru.validum.core.document.DocumentValidator;
import ru.validum.core.name.Gender;

@DisplayName("Документы ДУЛ в профиле физлица")
class IndividualProfileDocumentsTest {

  @Test
  @DisplayName("По умолчанию документов нет")
  void noDocumentsByDefault() {
    IndividualProfile profile =
      IndividualProfileGenerator.builder().seed(1L).build().generate();

    assertThat(profile.documents()).isEmpty();
  }

  @Test
  @DisplayName("Паспорт РФ и загранпаспорт генерируются и проходят валидатор")
  void passportRfAndForeign() {
    IndividualProfile profile = IndividualProfileGenerator.builder()
      .seed(2L)
      .age(30)
      .gender(Gender.MALE)
      .documents(DocumentType.PASSPORT_RF, DocumentType.FOREIGN_PASSPORT)
      .build()
      .generate();

    assertThat(profile.documents()).hasSize(2);
    assertThat(profile.documents())
      .allSatisfy(document -> assertThat(DocumentValidator.isValid(document)).isTrue());
  }

  @Test
  @DisplayName("ВНЖ у нерезидента допустим")
  void residencePermitForNonResident() {
    IndividualProfile profile = IndividualProfileGenerator.builder()
      .seed(3L)
      .age(30)
      .residency(Residency.nonResident(Country.TJ))
      .documents(DocumentType.RESIDENCE_PERMIT)
      .build()
      .generate();

    assertThat(profile.documents()).hasSize(1);
    assertThat(profile.documents().get(0).type()).isEqualTo(DocumentType.RESIDENCE_PERMIT);
    assertThat(DocumentValidator.isValid(profile.documents().get(0))).isTrue();
  }

  @Test
  @DisplayName("ВНЖ у резидента — ошибка")
  void residencePermitForResidentRejected() {
    assertThatThrownBy(
        () ->
          IndividualProfileGenerator.builder()
            .seed(4L)
            .age(30)
            .documents(DocumentType.RESIDENCE_PERMIT)
            .build()
            .generate())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("только для нерезидента");
  }

  @Test
  @DisplayName("Военный билет женщине — ошибка")
  void militaryIdForFemaleRejected() {
    assertThatThrownBy(
        () ->
          IndividualProfileGenerator.builder()
            .seed(5L)
            .age(25)
            .gender(Gender.FEMALE)
            .documents(DocumentType.MILITARY_ID)
            .build()
            .generate())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("только для мужчин");
  }

  @Test
  @DisplayName("Военный билет в 30 лет — ошибка призывного возраста")
  void militaryIdAfterConscriptionAgeRejected() {
    assertThatThrownBy(
        () ->
          IndividualProfileGenerator.builder()
            .seed(6L)
            .age(30)
            .gender(Gender.MALE)
            .documents(DocumentType.MILITARY_ID)
            .build()
            .generate())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("призывной возраст");
  }
}
