package ru.validum.core.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Генератор документов (DocumentGenerator)")
class DocumentGeneratorTest {

  private final DocumentGenerator generator = new DocumentGenerator(new Random(42));

  @Test
  @DisplayName("Документ любого вида проходит проверку маски")
  void allTypesAreValid() {
    for (DocumentType type : DocumentType.values()) {
      for (int i = 0; i < 25; i++) {
        IdentityDocument document = generator.generate(type);
        assertThat(DocumentValidator.isValid(document))
          .as("вид %s, документ %s", type, document.seriesAndNumber())
          .isTrue();
      }
    }
  }

  @Test
  @DisplayName("Паспорт РФ: серия двумя группами, код подразделения, срок по возрасту")
  void passportRf() {
    IdentityDocument document = generator.generate(DocumentType.PASSPORT_RF);

    assertThat(document.series()).matches("\\d{2} \\d{2}");
    assertThat(document.number()).matches("\\d{6}");
    assertThat(document.departmentCode()).matches("\\d{3}-\\d{3}");
    assertThat(document.expirationDate()).isNull();
  }

  @Test
  @DisplayName("Военный билет: серия из двух заглавных букв")
  void militaryId() {
    IdentityDocument document = generator.generate(DocumentType.MILITARY_ID);

    assertThat(document.series()).matches("[А-ЯЁ]{2}");
    assertThat(document.number()).matches("\\d{7}");
    assertThat(document.expirationDate()).isNull();
  }

  @Test
  @DisplayName("Загранпаспорт: серия биометрической линейки, срок 10 лет")
  void foreignPassport() {
    IdentityDocument document = generator.generate(DocumentType.FOREIGN_PASSPORT);

    assertThat(document.series()).isIn("71", "72", "73", "75", "76", "79");
    assertThat(document.expirationDate()).isEqualTo(document.issueDate().plusYears(10));
  }

  @Test
  @DisplayName("ВНЖ: серия 82 или 00, бессрочный")
  void residencePermit() {
    IdentityDocument document = generator.generate(DocumentType.RESIDENCE_PERMIT);

    assertThat(document.series()).isIn("82", "00");
    assertThat(document.number()).matches("\\d{7}");
    assertThat(document.expirationDate()).isNull();
  }

  @Test
  @DisplayName("РВП: номер 6 цифр, срок 3 года")
  void temporaryResidence() {
    IdentityDocument document = generator.generate(DocumentType.TEMPORARY_RESIDENCE);

    assertThat(document.number()).matches("\\d{6}");
    assertThat(document.expirationDate()).isEqualTo(document.issueDate().plusYears(3));
  }

  @Test
  @DisplayName("Одинаковый seed даёт одинаковые документы")
  void deterministic() {
    DocumentGenerator first = new DocumentGenerator(new Random(7));
    DocumentGenerator second = new DocumentGenerator(new Random(7));

    assertThat(first.generate(DocumentType.PASSPORT_RF))
      .isEqualTo(second.generate(DocumentType.PASSPORT_RF));
  }
}
