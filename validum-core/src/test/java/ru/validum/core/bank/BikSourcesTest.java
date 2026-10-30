package ru.validum.core.bank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Выбор справочника БИК (BikSources)")
class BikSourcesTest {

  /** Фикстура лежит в модуле; тест запускается из каталога модуля. */
  private static final Path FIXTURE = Path.of("src", "test", "resources", "bik-sample.xml");

  @Test
  @DisplayName("Явный путь к фикстуре даёт всех участников и только банки")
  void explicitPath() {
    ParticipantRegistry registry = BikSources.resolveParticipants(FIXTURE.toString());

    assertThat(registry.size()).isEqualTo(6);
    assertThat(registry.banks()).hasSize(3);
    assertThat(BikSources.resolve(FIXTURE.toString()).banks()).hasSize(3);
  }

  @Test
  @DisplayName("Замечание о загрузке источника уходит в переданный приёмник")
  void noticesCollect() {
    List<String> messages = new ArrayList<>();

    BikSources.resolveParticipants(FIXTURE.toString(), messages::add);

    assertThat(messages).anyMatch(message -> message.contains("6"));
  }

  @Test
  @DisplayName("Системное свойство подставляет файл, если явный путь не задан")
  void systemPropertyWorks() {
    String previous = System.getProperty(BikSources.PROPERTY);
    System.setProperty(BikSources.PROPERTY, FIXTURE.toAbsolutePath().toString());
    try {
      assertThat(BikSources.resolve(null).banks()).hasSize(3);
    } finally {
      restore(previous);
    }
  }

  @Test
  @DisplayName("Без файла — BikSourceNotFoundException с подсказкой про fetch-bik")
  void missingSourceThrows() {
    Assumptions.assumeTrue(
      System.getenv(BikSources.ENV_VAR) == null,
      "в окружении задан " + BikSources.ENV_VAR + " — проверка отката неприменима");
    String previous = System.getProperty(BikSources.PROPERTY);
    System.clearProperty(BikSources.PROPERTY);
    try {
      assertThatThrownBy(() -> BikSources.resolve(null))
        .isInstanceOf(BikSourceNotFoundException.class)
        .hasMessageContaining("fetch-bik");
    } finally {
      restore(previous);
    }
  }

  @Test
  @DisplayName("Ленивый источник ищет файл при первом обращении")
  void lazyResolvesOnFirstUse() {
    String previous = System.getProperty(BikSources.PROPERTY);
    BikSource lazy = BikSources.defaultSource();
    System.setProperty(BikSources.PROPERTY, FIXTURE.toAbsolutePath().toString());
    try {
      assertThat(lazy.banks()).hasSize(3);
    } finally {
      restore(previous);
    }
  }

  private static void restore(String previous) {
    if (previous == null) {
      System.clearProperty(BikSources.PROPERTY);
    } else {
      System.setProperty(BikSources.PROPERTY, previous);
    }
  }
}
