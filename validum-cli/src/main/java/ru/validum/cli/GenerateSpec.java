package ru.validum.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Спецификация набора {@code .validum.json} (W47-C1).
 *
 * <p>Одна декларация вместо простыни флагов:
 *
 * <pre>{@code
 * {"type": "individual", "count": 100, "seed": 42, "asOf": "2026-01-01",
 *  "format": "csv-semicolon", "output": "clients.csv", "unique": true,
 *  "options": {"region": "MOSCOW", "age": 35}}
 * }</pre>
 *
 * <p>Ключ {@code options} необязателен — его поля поднимаются на верхний уровень. Неизвестный ключ
 * (опечатка вроде {@code "formt"}) отклоняется с подсказкой: тихо проигнорированная опция — худший
 * вид ошибки в генераторе тестовых данных.
 */
public record GenerateSpec(
    String type,
    Integer count,
    Long seed,
    String asOf,
    String format,
    String output,
    Boolean unique,
    Boolean terrorist,
    Boolean blocked,
    Boolean noAccount,
    String region,
    Integer age,
    String gender,
    String bik,
    String okved,
    String organizationType,
    String organizationName,
    String documents,
    String documentsMode,
    String dialect,
    String table,
    Boolean noDdl) {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final Set<String> ALLOWED = Set.of(
    "type", "count", "seed", "asOf", "format", "output", "unique", "terrorist", "blocked",
    "noAccount", "region", "age", "gender", "bik", "okved", "organizationType",
    "organizationName", "documents", "documentsMode", "dialect", "table", "noDdl", "options");

  /** Читает и валидирует спецификацию; ошибки — {@link IllegalArgumentException} с понятным текстом. */
  public static GenerateSpec load(Path file) {
    JsonNode root;
    try {
      root = MAPPER.readTree(file.toFile());
    } catch (IOException e) {
      throw new IllegalArgumentException("Не удалось прочитать " + file + ": " + e.getMessage());
    }
    if (root == null || !root.isObject()) {
      throw new IllegalArgumentException("Спецификация должна быть JSON-объектом: " + file);
    }

    JsonNode merged = root.deepCopy();
    JsonNode options = merged.get("options");
    if (options != null) {
      if (!options.isObject()) {
        throw new IllegalArgumentException("Поле \"options\" должно быть объектом");
      }
      ((com.fasterxml.jackson.databind.node.ObjectNode) merged).remove("options");
      Iterator<String> names = options.fieldNames();
      while (names.hasNext()) {
        String name = names.next();
        if (!ALLOWED.contains(name)) {
          throw new IllegalArgumentException("Неизвестный ключ в \"options\": \"" + name + "\"");
        }
        if (!merged.has(name)) {
          ((com.fasterxml.jackson.databind.node.ObjectNode) merged).set(name, options.get(name));
        }
      }
    }

    List<String> unknown = new ArrayList<>();
    merged.fieldNames().forEachRemaining(name -> {
      if (!ALLOWED.contains(name)) unknown.add(name);
    });
    if (!unknown.isEmpty()) {
      throw new IllegalArgumentException("Неизвестные ключи: " + unknown
        + ". Допустимо: " + new LinkedHashSet<>(ALLOWED).stream().sorted().toList());
    }

    String type = normalizeType(text(merged, "type"));
    if (type == null) {
      throw new IllegalArgumentException(
        "Обязательное поле \"type\": individual | ip | organization | documents");
    }

    Integer count = integer(merged, "count");
    if (count != null && (count < 1 || count > 10_000)) {
      throw new IllegalArgumentException("Поле \"count\" должно быть в диапазоне 1..10000");
    }

    Long seed = longValue(merged, "seed");
    Boolean unique = bool(merged, "unique");
    if (Boolean.TRUE.equals(unique) && seed == null) {
      throw new IllegalArgumentException(
        "При \"unique\": true нужен \"seed\", иначе набор невоспроизводим");
    }
    if (seed == null && merged.has("seed") && !merged.get("seed").isNull()) {
      throw new IllegalArgumentException("Поле \"seed\" должно быть целым числом");
    }

    return new GenerateSpec(
      type,
      count,
      seed,
      text(merged, "asOf"),
      text(merged, "format"),
      text(merged, "output"),
      unique,
      bool(merged, "terrorist"),
      bool(merged, "blocked"),
      bool(merged, "noAccount"),
      text(merged, "region"),
      integer(merged, "age"),
      text(merged, "gender"),
      text(merged, "bik"),
      text(merged, "okved"),
      text(merged, "organizationType"),
      text(merged, "organizationName"),
      documents(merged),
      text(merged, "documentsMode"),
      text(merged, "dialect"),
      text(merged, "table"),
      bool(merged, "noDdl"));
  }

  /** Копия с другим путём вывода — {@code -o} в командной строке важнее файла. */
  public GenerateSpec withOutput(Path path) {
    return new GenerateSpec(
      type, count, seed, asOf, format, path.toString(), unique, terrorist, blocked, noAccount,
      region, age, gender, bik, okved, organizationType, organizationName, documents,
      documentsMode, dialect, table, noDdl);
  }

  /** Допускает русские и короткие синонимы: ФЛ/ИП/ЮЛ вместо громоздких английских. */
  private static String normalizeType(String raw) {
    if (raw == null) {
      return null;
    }
    return switch (raw.trim().toLowerCase(Locale.ROOT)) {
      case "individual", "fl", "фл", "физлицо", "person" -> "individual";
      case "ip", "ип", "предприниматель" -> "ip";
      case "organization", "ul", "юл", "organizationprofile", "org" -> "organization";
      case "documents", "document", "документы", "doc" -> "documents";
      default -> throw new IllegalArgumentException(
        "Неизвестный \"type\": \"" + raw
          + "\". Допустимо: individual | ip | organization | documents");
    };
  }

  private static String text(JsonNode node, String name) {
    JsonNode value = node.get(name);
    if (value == null || value.isNull()) {
      return null;
    }
    if (value.isTextual()) {
      return value.asText();
    }
    if (value.isNumber() || value.isBoolean()) {
      return value.asText();
    }
    throw new IllegalArgumentException("Поле \"" + name + "\" должно быть строкой");
  }

  private static Integer integer(JsonNode node, String name) {
    JsonNode value = node.get(name);
    if (value == null || value.isNull()) {
      return null;
    }
    if (value.isInt() || value.isLong()) {
      return value.asInt();
    }
    throw new IllegalArgumentException("Поле \"" + name + "\" должно быть целым числом");
  }

  private static Long longValue(JsonNode node, String name) {
    JsonNode value = node.get(name);
    if (value == null || value.isNull()) {
      return null;
    }
    if (value.isNumber()) {
      return value.asLong();
    }
    throw new IllegalArgumentException("Поле \"" + name + "\" должно быть целым числом");
  }

  private static Boolean bool(JsonNode node, String name) {
    JsonNode value = node.get(name);
    if (value == null || value.isNull()) {
      return null;
    }
    if (value.isBoolean()) {
      return value.asBoolean();
    }
    throw new IllegalArgumentException("Поле \"" + name + "\" должно быть true или false");
  }

  /** {@code "documents"} принимается и строкой {@code "21,22"}, и массивом {@code ["21","22"]}. */
  private static String documents(JsonNode node) {
    JsonNode value = node.get("documents");
    if (value == null || value.isNull()) {
      return null;
    }
    if (value.isTextual()) {
      return value.asText();
    }
    if (value.isArray()) {
      List<String> codes = new ArrayList<>();
      value.forEach(code -> codes.add(code.asText()));
      return String.join(",", codes);
    }
    throw new IllegalArgumentException("Поле \"documents\" должно быть строкой или массивом");
  }
}
