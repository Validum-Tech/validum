package ru.validum.core.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.TestProfiles;
import ru.validum.core.profile.IndividualProfile;

/** Вложенный JSON: структура профиля, а не плоская строка. */
class NestedWriterTest {

  @Test
  @DisplayName("Профиль разложен на passport / addresses / bank / cards / documents")
  void nestedStructure() {
    IndividualProfile profile = TestProfiles.individual(7_001);

    String json = NestedWriter.toJson(List.of(profile), 42L);

    assertThat(json).startsWith("{\"count\":1,\"seed\":42,\"profiles\":[{");
    assertThat(json).contains("\"passport\":{");
    assertThat(json).contains("\"addresses\":{\"registration\":{");
    assertThat(json).contains("\"contacts\":{");
    assertThat(json).contains("\"bank\":{\"bik\":");
    assertThat(json).contains("\"cards\":[{");
    assertThat(json).contains("\"pan\":\"220");
    assertThat(json).contains("\"expiry\":\"");
    assertThat(json).contains("\"documents\":[");
    assertThat(json).contains("\"isResident\":\"true\"");
    assertThat(json).doesNotContain("\n");
  }

  @Test
  @DisplayName("Без seed поле seed не выводится")
  void withoutSeed() {
    String json = NestedWriter.toJson(List.of(TestProfiles.individual(7_002)), null);

    assertThat(json).startsWith("{\"count\":1,\"profiles\":[");
    assertThat(json).doesNotContain("\"seed\"");
  }

  @Test
  @DisplayName("Пустой список профилей — валидный объект")
  void emptyList() {
    assertThat(NestedWriter.toJson(List.<IndividualProfile>of(), 5L)).isEqualTo("{\"count\":0,\"seed\":5,\"profiles\":[]}");
  }
}
