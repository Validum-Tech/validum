package ru.validum.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ru.validum.core.bank.BikSources;
import ru.validum.core.export.CsvExporter;
import ru.validum.core.export.IndividualProfileFlattener;
import ru.validum.core.export.IpProfileFlattener;
import ru.validum.core.export.OrganizationProfileFlattener;
import ru.validum.core.organization.IpProfile;
import ru.validum.core.organization.IpProfileGenerator;
import ru.validum.core.organization.OrganizationProfile;
import ru.validum.core.organization.OrganizationProfileGenerator;
import ru.validum.core.profile.IndividualProfile;
import ru.validum.core.profile.IndividualProfileBuilder;
import ru.validum.core.profile.IndividualProfileGenerator;
import ru.validum.core.sql.TeardownSql;

/**
 * Исполнение набора из спецификации {@code type=set}: организации → люди и ИП со ссылкой на
 * работодателя, затем таблицы и связи.
 *
 * <p>Порядок записи повторяет порядок загрузки в БД: сначала организации (на них ссылаются люди),
 * затем физические лица, затем ИП. Каждому участнику выдаётся свой seed
 * ({@code seed + номер * 10007}), поэтому набор воспроизводится целиком.
 */
public final class SetRunner {

  private static final long SEED_STEP = 10_007L;
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final SetSpec spec;
  private final Path output;
  private final long seed;
  private final LocalDate asOf;

  public SetRunner(Path specFile, Path outputOverride) {
    this.spec = SetSpec.load(specFile);
    Path declared = spec.output() == null ? null : Path.of(spec.output());
    this.output = outputOverride != null ? outputOverride : declared;
    this.seed = spec.seed() == null ? 42L : spec.seed();
    this.asOf = spec.asOf() == null ? LocalDate.now() : LocalDate.parse(spec.asOf());
  }

  /** Быстрый признак набора: {@code "type": "set"} прямо в файле (без полного разбора). */
  public static boolean isSet(Path file) {
    try {
      JsonNode root = MAPPER.readTree(file.toFile());
      JsonNode type = root == null ? null : root.get("type");
      String raw = type == null ? null : type.asText();
      return raw != null
        && ("set".equalsIgnoreCase(raw.trim())
          || "набор".equalsIgnoreCase(raw.trim())
          || "наборы".equalsIgnoreCase(raw.trim()));
    } catch (Exception e) {
      return false;
    }
  }

  /** Собрать и записать набор; возвращает код возврата CLI. */
  public int run() {
    if (output == null) {
      System.err.println("Ошибка: у набора должен быть \"output\" в спецификации или -o");
      return 2;
    }
    try {
      write();
      return 0;
    } catch (IOException e) {
      System.err.println("Ошибка записи: " + e.getMessage());
      return 1;
    }
  }

  private void write() throws IOException {
    List<SetSpec.Member> members = spec.members();
    Map<String, List<OrganizationProfile>> organizations = generateOrganizations(members);
    Map<String, List<IndividualProfile>> individuals = generateIndividuals(members, organizations);
    Map<String, List<IpProfile>> ips = generateIps(members, organizations);

    int written = 0;
    for (SetSpec.Member member : members) {
      List<Map<String, String>> rows = switch (member.kind()) {
        case "organization" -> organizations.get(member.name()).stream()
          .map(OrganizationProfileFlattener::flatten)
          .toList();
        case "individual" -> individuals.get(member.name()).stream()
          .map(p -> IndividualProfileFlattener.flatten(p))
          .toList();
        default -> ips.get(member.name()).stream().map(IpProfileFlattener::flatten).toList();
      };
      Path file = memberFile(member);
      OutputFiles.write(file, CsvExporter.toBytes(CsvExporter.toCsv(rows)));
      System.err.printf("# %-12s %-13s %d строк → %s%n", member.name(), member.kind(),
        rows.size(), file);
      written += rows.size();
    }
    writeRelations(individuals, ips);
    if (spec.teardownWanted()) {
      writeTeardown(organizations, individuals, ips);
    }
    System.err.printf("# набор: %d строк в %d файлах, seed=%d, asOf=%s%n",
      written, members.size() + (hasEmployerLinks() ? 1 : 0), seed, asOf);
  }

  private Map<String, List<OrganizationProfile>> generateOrganizations(List<SetSpec.Member> members) {
    Map<String, List<OrganizationProfile>> result = new LinkedHashMap<>();
    for (int index = 0; index < members.size(); index++) {
      SetSpec.Member member = members.get(index);
      if (!"organization".equals(member.kind())) {
        continue;
      }
      List<OrganizationProfile> profiles = new ArrayList<>();
      for (int i = 0; i < member.count(); i++) {
        profiles.add(OrganizationProfileGenerator.builder()
          .seed(seed + index * SEED_STEP + i)
          .bikSource(BikSources.defaultSource())
          .build()
          .generate());
      }
      result.put(member.name(), profiles);
    }
    return result;
  }

