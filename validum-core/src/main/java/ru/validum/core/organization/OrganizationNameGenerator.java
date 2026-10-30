package ru.validum.core.organization;

import java.util.List;
import java.util.Random;
import ru.validum.core.common.OrganizationType;
import ru.validum.core.random.DataGenerator;

/**
 * Генератор наименований организаций.
 *
 * <p>Формат: {@code ООО «Ромашка»}, {@code АО «Восход»}, {@code ПАО «Северная звезда»}.
 * Префикс берётся из {@link OrganizationType}, наименование — из набора.
 */
public final class OrganizationNameGenerator implements DataGenerator {

  private static final List<String> NAMES =
    List.of(
      "Ромашка", "Восход", "Заря", "Прогресс", "Северная звезда",
      "Гранит", "Спектр", "Азимут", "Радуга", "Импульс",
      "Ориентир", "Кристалл", "Маяк", "Вектор", "Стрела",
      "Полярная", "Гелиос", "Орбита", "Титан", "Атлант");

  private final Random random;

  public OrganizationNameGenerator() {
    this(DataGenerator.defaultRandom());
  }

  public OrganizationNameGenerator(Random random) {
    this.random = random;
  }

  /** Наименование для ООО (по умолчанию). */
  public String generate() {
    return generate(OrganizationType.LLC);
  }

  /** Наименование для указанного типа организации. */
  public String generate(OrganizationType type) {
    String name = NAMES.get(random.nextInt(NAMES.size()));
    return type.prefix() + " «" + name + "»";
  }
}
