package ru.validum.core.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.bank.BikSources;
import ru.validum.core.profile.IndividualProfile;
import ru.validum.core.profile.IndividualProfileGenerator;

@DisplayName("Русские заголовки CSV: без дублей и с обратимым разбором")
class RussianHeadersTest {

  private static Map<String, String> row() {
    IndividualProfile profile = IndividualProfileGenerator.builder()
      .bikSource(BikSources.resolve("src/test/resources/bik-sample.xml"))
      .seed(11)
      .asOf(LocalDate.of(2026, 1, 15))
      .age(35)
      .build()
      .generate();
    return IndividualProfileFlattener.flatten(profile);
  }

  @Test
  @DisplayName("Один ключ — одна русская метка: колонки в CSV не называются одинаково")
  void labelsAreUnique() {
    Set<String> labels = new HashSet<>();
    for (String key : row().keySet()) {
      String label = CsvExporter.russianHeader(key);
      assertThat(labels.add(label)).as("дубль заголовка «%s» для ключа %s", label, key).isTrue();
    }
  }

  @Test
  @DisplayName("Метка из CSV читается обратно в тот же ключ (иначе coverage/describe теряют колонку)")
  void labelsRoundTrip() {
    for (String key : row().keySet()) {
      String label = CsvExporter.russianHeader(key);
      assertThat(CsvExporter.internalKey(label)).as("обратный разбор «%s»", label).isEqualTo(key);
    }
  }

  @Test
  @DisplayName("Колонки таблицы документов не перекрывают паспортные")
  void documentTableLabelsDifferFromPassport() {
    assertThat(CsvExporter.russianHeader("passportIssuedBy"))
      .isNotEqualTo(CsvExporter.russianHeader("issuedBy"));
    assertThat(CsvExporter.russianHeader("passportIssueDate"))
      .isNotEqualTo(CsvExporter.russianHeader("issueDate"));
    assertThat(CsvExporter.russianHeader("passportDepartmentCode"))
      .isNotEqualTo(CsvExporter.russianHeader("departmentCode"));
  }

  @Test
  @DisplayName("Кем выдан паспорта присутствует в строке CSV с русскими заголовками")
  void issuedByIsPresentInExcelCsv() {
    Map<String, String> row = row();
    String csv = CsvExporter.toCsv(List.of(row), ';', true);
    String header = csv.lines().findFirst().orElseThrow();

    // В профиле паспортные колонки, а «(документ)» — это таблица документов, её здесь быть не должно.
    assertThat(header).contains("Кем выдан").doesNotContain("Кем выдан (документ)");
    assertThat(row.get("passportIssuedBy")).isNotBlank();
    assertThat(csv).contains(row.get("passportIssuedBy"));
  }
}
