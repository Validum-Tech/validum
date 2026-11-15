package ru.validum.core.profile;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.TestProfiles;
import ru.validum.core.export.IndividualProfileFlattener;
import ru.validum.core.export.ProfileFromCsv;

/** Связь «человек → работодатель»: ОГРН доезжает до плоской выдачи и переживает разбор CSV. */
class EmployerLinkTest {

  private static final String OGRN = "1027700132195";

  @Test
  @DisplayName("ОГРН работодателя попадает в плоскую выдачу")
  void employerAppearsInFlatOutput() {
    IndividualProfile profile = TestProfiles.builder(8_001).employer(OGRN).build().generate();

    Map<String, String> row = IndividualProfileFlattener.flatten(profile);

    assertThat(row).containsEntry("employerOgrn", OGRN);
  }

  @Test
  @DisplayName("Без работодателя колонка пустая, а не отсутствует")
  void withoutEmployerColumnIsEmpty() {
    IndividualProfile profile = TestProfiles.individual(8_002);

    Map<String, String> row = IndividualProfileFlattener.flatten(profile);

    assertThat(row).containsKey("employerOgrn");
    assertThat(row.get("employerOgrn")).isEmpty();
  }

  @Test
  @DisplayName("Круговая операция flatten → parse сохраняет работодателя")
  void roundTripKeepsEmployer() {
    IndividualProfile profile = TestProfiles.builder(8_003).employer(OGRN).build().generate();

    IndividualProfile restored =
        ProfileFromCsv.parse(IndividualProfileFlattener.flatten(profile));

    assertThat(restored.employerOgrn()).isEqualTo(OGRN);
  }
}
