package ru.validum.core.profile;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.bank.BikSource;
import ru.validum.core.bank.BikSources;
import ru.validum.core.common.Country;
import ru.validum.core.document.DocumentType;
import ru.validum.core.document.IdentityDocument;
import ru.validum.core.passport.PassportType;

class NonResidentAddressTest {

  private static final BikSource BIK = BikSources.resolve("src/test/resources/bik-sample.xml");
  private static final LocalDate AS_OF = LocalDate.of(2026, 11, 1);

  @Test
  @DisplayName("Нерезидент: все три адреса в стране проживания, паспорт — иностранный")
  void nonResidentLivesAbroad() {
    IndividualProfile profile =
      IndividualProfileGenerator.builder()
        .seed(7)
        .bikSource(BIK)
        .asOf(AS_OF)
        .age(30)
        .residency(Residency.nonResident(Country.KZ))
        .documentsAuto()
        .build()
        .generate();

    assertThat(profile.residency().isResident()).isFalse();
    assertThat(profile.registrationAddress().country()).isEqualTo(Country.KZ);
    assertThat(profile.postAddress().country()).isEqualTo(Country.KZ);
    assertThat(profile.currentAddress().country()).isEqualTo(Country.KZ);
    assertThat(profile.registrationAddress().city()).isEqualTo("Алматы");
    assertThat(profile.registrationAddress().postalCode()).startsWith("05");
    assertThat(profile.passport().type()).isEqualTo(PassportType.FOREIGN);
    assertThat(profile.passport().country()).isEqualTo(Country.KZ);
  }

  @Test
  @DisplayName("Резидент РФ по умолчанию остаётся на КЛАДР")
  void residentKeepsRussianAddress() {
    IndividualProfile profile =
      IndividualProfileGenerator.builder()
        .seed(8)
        .bikSource(BIK)
        .asOf(AS_OF)
        .age(30)
        .build()
        .generate();

    assertThat(profile.residency().isResident()).isTrue();
    assertThat(profile.registrationAddress().country()).isEqualTo(Country.RU);
    assertThat(profile.registrationAddress().city()).isNotBlank();
  }

  @Test
  @DisplayName("Явный --region важнее гражданства: нерезидент живёт в Москве")
  void regionWinsOverCitizenship() {
    IndividualProfile profile =
      IndividualProfileGenerator.builder()
        .seed(10)
        .bikSource(BIK)
        .asOf(AS_OF)
        .age(35)
        .region(ru.validum.core.common.Region.MOSCOW)
        .residency(Residency.nonResident(Country.TJ))
        .build()
        .generate();

    assertThat(profile.residency().isResident()).isFalse();
    assertThat(profile.registrationAddress().country()).isEqualTo(Country.RU);
    assertThat(profile.registrationAddress().region())
      .isEqualTo(ru.validum.core.common.Region.MOSCOW.regionName());
    assertThat(profile.passport().type()).isEqualTo(PassportType.FOREIGN);
    assertThat(profile.passport().country()).isEqualTo(Country.TJ);
  }

  @Test
  @DisplayName("Нерезидент из Беларуси получает белорусский ИНН-вид и минский адрес")
  void belarus() {
    IndividualProfile profile =
      IndividualProfileGenerator.builder()
        .seed(9)
        .bikSource(BIK)
        .asOf(AS_OF)
        .age(45)
        .residency(Residency.nonResident(Country.BY))
        .documentsAuto()
        .build()
        .generate();

    assertThat(profile.registrationAddress().city()).isEqualTo("Минск");
    assertThat(profile.registrationAddress().postalCode()).startsWith("22");
    assertThat(profile.documents()).extracting(IdentityDocument::type)
      .contains(DocumentType.PASSPORT_FOREIGN);
  }
}
