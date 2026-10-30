package ru.validum.core.profile;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.bank.BikSource;
import ru.validum.core.bank.BikSources;
import ru.validum.core.export.CsvFiller;
import ru.validum.core.export.IndividualProfileFlattener;
import ru.validum.core.export.ProfileFromCsv;

@DisplayName("Seed-детерминизм: дата расчёта asOf")
class SeedDeterminismTest {

  private static final BikSource BANKS = BikSources.resolve("src/test/resources/bik-sample.xml");
  private static final long SEED = 42L;

  private static IndividualProfileGenerator generator(LocalDate asOf) {
    return IndividualProfileGenerator.builder()
      .seed(SEED)
      .bikSource(BANKS)
      .asOf(asOf)
      .build();
  }

  @Test
  @DisplayName("Один seed и одна дата расчёта → одинаковый профиль")
  void sameSeedAndAsOf() {
    LocalDate asOf = LocalDate.of(2026, 1, 1);

    IndividualProfile first = generator(asOf).generate();
    IndividualProfile second = generator(asOf).generate();

    assertThat(first).isEqualTo(second);
    assertThat(first.asOf()).isEqualTo(asOf);
  }

  @Test
  @DisplayName("Дата расчёта попадает в профиль и в плоский CSV")
  void asOfReachesCsv() {
    LocalDate asOf = LocalDate.of(2026, 1, 1);

    IndividualProfile p = generator(asOf).generate();
    Map<String, String> row = IndividualProfileFlattener.flatten(p);

    assertThat(row).containsEntry("asOf", "2026-01-01");
    assertThat(row.get("age")).isEqualTo(String.valueOf(p.ageAt(asOf)));
  }

  @Test
  @DisplayName("Возраст на дату расчёта не зависит от сегодняшнего дня")
  void ageIsRelativeToAsOf() {
    LocalDate asOf = LocalDate.of(2026, 6, 1);

    IndividualProfile p =
      IndividualProfileGenerator.builder()
        .seed(SEED)
        .bikSource(BANKS)
        .asOf(asOf)
        .age(35)
        .build()
        .generate();

    assertThat(p.age()).isEqualTo(p.ageAt(asOf));
    assertThat(p.ageAt(asOf)).isEqualTo(35);
    assertThat(p.birthDate()).isBefore(asOf);
  }

  @Test
  @DisplayName("Даты паспорта отсчитываются от даты расчёта")
  void passportDatesFollowAsOf() {
    LocalDate asOf = LocalDate.of(2026, 1, 1);

    IndividualProfile p = generator(asOf).generate();

    assertThat(p.passport().issueDate()).isBefore(asOf);
    if (p.passport().expirationDate() != null) {
      assertThat(p.passport().expirationDate()).isAfter(p.passport().issueDate());
    }
  }

  @Test
  @DisplayName("Круговая операция CSV сохраняет дату расчёта")
  void roundTripKeepsAsOf() {
    LocalDate asOf = LocalDate.of(2026, 3, 15);
    IndividualProfile p = generator(asOf).generate();

    Map<String, String> flat = IndividualProfileFlattener.flatten(p);
    IndividualProfile parsed = ProfileFromCsv.parse(flat);

    assertThat(parsed.asOf()).isEqualTo(asOf);
    assertThat(IndividualProfileFlattener.flatten(parsed)).isEqualTo(flat);
  }

  @Test
  @DisplayName("Дозаполнение CSV берёт дату расчёта из конструктора")
  void fillerUsesAsOf() {
    LocalDate asOf = LocalDate.of(2026, 2, 1);
    CsvFiller filler = new CsvFiller(BANKS, new java.util.Random(SEED), asOf);

    Map<String, String> filled = filler.fillRow(Map.of("fullName", "Иванов Иван Иванович"));

    assertThat(filled).containsEntry("asOf", "2026-02-01");
  }

  @Test
  @DisplayName("Без даты расчёта профиль считает возраст на сегодня — как раньше")
  void withoutAsOfFallsBackToToday() {
    IndividualProfile p =
      IndividualProfileGenerator.builder().seed(SEED).bikSource(BANKS).build().generate();

    assertThat(p.age()).isEqualTo(p.ageAt(LocalDate.now()));
  }
}
