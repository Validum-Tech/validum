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
import picocli.CommandLine.Mixin;
import ru.validum.core.export.FieldSelection;
import ru.validum.core.bank.BikSource;
import ru.validum.core.document.DocumentType;
import ru.validum.core.export.CsvExporter;
import ru.validum.core.export.DocumentsCsv;
import ru.validum.core.export.DocumentsFormat;
import ru.validum.core.export.IndividualProfileFlattener;
import ru.validum.core.name.Gender;
import ru.validum.core.profile.AgeDistribution;
import ru.validum.core.profile.CorruptField;
import ru.validum.core.profile.IndividualProfile;
import ru.validum.core.profile.IndividualProfileBuilder;
import ru.validum.core.profile.IndividualProfileGenerator;

@Command(
  name = "individual",
  mixinStandardHelpOptions = true,
  description = "Сгенерировать профиль(и) физлица"
)
public final class GenerateIndividualCommand implements Callable<Integer> {

  @Mixin CommonOptions common = new CommonOptions();


  @Option(
    names = "--as-of",
    description = "Дата расчёта (ISO, например 2026-01-01): возраст и сроки документов считаются "
      + "от неё. Не задана — сегодня")
  String asOf;


  @Option(names = "--gender", description = "MALE или FEMALE")
  String gender;

  @Option(names = "--age", description = "Возраст (одно число); для набора используйте --age-range")
  Integer age;

  @Option(names = "--age-range",
    description = "Диапазон возраста MIN-MAX (0–110), например 18-70; каждый профиль берёт своё значение")
  String ageRange;

  @Option(names = "--age-distribution", defaultValue = "uniform",
    description = "Распределение возраста в диапазоне: uniform | realistic (когорты трудоспособных)")
  String ageDistribution;

  @Option(names = "--region", description = "Регион: имя (MOSCOW, SPB, …) или код субъекта (77)")
  String region;

  @Option(names = "--residency", defaultValue = "resident",
      description = "Резидентство: resident | non-resident")
  String residency;

  @Option(names = "--country",
      description = "Страна нерезидента (KZ, BY, UZ, KG, AM, GE, UA, DE, US); по умолчанию KZ")
  String country;

  @Option(names = "--bik",
    description = "Явный БИК банка (P3.3). Не найден в справочнике → exit 2")
  String bik;






  @Option(names = "--documents", split = ",",
    description = "Виды ДУЛ: коды СПДУЛ (21,22), имена (PASSPORT_RF) или auto — подобрать по возрасту и полу")
  String[] documents;

  @Option(names = "--documents-mode", defaultValue = "inline",
    description = "Как писать ДУЛ в CSV: inline | json | split (split пишет второй файл)")
  String documentsMode;


  @Option(
    names = "--corrupt",
    split = ",",
    description = "Намеренно испортить поля для негативных тестов: inn, snils, address")
  String[] corrupt;

  @Option(
    names = "--index",
    description = "Печатать индекс набора (№, идентификатор, ФИО, возраст, регион, банк, счёт) в stderr")
  boolean index;

  @Option(
    names = "--fields",
    paramLabel = "<колонки>",
    description = "Оставить только эти колонки (через запятую) или пресет short")
  String fields;

  @Option(names = "--no-account", description = "Без счёта")
  boolean noAccount;

  @Option(names = "--terrorist", description = "Флаг «террорист/экстремист»")
  boolean terrorist;

  @Option(names = "--blocked", description = "Флаг «заблокирован банком»")
  boolean blocked;

  @Option(names = "--unique",
    description = "Без повторов ИНН в наборе (требует --seed); при исчерпании вариантов — exit 2")
  boolean unique;

  @Option(names = "--unique-pool",
    description = "Сколько вариантов ИНН считать доступными (по умолчанию все 10^10). "
      + "Нужен, чтобы показать честный отказ на маленьком пуле: --unique-pool 10 -n 11")
  Long uniquePool;

  private String tableName() {
    return common.table != null && !common.table.isBlank()
      ? common.table
      : "validum_individual";
  }

