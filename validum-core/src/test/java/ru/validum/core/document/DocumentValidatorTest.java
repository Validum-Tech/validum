package ru.validum.core.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Проверка документов (DocumentValidator)")
class DocumentValidatorTest {

  private static final LocalDate ISSUE = LocalDate.of(2020, 5, 12);

  private static IdentityDocument passport(String series, String number, String departmentCode) {
    return new IdentityDocument(
      DocumentType.PASSPORT_RF, series, number, "ГУ МВД России по г. Москве", ISSUE, null,
      departmentCode);
  }

  @Test
  @DisplayName("Корректный паспорт проходит")
  void validPassport() {
    assertThat(DocumentValidator.isValid(passport("45 10", "123456", "770-001"))).isTrue();
    assertThat(DocumentValidator.isValid(passport("4510", "123456", "770-001"))).isTrue();
  }

  @Test
  @DisplayName("Неверная длина или алфавит серии — отказ")
  void badSeries() {
    assertThat(DocumentValidator.isValid(passport("451", "123456", "770-001"))).isFalse();
    assertThat(DocumentValidator.isValid(passport("45 АБ", "123456", "770-001"))).isFalse();
  }

  @Test
  @DisplayName("Неверная длина номера — отказ")
  void badNumber() {
    assertThat(DocumentValidator.isValid(passport("45 10", "12345", "770-001"))).isFalse();
    assertThat(DocumentValidator.isValid(passport("45 10", "1234567", "770-001"))).isFalse();
  }

  @Test
  @DisplayName("Паспорт РФ без кода подразделения или с кривым кодом — отказ")
  void badDepartmentCode() {
    assertThat(DocumentValidator.isValid(passport("45 10", "123456", null))).isFalse();
    assertThat(DocumentValidator.isValid(passport("45 10", "123456", "770001"))).isFalse();
  }

  @Test
  @DisplayName("Дата окончания раньше выдачи — отказ")
  void expirationBeforeIssue() {
    IdentityDocument broken = new IdentityDocument(
      DocumentType.DRIVER_LICENSE, "4412", "123456", "ГИБДД 0215", ISSUE, ISSUE.minusDays(1), null);

    assertThat(DocumentValidator.isValid(broken)).isFalse();
  }

  @Test
  @DisplayName("null и документ без типа — отказ")
  void nullSafety() {
    assertThat(DocumentValidator.isValid(null)).isFalse();
  }
}
