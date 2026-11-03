package ru.validum.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import ru.validum.core.bank.BikSource;
import ru.validum.core.document.DocumentType;
import ru.validum.core.export.CsvExporter;
import ru.validum.core.export.DocumentsCsv;
import ru.validum.core.export.DocumentsFormat;
import ru.validum.core.export.IndividualProfileFlattener;
import ru.validum.core.name.Gender;
import ru.validum.core.profile.IndividualProfile;
import ru.validum.core.profile.IndividualProfileGenerator;

@Command(
  name = "individual",
  mixinStandardHelpOptions = true,
  description = "Сгенерировать профиль(и) физлица"
)
public final class GenerateIndividualCommand implements Callable<Integer> {

  @Option(names = "--seed", description = "Базовый seed (по умолчанию — System.nanoTime())")
  Long seed;

  @Option(
    names = "--as-of",
    description = "Дата расчёта (ISO, например 2026-01-01): возраст и сроки документов считаются "
      + "от неё. Не задана — сегодня")
  String asOf;

  @Option(names = {"-n", "--count"}, defaultValue = "1",
    description = "Количество профилей (1–10000, по умолчанию 1)")
  int count;

  @Option(names = "--gender", description = "MALE или FEMALE")
  String gender;

  @Option(names = "--age", description = "Возраст")
  Integer age;

  @Option(names = "--region", description = "Регион: имя (MOSCOW, SPB, …) или код субъекта (77)")
  String region;

  @Option(names = "--bik",
    description = "Явный БИК банка (P3.3). Не найден в справочнике → exit 2")
  String bik;

  @Option(names = "--format", defaultValue = "json",
    description = "Формат: json | json-array | csv | csv-semicolon (русский Excel) | sql")
  String format;

  @Option(names = "--dialect", description = "SQL-диалект: postgres | oracle | mysql")
  String dialect = "postgres";

  @Option(names = "--table", description = "Имя таблицы для --format sql")
  String table = "validum_individual";

  @Option(names = "--no-ddl", description = "Без CREATE TABLE, только INSERT")
  boolean noDdl;


  @Option(names = "--documents", split = ",",
    description = "Виды ДУЛ: коды СПДУЛ (21,22) или имена (PASSPORT_RF,FOREIGN_PASSPORT)")
  String[] documents;

  @Option(names = "--documents-mode", defaultValue = "inline",
    description = "Как писать ДУЛ в CSV: inline | json | split (split пишет второй файл)")
  String documentsMode;

  @Option(names = {"-o", "--output"}, description = "Путь к файлу (по умолчанию stdout)")
  Path output;

  @Option(names = "--no-account", description = "Без счёта")
  boolean noAccount;

  @Option(names = "--terrorist", description = "Флаг «террорист/экстремист»")
  boolean terrorist;

  @Option(names = "--blocked", description = "Флаг «заблокирован банком»")
  boolean blocked;

  @Option(names = "--unique",
    description = "Без повторов ИНН в наборе (требует --seed); при исчерпании вариантов — exit 2")
  boolean unique;

  @Override
  public Integer call() throws Exception {
    if (count < 1 || count > 10_000) {
      System.err.println("Ошибка: --count должен быть в диапазоне 1..10000");
      return 2;
    }
    if (unique && seed == null) {
      System.err.println("Ошибка: --unique требует --seed, иначе набор невоспроизводим");
      return 2;
    }
    long baseSeed = seed != null ? seed : System.nanoTime();
    BikSource bikSource = CliBik.source();

    List<IndividualProfile> profiles = new ArrayList<>(count);
    LocalDate asOfDate;
    try {
      asOfDate = AsOfOption.parse(asOf);
      UniqueGuard guard = unique ? new UniqueGuard("ИНН", 10_000_000_000L) : null;
      for (int i = 0; i < count; i++) {
        long profileSeed = baseSeed + i;
        profiles.add(guard == null
          ? newBuilder(profileSeed, bikSource, asOfDate).build().generate()
          : guard.next(profileSeed,
              s -> newBuilder(s, bikSource, asOfDate).build().generate(),
              IndividualProfile::inn));
      }
    } catch (IllegalArgumentException e) {
      System.err.println("Ошибка: " + e.getMessage());
      return 2;
    }

    OutputFormat fmt = OutputFormat.fromString(format);
    DocumentsFormat docFormat =
      fmt == OutputFormat.CSV ? DocumentsFormat.fromString(documentsMode) : DocumentsFormat.INLINE;
    if (docFormat == DocumentsFormat.SPLIT && output == null) {
      System.err.println(
        "Ошибка: --documents-mode split требует -o: вторая таблица пишется рядом с профилями");
      return 2;
    }

    List<Map<String, String>> rows = IndividualProfileFlattener.flattenAll(profiles, docFormat);
    OutputMeta.print(System.err, baseSeed, asOfDate, seed == null);
    OutputWriter.of(output, fmt, dialect, table, !noDdl).write(rows, baseSeed);
    if (docFormat == DocumentsFormat.SPLIT) {
      writeDocumentsTable(profiles, output);
    }
    return 0;
  }

  /** Свежий билдер профиля под конкретный seed: опции командной строки применяются заново. */
  private IndividualProfileGenerator.Builder newBuilder(long profileSeed, BikSource bikSource, LocalDate asOfDate) {
    IndividualProfileGenerator.Builder b = IndividualProfileGenerator.builder()
      .seed(profileSeed)
      .bikSource(bikSource)
      .withAccount(!noAccount);
    if (gender != null) b.gender(Gender.valueOf(gender.toUpperCase(Locale.ROOT)));
    if (age != null) b.age(age);
    if (region != null) b.region(RegionOption.parse(region));
    if (bik != null) b.bik(bik);
    if (asOfDate != null) b.asOf(asOfDate);
    if (documents != null) b.documents(parseTypes(documents));
    if (terrorist) b.terrorist(true);
    if (blocked) b.blocked(true);
    return b;
  }

  private static List<DocumentType> parseTypes(String[] values) {
    List<DocumentType> types = new ArrayList<>(values.length);
    for (String value : values) {
      String v = value.trim();
      if (v.isEmpty()) {
        continue;
      }
      types.add(v.chars().allMatch(Character::isDigit)
        ? DocumentType.fromCode(v)
        : DocumentType.valueOf(v.toUpperCase(Locale.ROOT)));
    }
    return types;
  }

  private static void writeDocumentsTable(List<IndividualProfile> profiles, Path output)
      throws IOException {
    List<Map<String, String>> rows = new ArrayList<>();
    for (IndividualProfile profile : profiles) {
      rows.addAll(DocumentsCsv.toRows(profile.inn(), profile.documents()));
    }
    String csv = rows.isEmpty()
      ? "\uFEFF" + String.join(",", DocumentsCsv.SPLIT_HEADER) + "\r\n"
      : CsvExporter.toCsv(rows);
    Files.write(documentsPath(output), CsvExporter.toBytes(csv));
  }

  private static Path documentsPath(Path output) {
    String name = output.getFileName().toString();
    int dot = name.lastIndexOf('.');
    String target = dot > 0 ? name.substring(0, dot) + "-documents.csv" : name + "-documents.csv";
    return output.resolveSibling(target);
  }
}
