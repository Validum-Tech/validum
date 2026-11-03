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
import ru.validum.core.bank.BikSource;
import ru.validum.core.export.IpProfileFlattener;
import ru.validum.core.name.Gender;
import ru.validum.core.organization.IpProfile;
import ru.validum.core.organization.IpProfileGenerator;
import ru.validum.core.profile.IndividualProfileGenerator;

@Command(
  name = "ip",
  mixinStandardHelpOptions = true,
  description = "Сгенерировать профиль(и) ИП"
)
public final class GenerateIpCommand implements Callable<Integer> {

  @Option(names = "--seed", description = "Базовый seed")
  Long seed;

  @Option(
    names = "--as-of",
    description = "Дата расчёта (ISO, например 2026-01-01): возраст и сроки документов считаются "
      + "от неё. Не задана — сегодня")
  String asOf;

  @Option(names = {"-n", "--count"}, defaultValue = "1", description = "Количество (1–10000)")
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

  @Option(
    names = "--okved",
    description = "Код ОКВЭД-2 (62.01, 41.20 …). Иначе — случайный из справочника")
  String okved;

  @Option(names = "--format", defaultValue = "json",
    description = "json | json-array | csv | csv-semicolon (русский Excel) | sql")
  String format;

  @Option(names = "--dialect", description = "SQL-диалект: postgres | oracle | mysql")
  String dialect = "postgres";

  @Option(names = "--table", description = "Имя таблицы для --format sql")
  String table = "validum_ip";

  @Option(names = "--no-ddl", description = "Без CREATE TABLE, только INSERT")
  boolean noDdl;


  @Option(names = {"-o", "--output"}, description = "Путь к файлу")
  Path output;

  @Option(names = "--terrorist") boolean terrorist;
  @Option(names = "--blocked") boolean blocked;

  @Option(names = "--unique",
    description = "Без повторов ОГРНИП в наборе (требует --seed); при исчерпании вариантов — exit 2")
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

    List<IpProfile> profiles = new ArrayList<>(count);
    LocalDate asOfDate;
    try {
      asOfDate = AsOfOption.parse(asOf);
      UniqueGuard guard = unique ? new UniqueGuard("ОГРНИП", 1_000_000_000_000_000L) : null;
      for (int i = 0; i < count; i++) {
        long profileSeed = baseSeed + i;
        profiles.add(guard == null
          ? generateOne(profileSeed, bikSource, asOfDate)
          : guard.next(profileSeed, s -> generateOne(s, bikSource, asOfDate), IpProfile::ogrnip));
      }
    } catch (IllegalArgumentException e) {
      System.err.println("Ошибка: " + e.getMessage());
      return 2;
    }

    List<Map<String, String>> rows = IpProfileFlattener.flattenAll(profiles);
    OutputFormat fmt = OutputFormat.fromString(format);
    OutputMeta.print(System.err, baseSeed, asOfDate, seed == null);
    OutputWriter.of(output, fmt, dialect, table, !noDdl).write(rows, baseSeed);
    return 0;
  }

  /** Один профиль ИП под конкретный seed: опции командной строки применяются заново. */
  private IpProfile generateOne(long profileSeed, BikSource bikSource, LocalDate asOfDate) {
    IndividualProfileGenerator.Builder b = IndividualProfileGenerator.builder()
      .seed(profileSeed)
      .bikSource(bikSource);
    if (gender != null) b.gender(Gender.valueOf(gender.toUpperCase(Locale.ROOT)));
    if (age != null) b.age(age);
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
}
