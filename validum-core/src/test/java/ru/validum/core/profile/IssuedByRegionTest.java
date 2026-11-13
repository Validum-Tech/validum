package ru.validum.core.profile;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.bank.BikSource;
import ru.validum.core.bank.BikSources;
import ru.validum.core.common.Region;
import ru.validum.core.document.DocumentType;
import ru.validum.core.document.IdentityDocument;

@DisplayName("«Кем выдан» согласован с регионом прописки")
class IssuedByRegionTest {

  private static final BikSource BIK = BikSources.resolve("src/test/resources/bik-sample.xml");
  private static final LocalDate AS_OF = LocalDate.of(2026, 11, 1);

  @Test
  @DisplayName("Паспорт РФ выдан в регионе прописки, а не в Екатеринбурге")
  void passportIssuedInRegistrationRegion() {
    IndividualProfile profile =
      IndividualProfileGenerator.builder()
        .seed(11)
        .bikSource(BIK)
        .asOf(AS_OF)
        .region(Region.MO)
        .age(30)
        .build()
        .generate();

    assertThat(profile.passport().issuedBy()).contains("Московской области");
    assertThat(profile.passport().issuedBy()).doesNotContain("Екатеринбург");
    assertThat(profile.passport().issuedBy()).contains("УФМС России по");
  }

  @Test
  @DisplayName("Военный билет выдан военкоматом региона прописки")
  void militaryIdIssuedInRegion() {
    IndividualProfile profile =
      IndividualProfileGenerator.builder()
        .seed(12)
        .bikSource(BIK)
        .asOf(AS_OF)
        .region(Region.KRASNODAR)
        .age(25)
        .documents(DocumentType.MILITARY_ID)
        .build()
        .generate();

    List<IdentityDocument> documents = profile.documents();
    assertThat(documents).hasSize(1);
    assertThat(documents.get(0).issuedBy()).contains("Краснодарского края");
  }

  @Test
  @DisplayName("Сто профилей одного региона — ни одного чужого «кем выдан»")
  void allProfilesStayInRegion() {
    for (int i = 0; i < 100; i++) {
      IndividualProfile profile =
        IndividualProfileGenerator.builder()
          .seed(i)
          .bikSource(BIK)
          .asOf(AS_OF)
          .region(Region.TATARSTAN)
          .age(25)
          .documents(DocumentType.MILITARY_ID)
          .build()
          .generate();

      assertThat(profile.passport().issuedBy()).contains("Республике Татарстан");
      assertThat(profile.documents().get(0).issuedBy()).contains("Республики Татарстан");
    }
  }

  @Test
  @DisplayName("Без региона выдача остаётся в справочнике УФМС")
  void withoutRegionStillValidIssuer() {
    IndividualProfile profile =
      IndividualProfileGenerator.builder()
        .seed(13)
        .bikSource(BIK)
        .asOf(AS_OF)
        .age(40)
        .build()
        .generate();

    assertThat(profile.passport().issuedBy()).contains("УФМС России по");
  }
}
