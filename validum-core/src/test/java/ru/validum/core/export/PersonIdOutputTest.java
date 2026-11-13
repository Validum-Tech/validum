package ru.validum.core.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.bank.BikSource;
import ru.validum.core.bank.BikSources;
import ru.validum.core.common.PersonIds;
import ru.validum.core.profile.IndividualProfile;
import ru.validum.core.profile.IndividualProfileGenerator;

@DisplayName("personId в плоском выводе и CSV")
class PersonIdOutputTest {

  private static final BikSource BANKS = BikSources.resolve("src/test/resources/bik-sample.xml");

  private static IndividualProfile profile(long seed) {
    return IndividualProfileGenerator.builder()
      .seed(seed)
      .bikSource(BANKS)
      .asOf(LocalDate.of(2026, 1, 1))
      .build()
      .generate();
  }

  @Test
  @DisplayName("героем профиля становится seed: personId в JSON-словаре и в CSV")
  void personIdInFlattenedOutput() {
    IndividualProfile p = profile(42);

    Map<String, String> flat = IndividualProfileFlattener.flatten(p);
    assertThat(flat.get("personId")).isEqualTo(PersonIds.of(42));

    String csv = CsvExporter.toCsv(List.of(flat), ';', true);
    assertThat(csv).contains("Идентификатор;");
    assertThat(csv).contains(PersonIds.of(42));
  }

  @Test
  @DisplayName("different seed — different personId в выводе")
  void differentSeedDifferentId() {
    assertThat(IndividualProfileFlattener.flatten(profile(1)).get("personId"))
      .isNotEqualTo(IndividualProfileFlattener.flatten(profile(2)).get("personId"));
  }
}
