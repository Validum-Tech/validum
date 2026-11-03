package ru.validum.cli;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import ru.validum.core.document.DocumentGenerator;
import ru.validum.core.document.DocumentType;
import ru.validum.core.document.IdentityDocument;

@Command(
  name = "documents",
  mixinStandardHelpOptions = true,
  description = "Сгенерировать документы, удостоверяющие личность (СПДУЛ ФНС)"
)
public final class GenerateDocumentsCommand implements Callable<Integer> {

  @Option(names = "--type", required = true,
    description = "Виды документов: коды СПДУЛ (21,22,12) или имена (PASSPORT_RF,FOREIGN_PASSPORT)")
  String type;

  @Option(names = "--seed", description = "Базовый seed (по умолчанию — System.nanoTime())")
  Long seed;

  @Option(
    names = "--as-of",
    description = "Дата расчёта (ISO, например 2026-01-01): возраст и сроки документов считаются "
      + "от неё. Не задана — сегодня")
  String asOf;

  @Option(names = {"-n", "--count"}, defaultValue = "1",
    description = "Сколько наборов сгенерировать (1–10000, по умолчанию 1)")
  int count;

  @Option(names = "--format", defaultValue = "json",
    description = "Формат: json | json-array | csv | csv-semicolon (русский Excel) | sql")
  String format;

  @Option(names = "--dialect", description = "SQL-диалект: postgres | oracle | mysql")
  String dialect = "postgres";

  @Option(names = "--table", description = "Имя таблицы для --format sql")
  String table = "validum_documents";

  @Option(names = "--no-ddl", description = "Без CREATE TABLE, только INSERT")
  boolean noDdl;


  @Option(names = {"-o", "--output"}, description = "Путь к файлу (по умолчанию stdout)")
  Path output;

  @Override
  public Integer call() throws Exception {
    if (count < 1 || count > 10_000) {
      System.err.println("Ошибка: --count должен быть в диапазоне 1..10000");
      return 2;
    }
    List<DocumentType> types;
    try {
      types = parseTypes(type);
    } catch (IllegalArgumentException e) {
      System.err.println("Ошибка: " + e.getMessage());
      return 2;
    }

    long baseSeed = seed != null ? seed : System.nanoTime();
    LocalDate asOfDate;
    try {
      asOfDate = AsOfOption.parse(asOf);
    } catch (IllegalArgumentException e) {
      System.err.println("Ошибка: " + e.getMessage());
      return 2;
    }
    DocumentGenerator generator = new DocumentGenerator(new Random(baseSeed));

    List<Map<String, String>> rows = new ArrayList<>(count * types.size());
    for (int i = 0; i < count; i++) {
      for (DocumentType documentType : types) {
        rows.add(row(asOfDate == null ? generator.generate(documentType) : generator.generate(documentType, asOfDate)));
      }
    }

    OutputMeta.print(System.err, baseSeed, asOfDate, seed == null);
    OutputWriter.of(output, OutputFormat.fromString(format), dialect, table, !noDdl)
      .write(rows, baseSeed);
    return 0;
  }

  private static List<DocumentType> parseTypes(String raw) {
    List<DocumentType> types = new ArrayList<>();
    for (String part : raw.split(",")) {
      String trimmed = part.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      types.add(parseType(trimmed));
    }
    if (types.isEmpty()) {
      throw new IllegalArgumentException("--type не содержит ни одного вида документа");
    }
    return List.copyOf(types);
  }

  private static DocumentType parseType(String value) {
    if (value.chars().allMatch(Character::isDigit)) {
      return DocumentType.fromCode(value);
    }
    try {
      return DocumentType.valueOf(value.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
        "Неизвестный вид документа: " + value
          + ". Допустимо: код СПДУЛ (например, 21) или имя (PASSPORT_RF).");
    }
  }

  private static Map<String, String> row(IdentityDocument document) {
    Map<String, String> row = new LinkedHashMap<>();
    row.put("spdulCode", document.type().spdulCode());
    row.put("title", document.type().title());
    row.put("series", document.series());
    row.put("number", document.number());
    row.put("issuedBy", document.issuedBy());
    row.put("issueDate", document.issueDate().toString());
    row.put("expirationDate",
      document.expirationDate() == null ? null : document.expirationDate().toString());
    row.put("departmentCode", document.departmentCode());
    return row;
  }
}
