package ru.validum.core.export;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import ru.validum.core.document.DocumentType;
import ru.validum.core.document.IdentityDocument;

/**
 * Список ДУЛ ↔ плоский CSV: три представления и обратный разбор.
 *
 * <p>Колонка в плоской строке профиля — {@link #COLUMN} ({@code documents}). Формат ячейки
 * распознаётся автоматически ({@link #parse}): начинается с {@code [} — JSON, иначе мини-таблица
 * через {@code |}. Пустая ячейка — пустой список.
 *
 * <p>Режим split пишет отдельную таблицу ({@link #SPLIT_HEADER}), где ключом связи служит {@code
 * profileKey} (ИНН человека); обратная сборка — {@link #groupRows}.
 *
 * <p>Зависимостей нет: JSON пишется и читается вручную, потому что core обязан оставаться
 * zero-dependency.
 */
public final class DocumentsCsv {

  /** Имя колонки документов в плоской строке профиля. */
  public static final String COLUMN = "documents";

  /** Колонки отдельной таблицы documents.csv (режим split). */
  public static final List<String> SPLIT_HEADER = List.of(
    "profileKey", "spdulCode", "title", "series", "number",
    "issuedBy", "issueDate", "expirationDate", "departmentCode");

  private static final char FIELD_SEPARATOR = '|';
  private static final int INLINE_FIELD_COUNT = 7;

  private DocumentsCsv() {}

  // ------------------------------------------------------------------ inline

  /** Пишет документы мини-таблицей: одна строка на документ, поля через {@code |}. */
  public static String inline(List<IdentityDocument> documents) {
    if (documents == null || documents.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (IdentityDocument d : documents) {
      if (sb.length() > 0) {
        sb.append('\n');
      }
      sb.append(escapeInline(token(d.type()))).append(FIELD_SEPARATOR)
        .append(escapeInline(d.series())).append(FIELD_SEPARATOR)
        .append(escapeInline(d.number())).append(FIELD_SEPARATOR)
        .append(escapeInline(d.issuedBy())).append(FIELD_SEPARATOR)
        .append(str(d.issueDate())).append(FIELD_SEPARATOR)
        .append(str(d.expirationDate())).append(FIELD_SEPARATOR)
        .append(escapeInline(d.departmentCode()));
    }
    return sb.toString();
  }

  /** Разбирает мини-таблицу, написанную {@link #inline}. */
  public static List<IdentityDocument> parseInline(String cell) {
    if (cell == null || cell.isBlank()) {
      return List.of();
    }
    List<IdentityDocument> result = new ArrayList<>();
    for (String line : cell.split("\n", -1)) {
      if (line.isBlank()) {
        continue;
      }
      List<String> fields = splitInline(line);
      if (fields.size() != INLINE_FIELD_COUNT) {
        throw new IllegalArgumentException(
          "Ожидалось " + INLINE_FIELD_COUNT + " полей документа через '|', а найдено "
            + fields.size() + ": " + line);
      }
      LocalDate issueDate = date(fields.get(4), "дата выдачи");
      if (issueDate == null) {
        throw new IllegalArgumentException("В документе нет даты выдачи: " + line);
      }
      result.add(new IdentityDocument(
        tokenToType(fields.get(0)),
        fields.get(1),
        fields.get(2),
        blankToNull(fields.get(3)),
        issueDate,
        date(fields.get(5), "дата окончания"),
        blankToNull(fields.get(6))));
    }
    return List.copyOf(result);
  }

  private static String escapeInline(String value) {
    if (value == null) {
      return "";
    }
    return value.replace("\\", "\\\\").replace("|", "\\|");
  }

  private static List<String> splitInline(String line) {
    List<String> parts = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      if (c == '\\' && i + 1 < line.length()) {
        current.append(line.charAt(++i));
      } else if (c == FIELD_SEPARATOR) {
        parts.add(current.toString());
        current.setLength(0);
      } else {
        current.append(c);
      }
    }
    parts.add(current.toString());
    return parts;
  }

  // -------------------------------------------------------------------- json