  @Override
  public Integer call() throws Exception {
    if (common.count < 1 || common.count > 10_000) {
      System.err.println("Ошибка: --count должен быть в диапазоне 1..10000");
      return 2;
    }
    if (unique && common.seed == null) {
      System.err.println("Ошибка: --unique требует --seed, иначе набор невоспроизводим");
      return 2;
    }
    long baseSeed = common.seed != null ? common.seed : System.nanoTime();
    BikSource bikSource = CliBik.source();

    List<IndividualProfile> profiles = new ArrayList<>(common.count);
    LocalDate asOfDate;
    try {
      asOfDate = AsOfOption.parse(asOf);
      long pool = uniquePool != null && uniquePool > 0 ? uniquePool : 10_000_000_000L;
      UniqueGuard guard = unique ? new UniqueGuard("ИНН", pool) : null;
      for (int i = 0; i < common.count; i++) {
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

    OutputFormat fmt = OutputFormat.fromString(common.format);
    DocumentsFormat docFormat =
      fmt == OutputFormat.CSV ? DocumentsFormat.fromString(documentsMode) : DocumentsFormat.INLINE;
    if (docFormat == DocumentsFormat.SPLIT && common.output == null) {
      System.err.println(
        "Ошибка: --documents-mode split требует -o: вторая таблица пишется рядом с профилями");
      return 2;
    }

    List<Map<String, String>> rows = FieldSelection.keep(
      IndividualProfileFlattener.flattenAll(profiles, docFormat), fields);
    OutputMeta.print(System.err, baseSeed, asOfDate, common.seed == null);
    if (corrupt != null && corrupt.length > 0) {
      System.err.println("# испорчено: " + String.join(",", corrupt).toLowerCase(Locale.ROOT));
    }
    OutputWriter.of(common.output, fmt, common.dialect, tableName(), !common.noDdl).write(rows, baseSeed);
    if (docFormat == DocumentsFormat.SPLIT) {
      writeDocumentsTable(profiles, common.output);
    }
    if (index) {
      printIndex(profiles);
    }
    return 0;
  }

  /** Индекс набора — по нему QA выбирает человека и зовёт {@code describe --person}. */
  private static void printIndex(List<IndividualProfile> profiles) {
    System.err.println();
    System.err.printf("%-5s %-26s %-34s %7s  %-22s %-38s %-20s%n",
      "№", "Идентификатор", "ФИО", "Возраст", "Регион", "Банк", "Счёт");
    for (int i = 0; i < profiles.size(); i++) {
      IndividualProfile p = profiles.get(i);
      String bank = p.hasAccount()
        ? p.bank().name() + " · " + p.bank().bik()
        : "—";
      System.err.printf("%-5d %-26s %-34s %7d  %-22s %-38s %-20s%n",
        i + 1,
        p.personId() == null ? "—" : p.personId(),
        p.fullName(),
        p.age(),
        p.registrationAddress().region() == null ? "—" : p.registrationAddress().region(),
        bank,
        p.hasAccount() ? p.account() : "—");
    }
    System.err.println();
    System.err.println("# выбрать человека: validum describe --file <набор> --person <идентификатор>");
  }

  /** Свежий билдер профиля под конкретный seed: опции командной строки применяются заново. */
  private IndividualProfileBuilder newBuilder(long profileSeed, BikSource bikSource, LocalDate asOfDate) {
    IndividualProfileBuilder b = IndividualProfileGenerator.builder()
      .seed(profileSeed)
      .bikSource(bikSource)
      .withAccount(!noAccount);
    if (gender != null) b.gender(Gender.valueOf(gender.toUpperCase(Locale.ROOT)));
    if (age != null) b.age(age);
    // Распределение применяется и без диапазона: --age-distribution realistic сам
    // раздвигает когорты от 0 до 110, иначе флаг молча игнорировался.
    String distribution = ageDistribution == null || ageDistribution.isBlank() ? "uniform" : ageDistribution;
    b.ageDistribution(parseAgeDistribution(distribution));
    if (ageRange != null) {
      int[] range = parseAgeRange(ageRange);
      b.ageRange(range[0], range[1]);
    }
    if (region != null) b.region(RegionOption.parse(region));
    b.residency(parseResidency(residency, country));
    if (bik != null) b.bik(bik);
    if (asOfDate != null) b.asOf(asOfDate);
    applyDocuments(b);
    if (corrupt != null && corrupt.length > 0) {
      b.corrupt(parseCorrupt(corrupt));
    }
    if (terrorist) b.terrorist(true);
    if (blocked) b.blocked(true);
    return b;
  }

  /**
   * Резидентство: {@code resident} (по умолчанию) или {@code non-resident} со страной.
   * Страна нерезидента задаётся {@code --country}; по умолчанию Казахстан — самый частый
   * кейс трудовой миграции в банковских данных.
   */
  private static ru.validum.core.profile.Residency parseResidency(String raw, String countryRaw) {
    boolean nonResident = raw != null && raw.trim().equalsIgnoreCase("non-resident");
    if (countryRaw == null || countryRaw.isBlank()) {
      return nonResident
        ? ru.validum.core.profile.Residency.nonResident(ru.validum.core.common.Country.KZ)
        : ru.validum.core.profile.Residency.residentRU();
    }
    ru.validum.core.common.Country parsed;
    try {
      parsed = ru.validum.core.common.Country.valueOf(countryRaw.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
        "Неизвестная страна: '" + countryRaw + "'. Допустимо: KZ, BY, UZ, KG, AM, GE, UA, DE, US, TJ, RU");
    }
    if (!nonResident) {
      return ru.validum.core.profile.Residency.residentRU();
    }
    return ru.validum.core.profile.Residency.nonResident(parsed);
  }

  private static int[] parseAgeRange(String raw) {
    String[] parts = raw.trim().split("-", 2);
    if (parts.length == 2) {
      try {
        return new int[] { Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()) };
      } catch (NumberFormatException ignored) {
        // падаем ниже с общим сообщением
      }
    }
    throw new IllegalArgumentException(
      "Возрастной диапазон задан неверно: '" + raw + "'. Ожидается MIN-MAX, например 18-70");
  }

  private static AgeDistribution parseAgeDistribution(String raw) {
    if (raw == null || raw.isBlank()) return AgeDistribution.UNIFORM;
    try {
      return AgeDistribution.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
        "Неизвестное распределение возраста: '" + raw + "'. Допустимо: uniform, realistic");
    }
  }

