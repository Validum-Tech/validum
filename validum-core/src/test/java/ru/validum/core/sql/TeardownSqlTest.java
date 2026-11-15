package ru.validum.core.sql;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Откат: DELETE в обратном порядке, без DDL. */
class TeardownSqlTest {

  @Test
  @DisplayName("Таблицы удаляются в порядке, обратном загрузке: сначала дети, потом родители")
  void reverseOrder() {
    String sql = TeardownSql.build(
      "validum 0.5.0 · набор · seed=42",
      List.of(
        new TeardownSql.Table("validum_organization", "ogrn", List.of("1027700132195")),
        new TeardownSql.Table("validum_individual", "personId", List.of("pd-1", "pd-2"))));

    assertThat(sql).startsWith("-- validum 0.5.0");
    assertThat(sql.indexOf("DELETE FROM validum_individual")).isLessThan(sql.indexOf("DELETE FROM validum_organization"));
    assertThat(sql).doesNotContain("CREATE", "DROP", "TRUNCATE");
  }

  @Test
  @DisplayName("Значения экранируются, пустые таблицы пропускаются")
  void quotesAndEmpty() {
    String sql = TeardownSql.build(
      "набор",
      List.of(
        new TeardownSql.Table("validum_individual", "personId", List.of("pd-o'brien")),
        new TeardownSql.Table("validum_ip", "personId", List.of())));

    assertThat(sql).contains("'pd-o''brien'");
    assertThat(sql).doesNotContain("validum_ip");
  }

  @Test
  @DisplayName("Длинный список режется на пачки по 200 — одна строка на пачку")
  void chunking() {
    List<String> keys = java.util.stream.IntStream.rangeClosed(1, 450)
      .mapToObj(i -> "pd-" + i)
      .toList();
    String sql = TeardownSql.build("набор", List.of(new TeardownSql.Table("validum_individual", "personId", keys)));

    assertThat(sql.lines().filter(line -> line.startsWith("DELETE FROM")).count()).isEqualTo(3);
    assertThat(sql).contains("'pd-450'");
  }
}