  /** Пишет документы компактным JSON-массивом объектов. */
  public static String json(List<IdentityDocument> documents) {
    if (documents == null || documents.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < documents.size(); i++) {
      if (i > 0) {
        sb.append(',');
      }
      IdentityDocument d = documents.get(i);
      sb.append("{\"spdulCode\":").append(jsonString(token(d.type())))
        .append(",\"title\":").append(jsonString(d.type().title()))
        .append(",\"series\":").append(jsonString(d.series()))
        .append(",\"number\":").append(jsonString(d.number()))
        .append(",\"issuedBy\":").append(jsonString(d.issuedBy()))
        .append(",\"issueDate\":").append(jsonString(str(d.issueDate())))
        .append(",\"expirationDate\":")
        .append(d.expirationDate() == null ? "null" : jsonString(d.expirationDate().toString()))
        .append(",\"departmentCode\":").append(jsonString(d.departmentCode()))
        .append('}');
    }
    return sb.append(']').toString();
  }

  /** Разбирает JSON-массив, написанный {@link #json}. */
  public static List<IdentityDocument> parseJson(String cell) {
    if (cell == null || cell.isBlank()) {
      return List.of();
    }
    JsonParser parser = new JsonParser(cell);
    List<IdentityDocument> documents = parser.parseArray();
    parser.skipWhitespace();
    if (!parser.atEnd()) {
      throw new IllegalArgumentException("Лишние символы после JSON-массива документов");
    }
    return List.copyOf(documents);
  }

