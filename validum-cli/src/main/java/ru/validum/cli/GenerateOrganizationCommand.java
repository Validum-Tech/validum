package ru.validum.cli;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import ru.validum.core.common.OrganizationType;
import ru.validum.core.common.Region;
import ru.validum.core.export.OrganizationProfileFlattener;
import ru.validum.core.organization.OrganizationProfile;
import ru.validum.core.organization.OrganizationProfileGenerator;

@Command(
  name = "organization",
  mixinStandardHelpOptions = true,
  description = "Сгенерировать профиль(и) юрлица"
)
public final class GenerateOrganizationCommand implements Callable<Integer> {

  @Option(names = "--seed", description = "Базовый seed")
  Long seed;

  @Option(names = {"-n", "--count"}, defaultValue = "1", description = "Количество (1–10000)")
  int count;

  @Option(names = "--type", description = "ОПФ: LLC | JSC | PJSC | NCO | FOUNDATION | SE | ME")
  String type;

  @Option(names = "--name", description = "Явное наименование (иначе — генерируется)")
  String name;

  @Option(names = "--region", description = "Регион: имя (MOSCOW, SPB, …) или код субъекта (77)")
  String region;

  @Option(
    names = "--okved",
    description = "Код ОКВЭД-2 (62.01, 41.20 …). Иначе — случайный из справочника")
  String okved;

  @Option(
    names = "--bank-bik",
    description = "БИК банка компании (9 цифр). Иначе — случайный из справочника")
  String bankBik;

  @Option(names = "--format", defaultValue = "json", description = "json | json-array | csv")
  String format;

  @Option(names = "--dialect", description = "SQL-диалект: postgres | oracle | mysql")
  String dialect = "postgres";

  @Option(names = "--table", description = "Имя таблицы для --format sql")
  String table = "validum_organization";

  @Option(names = "--no-ddl", description = "Без CREATE TABLE, только INSERT")
  boolean noDdl;


  @Option(names = {"-o", "--output"}, description = "Путь к файлу")
  Path output;

  @Option(names = "--terrorist") boolean terrorist;
  @Option(names = "--blocked") boolean blocked;

  @Override
  public Integer call() throws Exception {
    if (count < 1 || count > 10_000) {
      System.err.println("Ошибка: --count должен быть в диапазоне 1..10000");
      return 2;
    }
    long baseSeed = seed != null ? seed : System.nanoTime();
    Region regionValue;
    try {
      regionValue = region != null ? RegionOption.parse(region) : null;
    } catch (IllegalArgumentException e) {
      System.err.println("Ошибка: " + e.getMessage());
      return 2;
    }

    List<OrganizationProfile> profiles = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      OrganizationProfileGenerator.Builder b = OrganizationProfileGenerator.builder()
        .seed(baseSeed + i);
      if (type != null) b.organizationType(
        OrganizationType.valueOf(type.toUpperCase(Locale.ROOT)));
      if (name != null) b.organizationName(name);
      if (regionValue != null) b.region(regionValue);
      if (okved != null) b.okved(okved);
      if (bankBik != null) b.bik(bankBik);
      if (terrorist) b.terrorist(true);
      if (blocked) b.blocked(true);
      try {
        profiles.add(b.build().generate());
      } catch (IllegalArgumentException e) {
        System.err.println("Ошибка: " + e.getMessage());
        return 2;
      }
    }

    List<Map<String, String>> rows = OrganizationProfileFlattener.flattenAll(profiles);
    OutputFormat fmt = OutputFormat.fromString(format);
    OutputWriter.of(output, fmt, dialect, table, !noDdl).write(rows, baseSeed);
    return 0;
  }
}
