package ru.validum.core.export;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Генератор CSV из плоских строк.
 *
 * <p>Работает с {@code List<Map<String,String>>}: каждая строка — это уже развёрнутый профиль
 * (см. {@link IndividualProfileFlattener}, {@link IpProfileFlattener}, {@link
 * OrganizationProfileFlattener}). Так CSV-формат живёт в core, а не в API-модуле — его
 * используют и REST-контроллер, и CLI (P3.1).
 *
 * <p>Пишет в UTF-8 с BOM ({@code \uFEFF}) — Excel корректно открывает кириллицу.
 * Заголовки берутся из ключей первой строки в порядке вставки (LinkedHashMap → стабильный
 * порядок колонок). Экранирует запятые, кавычки и переносы строк по RFC 4180.
 */
public final class CsvExporter {

  private static final String BOM = "\uFEFF";
  private static final String DELIMITER = ",";
  private static final String NEWLINE = "\r\n";

  private CsvExporter() {}

  /**
   * Собирает CSV из плоских строк.
   *
   * @param rows список строк (каждая — LinkedHashMap с колонками)
   * @return CSV-строка с BOM, заголовком и данными. Для пустого списка — только BOM.
   */
  public static String toCsv(List<Map<String, String>> rows) {
    if (rows.isEmpty()) {
      return BOM;
    }
    StringBuilder sb = new StringBuilder(BOM);
    List<String> headers = List.copyOf(rows.get(0).keySet());
    sb.append(headers.stream().map(CsvExporter::escape).collect(Collectors.joining(DELIMITER)))
      .append(NEWLINE);
    for (Map<String, String> row : rows) {
      sb.append(headers.stream()
          .map(h -> escape(row.getOrDefault(h, "")))
          .collect(Collectors.joining(DELIMITER)))
        .append(NEWLINE);
    }
    return sb.toString();
  }

  /**
   * Экранирует одно значение по RFC 4180: оборачивает в кавычки, если внутри есть запятая,
   * кавычка или перенос строки; внутренние кавычки удваиваются.
   */
  public static String escape(String value) {
    if (value == null || value.isEmpty()) {
      return "";
    }
    if (value.contains(DELIMITER)
      || value.contains("\"")
      || value.contains("\n")
      || value.contains("\r")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }

  /** Конвертирует строку в UTF-8 байты (для ResponseEntity и file output). */
  public static byte[] toBytes(String csv) {
    return csv.getBytes(StandardCharsets.UTF_8);
  }
}
