package ru.validum.core.name;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;
import ru.validum.core.random.DataGenerator;

/**
 * Генератор ФИО.
 *
 * <p>Поддерживает два алфавита: {@link FioScript#CYRILLIC} (по умолчанию) и {@link
 * FioScript#LATIN} — для тестов валидации АБС, где ожидается отказ по недопустимым символам.
 *
 * <p>Для каждого пола — отдельный список имён и парных отчеств. Женская фамилия оканчивается на
 * «а» (cyrillic) / «a» (latin).
 */
public final class NameGenerator implements DataGenerator {

  private static List<String> loadLines(String resource) {
    try (var stream = NameGenerator.class.getResourceAsStream(resource)) {
      if (stream == null) {
        throw new IllegalStateException("Resource not found: " + resource);
      }
      try (var reader = new BufferedReader(
        new InputStreamReader(stream, StandardCharsets.UTF_8))) {
        return reader.lines()
          .map(String::trim)
          .filter(s -> !s.isEmpty())
          .toList();
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to load " + resource, e);
    }
  }

  // ── Кириллица ────────────────────────────────────────────────────────────

  private static final List<String> MALE_NAMES_CYR = loadLines("/names/male_names_cyr.txt");

  private static final List<String> MALE_PATRONYMICS_CYR = loadLines("/names/male_patronymics_cyr.txt");

  private static final List<String> FEMALE_NAMES_CYR = loadLines("/names/female_names_cyr.txt");

  private static final List<String> FEMALE_PATRONYMICS_CYR = loadLines("/names/female_patronymics_cyr.txt");

  private static final List<String> SURNAME_BASES_CYR = loadLines("/names/surnames_cyr.txt");

  // ── Латиница ─────────────────────────────────────────────────────────────

  private static final List<String> MALE_NAMES_LAT = loadLines("/names/male_names_lat.txt");

  private static final List<String> MALE_PATRONYMICS_LAT = loadLines("/names/male_patronymics_lat.txt");

  private static final List<String> FEMALE_NAMES_LAT = loadLines("/names/female_names_lat.txt");

  private static final List<String> FEMALE_PATRONYMICS_LAT = loadLines("/names/female_patronymics_lat.txt");

  private static final List<String> SURNAME_BASES_LAT = loadLines("/names/surnames_lat.txt");

  private final Random random;

  public NameGenerator() {
    this(DataGenerator.defaultRandom());
  }

  public NameGenerator(Random random) {
    this.random = random;
  }

  /** Случайное ФИО в кириллице. */
  public PersonName generate() {
    return generate(random.nextBoolean() ? Gender.MALE : Gender.FEMALE);
  }

  /** ФИО указанного пола в кириллице. */
  public PersonName generate(Gender gender) {
    return generate(gender, FioScript.CYRILLIC);
  }

  /** ФИО указанного пола в указанном алфавите. */
  public PersonName generate(Gender gender, FioScript script) {
    return script == FioScript.LATIN ? generateLatin(gender) : generateCyrillic(gender);
  }

  private PersonName generateCyrillic(Gender gender) {
    if (gender == Gender.MALE) {
      int i = random.nextInt(MALE_NAMES_CYR.size());
      String surname = SURNAME_BASES_CYR.get(random.nextInt(SURNAME_BASES_CYR.size()));
      return new PersonName(
        surname, MALE_NAMES_CYR.get(i), MALE_PATRONYMICS_CYR.get(i), Gender.MALE);
    }
    int i = random.nextInt(FEMALE_NAMES_CYR.size());
    String baseSurname = SURNAME_BASES_CYR.get(random.nextInt(SURNAME_BASES_CYR.size()));
    return new PersonName(
      baseSurname + "а",
      FEMALE_NAMES_CYR.get(i),
      FEMALE_PATRONYMICS_CYR.get(i),
      Gender.FEMALE);
  }

  private PersonName generateLatin(Gender gender) {
    if (gender == Gender.MALE) {
      int i = random.nextInt(MALE_NAMES_LAT.size());
      String surname = SURNAME_BASES_LAT.get(random.nextInt(SURNAME_BASES_LAT.size()));
      return new PersonName(
        surname, MALE_NAMES_LAT.get(i), MALE_PATRONYMICS_LAT.get(i), Gender.MALE);
    }
    int i = random.nextInt(FEMALE_NAMES_LAT.size());
    String baseSurname = SURNAME_BASES_LAT.get(random.nextInt(SURNAME_BASES_LAT.size()));
    return new PersonName(
      baseSurname + "a",
      FEMALE_NAMES_LAT.get(i),
      FEMALE_PATRONYMICS_LAT.get(i),
      Gender.FEMALE);
  }
}