  private Map<String, List<IndividualProfile>> generateIndividuals(
      List<SetSpec.Member> members, Map<String, List<OrganizationProfile>> organizations) {
    Map<String, List<IndividualProfile>> result = new LinkedHashMap<>();
    for (int index = 0; index < members.size(); index++) {
      SetSpec.Member member = members.get(index);
      if (!"individual".equals(member.kind())) {
        continue;
      }
      List<IndividualProfile> profiles = new ArrayList<>();
      for (int i = 0; i < member.count(); i++) {
        IndividualProfileBuilder builder = IndividualProfileGenerator.builder()
          .seed(seed + index * SEED_STEP + i)
          .asOf(asOf)
          .bikSource(BikSources.defaultSource());
        String employer = employerOgrn(member, organizations, i);
        if (employer != null) {
          builder.employer(employer);
        }
        profiles.add(builder.build().generate());
      }
      result.put(member.name(), profiles);
    }
    return result;
  }

  private Map<String, List<IpProfile>> generateIps(
      List<SetSpec.Member> members, Map<String, List<OrganizationProfile>> organizations) {
    Map<String, List<IpProfile>> result = new LinkedHashMap<>();
    for (int index = 0; index < members.size(); index++) {
      SetSpec.Member member = members.get(index);
      if (!"ip".equals(member.kind())) {
        continue;
      }
      List<IpProfile> profiles = new ArrayList<>();
      for (int i = 0; i < member.count(); i++) {
        long profileSeed = seed + index * SEED_STEP + i;
        IndividualProfileBuilder builder = IndividualProfileGenerator.builder()
          .seed(profileSeed)
          .asOf(asOf)
          .bikSource(BikSources.defaultSource());
        String employer = employerOgrn(member, organizations, i);
        if (employer != null) {
          builder.employer(employer);
        }
        profiles.add(IpProfileGenerator.from(builder.build().generate(), profileSeed));
      }
      result.put(member.name(), profiles);
    }
    return result;
  }

  /** Работодатель по кругу: i-й человек получает i-ю организацию участника. */
  private String employerOgrn(
      SetSpec.Member member, Map<String, List<OrganizationProfile>> organizations, int index) {
    if (member.employer() == null) {
      return null;
    }
    List<OrganizationProfile> pool = organizations.get(member.employer());
    return pool.get(index % pool.size()).ogrn();
  }

  private void writeRelations(
      Map<String, List<IndividualProfile>> individuals, Map<String, List<IpProfile>> ips)
      throws IOException {
    List<Map<String, String>> rows = new ArrayList<>();
    for (List<IndividualProfile> profiles : individuals.values()) {
      for (IndividualProfile profile : profiles) {
        if (profile.employerOgrn() != null) {
          rows.add(link(profile.personId(), profile.employerOgrn()));
        }
      }
    }
    for (List<IpProfile> profiles : ips.values()) {
      for (IpProfile profile : profiles) {
        if (profile.base().employerOgrn() != null) {
          rows.add(link(profile.base().personId(), profile.base().employerOgrn()));
        }
      }
    }
    if (rows.isEmpty()) {
      return;
    }
    OutputFiles.write(relationsFile(), CsvExporter.toBytes(CsvExporter.toCsv(rows)));
    System.err.printf("# %-12s %-13s %d строк → %s%n", "relations", "link", rows.size(),
      relationsFile());
  }

  /** Скрипт отката: организации удаляются последними, потому что на них ссылаются люди. */
  private void writeTeardown(
      Map<String, List<OrganizationProfile>> organizations,
      Map<String, List<IndividualProfile>> individuals,
      Map<String, List<IpProfile>> ips) throws IOException {
    List<TeardownSql.Table> tables = new ArrayList<>();
    organizations.values().forEach(list -> tables.add(new TeardownSql.Table(
      "validum_organization", "ogrn", list.stream().map(OrganizationProfile::ogrn).toList())));
    individuals.values().forEach(list -> tables.add(new TeardownSql.Table(
      "validum_individual", "personId", list.stream().map(IndividualProfile::personId).toList())));
    ips.values().forEach(list -> tables.add(new TeardownSql.Table(
      "validum_ip", "personId", list.stream().map(ip -> ip.base().personId()).toList())));

    String sql = TeardownSql.build(
      "validum " + ru.validum.core.Version.version() + ", набор, seed=" + seed, tables);
    Path file = teardownFile();
    OutputFiles.write(file, sql.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    System.err.printf("# %-12s %-13s %d таблиц → %s%n", "teardown", "delete", tables.size(), file);
  }

  private Path teardownFile() {
    String name = output.getFileName().toString();
    int dot = name.lastIndexOf('.');
    String stem = dot > 0 ? name.substring(0, dot) : name;
    return output.resolveSibling(stem + "-teardown.sql");
  }

  private static Map<String, String> link(String personId, String ogrn) {
    Map<String, String> row = new LinkedHashMap<>();
    row.put("personId", personId == null ? "" : personId);
    row.put("employerOgrn", ogrn);
    return row;
  }

  private boolean hasEmployerLinks() {
    return spec.members().stream().anyMatch(m -> m.employer() != null);
  }

  private Path memberFile(SetSpec.Member member) {
    String name = output.getFileName().toString();
    int dot = name.lastIndexOf('.');
    String stem = dot > 0 ? name.substring(0, dot) : name;
    String suffix = switch (member.kind()) {
      case "organization" -> "organizations";
      case "individual" -> "individuals";
      default -> "ip";
    };
    return output.resolveSibling(stem + "-" + suffix + ".csv");
  }

  private Path relationsFile() {
    String name = output.getFileName().toString();
    int dot = name.lastIndexOf('.');
    String stem = dot > 0 ? name.substring(0, dot) : name;
    return output.resolveSibling(stem + "-relations.csv");
  }
}
