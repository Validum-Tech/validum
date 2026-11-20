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

  /**
   * Сколько различных кириллических ФИО генератор может выдать в принципе.
   *
   * <p>Фамилий × (мужских пар «имя + отчество» + женских пар). Используется CLI-флагом {@code
   * --unique-name}, чтобы честно отказать, когда пул исчерпан.
   */
  public static long capacity() {
    long male = (long) MALE_NAMES_CYR.size() * MALE_PATRONYMICS_CYR.size();
    long female = (long) FEMALE_NAMES_CYR.size() * FEMALE_PATRONYMICS_CYR.size();
    return (long) SURNAME_BASES_CYR.size() * (male + female);
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

  /**
   * Женская форма фамилии по её типу.
   *
   * <p>Фамилии на «-ский/-цкий» меняют «-ий» на «-ая», неизменяемые («-ко», «-енко», «-ук», «-юк»,
   * «-ич», «-ян», «-дзе», «-швили») остаются как есть, остальные получают окончание «-а».
   */
  static String femaleSurname(String base) {
    if (base.endsWith("ский") || base.endsWith("цкий")) {
      return base.substring(0, base.length() - 2) + "ая";
    }
    if (base.endsWith("ко") || base.endsWith("енко") || base.endsWith("ук") || base.endsWith("юк")
      || base.endsWith("ич") || base.endsWith("ян") || base.endsWith("дзе") || base.endsWith("швили")) {
      return base;
    }
    return base + "а";
  }

  /** То же правило для латиницы: «-sky/-tsky» → «-skaya/-tskaya», «-ko/-enko/...» не меняются. */
  static String femaleSurnameLatin(String base) {
    if (base.endsWith("sky") || base.endsWith("tsky")) {
      return base.substring(0, base.length() - 1) + "aya";
    }
    if (base.endsWith("ko") || base.endsWith("enko") || base.endsWith("uk") || base.endsWith("yuk")
      || base.endsWith("ich") || base.endsWith("yan")) {
      return base;
    }
    return base + "a";
  }

  private PersonName generateCyrillic(Gender gender) {
    if (gender == Gender.MALE) {
      String surname = SURNAME_BASES_CYR.get(random.nextInt(SURNAME_BASES_CYR.size()));
      String firstName = MALE_NAMES_CYR.get(random.nextInt(MALE_NAMES_CYR.size()));
      String patronymic = MALE_PATRONYMICS_CYR.get(random.nextInt(MALE_PATRONYMICS_CYR.size()));
      return new PersonName(surname, firstName, patronymic, Gender.MALE);
    }
    String baseSurname = SURNAME_BASES_CYR.get(random.nextInt(SURNAME_BASES_CYR.size()));
    String firstName = FEMALE_NAMES_CYR.get(random.nextInt(FEMALE_NAMES_CYR.size()));
    String patronymic = FEMALE_PATRONYMICS_CYR.get(random.nextInt(FEMALE_PATRONYMICS_CYR.size()));
    return new PersonName(femaleSurname(baseSurname), firstName, patronymic, Gender.FEMALE);
  }

  private PersonName generateLatin(Gender gender) {
    if (gender == Gender.MALE) {
      String surname = SURNAME_BASES_LAT.get(random.nextInt(SURNAME_BASES_LAT.size()));
      String firstName = MALE_NAMES_LAT.get(random.nextInt(MALE_NAMES_LAT.size()));
      String patronymic = MALE_PATRONYMICS_LAT.get(random.nextInt(MALE_PATRONYMICS_LAT.size()));
      return new PersonName(surname, firstName, patronymic, Gender.MALE);
    }
    String baseSurname = SURNAME_BASES_LAT.get(random.nextInt(SURNAME_BASES_LAT.size()));
    String firstName = FEMALE_NAMES_LAT.get(random.nextInt(FEMALE_NAMES_LAT.size()));
    String patronymic = FEMALE_PATRONYMICS_LAT.get(random.nextInt(FEMALE_PATRONYMICS_LAT.size()));
    return new PersonName(femaleSurnameLatin(baseSurname), firstName, patronymic, Gender.FEMALE);
  }
}
