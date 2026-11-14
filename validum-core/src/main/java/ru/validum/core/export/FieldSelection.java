package ru.validum.core.export;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Выбор колонок на выходе: {@code --fields fullName,inn,age} или пресет {@code short}.
 *
 * <p>Фильтрация идёт по уже развёрнутым строкам (плоская выдача), поэтому работает для CSV,
 * JSON, JSON Array и SQL одинаково. Порядок колонок — как в профиле, чтобы CSV оставался
 * стабильным между прогонами.
 */
public final class FieldSelection {

  /** «Человеческий» набор: кто это, ключевые реквизиты и адрес — без служебных полей. */
  private static final List<String> SHORT_KEYS = List.of(
    "personId",
    "fullName",
    "gender",
    "birthDate",
    "age",
    "inn",
    "snils",
    "passportSeries",
    "passportNumber",
    "passportIssuedBy",
    "registrationRegion",
    "registrationCity",
    "bankBik",
    "account",
    "documents");

  /** Тот же пресет для ЮЛ: у организаций нет паспорта, но есть ОГРН/КПП/ОКВЭД. */
  private static final List<String> SHORT_KEYS_ORGANIZATION = List.of(
    "organizationName",
    "organizationType",
    "inn",
    "kpp",
    "ogrn",
    "okved",
    "okvedName",
    "registrationRegion",
    "registrationCity",
    "bankBik",
    "account");

  private FieldSelection() {}

  /**
   * Оставляет только запрошенные колонки.
   *
   * @param rows уже развёрнутые строки
   * @param spec список через запятую либо {@code short}; пусто — вернуть как есть
   * @throws IllegalArgumentException если запрошено поле, которого в наборе нет
   */
  public static List<Map<String, String>> keep(List<Map<String, String>> rows, String spec) {
    if (rows.isEmpty() || spec == null || spec.isBlank()) {
      return rows;
    }
    List<String> keys = resolve(rows.get(0), spec);
    List<Map<String, String>> result = new ArrayList<>(rows.size());
    for (Map<String, String> row : rows) {
      Map<String, String> kept = new LinkedHashMap<>();
      for (Map.Entry<String, String> entry : row.entrySet()) {
        if (keys.contains(entry.getKey())) {
          kept.put(entry.getKey(), entry.getValue());
        }
      }
      result.add(kept);
    }
    return result;
  }

  private static List<String> resolve(Map<String, String> sample, String spec) {
    if ("short".equals(spec.trim().toLowerCase(Locale.ROOT))) {
      Set<String> present = sample.keySet();
      List<String> preset = new ArrayList<>();
      for (String key : SHORT_KEYS) {
        if (present.contains(key)) {
          preset.add(key);
        }
      }
      for (String key : SHORT_KEYS_ORGANIZATION) {
        if (present.contains(key) && !preset.contains(key)) {
          preset.add(key);
        }
      }
      if (preset.isEmpty()) {
        throw new IllegalArgumentException(
          "Пресет short не подходит этому набору: в нём нет ожидаемых колонок");
      }
      return preset;
    }

    List<String> requested = new ArrayList<>();
    for (String raw : spec.split(",")) {
      String key = raw.trim();
      if (!key.isEmpty()) {
        requested.add(key);
      }
    }
    List<String> unknown = new ArrayList<>();
    for (String key : requested) {
      if (!sample.containsKey(key)) {
        unknown.add(key);
      }
    }
    if (!unknown.isEmpty()) {
      throw new IllegalArgumentException(
        "Неизвестные колонки: " + String.join(", ", unknown)
          + ". В наборе есть, например: " + preview(sample));
    }
    return requested;
  }

  private static String preview(Map<String, String> sample) {
    List<String> keys = new ArrayList<>(sample.keySet());
    int limit = Math.min(keys.size(), 10);
    return String.join(", ", keys.subList(0, limit)) + (keys.size() > limit ? ", …" : "");
  }
}
