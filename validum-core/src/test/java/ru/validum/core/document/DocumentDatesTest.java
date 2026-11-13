package ru.validum.core.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.bank.BikSource;
import ru.validum.core.bank.BikSources;
import ru.validum.core.name.Gender;
import ru.validum.core.profile.IndividualProfile;
import ru.validum.core.profile.IndividualProfileGenerator;
import ru.validum.core.profile.Residency;

/**
 * Даты документов привязаны к дате рождения: паспорт не может быть выдан пятилетнему,
 * права — семнадцатилетнему, а срок действия не истёк на дату расчёта.
 */
class DocumentDatesTest {

  private static final LocalDate AS_OF = LocalDate.of(2026, 11, 1);
  private static final BikSource BIK = BikSources.resolve("src/test/resources/bik-sample.xml");
  private static final Residency RESIDENT = Residency.residentRU();

  private static DocumentGenerator generator() {
    return new DocumentGenerator(new Random(17));
  }

  @Test
  @DisplayName("Права: выдача не раньше 18 лет, срок 10 лет, на дату расчёта действуют")
  void driverLicenseRespectsAdulthood() {
    LocalDate birth = AS_OF.minusYears(40);
    IdentityDocument license = generator().generate(DocumentType.DRIVER_LICENSE, birth, AS_OF);

    assertThat(license.issueDate()).isBetween(birth.plusYears(18), AS_OF);
    assertThat(license.expirationDate()).isEqualTo(license.issueDate().plusYears(10));
    assertThat(license.expirationDate()).isAfter(AS_OF);
  }

  @Test
  @DisplayName("Военный билет: выдача только в призывном окне 18–27 лет")
  void militaryIdWithinConscriptionWindow() {
    LocalDate birth = AS_OF.minusYears(25);
    IdentityDocument military = generator().generate(DocumentType.MILITARY_ID, birth, AS_OF);

    assertThat(military.issueDate()).isBetween(birth.plusYears(18), birth.plusYears(27));
    assertThat(military.expirationDate()).isNull();
  }

  @Test
  @DisplayName("Срочный документ не может быть просрочен на дату расчёта")
  void expiringDocumentIsNotExpired() {
    LocalDate birth = AS_OF.minusYears(30);
    IdentityDocument foreign = generator().generate(DocumentType.FOREIGN_PASSPORT, birth, AS_OF);

    assertThat(foreign.expirationDate()).isAfter(AS_OF);
    assertThat(foreign.issueDate()).isBetween(AS_OF.minusYears(10), AS_OF);
  }

  @Test
  @DisplayName("Профиль 20 лет: паспорт РФ выдан не раньше 14 лет")
  void profilePassportIssuedAfterFourteen() {
    IndividualProfile profile =
      IndividualProfileGenerator.builder()
        .seed(5)
        .bikSource(BIK)
        .asOf(AS_OF)
        .age(20)
        .documents(List.of(DocumentType.PASSPORT_RF))
        .build()
        .generate();

    IdentityDocument passport = profile.documents().get(0);
    assertThat(passport.issueDate())
      .isAfterOrEqualTo(profile.birthDate().plusYears(14))
      .isBeforeOrEqualTo(AS_OF);
  }

  @Test
  @DisplayName("Правила не пропускают права до 18 лет")
  void rulesRejectDriverLicenseForMinor() {
    assertThatThrownBy(() ->
        DocumentRules.validate(List.of(DocumentType.DRIVER_LICENSE), Gender.MALE, 17, RESIDENT))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Водительское удостоверение — с 18 лет");
  }
}
