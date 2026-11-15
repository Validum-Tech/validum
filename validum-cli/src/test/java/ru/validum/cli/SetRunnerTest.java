package ru.validum.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;
import ru.validum.core.export.CsvImporter;

/** Набор из спецификации: организация + люди + ИП со связью «человек → работодатель». */
class SetRunnerTest {

  @TempDir
  Path tmp;

  /** Путь в JSON — с прямыми слэшами, иначе на Windows ломается экранирование. */
  private String jsonPath(String name) {
    return tmp.resolve(name).toAbsolutePath().toString().replace("\\", "/");
  }

  private int run(Path spec) {
    return new CommandLine(new ValidumCli()).execute("generate", "--spec", spec.toString());
  }


  private static List<Map<String, String>> readRows(Path file) throws IOException {
    String text = Files.readString(file, StandardCharsets.UTF_8);
    char delimiter = CsvImporter.detectDelimiter(text.lines().findFirst().orElse(""));
    return CsvImporter.parse(text, delimiter);
  }

  @Test
  @DisplayName("Один участник: файл на месте и столько строк, сколько просили")
  void singleMember() throws Exception {
    String spec = """
      {"type":"set","seed":11,"asOf":"2026-11-15","output":"%s",
       "members":[{"name":"people","kind":"individual","count":2}]}
      """.formatted(jsonPath("set.csv"));
    Path file = tmp.resolve("set.json");
    Files.writeString(file, spec, StandardCharsets.UTF_8);

    assertThat(run(file)).isZero();

    List<Map<String, String>> rows =
      readRows(tmp.resolve("set-individuals.csv"));
    assertThat(rows).hasSize(2);
  }

  @Test
  @DisplayName("Работодатель — ОГРН организации из набора, а не случайная строка")
  void employerLinksAreReal() throws Exception {
    String spec = """
      {"type":"set","seed":13,"asOf":"2026-11-15","output":"%s",
       "members":[{"name":"orgs","kind":"organization","count":2},
                  {"name":"people","kind":"individual","count":3,"employer":"orgs"}]}
      """.formatted(jsonPath("linked.csv"));
    Path file = tmp.resolve("linked.json");
    Files.writeString(file, spec, StandardCharsets.UTF_8);

    assertThat(run(file)).isZero();

    List<Map<String, String>> organizations =
      readRows(tmp.resolve("linked-organizations.csv"));
    List<Map<String, String>> people = readRows(tmp.resolve("linked-individuals.csv"));

    Set<String> orgs = organizations.stream()
      .map(row -> row.get("ogrn"))
      .collect(Collectors.toSet());
    assertThat(orgs).hasSize(2);

    List<Map<String, String>> relations = readRows(tmp.resolve("linked-relations.csv"));
    assertThat(relations).hasSize(3);
    assertThat(relations).allSatisfy(link -> {
      assertThat(orgs).contains(link.get("employerOgrn"));
      assertThat(link.get("personId")).isNotBlank();
    });
  }

  @Test
  @DisplayName("ИП тоже получает работодателя — он живёт в base профиля")
  void ipWithEmployerOgrn() throws Exception {
    String spec = """
      {"type":"set","seed":21,"asOf":"2026-11-15","output":"%s",
       "members":[{"name":"orgs","kind":"organization","count":1},
                  {"name":"ips","kind":"ip","count":2,"employer":"orgs"}]}
      """.formatted(jsonPath("ips.csv"));
    Path file = tmp.resolve("ips.json");
    Files.writeString(file, spec, StandardCharsets.UTF_8);

    assertThat(run(file)).isZero();

    List<Map<String, String>> ips = readRows(tmp.resolve("ips-ip.csv"));
    assertThat(ips).hasSize(2);
    assertThat(ips).allSatisfy(row -> assertThat(row.get("employerOgrn")).isNotBlank());
  }

  @Test
  @DisplayName("Ссылка на несуществующего работодателя — понятная ошибка и код 2")
  void invalidEmployerFails() throws Exception {
    String spec = """
      {"type":"set","seed":1,"asOf":"2026-11-15","output":"%s",
       "members":[{"name":"people","kind":"individual","count":1,"employer":"missing"}]}
      """.formatted(jsonPath("broken.csv"));
    Path file = tmp.resolve("broken.json");
    Files.writeString(file, spec, StandardCharsets.UTF_8);

    assertThat(run(file)).isEqualTo(2);

    String spec2 = """
      {"type":"set","seed":1,"asOf":"2026-11-15","output":"%s",
       "members":[{"name":"people","kind":"individual","count":1},
                  {"name":"self","kind":"individual","count":1,"employer":"people"}]}
      """.formatted(jsonPath("broken2.csv"));
    Files.writeString(file, spec2, StandardCharsets.UTF_8);

    assertThat(run(file)).isEqualTo(2);
  }
}