  /** {@code --documents auto} — набор по возрасту и полу; иначе явный список кодов/имён. */
  /** Разбор {@code --corrupt}: регистр не важен, опечатка отклоняется с подсказкой. */
  private static CorruptField[] parseCorrupt(String[] raw) {
    java.util.List<CorruptField> fields = new java.util.ArrayList<>();
    for (String item : raw) {
      if (item == null || item.isBlank()) {
        continue;
      }
      try {
        fields.add(CorruptField.valueOf(item.trim().toUpperCase(Locale.ROOT)));
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException(
          "Неизвестное поле для --corrupt: '" + item.trim() + "'. Допустимо: inn, snils, address");
      }
    }
    return fields.toArray(new CorruptField[0]);
  }

  private void applyDocuments(IndividualProfileBuilder b) {
    if (documents == null || documents.length == 0) {
      return;
    }
    if (documents.length == 1 && documents[0].trim().equalsIgnoreCase("auto")) {
      b.documentsAuto();
      return;
    }
    b.documents(parseTypes(documents));
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
    OutputFiles.write(documentsPath(output), CsvExporter.toBytes(csv));
  }

  private static Path documentsPath(Path output) {
    String name = output.getFileName().toString();
    int dot = name.lastIndexOf('.');
    String target = dot > 0 ? name.substring(0, dot) + "-documents.csv" : name + "-documents.csv";
    return output.resolveSibling(target);
  }
}
