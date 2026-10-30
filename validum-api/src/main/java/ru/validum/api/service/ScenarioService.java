package ru.validum.api.service;

import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import ru.validum.api.dto.IndividualProfileDto;
import ru.validum.api.mapper.ProfileMapper;
import ru.validum.core.profile.IndividualProfile;
import ru.validum.core.profile.ScenarioCatalog;

/**
 * Диспетчер именованных сценариев генерации профиля физлица.
 *
 * <p>Тонкая обёртка над {@link ScenarioCatalog}: каталог живёт в core, поэтому CLI и REST не могут
 * разойтись. Slug'и — часть публичного API и не меняются без мажорной версии.
 */
@Service
public class ScenarioService {

  private final ProfileMapper mapper;

  public ScenarioService(ProfileMapper mapper) {
    this.mapper = mapper;
  }

  /** Список доступных slug'ов сценариев (в порядке объявления). */
  public List<String> listSlugs() {
    return ScenarioCatalog.allSlugs();
  }

  /** Сгенерировать профиль по имени сценария. {@code seed == null} — случайный. */
  public IndividualProfileDto generate(String slug, Long seed) {
    ScenarioCatalog scenario =
      ScenarioCatalog.bySlug(slug)
        .orElseThrow(() -> new NoSuchElementException("Unknown scenario: " + slug));

    IndividualProfile profile = (seed == null) ? scenario.generate() : scenario.generate(seed);

    return mapper.toDto(profile);
  }
}
