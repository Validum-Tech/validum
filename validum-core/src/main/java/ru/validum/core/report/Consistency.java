package ru.validum.core.report;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import ru.validum.core.common.Region;
import ru.validum.core.document.DocumentType;
import ru.validum.core.document.IdentityDocument;
import ru.validum.core.export.DocumentsCsv;

/**
 * Проверка согласованности готового набора: не только «поле заполнено и валидно», но и «поля
 * не противоречат друг другу». Отвечает на вопрос, который задают перед заливкой в стенд:
 * возраст не спорит с документами, паспорт не выдан в чужом регионе, нерезидент не получил
 * паспорт РФ, у счёта есть банк.
 *
 * <p>Работает по плоским строкам (CSV/JSON), поэтому не требует повторной генерации: набор
 * проверяется ровно в том виде, в каком его получили.
 */
public final class Consistency {

  /** Одно нарушение: номер строки в файле (строка 1 — заголовок), правило и подробность. */
  public record Violation(int row, String rule, String detail) {

    @Override
    public String toString() {
      return "строка " + row + ": " + rule + " — " + detail;
    }
  }

  private Consistency() {}

  /** Пустой список — набор согласован. */
  public static List<Violation> check(List<Map<String, String>> rows) {
    List<Violation> found = new ArrayList<>();
    for (int i = 0; i < rows.size(); i++) {
      Map<String, String> row = rows.get(i);
      int rowNumber = i + 2;
      checkDocumentsAge(row, rowNumber, found);
      checkResidency(row, rowNumber, found);
      checkIssuer(row, rowNumber, found);
      checkRegistrationRegion(row, rowNumber, found);
      checkAccountBank(row, rowNumber, found);
    }
    return List.copyOf(found);
  }

  /** Распределения по ключевым полям: сколько раз встретилось каждое значение. */
  public static Map<String, Map<String, Integer>> distributions(List<Map<String, String>> rows) {
    Map<String, Map<String, Integer>> result = new LinkedHashMap<>();
    result.put("пол", counts(rows, row -> row.get("gender")));
    result.put("резидентство", counts(rows, Consistency::residencyLabel));
    result.put("регион", counts(rows, Consistency::regionLabel));
    result.put("банк", counts(rows, row -> row.get("bankBik")));
    result.put("семейное положение", counts(rows, row -> row.get("familyStatus")));
    return result;
  }

  /** Возраст: минимум, максимум и среднее по строкам, где он указан. */
  public static Map<String, String> ageSummary(List<Map<String, String>> rows) {
    int min = Integer.MAX_VALUE;
    int max = Integer.MIN_VALUE;
    long sum = 0;
    int count = 0;
    for (Map<String, String> row : rows) {
      Integer age = intOf(row.get("age"));
      if (age == null) {
        continue;
      }
      min = Math.min(min, age);
      max = Math.max(max, age);
      sum += age;
      count++;
    }
    if (count == 0) {
      return Map.of();
    }
    return Map.of(
      "мин", String.valueOf(min),
      "макс", String.valueOf(max),
      "средний", String.format(java.util.Locale.ROOT, "%.1f", (double) sum / count));
  }

  // ------------------------------------------------------------------ правила

  private static void checkDocumentsAge(
    Map<String, String> row, int rowNumber, List<Violation> found) {
    Integer age = intOf(row.get("age"));
    if (age == null) {
      return;
    }
    for (IdentityDocument document : documents(row)) {
      DocumentType type = document.type();
      if (age < type.minAge()) {
        found.add(new Violation(
          rowNumber, "документ/возраст",
          type + " при возрасте " + age + " (минимальный — " + type.minAge() + ")"));
      } else if (type.maxAge() != null && age > type.maxAge()) {
        found.add(new Violation(
          rowNumber, "документ/возраст",
          type + " при возрасте " + age + " (максимальный — " + type.maxAge() + ")"));
      }
    }
  }

  private static void checkResidency(
    Map<String, String> row, int rowNumber, List<Violation> found) {
    String resident = row.get("isResident");
    if (!"false".equalsIgnoreCase(resident)) {
      return;
    }
    String passportType = row.get("passportType");
    if (passportType != null && !passportType.isBlank() && !"FOREIGN".equals(passportType)) {
      found.add(new Violation(
        rowNumber, "резидентство/паспорт",
        "нерезидент с паспортом типа " + passportType));
    }
    for (IdentityDocument document : documents(row)) {
      if (document.type() == DocumentType.PASSPORT_RF) {
        found.add(new Violation(
          rowNumber, "резидентство/документы", "нерезидент с паспортом РФ как ДУЛ"));
      }
    }
  }

