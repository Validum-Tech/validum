package ru.validum.core.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.document.DocumentType;
import ru.validum.core.document.IdentityDocument;
import ru.validum.core.export.DocumentsCsv;

@DisplayName("Согласованность набора: возраст, документы, регион, счёт")
class ConsistencyTest {

  private static Map<String, String> row(String... pairs) {
    Map<String, String> row = new LinkedHashMap<>();
    for (int i = 0; i + 1 < pairs.length; i += 2) {
      row.put(pairs[i], pairs[i + 1]);
    }
    return row;
  }

  @Test
  @DisplayName("Согласованный набор — нарушений нет")
  void cleanSetHasNoViolations() {
    List<Map<String, String>> rows = List.of(
      row("age", "30", "isResident", "true", "passportType", "RF",
        "passportSeries", "5047", "passportIssuedBy", "Отделением УФМС России по Московской области",
        "account", "40817810000000000001", "bankBik", "044525225"),
      row("age", "25", "isResident", "true", "passportType", "RF",
        "passportSeries", "7700", "passportIssuedBy", "Отделом УФМС России по г. Москве"));

    assertThat(Consistency.check(rows)).isEmpty();
  }

  @Test
  @DisplayName("Водительские права у пятнадцатилетнего — нарушение с номером строки")
  void driverLicenseAtFifteenIsViolation() {
    String documents = DocumentsCsv.inline(List.of(
      new IdentityDocument(
        DocumentType.PASSPORT_RF, "45 12", "486560", "Отделом УФМС России по г. Москве",
        LocalDate.of(2015, 1, 1), null, "770-001"),
      new IdentityDocument(
        DocumentType.DRIVER_LICENSE, "7712", "381911", "ГИБДД 0215",
        LocalDate.of(2024, 1, 1), LocalDate.of(2034, 1, 1), null)));
    List<Map<String, String>> rows = List.of(row("age", "15", "documents", documents));

    List<Consistency.Violation> violations = Consistency.check(rows);

    assertThat(violations).hasSize(1);
    assertThat(violations.get(0).row()).isEqualTo(2);
    assertThat(violations.get(0).rule()).isEqualTo("документ/возраст");
    assertThat(violations.get(0).detail()).contains("DRIVER_LICENSE").contains("15");
  }

  @Test
  @DisplayName("Нерезидент с паспортом РФ — нарушение")
  void nonResidentWithRfPassport() {
    List<Map<String, String>> rows = List.of(
      row("age", "30", "isResident", "false", "passportType", "RF",
        "passportSeries", "5047", "passportIssuedBy", "Отделением УФМС России по Московской области"));

    List<Consistency.Violation> violations = Consistency.check(rows);

    assertThat(violations).anyMatch(v -> v.rule().equals("резидентство/паспорт"));
  }

  @Test
  @DisplayName("Паспорт выдан не в том регионе, что серия")
  void issuerMismatch() {
    List<Map<String, String>> rows = List.of(
      row("age", "30", "isResident", "true", "passportType", "RF",
        "passportSeries", "5047", "passportIssuedBy", "Отделом УФМС России по г. Москве"));

    List<Consistency.Violation> violations = Consistency.check(rows);

    assertThat(violations).anyMatch(v -> v.rule().equals("регион/кем выдан")
      && v.detail().contains("5047"));
  }

  @Test
  @DisplayName("Счёт без банка — нарушение")
  void accountWithoutBank() {
    List<Map<String, String>> rows = List.of(
      row("age", "30", "account", "40817810000000000001"));

    assertThat(Consistency.check(rows))
      .anyMatch(v -> v.rule().equals("счёт/банк"));
  }

  @Test
  @DisplayName("Распределения считают пол, регион и банк")
  void distributionsCountValues() {
    List<Map<String, String>> rows = List.of(
      row("gender", "MALE", "passportSeries", "5047", "bankBik", "044525225",
        "isResident", "true", "familyStatus", "Женат/Замужем"),
      row("gender", "FEMALE", "passportSeries", "5047", "bankBik", "044030653",
        "isResident", "true", "familyStatus", "Холост/Не замужем"),
      row("gender", "MALE", "passportSeries", "7700", "bankBik", "044525225",
        "isResident", "false", "familyStatus", "Холост/Не замужем"));

    Map<String, Map<String, Integer>> distributions = Consistency.distributions(rows);

    assertThat(distributions.get("пол")).containsEntry("MALE", 2).containsEntry("FEMALE", 1);
    assertThat(distributions.get("банк")).containsEntry("044525225", 2);
    assertThat(distributions.get("резидентство")).containsEntry("резидент", 2);
    assertThat(distributions.get("регион")).containsEntry("Московской области", 2);
  }

  @Test
  @DisplayName("Возраст: минимум, максимум и среднее")
  void ageSummaryCountsRange() {
    List<Map<String, String>> rows = List.of(
      row("age", "20"), row("age", "40"), row("age", "30"));

    Map<String, String> summary = Consistency.ageSummary(rows);

    assertThat(summary).containsEntry("мин", "20").containsEntry("макс", "40")
      .containsEntry("средний", "30.0");
  }
}
