package ru.validum.cli;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import ru.validum.core.bank.BikSource;
import ru.validum.core.document.IdentityDocument;
import ru.validum.core.export.CsvExporter;
import ru.validum.core.export.CsvFiller;
import ru.validum.core.export.CsvImporter;
import ru.validum.core.export.DocumentsCsv;

@Command(
  name = "individual",
  mixinStandardHelpOptions = true,
  description = "Дополнить CSV с профилями физлиц: пустые ячейки → валидные значения"
)
public final class FillIndividualCommand implements Callable<Integer> {

  @Option(names = {"-i", "--input"}, required = true, description = "Входной CSV (UTF-8)")
  Path input;

  @Option(names = {"-o", "--output"}, description = "Выходной CSV (по умолчанию stdout)")
  Path output;

  @Option(names = "--seed", description = "Базовый seed (по умолчанию — случайный)")
  Long seed;

  @Option(
    names = "--as-of",
    description = "Дата расчёта (ISO, например 2026-01-01): возраст и сроки документов считаются "
      + "от неё. Не задана — сегодня")
  String asOf;

  @Option(names = "--documents-file",
    description = "CSV с ДУЛ (колонка profileKey): документы подклеиваются обратно по ИНН")
  Path documentsFile;

  @Override
  public Integer call() throws Exception {
    List<Map<String, String>> rows;
    try {
      rows = CsvImporter.parse(InputFiles.read(input));
    } catch (IllegalArgumentException e) {
      return InputFiles.fail(e);
    }
    if (rows.isEmpty()) {
      System.err.println("Ошибка: во входном CSV нет строк с данными");
      return 2;
    }
    if (documentsFile != null) {
      Map<String, List<IdentityDocument>> byKey;
      try {
        byKey = DocumentsCsv.groupRows(CsvImporter.parse(InputFiles.read(documentsFile)));
      } catch (IllegalArgumentException e) {
        return InputFiles.fail(e);
      }
      for (Map<String, String> row : rows) {
        attachDocuments(row, byKey);
      }
    }

    BikSource bikSource = CliBik.source();
    LocalDate asOfDate;
    try {
      asOfDate = AsOfOption.parse(asOf);
    } catch (IllegalArgumentException e) {
      System.err.println("Ошибка: " + e.getMessage());
      return 2;
    }
    CsvFiller filler = new CsvFiller(bikSource, seed != null ? new Random(seed) : null, asOfDate);
    List<Map<String, String>> filled = filler.fill(rows);
    String csv = CsvExporter.toCsv(filled);

    if (output != null) {
      OutputFiles.write(output, CsvExporter.toBytes(csv));
    } else {
      System.out.print(csv);
    }
    return 0;
  }

  /** Подклеивает документы из второй таблицы в колонку {@code documents} по ИНН. */
  private static void attachDocuments(
      Map<String, String> row, Map<String, List<IdentityDocument>> byKey) {
    String existing = row.get(DocumentsCsv.COLUMN);
    if (existing != null && !existing.isBlank()) {
      return;
    }
    String key = row.get("inn");
    if (key == null) {
      return;
    }
    List<IdentityDocument> documents = byKey.get(key);
    if (documents != null && !documents.isEmpty()) {
      row.put(DocumentsCsv.COLUMN, DocumentsCsv.inline(documents));
    }
  }
}
