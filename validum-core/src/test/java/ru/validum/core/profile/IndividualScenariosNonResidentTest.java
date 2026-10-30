package ru.validum.core.profile;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.common.Country;
import ru.validum.core.passport.PassportType;

@DisplayName("IndividualScenarios — нерезидент (Итерация 5)")
class IndividualScenariosNonResidentTest {

  private static final long SEED = 42L;

  @Test
  @DisplayName("createNonResident(TJ): residency + иностранный паспорт, всё согласовано")
  void nonResidentTajikistan() {
    var p = IndividualScenarios.createNonResident(Country.TJ, SEED);

    assertThat(p.residency().isResident()).isFalse();
    assertThat(p.residency().residentCountry()).isEqualTo(Country.TJ);
    assertThat(p.residency().citizenship()).isEqualTo(Country.TJ);
    assertThat(p.residency().taxResidentCountry()).isEqualTo(Country.TJ);
    assertThat(p.passport().type()).isEqualTo(PassportType.FOREIGN);
    assertThat(p.passport().country()).isEqualTo(Country.TJ);
  }

  @Test
  @DisplayName("createNonResident(KZ): всё согласовано под KZ")
  void nonResidentKazakhstan() {
    var p = IndividualScenarios.createNonResident(Country.KZ, SEED);
    assertThat(p.residency().residentCountry()).isEqualTo(Country.KZ);
    assertThat(p.passport().country()).isEqualTo(Country.KZ);
  }

  @Test
  @DisplayName("Детерминизм: одинаковый seed → одинаковый профиль")
  void deterministic() {
    var a = IndividualScenarios.createNonResident(Country.TJ, SEED);
    var b = IndividualScenarios.createNonResident(Country.TJ, SEED);
    assertThat(a).isEqualTo(b);
  }
}
