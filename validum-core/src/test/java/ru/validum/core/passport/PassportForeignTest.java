package ru.validum.core.passport;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.common.Country;
import ru.validum.core.random.DataGenerator;

@DisplayName("PassportGenerator — иностранный паспорт")
class PassportForeignTest {

  private final PassportGenerator generator =
    new PassportGenerator(DataGenerator.seededRandom(42L));

  @Test
  @DisplayName("FOREIGN: type=FOREIGN, country=TJ, issuedBy содержит страну")
  void foreignPassportMetadata() {
    Passport passport =
      generator.generate(
        LocalDate.of(1990, 5, 15),
        LocalDate.of(2026, 10, 15),
        true,
        PassportType.FOREIGN,
        Country.TJ);

    assertThat(passport.type()).isEqualTo(PassportType.FOREIGN);
    assertThat(passport.country()).isEqualTo(Country.TJ);
    assertThat(passport.issuedBy()).contains(Country.TJ.countryName());
    assertThat(passport.series()).hasSize(4);
    assertThat(passport.number()).hasSize(6);
  }

  @Test
  @DisplayName("RF по умолчанию: type=RF, country=RU")
  void rfPassportDefault() {
    Passport passport = generator.generate(LocalDate.of(1990, 5, 15));
    assertThat(passport.type()).isEqualTo(PassportType.RF);
    assertThat(passport.country()).isEqualTo(Country.RU);
  }

  @Test
  @DisplayName("Константы AGE_FIRST_ISSUE / AGE_FIRST_EXCHANGE / AGE_SECOND_EXCHANGE публичны")
  void ageConstantsExposed() {
    assertThat(PassportGenerator.AGE_FIRST_ISSUE).isEqualTo(14);
    assertThat(PassportGenerator.AGE_FIRST_EXCHANGE).isEqualTo(20);
    assertThat(PassportGenerator.AGE_SECOND_EXCHANGE).isEqualTo(45);
  }
}