  private static String jsonString(String value) {
    if (value == null) {
      return "null";
    }
    StringBuilder sb = new StringBuilder("\"");
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '"' -> sb.append("\\\"");
        case '\\' -> sb.append("\\\\");
        case '\n' -> sb.append("\\n");
        case '\r' -> sb.append("\\r");
        case '\t' -> sb.append("\\t");
        default -> sb.append(c);
      }
    }
    return sb.append('"').toString();
  }

  private static final class JsonParser {

    private final String text;
    private int pos;

    JsonParser(String text) {
      this.text = text;
    }

    List<IdentityDocument> parseArray() {
      List<IdentityDocument> result = new ArrayList<>();
      skipWhitespace();
      expect('[');
      skipWhitespace();
      if (peek() == ']') {
        pos++;
        return result;
      }
      while (true) {
        result.add(parseObject());
        skipWhitespace();
        char c = next();
        if (c == ']') {
          return result;
        }
        if (c != ',') {
          throw error("ожидалась ',' или ']'");
        }
      }
    }

    private IdentityDocument parseObject() {
      Map<String, String> values = new LinkedHashMap<>();
      skipWhitespace();
      expect('{');
      skipWhitespace();
      if (peek() == '}') {
        pos++;
        return toDocument(values);
      }
      while (true) {
        skipWhitespace();
        String key = parseString();
        skipWhitespace();
        expect(':');
        skipWhitespace();
        values.put(key, parseValue());
        skipWhitespace();
        char c = next();
        if (c == '}') {
          return toDocument(values);
        }
        if (c != ',') {
          throw error("ожидалась ',' или '}'");
        }
      }
    }

    private IdentityDocument toDocument(Map<String, String> values) {
      LocalDate issueDate = date(values.get("issueDate"), "дата выдачи");
      if (issueDate == null) {
        throw new IllegalArgumentException("В JSON документа нет даты выдачи");
      }
      return new IdentityDocument(
        tokenToType(values.get("spdulCode")),
        blankToEmpty(values.get("series")),
        blankToEmpty(values.get("number")),
        blankToNull(values.get("issuedBy")),
        issueDate,
        date(values.get("expirationDate"), "дата окончания"),
        blankToNull(values.get("departmentCode")));
    }

    private String parseValue() {
      if (peek() == '"') {
        return parseString();
      }
      if (text.startsWith("null", pos)) {
        pos += 4;
        return null;
      }
      throw error("ожидалась строка или null");
    }

    private String parseString() {
      expect('"');
      StringBuilder sb = new StringBuilder();
      while (true) {
        char c = next();
        if (c == '"') {
          return sb.toString();
        }
        if (c != '\\') {
          sb.append(c);
          continue;
        }
        char escaped = next();
        switch (escaped) {
          case '"' -> sb.append('"');
          case '\\' -> sb.append('\\');
          case '/' -> sb.append('/');
          case 'b' -> sb.append('\b');
          case 'f' -> sb.append('\f');
          case 'n' -> sb.append('\n');
          case 'r' -> sb.append('\r');
          case 't' -> sb.append('\t');
          case 'u' -> {
            if (pos + 4 > text.length()) {
              throw error("обрезанная escape-последовательность \\u");
            }
            sb.append((char) Integer.parseInt(text.substring(pos, pos + 4), 16));
            pos += 4;
          }
          default -> throw error("неизвестная escape-последовательность \\" + escaped);
        }
      }
    }

    void skipWhitespace() {
      while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
        pos++;
      }
    }

    boolean atEnd() {
      return pos >= text.length();
    }

    private char peek() {
      if (pos >= text.length()) {
        throw error("неожиданный конец JSON");
      }
      return text.charAt(pos);
    }

    private char next() {
      char c = peek();
      pos++;
      return c;
    }

    private void expect(char expected) {
      char c = next();
      if (c != expected) {
        throw error("ожидался '" + expected + "', а найден '" + c + "'");
      }
    }

    private IllegalArgumentException error(String message) {
      return new IllegalArgumentException(
        "Некорректный JSON документов (позиция " + pos + "): " + message);
    }
  }

  // ------------------------------------------------------------------- split

  /** Раскладывает документы человека в строки таблицы documents.csv. */
  public static List<Map<String, String>> toRows(
      String profileKey, List<IdentityDocument> documents) {
    List<Map<String, String>> rows = new ArrayList<>();
    if (documents == null) {
      return rows;
    }
    for (IdentityDocument d : documents) {
      Map<String, String> row = new LinkedHashMap<>();
      row.put("profileKey", profileKey);
      row.put("spdulCode", token(d.type()));
      row.put("title", d.type().title());
      row.put("series", d.series());
      row.put("number", d.number());
      row.put("issuedBy", d.issuedBy());
      row.put("issueDate", str(d.issueDate()));
      row.put("expirationDate", str(d.expirationDate()));
      row.put("departmentCode", d.departmentCode());
      rows.add(row);
    }
    return rows;
  }

  /** Собирает документы обратно: {@code profileKey → список документов}. */
  public static Map<String, List<IdentityDocument>> groupRows(List<Map<String, String>> rows) {
    Map<String, List<IdentityDocument>> grouped = new LinkedHashMap<>();
    for (Map<String, String> row : rows) {
      String key = row.get("profileKey");
      if (key == null || key.isBlank()) {
        throw new IllegalArgumentException("В строке документов нет profileKey: " + row);
      }
      LocalDate issueDate = date(row.get("issueDate"), "дата выдачи");
      if (issueDate == null) {
        throw new IllegalArgumentException("В строке документа нет даты выдачи: " + row);
      }
      IdentityDocument document = new IdentityDocument(
        tokenToType(row.get("spdulCode")),
        blankToEmpty(row.get("series")),
        blankToEmpty(row.get("number")),
        blankToNull(row.get("issuedBy")),
        issueDate,
        date(row.get("expirationDate"), "дата окончания"),
        blankToNull(row.get("departmentCode")));
      grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(document);
    }
    Map<String, List<IdentityDocument>> frozen = new LinkedHashMap<>();
    grouped.forEach((key, value) -> frozen.put(key, List.copyOf(value)));
    return frozen;
  }

  // ------------------------------------------------------------------ общее

  /** Разбирает ячейку, определяя формат по содержимому. */
  public static List<IdentityDocument> parse(String cell) {
    if (cell == null || cell.isBlank()) {
      return List.of();
    }
    String trimmed = cell.trim();
    return trimmed.startsWith("[") ? parseJson(trimmed) : parseInline(trimmed);
  }

  private static String token(DocumentType type) {
    return type.spdulCode() != null ? type.spdulCode() : type.name();
  }

  private static DocumentType tokenToType(String token) {
    if (token == null || token.isBlank()) {
      throw new IllegalArgumentException("Пустой вид документа в CSV ДУЛ");
    }
    String value = token.trim();
    if (value.chars().allMatch(Character::isDigit)) {
      return DocumentType.fromCode(value);
    }
    return DocumentType.valueOf(value.toUpperCase(Locale.ROOT));
  }

  private static LocalDate date(String value, String field) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(value.trim());
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException("Некорректная " + field + " документа: " + value, e);
    }
  }

  private static String str(LocalDate date) {
    return date == null ? "" : date.toString();
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private static String blankToEmpty(String value) {
    return value == null ? "" : value;
  }
}
