package ru.validum.core.report;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ru.validum.core.account.AccountValidator;
import ru.validum.core.validation.InnValidator;
import ru.validum.core.validation.KppValidator;
import ru.validum.core.validation.OgrnValidator;
import ru.validum.core.validation.OgrnipValidator;
import ru.validum.core.validation.OkpoValidator;
import ru.validum.core.validation.SnilsValidator;

/**
 * Отчёт о покрытии готового набора: для каждого поля — доля заполненных, число различных значений и
 * сколько значений не проходит валидацию. Нужен, чтобы перед заливкой в стенд увидеть «дыры» набора
 * (пустые СНИЛС, невалидные счета) и не искать их потом руками.
 *
 * <p>Валидируются только поля с публичной проверкой контрольной суммы; остальные — по полноте.
 */
public final class Coverage {

  /** Порядок полей в отчёте; чего нет в наборе — не печатается. */
  public static final List<String> PREFERRED_ORDER = List.of(
    "fullName", "gender", "birthDate", "age", "inn", "snils",
    "passportSeries", "passportNumber", "passportIssuedAt", "passportIssuer",
    "organizationName", "organizationType", "ogrn", "ogrnip", "kpp", "okpo",
    "bankBik", "account", "address", "okved", "asOf");

  /** Есть ли публичная проверка контрольной суммы у поля. */
  public static boolean isChecked(String field) {
    return switch (field) {
      case "inn", "snils", "ogrn", "ogrnip", "account", "kpp", "okpo", "bankBik" -> true;
      default -> false;
    };
  }

  /**
   * Проверка значения; {@code null} — поле не проверяется.
   *
   * @param row вся строка: счёту нужен БИК банка, чтобы посчитать условный номер подразделения
   */
  public static Boolean valid(String field, String value, Map<String, String> row) {
    if (value == null || value.isBlank()) {
      return false;
    }
    return switch (field) {
      case "inn" -> InnValidator.isValid(value);
      case "snils" -> SnilsValidator.isValid(value);
      case "ogrn" -> OgrnValidator.isValidLegalEntity(value);
      case "ogrnip" -> OgrnipValidator.isValid(value);
      case "kpp" -> KppValidator.isValid(value);
      case "okpo" -> OkpoValidator.isValid(value);
      // BikValidator требует справочник; здесь проверяем только формат (9 цифр).
      case "bankBik" -> value.matches("\\d{9}");
      case "account" -> AccountValidator.isValidAuto(value, bankOf(row));
      default -> null;
    };
  }

  private static String bankOf(Map<String, String> row) {
    for (String key : List.of("bankBik", "bik")) {
      String value = row.get(key);
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }

  /** Поля в порядке отчёта: сначала известные, затем прочие по алфавиту. */
  public static List<String> fieldsOf(List<Map<String, String>> rows) {
    Set<String> keys = new LinkedHashSet<>();
    for (Map<String, String> row : rows) {
      keys.addAll(row.keySet());
    }
    List<String> ordered = new ArrayList<>();
    for (String field : PREFERRED_ORDER) {
      if (keys.remove(field)) {
        ordered.add(field);
      }
    }
    keys.stream().sorted().forEach(ordered::add);
    return ordered;
  }

  /** Статистика по одному полю, посчитанная на наборе. */
  public static FieldStat stat(String field, List<Map<String, String>> rows) {
    int total = rows.size();
    int present = 0;
    int blank = 0;
    int invalid = 0;
    Set<String> distinct = new LinkedHashSet<>();
    for (Map<String, String> row : rows) {
      String value = row.get(field);
      if (value == null) {
        continue;
      }
      if (value.isBlank()) {
        blank++;
        continue;
      }
      present++;
      distinct.add(value);
      if (isChecked(field) && Boolean.FALSE.equals(valid(field, value, row))) {
        invalid++;
      }
    }
    return new FieldStat(field, total, present, blank, invalid, distinct.size());
  }

  /** Статистика по всем полям набора. */
  public static List<FieldStat> analyze(List<Map<String, String>> rows) {
    List<FieldStat> stats = new ArrayList<>();
    for (String field : fieldsOf(rows)) {
      stats.add(stat(field, rows));
    }
    return stats;
  }

  /**
   * Итог по одному полю.
   *
   * @param present сколько строк с непустым значением
   * @param blank   сколько строк, где колонка есть, но пуста
   * @param invalid сколько непустых значений не прошли проверку (0 у непроверяемых полей)
   * @param distinct сколько различных значений
   */
  public record FieldStat(String field, int total, int present, int blank, int invalid, int distinct) {

    /** Полнота в процентах (заполнено / всего строк). */
    public int fillPercent() {
      return total == 0 ? 0 : (int) Math.round(100.0 * present / total);
    }

    /** Все непустые значения прошли проверку (или поле не проверяется). */
    public boolean clean() {
      return invalid == 0;
    }
  }
}
