package ru.validum.core.export;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Читает CSV (RFC 4180) в список плоских строк.
 *
 * <p>Обратная операция к {@link CsvExporter}: первая строка — заголовки, дальше по строке на
 * запись. Понимает:
 *
 * <ul>
 *   <li>UTF-8 BOM в начале файла (его пишет {@link CsvExporter} для Excel);
 *   <li>поля в кавычках с запятыми, удвоенными кавычками и переносами строк внутри;
 *   <li>разделители строк {@code \n}, {@code \r\n} и {@code \r};
 *   <li>недостающие в строке ячейки — как пустые строки.
 * </ul>
 *
 * <p>Пустые строки (например, завершающий перевод строки) пропускаются.
 */
public final class CsvImporter {

  private static final char BOM = '\uFEFF';

  private CsvImporter() {}

  /** Разбирает CSV-строку с разделителем по умолчанию (запятая). */
  public static List<Map<String, String>> parse(String csv) {
    return parse(csv, ',');
  }

  /** Разбирает CSV-строку с явным разделителем ({@code ','} или {@code ';'}). */
  public static List<Map<String, String>> parse(String csv, char delimiter) {
    return parse(new StringReader(csv == null ? "" : csv), delimiter);
  }

  /** Разбирает CSV из потока с разделителем по умолчанию (запятая). */
  public static List<Map<String, String>> parse(Reader reader) {
    return parse(reader, ',');
  }

  /** Разбирает CSV из потока с явным разделителем. */
  public static List<Map<String, String>> parse(Reader reader, char delimiter) {
    List<List<String>> rows = readRows(readAll(reader), delimiter);
    if (rows.isEmpty()) {
      return List.of();
    }
    List<String> headers = rows.get(0);
    List<Map<String, String>> out = new ArrayList<>(rows.size() - 1);
    for (int r = 1; r < rows.size(); r++) {
      List<String> cells = rows.get(r);
      Map<String, String> row = new LinkedHashMap<>();
      for (int c = 0; c < headers.size(); c++) {
        row.put(headers.get(c), c < cells.size() ? cells.get(c) : "");
      }
      out.add(row);
    }
    return out;
  }

  /**
   * Низкоуровневый разбор в строки из ячеек. Виден пакету для тестов.
   *
   * <p>Пустые строки отбрасываются целиком — они не несут данных.
   */
  static List<List<String>> readRows(String text) {
    return readRows(text, ',');
  }

  static List<List<String>> readRows(String text, char delimiter) {
    if (!text.isEmpty() && text.charAt(0) == BOM) {
      text = text.substring(1);
    }
    List<List<String>> rows = new ArrayList<>();
    List<String> row = new ArrayList<>();
    StringBuilder field = new StringBuilder();
    boolean inQuotes = false;
    int i = 0;
    int n = text.length();

    while (i < n) {
      char c = text.charAt(i);
      if (inQuotes) {
        if (c == '"') {
          if (i + 1 < n && text.charAt(i + 1) == '"') {
            field.append('"');
            i += 2;
          } else {
            inQuotes = false;
            i++;
          }
        } else {
          field.append(c);
          i++;
        }
        continue;
      }
      if (c == '"') {
        inQuotes = true;
        i++;
        continue;
      }
      if (c == delimiter) {
        row.add(field.toString());
        field.setLength(0);
        i++;
        continue;
      }
      if (c == '\r' || c == '\n') {
        row.add(field.toString());
        field.setLength(0);
        addRow(rows, row);
        row = new ArrayList<>();
        i += (c == '\r' && i + 1 < n && text.charAt(i + 1) == '\n') ? 2 : 1;
        continue;
      }
      field.append(c);
      i++;
    }

    if (field.length() > 0 || !row.isEmpty()) {
      row.add(field.toString());
      addRow(rows, row);
    }
    return rows;
  }

  private static void addRow(List<List<String>> rows, List<String> row) {
    if (!(row.size() == 1 && row.get(0).isEmpty())) {
      rows.add(row);
    }
  }

  /**
   * Определяет разделитель по строке заголовков: {@code ';'} у «дружелюбного» CSV для Excel,
   * {@code ','} у машинного. Кавычки учитываются, чтобы не считать разделители внутри значений.
   */
  public static char detectDelimiter(String headerLine) {
    int commas = 0;
    int semicolons = 0;
    boolean quoted = false;
    for (int i = 0; i < headerLine.length(); i++) {
      char c = headerLine.charAt(i);
      if (c == '"') {
        quoted = !quoted;
      } else if (!quoted && c == ',') {
        commas++;
      } else if (!quoted && c == ';') {
        semicolons++;
      }
    }
    return semicolons > commas ? ';' : ',';
  }

  /**
   * Снимает обёртку {@code ="…"}: так {@link CsvExporter} помечает кодовые колонки, чтобы Excel не
   * превращал ИНН в {@code 5,86E+11}. При чтении значения возвращаются как есть.
   */
  public static String unquoteFormula(String value) {
    if (value != null
        && value.length() >= 3
        && value.startsWith("=\"")
        && value.endsWith("\"")) {
      return value.substring(2, value.length() - 1);
    }
    return value;
  }

  private static String readAll(Reader reader) {
    try {
      StringBuilder sb = new StringBuilder();
      char[] buf = new char[4096];
      int read;
      while ((read = reader.read(buf)) != -1) {
        sb.append(buf, 0, read);
      }
      return sb.toString();
    } catch (IOException e) {
      throw new UncheckedIOException("Не удалось прочитать CSV", e);
    }
  }
}
