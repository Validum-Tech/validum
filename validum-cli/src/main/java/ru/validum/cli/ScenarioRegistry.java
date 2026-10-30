package ru.validum.cli;

import java.util.List;
import java.util.Optional;
import ru.validum.core.profile.ScenarioCatalog;

/**
 * Фасад над {@link ScenarioCatalog} для CLI.
 *
 * <p>Отдельного реестра больше нет: каталог сценариев один и живёт в core, поэтому CLI и REST
 * всегда видят одинаковый список slug'ов.
 */
public final class ScenarioRegistry {

  private ScenarioRegistry() {}

  /** Все slug'и в порядке объявления. */
  public static List<String> allSlugs() {
    return ScenarioCatalog.allSlugs();
  }

  /** Найти сценарий по slug. */
  public static Optional<ScenarioCatalog> bySlug(String slug) {
    return ScenarioCatalog.bySlug(slug);
  }
}