  private static void checkIssuer(
    Map<String, String> row, int rowNumber, List<Violation> found) {
    String issuedBy = row.get("passportIssuedBy");
    if (issuedBy == null || issuedBy.isBlank()) {
      return;
    }
    String type = row.get("passportType");
    if (type != null && !type.isBlank() && !"RF".equals(type)) {
      return;
    }
    String series = row.get("passportSeries");
    if (series == null || series.length() < 2) {
      return;
    }
    String prefix = series.substring(0, 2);
    Region region = Region.byCode(prefix);
    if (region == null) {
      found.add(new Violation(
        rowNumber, "регион/кем выдан", "серия " + series + " — неизвестный код региона " + prefix));
      return;
    }
    String expected = region.department().ufms();
    if (!issuedBy.contains(expected)) {
      found.add(new Violation(
        rowNumber, "регион/кем выдан",
        "серия " + series + " (" + region + "), а выдал «" + issuedBy + "»"));
    }
    String departmentCode = row.get("passportDepartmentCode");
    if (departmentCode != null
      && !departmentCode.isBlank()
      && !departmentCode.startsWith(prefix)) {
      found.add(new Violation(
        rowNumber, "серия/код подразделения",
        "серия " + series + ", а код подразделения " + departmentCode));
    }
  }

  /** Паспорт РФ выдан в регионе прописки: серия и адрес не должны противоречить. */
  private static void checkRegistrationRegion(
    Map<String, String> row, int rowNumber, List<Violation> found) {
    String type = row.get("passportType");
    if (type != null && !type.isBlank() && !"RF".equals(type)) {
      return;
    }
    String resident = row.get("isResident");
    if (resident != null && !resident.isBlank() && !Boolean.parseBoolean(resident)) {
      return;
    }
    String series = row.get("passportSeries");
    String registrationRegion = row.get("registrationRegion");
    if (series == null || series.length() < 2
      || registrationRegion == null || registrationRegion.isBlank()) {
      return;
    }
    Region region = Region.byCode(series.substring(0, 2));
    if (region == null) {
      return;
    }
    // Имя региона в КЛАДР и в Region может отличаться суффиксом («Московская» / «Московская
    // область»), поэтому сравниваем через справочник, а не строкой.
    Region declared = Region.byNameOrCode(registrationRegion.trim());
    boolean sameRegion = declared != null
      ? declared == region
      : registrationRegion.trim().equals(region.regionName());
    if (!sameRegion) {
      found.add(new Violation(
        rowNumber, "серия/регион прописки",
        "серия " + series + " (" + region.regionName() + "), прописка — " + registrationRegion));
    }
  }

  private static void checkAccountBank(
    Map<String, String> row, int rowNumber, List<Violation> found) {
    String account = row.get("account");
    String bik = row.get("bankBik");
    if (account != null && !account.isBlank() && (bik == null || bik.isBlank())) {
      found.add(new Violation(rowNumber, "счёт/банк", "счёт без БИК банка"));
    }
  }

  // ------------------------------------------------------------------ утилиты

  private static List<IdentityDocument> documents(Map<String, String> row) {
    String cell = row.get(DocumentsCsv.COLUMN);
    if (cell == null || cell.isBlank()) {
      return List.of();
    }
    try {
      return DocumentsCsv.parseInline(cell);
    } catch (RuntimeException e) {
      return List.of();
    }
  }

  private static String residencyLabel(Map<String, String> row) {
    String resident = row.get("isResident");
    if (resident == null || resident.isBlank()) {
      return null;
    }
    return "true".equalsIgnoreCase(resident) ? "резидент" : "нерезидент";
  }

  private static String regionLabel(Map<String, String> row) {
    String series = row.get("passportSeries");
    if (series != null && series.length() >= 2) {
      Region region = Region.byCode(series.substring(0, 2));
      if (region != null) {
        return region.department().ufms();
      }
    }
    return row.get("registrationCity");
  }

  private static Map<String, Integer> counts(
    List<Map<String, String>> rows, Function<Map<String, String>, String> extractor) {
    Map<String, Integer> counts = new LinkedHashMap<>();
    for (Map<String, String> row : rows) {
      String value = extractor.apply(row);
      if (value == null || value.isBlank()) {
        continue;
      }
      counts.merge(value, 1, Integer::sum);
    }
    return counts;
  }

  private static Integer intOf(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
