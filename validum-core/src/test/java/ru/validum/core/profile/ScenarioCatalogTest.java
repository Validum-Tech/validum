package ru.validum.core.profile;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.document.DocumentType;
import ru.validum.core.document.DocumentValidator;
import ru.validum.core.document.IdentityDocument;
import ru.validum.core.name.Gender;

@DisplayName("Каталог сценариев (ScenarioCatalog)")
class ScenarioCatalogTest {

  @Test
  @DisplayName("Девять slug'ов в стабильном порядке")
  void nineSlugsInOrder() {
    assertThat(ScenarioCatalog.allSlugs())
      .containsExactly(
        "adult-resident-with-account",
        "adult-resident-without-account",
        "pensioner-male",
        "pensioner-female",
        "salary-client",
        "non-resident-with-residence-permit",
        "foreigner-with-visa",
        "serviceman",
        "minor");
  }

  @Test
  @DisplayName("bySlug: находит каждый, неизвестный и null — пусто")
  void bySlug() {
    for (String slug : ScenarioCatalog.allSlugs()) {
      assertThat(ScenarioCatalog.bySlug(slug)).as("slug %s", slug).isPresent();
    }
    assertThat(ScenarioCatalog.bySlug("refugee")).isEmpty();
    assertThat(ScenarioCatalog.bySlug(null)).isEmpty();
  }

  @Test
  @DisplayName("Каждый сценарий генерируется, документы проходят валидатор")
  void everyScenarioGeneratesValidDocuments() {
    for (ScenarioCatalog scenario : ScenarioCatalog.values()) {
      IndividualProfile profile = scenario.generate(7L);
      assertThat(profile).as("сценарий %s", scenario.slug()).isNotNull();
      for (IdentityDocument doc : profile.documents()) {
        assertThat(DocumentValidator.isValid(doc))
          .as("%s: %s %s %s", scenario.slug(), doc.type(), doc.series(), doc.number())
          .isTrue();
      }
    }
  }

  @Test
  @DisplayName("non-resident-with-residence-permit: нерезидент, есть ВНЖ")
  void nonResidentWithResidencePermit() {
    IndividualProfile p = ScenarioCatalog.NON_RESIDENT_WITH_RESIDENCE_PERMIT.generate(42L);

    assertThat(p.residency().isResident()).isFalse();
    assertThat(types(p.documents()))
      .contains(DocumentType.PASSPORT_FOREIGN, DocumentType.RESIDENCE_PERMIT);
    assertThat(p.account()).isNotBlank();
  }

  @Test
  @DisplayName("foreigner-with-visa: нерезидент, есть виза")
  void foreignerWithVisa() {
    IndividualProfile p = ScenarioCatalog.FOREIGNER_WITH_VISA.generate(42L);

    assertThat(p.residency().isResident()).isFalse();
    assertThat(types(p.documents())).contains(DocumentType.VISA);
  }

  @Test
  @DisplayName("serviceman: мужчина призывного возраста с военным билетом")
  void serviceman() {
    IndividualProfile p = ScenarioCatalog.SERVICEMAN.generate(42L);

    assertThat(p.gender()).isEqualTo(Gender.MALE);
    assertThat(p.age()).isBetween(18, 27);
    assertThat(types(p.documents())).contains(DocumentType.MILITARY_ID);
  }

  @Test
  @DisplayName("minor: младше 18, паспорт РФ, счёт есть")
  void minor() {
    IndividualProfile p = ScenarioCatalog.MINOR.generate(42L);

    assertThat(p.age()).isLessThan(18);
    assertThat(types(p.documents())).contains(DocumentType.PASSPORT_RF);
    assertThat(p.account()).isNotBlank();
  }

  @Test
  @DisplayName("Детерминизм новых сценариев: seed → одинаковый ИНН")
  void deterministic() {
    for (ScenarioCatalog scenario : List.of(
      ScenarioCatalog.NON_RESIDENT_WITH_RESIDENCE_PERMIT,
      ScenarioCatalog.FOREIGNER_WITH_VISA,
      ScenarioCatalog.SERVICEMAN,
      ScenarioCatalog.MINOR)) {
      assertThat(scenario.generate(99L).inn())
        .as("сценарий %s", scenario.slug())
        .isEqualTo(scenario.generate(99L).inn());
    }
  }

  private static List<DocumentType> types(List<IdentityDocument> documents) {
    return documents.stream().map(IdentityDocument::type).toList();
  }
}
