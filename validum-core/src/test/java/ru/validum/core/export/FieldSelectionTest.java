package ru.validum.core.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Выбор колонок: --fields и пресет short")
class FieldSelectionTest {

  @Test
  @DisplayName("Пустой запрос оставляет строки как есть")
  void blankKeepsEverything() {
    List<Map<String, String>> rows = List.of(row());
    assertThat(FieldSelection.keep(rows, null)).isSameAs(rows);
    assertThat(FieldSelection.keep(rows, "  ")).isSameAs(rows);
  }

  @Test
  @DisplayName("Пресет short оставляет только человеческие колонки")
  void shortPreset() {
    List<Map<String, String>> kept = FieldSelection.keep(List.of(row()), "short");
    assertThat(kept).hasSize(1);
    assertThat(kept.get(0).keySet())
      .contains("fullName", "inn", "age", "registrationRegion")
      .doesNotContain("secretWord", "terrorist", "asOf");
  }

  @Test
  @DisplayName("Для ЮЛ пресет берёт организациионные колонки")
  void shortPresetForOrganization() {
    Map<String, String> org = new LinkedHashMap<>();
    org.put("organizationName", "ООО «Ромашка»");
    org.put("ogrn", "1027700132195");
    org.put("kpp", "770701001");
    org.put("okved", "62.01");
    org.put("terrorist", "false");
    List<Map<String, String>> kept = FieldSelection.keep(List.of(org), "short");
    assertThat(kept.get(0).keySet()).contains("organizationName", "ogrn", "kpp", "okved");
    assertThat(kept.get(0)).doesNotContainKey("terrorist");
  }

  @Test
  @DisplayName("Явный список оставляет ровно эти колонки в порядке профиля")
  void explicitList() {
    List<Map<String, String>> kept = FieldSelection.keep(List.of(row()), "inn,fullName");
    assertThat(kept.get(0).keySet()).containsExactly("fullName", "inn");
  }

  @Test
  @DisplayName("Неизвестная колонка — понятная ошибка со списком доступных")
  void unknownColumn() {
    assertThatThrownBy(() -> FieldSelection.keep(List.of(row()), "inn,passport"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Неизвестные колонки: passport")
      .hasMessageContaining("fullName");
  }

  private static Map<String, String> row() {
    Map<String, String> row = new LinkedHashMap<>();
    row.put("personId", "pd-1");
    row.put("fullName", "Иванов Иван Иванович");
    row.put("gender", "MALE");
    row.put("age", "30");
    row.put("inn", "500100732259");
    row.put("registrationRegion", "Московская");
    row.put("terrorist", "false");
    row.put("secretWord", "кот");
    row.put("asOf", "2026-01-15");
    return row;
  }
}
