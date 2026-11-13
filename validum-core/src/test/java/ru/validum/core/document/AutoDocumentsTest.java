package ru.validum.core.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.bank.BikSource;
import ru.validum.core.bank.BikSources;
import ru.validum.core.name.Gender;
import ru.validum.core.profile.AgeDistribution;
import ru.validum.core.profile.IndividualProfile;
import ru.validum.core.profile.IndividualProfileGenerator;
import ru.validum.core.profile.Residency;

/**
 * {@code --documents auto}: набор ДУЛ подбирается по возрасту и полу и всегда проходит
 * собственные правила — автонабор не должен падать на {@link DocumentRules#validate}.
 */
class AutoDocumentsTest {

  private static final BikSource BIK = BikSources.resolve("src/test/resources/bik-sample.xml");
  private static final LocalDate AS_OF = LocalDate.of(2026, 11, 1);

  private static List<IndividualProfile> profiles(int count, long seed) {
    IndividualProfileGenerator gen =
      IndividualProfileGenerator.builder()
        .seed(seed)
        .bikSource(BIK)
        .asOf(AS_OF)
        .ageRange(0, 100)
        .ageDistribution(AgeDistribution.UNIFORM)
        .documentsAuto()
        .build();
    List<IndividualProfile> result = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      result.add(gen.generate());
    }
    return result;
  }

  @Test
  @DisplayName("1000 автонаборов: ни одного нарушения правил, дети без паспорта РФ")
  void autoSelectNeverViolatesRules() {
    for (IndividualProfile profile : profiles(1000, 11)) {
      int age = profile.ageAt(AS_OF);
      List<DocumentType> types = profile.documents().stream().map(IdentityDocument::type).toList();

      // Правила согласованы с набором — исключение здесь означало бы рассинхрон.
      DocumentRules.validate(types, profile.gender(), age, profile.residency());

      if (age < 14) {
        assertThat(types).as("до 14 лет: %s", age).isEmpty();
      } else {
        assertThat(types).as("с 14 лет: %s", age).contains(DocumentType.PASSPORT_RF);
      }
      if (age >= 18) {
        assertThat(types).as("взрослый %s", age).contains(DocumentType.DRIVER_LICENSE);
      }
      if (profile.gender() == Gender.MALE && age >= 18 && age <= 27) {
        assertThat(types).as("мужчина призывного возраста %s", age).contains(DocumentType.MILITARY_ID);
      }
    }
  }

  @Test
  @DisplayName("Даты автонабора тоже привязаны к возрасту")
  void autoDocumentsRespectBirthDate() {
    for (IndividualProfile profile : profiles(100, 23)) {
      for (IdentityDocument document : profile.documents()) {
        assertThat(document.issueDate())
          .isAfterOrEqualTo(profile.birthDate().plusYears(document.type().minAge()))
          .isBeforeOrEqualTo(AS_OF);
      }
    }
  }

  @Test
  @DisplayName("Нерезидент получает паспорт иностранца, а не паспорт РФ")
  void nonResidentGetsForeignPassport() {
    IndividualProfile profile =
      IndividualProfileGenerator.builder()
        .seed(4)
        .bikSource(BIK)
        .asOf(AS_OF)
        .age(35)
        .residency(Residency.nonResident(ru.validum.core.common.Country.KZ))
        .documentsAuto()
        .build()
        .generate();

    List<DocumentType> types = profile.documents().stream().map(IdentityDocument::type).toList();
    assertThat(types).contains(DocumentType.PASSPORT_FOREIGN).doesNotContain(DocumentType.PASSPORT_RF);
    assertThat(types).contains(DocumentType.RESIDENCE_PERMIT);
  }
}
