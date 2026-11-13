package ru.validum.cli;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import ru.validum.core.report.Consistency;
import ru.validum.core.report.Coverage;

/**
 * Отчёт о покрытии: во что превратился набор файлом — сколько строк заполнено, сколько значений
 * различных и проходят ли ключевые реквизиты контрольные суммы. Работает и с CSV (машинным и
 * «дружелюбным» для Excel), и с JSON / JSON-массивом.
 */
@Command(
  name = "coverage",
  description = "Отчёт о покрытии набора: полнота и валидность ключевых полей")
public final class CoverageCommand implements Callable<Integer> {

  @Option(
    names = "--file",
    required = true,
    paramLabel = "<csv|json>",
    description = "Файл набора: CSV или JSON")
  Path file;

  @Option(
    names = "--format",
    paramLabel = "<fmt>",
    description = "csv|csv-semicolon|json|json-array (по умолчанию — по расширению файла)")
  String format;

  @Override
  public Integer call() {
    try {
      List<Map<String, String>> rows = read();
      if (rows.isEmpty()) {
        System.err.println("Ошибка: во входном файле нет ни одной записи");
        return 2;
      }
      print(rows);
      return 0;
    } catch (java.nio.file.NoSuchFileException e) {
      System.err.println("Ошибка: файл не найден — " + file.toAbsolutePath());
      return 2;
    } catch (IOException e) {
      System.err.println("Ошибка чтения " + file + ": " + e.getMessage());
      return 2;
    } catch (RuntimeException e) {
      System.err.println("Ошибка: " + e.getMessage());
      return 2;
    }
  }

  private List<Map<String, String>> read() throws IOException {
    return DatasetReader.read(file, format);
  }

  private void print(List<Map<String, String>> rows) {
    System.out.println(
      "# validum " + BuildInfo.version() + " coverage · file=" + file + " · rows=" + rows.size());
    System.out.println();

    String header = "%-18s %11s %10s %11s".formatted("поле", "заполнено", "различных", "невалидных");
    System.out.println(header);
    System.out.println("-".repeat(header.length()));

    List<Coverage.FieldStat> stats = Coverage.analyze(rows);
    int invalidFields = 0;
    int blankFields = 0;
    for (Coverage.FieldStat stat : stats) {
      String checked = Coverage.isChecked(stat.field()) ? String.valueOf(stat.invalid()) : "—";
      System.out.printf(
        "%-18s %5d%% (%d/%d) %10d %11s%n",
        stat.field(), stat.fillPercent(), stat.present(), stat.total(), stat.distinct(), checked);
      if (stat.invalid() > 0) {
        invalidFields++;
      }
      if (stat.blank() > 0) {
        blankFields++;
      }
    }
    System.out.println();
    System.out.printf(
      "итог: %d полей, %d с невалидными значениями, %d с пустыми%n",
      stats.size(), invalidFields, blankFields);

    printConsistency(rows);
    printDistributions(rows);
  }

  private void printConsistency(List<Map<String, String>> rows) {
    List<Consistency.Violation> violations = Consistency.check(rows);
    System.out.println();
    if (violations.isEmpty()) {
      System.out.println("Согласованность: нарушений нет");
      return;
    }
    System.out.println("Согласованность: " + violations.size() + " нарушений");
    for (Consistency.Violation violation : violations) {
      System.out.println("  " + violation);
    }
  }

  private void printDistributions(List<Map<String, String>> rows) {
    System.out.println();
    System.out.println("Распределения:");
    Map<String, String> ages = Consistency.ageSummary(rows);
    if (!ages.isEmpty()) {
      System.out.printf(
        "  возраст: %s–%s, средний %s%n", ages.get("мин"), ages.get("макс"), ages.get("средний"));
    }
    for (Map.Entry<String, Map<String, Integer>> entry
        : Consistency.distributions(rows).entrySet()) {
      Map<String, Integer> counts = entry.getValue();
      if (counts.isEmpty()) {
        continue;
      }
      int total = counts.values().stream().mapToInt(Integer::intValue).sum();
      String values = counts.entrySet().stream()
        .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
        .limit(6)
        .map(e -> "%s %d%%".formatted(e.getKey(), Math.round(100.0 * e.getValue() / total)))
        .collect(java.util.stream.Collectors.joining(", "));
      String tail = counts.size() > 6 ? " (+%d ещё)".formatted(counts.size() - 6) : "";
      System.out.printf("  %s: %s%s%n", entry.getKey(), values, tail);
    }
  }

  /** Регистрация в корневой команде выполняется через {@link CommandLine}; метод нужен для тестов. */
  static CommandLine commandLine() {
    return new CommandLine(new CoverageCommand());
  }
}
