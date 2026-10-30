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

  /** Разбирает CSV-строку. */
  public static List<Map<String, String>> parse(String csv) {
    return parse(new StringReader(csv == null ? "" : csv));
  }

  /** Разбирает CSV из потока. */
  public static List<Map<String, String>> parse(Reader reader) {
    List<List<String>> rows = readRows(readAll(reader));
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
      if (c == ',') {
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
