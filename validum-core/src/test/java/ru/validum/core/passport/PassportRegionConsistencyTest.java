package ru.validum.core.passport;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.bank.BikSources;
import ru.validum.core.common.Region;
import ru.validum.core.export.IndividualProfileFlattener;
import ru.validum.core.profile.IndividualProfile;
import ru.validum.core.profile.IndividualProfileGenerator;
import ru.validum.core.report.Consistency;

@DisplayName("Паспорт: серия, «кем выдан» и код подразделения из одного региона")
class PassportRegionConsistencyTest {

  private static List<IndividualProfile> profiles(int count) {
    List<IndividualProfile> result = new java.util.ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      result.add(
        IndividualProfileGenerator.builder()
          .bikSource(BikSources.resolve("src/test/resources/bik-sample.xml"))
          .seed(21 + i)
          .asOf(LocalDate.of(2026, 1, 15))
          .ageRange(18, 70)
          .build()
          .generate());
    }
    return result;
  }

  @Test
  @DisplayName("Префикс серии всегда соответствует региону из перечисления")
  void seriesPrefixIsKnownRegion() {
    for (IndividualProfile profile : profiles(200)) {
      Passport passport = profile.passport();
      if (passport.type() != PassportType.RF) {
        continue;
      }
      String prefix = passport.series().substring(0, 2);
      assertThat(Region.byCode(prefix))
        .as("серия %s — код региона %s", passport.series(), prefix)
        .isNotNull();
    }
  }

  @Test
  @DisplayName("«Кем выдан» и код подразделения согласованы с серией")
  void issuerAndDepartmentCodeMatchSeries() {
    for (IndividualProfile profile : profiles(200)) {
      Passport passport = profile.passport();
      if (passport.type() != PassportType.RF) {
        continue;
      }
      String prefix = passport.series().substring(0, 2);
      Region region = Region.byCode(prefix);
      assertThat(passport.issuedBy())
        .as("серия %s, выдал «%s»", passport.series(), passport.issuedBy())
        .contains(region.department().ufms());
      assertThat(passport.departmentCode())
        .as("серия %s, код подразделения %s", passport.series(), passport.departmentCode())
        .startsWith(prefix);
    }
  }

  @Test
  @DisplayName("Проверка согласованности ловит чужой регион выдачи и чужой код подразделения")
  void consistencyFlagsForeignIssuer() {
    IndividualProfile profile = profiles(1).get(0);
    Map<String, String> row = IndividualProfileFlattener.flatten(profile);
    String prefix = profile.passport().series().substring(0, 2);
    Region foreign = java.util.Arrays.stream(Region.values())
      .filter(r -> !r.code().equals(prefix))
      .findFirst()
      .orElseThrow();

    Map<String, String> broken = new java.util.LinkedHashMap<>(row);
    broken.put("passportIssuedBy", "Отделом УФМС России по " + foreign.department().ufms());
    broken.put("passportDepartmentCode", foreign.code() + "1-001");

    List<Consistency.Violation> violations = Consistency.check(List.of(broken));

    assertThat(violations).anyMatch(v -> v.rule().equals("регион/кем выдан"));
    assertThat(violations).anyMatch(v -> v.rule().equals("серия/код подразделения"));
  }

  @Test
  @DisplayName("Прописка в другом регионе, чем серия паспорта, — нарушение")
  void consistencyFlagsForeignRegistration() {
    IndividualProfile profile = profiles(1).get(0);
    Map<String, String> row = IndividualProfileFlattener.flatten(profile);
    String prefix = profile.passport().series().substring(0, 2);
    Region foreign = java.util.Arrays.stream(Region.values())
      .filter(r -> !r.code().equals(prefix))
      .findFirst()
      .orElseThrow();

    Map<String, String> broken = new java.util.LinkedHashMap<>(row);
    broken.put("registrationRegion", foreign.regionName());

    assertThat(Consistency.check(List.of(broken)))
      .anyMatch(v -> v.rule().equals("серия/регион прописки"));
  }

  @Test
  @DisplayName("Сгенерированный резидент: серия совпадает с регионом прописки")
  void seriesMatchesRegistrationRegion() {
    for (IndividualProfile profile : profiles(100)) {
      Passport passport = profile.passport();
      if (passport.type() != PassportType.RF) {
        continue;
      }
      Region region = Region.byCode(passport.series().substring(0, 2));
      assertThat(profile.registrationAddress().region())
        .as("серия %s → %s, а прописка в %s", passport.series(), region.regionName(),
          profile.registrationAddress().region())
        .isEqualTo(region.regionName());
    }
  }
}
