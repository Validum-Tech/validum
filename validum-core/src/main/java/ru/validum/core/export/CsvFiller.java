package ru.validum.core.export;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import ru.validum.core.bank.Bank;
import ru.validum.core.bank.BikSource;
import ru.validum.core.bank.BikSources;
import ru.validum.core.name.Gender;
import ru.validum.core.profile.IndividualProfile;
import ru.validum.core.profile.IndividualProfileGenerator;

/**
 * Заполняет пустые ячейки CSV валидными значениями (сценарий P4.1).
 *
 * <p>Для каждой строки генерируется обычный связанный {@link IndividualProfile}, из него
 * получается плоская map через {@link IndividualProfileFlattener}, и в исходную строку
 * подставляются только те значения, где ячейка пустая. Непустые значения не трогаются никогда —
 * это контракт «fill gaps», а не «нормализуй всё».
 *
 * <p><b>Подсказки из строки.</b> Чтобы сгенерированные значения не противоречили уже заполненным,
 * из строки читаются:
 *
 * <ul>
 *   <li>{@code gender} — пол ФИО и паспорта;
 *   <li>{@code age} — возраст и, как следствие, дата рождения;
 *   <li>{@code bankBik} — если банк найден в справочнике, остальные банковские колонки берутся из
 *       него; если БИК в справочнике не найден, банковские колонки не заполняются вовсе, чтобы не
 *       подмешать чужой банк.
 * </ul>
 *
 * <p>Колонки, которых не было в исходном CSV, добавляются в конец строки — на выходе получается
 * полный профиль, пригодный для повторного {@link ProfileFromCsv}.
 */
public final class CsvFiller {

  private final BikSource bikSource;
  private final Random random;
  private final LocalDate asOf;

  /** Справочник по умолчанию ({@link BikSources#defaultSource()}), случайный seed. */
  public CsvFiller() {
    this(BikSources.defaultSource(), null);
  }

  /** Свой справочник банков, случайный seed. */
  public CsvFiller(BikSource bikSource) {
    this(bikSource, null);
  }

  /**
   * Свой справочник и свой {@link Random} (для детерминированных тестов).
   *
   * @param random если {@code null} — используется случайный seed генератора
   */
  public CsvFiller(BikSource bikSource, Random random) {
    this(bikSource, random, null);
  }

  /**
   * Свой справочник, свой {@link Random} и фиксированная дата расчёта.
   *
   * @param asOf дата, от которой считаются возраст и сроки документов; {@code null} — сегодня
   */
  public CsvFiller(BikSource bikSource, Random random, LocalDate asOf) {
    this.bikSource = bikSource;
    this.random = random;
    this.asOf = asOf;
  }

  /** Заполняет список строк. */
  public List<Map<String, String>> fill(List<Map<String, String>> rows) {
    List<Map<String, String>> out = new ArrayList<>(rows.size());
    for (Map<String, String> row : rows) {
      out.add(fillRow(row));
    }
    return out;
  }

  /** Заполняет одну строку. */
  public Map<String, String> fillRow(Map<String, String> row) {
    IndividualProfileGenerator.Builder builder =
      IndividualProfileGenerator.builder().bikSource(bikSource);
    if (random != null) {
      builder.random(random);
    }
    LocalDate rowAsOf = asOf != null ? asOf : parseAsOf(row.get("asOf"));
    if (rowAsOf != null) {
      builder.asOf(rowAsOf);
    }

    String gender = row.get("gender");
    if (gender != null && !gender.isBlank()) {
      try {
        builder.gender(Gender.valueOf(gender.trim()));
      } catch (IllegalArgumentException ignored) {
        // неизвестный пол — оставим случайный
      }
    }

    String age = row.get("age");
    if (age != null && !age.isBlank()) {
      try {
        builder.age(Integer.parseInt(age.trim()));
      } catch (NumberFormatException ignored) {
        // неизвестный возраст — оставим дефолт
      }
    }

    boolean fillBank = true;
    String bik = row.get("bankBik");
    if (bik != null && !bik.isBlank()) {
      Optional<Bank> found = bikSource.findByBik(bik.trim());
      if (found.isPresent()) {
        builder.bank(found.get());
      } else {
        fillBank = false;
      }
    }

    IndividualProfile profile = builder.build().generate();
    Map<String, String> generated = IndividualProfileFlattener.flatten(profile);

    Map<String, String> merged = new LinkedHashMap<>();
    for (Map.Entry<String, String> entry : row.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      if (isBlank(value)) {
        merged.put(key, !fillBank && key.startsWith("bank") ? "" : valueOf(generated, key));
      } else {
        merged.put(key, value);
      }
    }
    for (Map.Entry<String, String> entry : generated.entrySet()) {
      String key = entry.getKey();
      if (merged.containsKey(key)) {
        continue;
      }
      if (!fillBank && key.startsWith("bank")) {
        continue;
      }
      merged.put(key, valueOf(generated, key));
    }
    return merged;
  }

  private static String valueOf(Map<String, String> map, String key) {
    String value = map.get(key);
    return value == null ? "" : value;
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private static LocalDate parseAsOf(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(value.trim());
    } catch (DateTimeParseException e) {
      return null;
    }
  }
}
