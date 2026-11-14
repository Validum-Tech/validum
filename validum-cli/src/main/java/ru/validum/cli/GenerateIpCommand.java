package ru.validum.cli;

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
import ru.validum.core.export.IpProfileFlattener;
import ru.validum.core.name.Gender;
import ru.validum.core.organization.IpProfile;
import ru.validum.core.organization.IpProfileGenerator;
import ru.validum.core.profile.AgeDistribution;
import ru.validum.core.profile.IndividualProfileBuilder;
import ru.validum.core.profile.IndividualProfileGenerator;

@Command(
  name = "ip",
  mixinStandardHelpOptions = true,
  description = "Сгенерировать профиль(и) ИП"
)
public final class GenerateIpCommand implements Callable<Integer> {

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

  @Option(names = "--bik",
    description = "Явный БИК банка (P3.3). Не найден в справочнике → exit 2")
  String bik;

  @Option(
    names = "--okved",
    description = "Код ОКВЭД-2 (62.01, 41.20 …). Иначе — случайный из справочника")
  String okved;

  @Option(
    names = "--fields",
    paramLabel = "<колонки>",
    description = "Оставить только эти колонки (через запятую) или пресет short")
  String fields;








  @Option(names = "--terrorist") boolean terrorist;
  @Option(names = "--blocked") boolean blocked;

  @Option(names = "--unique",
    description = "Без повторов ОГРНИП в наборе (требует --seed); при исчерпании вариантов — exit 2")
  boolean unique;

  private String tableName() {
    return common.table != null && !common.table.isBlank()
      ? common.table
      : "validum_ip";
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

    List<IpProfile> profiles = new ArrayList<>(common.count);
    LocalDate asOfDate;
    try {
      asOfDate = AsOfOption.parse(asOf);
      UniqueGuard guard = unique ? new UniqueGuard("ОГРНИП", 1_000_000_000_000_000L) : null;
      for (int i = 0; i < common.count; i++) {
        long profileSeed = baseSeed + i;
        profiles.add(guard == null
          ? generateOne(profileSeed, bikSource, asOfDate)
          : guard.next(profileSeed, s -> generateOne(s, bikSource, asOfDate), IpProfile::ogrnip));
      }
    } catch (IllegalArgumentException e) {
      System.err.println("Ошибка: " + e.getMessage());
      return 2;
    }

    List<Map<String, String>> rows =
      FieldSelection.keep(IpProfileFlattener.flattenAll(profiles), fields);
    OutputFormat fmt = OutputFormat.fromString(common.format);
    OutputMeta.print(System.err, baseSeed, asOfDate, common.seed == null);
    OutputWriter.of(common.output, fmt, common.dialect, tableName(), !common.noDdl).write(rows, baseSeed);
    return 0;
  }

  /** Один профиль ИП под конкретный seed: опции командной строки применяются заново. */
  private IpProfile generateOne(long profileSeed, BikSource bikSource, LocalDate asOfDate) {
    IndividualProfileBuilder b = IndividualProfileGenerator.builder()
      .seed(profileSeed)
      .bikSource(bikSource);
    if (gender != null) b.gender(Gender.valueOf(gender.toUpperCase(Locale.ROOT)));
    if (age != null) b.age(age);
    b.ageDistribution(parseAgeDistribution(ageDistribution));
    if (ageRange != null) {
      int[] range = parseAgeRange(ageRange);
      b.ageRange(range[0], range[1]);
    }
    if (region != null) b.region(RegionOption.parse(region));
    if (bik != null) b.bik(bik);
    if (asOfDate != null) b.asOf(asOfDate);
    if (terrorist) b.terrorist(true);
    if (blocked) b.blocked(true);
    IndividualProfileGenerator generator = b.build();
    return okved != null
      ? IpProfileGenerator.generate(generator, profileSeed, okved)
      : IpProfileGenerator.generate(generator, profileSeed);
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
}
