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
import ru.validum.core.export.CsvExporter;
import ru.validum.core.export.CsvImporter;

/**
 * Чтение готового набора — CSV (машинный и «дружелюбный» для Excel) или JSON/JSON-массив.
 *
 * <p>Один разбор на все команды, которые работают с уже сгенерированными файлами ({@code coverage},
 * {@code describe}): иначе «coverage видит не то, что describe» — классический источник тихих
 * расхождений в отчётах.
 *
 * <p>CSV разбирает {@link CsvImporter} — тот же RFC 4180-парсер, что и в ядре: понимает кавычки,
 * переносы строк внутри значений, BOM и оба разделителя. Здесь добавляется только перевод русских
 * заголовков в ключи и снятие Excel-обёртки {@code ="…"}.
 */
final class DatasetReader {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private DatasetReader() {}

  /** Строки набора с ключами-колонками; у «дружелюбного» CSV заголовки переводятся в ключи. */
  static List<Map<String, String>> read(Path file) throws IOException {
    return read(file, null);
  }

  static List<Map<String, String>> read(Path file, String format) throws IOException {
    String mode = format == null ? null : format.toLowerCase(Locale.ROOT).replace('-', '_');
    if (mode == null) {
      mode = file.toString().toLowerCase(Locale.ROOT).endsWith(".csv") ? "csv" : "json";
    }
    return mode.startsWith("csv") ? readCsv(file) : readJson(file);
  }

  private static List<Map<String, String>> readCsv(Path file) throws IOException {
    String text = Files.readString(file, StandardCharsets.UTF_8);
    if (text.isBlank()) {
      return List.of();
    }
    char delimiter = CsvImporter.detectDelimiter(firstLine(text));
    List<Map<String, String>> parsed = CsvImporter.parse(text, delimiter);
    List<Map<String, String>> rows = new ArrayList<>(parsed.size());
    for (Map<String, String> parsedRow : parsed) {
      Map<String, String> row = new LinkedHashMap<>();
      parsedRow.forEach(
        (column, value) ->
          row.put(CsvExporter.internalKey(column.trim()), CsvImporter.unquoteFormula(value))
      );
      rows.add(row);
    }
    return rows;
  }

  private static String firstLine(String text) {
    int end = text.indexOf('\n');
    String line = end < 0 ? text : text.substring(0, end);
    return line.endsWith("\r") ? line.substring(0, line.length() - 1) : line;
  }

  private static List<Map<String, String>> readJson(Path file) throws IOException {
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
}
