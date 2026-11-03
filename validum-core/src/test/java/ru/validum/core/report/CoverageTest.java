package ru.validum.core.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.validum.core.bank.BikSources;
import ru.validum.core.profile.IndividualProfile;
import ru.validum.core.profile.IndividualProfileGenerator;

class CoverageTest {

  private static IndividualProfileGenerator generator;

  @BeforeAll
  static void setUp() {
    generator = IndividualProfileGenerator.builder()
      .bikSource(BikSources.resolve("src/test/resources/bik-sample.xml"))
      .seed(7)
      .build();
  }

  @Test
  void countsFillBlankAndValidity() {
    IndividualProfile good = generator.generate();
    List<Map<String, String>> rows = List.of(
      Map.of("fullName", "Иванов Иван Иванович", "inn", good.inn(), "snils", good.snils()),
      Map.of("fullName", "Петров Пётр Петрович", "inn", "123456789012", "snils", good.snils()),
      Map.of("fullName", "", "inn", "", "snils", good.snils()));

    Coverage.FieldStat inn = Coverage.stat("inn", rows);
    assertThat(inn.total()).isEqualTo(3);
    assertThat(inn.present()).isEqualTo(2);
    assertThat(inn.blank()).isEqualTo(1);
    assertThat(inn.invalid()).isEqualTo(1);
    assertThat(inn.distinct()).isEqualTo(2);
    assertThat(inn.fillPercent()).isEqualTo(67);
    assertThat(inn.clean()).isFalse();

    Coverage.FieldStat snils = Coverage.stat("snils", rows);
    assertThat(snils.invalid()).isZero();
    assertThat(snils.clean()).isTrue();
  }

  @Test
  void fieldsGoInPreferredOrderThenAlphabetically() {
    List<Map<String, String>> rows = List.of(
      Map.of("inn", "1", "fullName", "Иванов", "unexpected", "x"));

    assertThat(Coverage.fieldsOf(rows)).containsExactly("fullName", "inn", "unexpected");
    assertThat(Coverage.analyze(rows)).hasSize(3);
  }

  @Test
  void uncheckedFieldHasNoInvalidCount() {
    List<Map<String, String>> rows = List.of(Map.of("fullName", "Иванов"));
    Coverage.FieldStat stat = Coverage.stat("fullName", rows);
    assertThat(stat.invalid()).isZero();
    assertThat(stat.distinct()).isEqualTo(1);
  }
}
