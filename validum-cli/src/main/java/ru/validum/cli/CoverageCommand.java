package ru.validum.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
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

  private static final ObjectMapper MAPPER = new ObjectMapper();

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
    } catch (IOException e) {
      System.err.println("Ошибка чтения " + file + ": " + e.getMessage());
      return 2;
    } catch (RuntimeException e) {
      System.err.println("Ошибка: " + e.getMessage());
      return 2;
    }
  }

  private List<Map<String, String>> read() throws IOException {
    String mode = format == null ? null : format.toLowerCase(Locale.ROOT).replace('-', '_');
    if (mode == null) {
      mode = file.toString().toLowerCase(Locale.ROOT).endsWith(".csv") ? "csv" : "json";
    }
    return mode.startsWith("csv") ? readCsv() : readJson();
  }

  private List<Map<String, String>> readCsv() throws IOException {
    List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
    List<String> present = lines.stream().filter(line -> !line.isBlank()).toList();
    if (present.isEmpty()) {
      return List.of();
    }
    char delimiter = detectDelimiter(present.get(0));
    List<String> header = parseLine(stripBom(present.get(0)), delimiter);
    List<Map<String, String>> rows = new ArrayList<>();
    for (int i = 1; i < present.size(); i++) {
      List<String> values = parseLine(present.get(i), delimiter);
      Map<String, String> row = new LinkedHashMap<>();
      for (int c = 0; c < header.size(); c++) {
        row.put(header.get(c).trim(), c < values.size() ? values.get(c) : "");
      }
      rows.add(row);
    }
    return rows;
  }

  private static String stripBom(String line) {
    return line.startsWith("\uFEFF") ? line.substring(1) : line;
  }

  private static char detectDelimiter(String header) {
    long commas = header.chars().filter(c -> c == ',').count();
    long semicolons = header.chars().filter(c -> c == ';').count();
    return semicolons > commas ? ';' : ',';
  }

  private static List<String> parseLine(String line, char delimiter) {
    List<String> values = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean quoted = false;
    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      if (quoted) {
        if (c == '"') {
          if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
            current.append('"');
            i++;
          } else {
            quoted = false;
          }
        } else {
          current.append(c);
        }
      } else if (c == '"') {
        quoted = true;
      } else if (c == delimiter) {
        values.add(current.toString());
        current.setLength(0);
      } else {
        current.append(c);
      }
    }
    values.add(current.toString());
    return values;
  }

  private List<Map<String, String>> readJson() throws IOException {
    JsonNode root = MAPPER.readTree(file.toFile());
    List<JsonNode> items = new ArrayList<>();
    if (root.isArray()) {
      root.forEach(items::add);
    } else {
      items.add(root);
    }
    List<Map<String, String>> rows = new ArrayList<>();
    for (JsonNode item : items) {
      Map<String, String> row = new LinkedHashMap<>();
      item.fields().forEachRemaining(entry -> row.put(entry.getKey(), text(entry.getValue())));
      rows.add(row);
    }
    return rows;
  }

  private static String text(JsonNode node) {
    if (node == null || node.isNull()) {
      return "";
    }
    if (node.isValueNode()) {
      return node.asText();
    }
    return node.toString();
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
  }

  /** Регистрация в корневой команде выполняется через {@link CommandLine}; метод нужен для тестов. */
  static CommandLine commandLine() {
    return new CommandLine(new CoverageCommand());
  }
}
