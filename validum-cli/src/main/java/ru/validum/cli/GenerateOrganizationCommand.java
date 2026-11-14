package ru.validum.cli;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Mixin;
import ru.validum.core.export.FieldSelection;
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

  @Mixin CommonOptions common = new CommonOptions();



  @Option(names = "--type", description = "ОПФ: LLC | JSC | PJSC | NCO | FOUNDATION | SE | ME")
  String type;

  @Option(names = "--name", description = "Явное наименование (иначе — генерируется)")
  String name;

  @Option(
    names = "--fields",
    paramLabel = "<колонки>",
    description = "Оставить только эти колонки (через запятую) или пресет short")
  String fields;

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







  @Option(names = "--terrorist") boolean terrorist;
  @Option(names = "--blocked") boolean blocked;

  @Option(names = "--unique",
    description = "Без повторов ОГРН в наборе (требует --seed); при исчерпании вариантов — exit 2")
  boolean unique;

  private String tableName() {
    return common.table != null && !common.table.isBlank()
      ? common.table
      : "validum_organization";
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
    Region regionValue;
    try {
      regionValue = region != null ? RegionOption.parse(region) : null;
    } catch (IllegalArgumentException e) {
      System.err.println("Ошибка: " + e.getMessage());
      return 2;
    }

    List<OrganizationProfile> profiles = new ArrayList<>(common.count);
    UniqueGuard guard = unique ? new UniqueGuard("ОГРН", 10_000_000_000_000L) : null;
    try {
      for (int i = 0; i < common.count; i++) {
        long profileSeed = baseSeed + i;
        profiles.add(guard == null
          ? generateOne(profileSeed, regionValue)
          : guard.next(profileSeed, s -> generateOne(s, regionValue), OrganizationProfile::ogrn));
      }
    } catch (IllegalArgumentException e) {
      System.err.println("Ошибка: " + e.getMessage());
      return 2;
    }

    List<Map<String, String>> rows =
      FieldSelection.keep(OrganizationProfileFlattener.flattenAll(profiles), fields);
    OutputFormat fmt = OutputFormat.fromString(common.format);
    OutputMeta.print(System.err, baseSeed, null, common.seed == null);
    OutputWriter.of(common.output, fmt, common.dialect, tableName(), !common.noDdl).write(rows, baseSeed);
    return 0;
  }

  /** Один профиль ЮЛ под конкретный seed: опции командной строки применяются заново. */
  private OrganizationProfile generateOne(long profileSeed, Region regionValue) {
    OrganizationProfileGenerator.Builder b = OrganizationProfileGenerator.builder()
      .seed(profileSeed);
    if (type != null) b.organizationType(
      OrganizationType.valueOf(type.toUpperCase(Locale.ROOT)));
    if (name != null) b.organizationName(name);
    if (regionValue != null) b.region(regionValue);
    if (okved != null) b.okved(okved);
    if (bankBik != null) b.bik(bankBik);
    if (terrorist) b.terrorist(true);
    if (blocked) b.blocked(true);
    return b.build().generate();
  }
}
