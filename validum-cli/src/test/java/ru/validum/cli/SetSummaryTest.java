package ru.validum.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;
import ru.validum.core.export.CsvImporter;

/** Сводка набора: все файлы созданы, порядок и детерминизм предсказуемы. */
class SetSummaryTest {

  @TempDir
  Path tmp;

  private Path write(String name, String json) throws Exception {
    Path file = tmp.resolve(name);
    Files.writeString(file, json, StandardCharsets.UTF_8);
    return file;
  }

  private String jsonPath(String name) {
    return tmp.resolve(name).toAbsolutePath().toString().replace("\\", "/");
  }


  private static List<Map<String, String>> readRows(Path file) throws IOException {
    String text = Files.readString(file, StandardCharsets.UTF_8);
    char delimiter = CsvImporter.detectDelimiter(text.lines().findFirst().orElse(""));
    return CsvImporter.parse(text, delimiter);
  }

  @Test
  @DisplayName("Набор из четырёх участников даёт четыре файла и таблицу связей")
  void summaryListsEveryTable() throws Exception {
    String spec = """
      {"type":"set","seed":5,"asOf":"2026-11-15","output":"%s",
       "members":[{"name":"orgs","kind":"organization","count":1},
                  {"name":"people","kind":"individual","count":2,"employer":"orgs"},
                  {"name":"ips","kind":"ip","count":1,"employer":"orgs"}]}
      """.formatted(jsonPath("all.csv"));
    Path file = write("all.json", spec);

    assertThat(new CommandLine(new ValidumCli()).execute("generate", "--spec", file.toString()))
      .isZero();

    assertThat(tmp.resolve("all-organizations.csv")).exists();
    assertThat(tmp.resolve("all-individuals.csv")).exists();
    assertThat(tmp.resolve("all-ip.csv")).exists();
    assertThat(tmp.resolve("all-relations.csv")).exists();
  }

  @Test
  @DisplayName("Один seed — один и тот же набор байт в байт")
  void sameSeedGivesSameBytes() throws Exception {
    String specTemplate = """
      {"type":"set","seed":77,"asOf":"2026-11-15","output":"%s",
       "members":[{"name":"orgs","kind":"organization","count":1},
                  {"name":"people","kind":"individual","count":2,"employer":"orgs"}]}
      """;
    Path first = write("a.json", specTemplate.formatted(jsonPath("a.csv")));
    Path second = write("b.json", specTemplate.formatted(jsonPath("b.csv")));

    assertThat(new CommandLine(new ValidumCli()).execute("generate", "--spec", first.toString()))
      .isZero();
    assertThat(new CommandLine(new ValidumCli()).execute("generate", "--spec", second.toString()))
      .isZero();

    List<String> a = Files.readAllLines(tmp.resolve("a-individuals.csv"), StandardCharsets.UTF_8);
    List<String> b = Files.readAllLines(tmp.resolve("b-individuals.csv"), StandardCharsets.UTF_8);
    assertThat(a).isEqualTo(b);
  }

  @Test
  @DisplayName("Файл набора: каждый участник получает собственный seed, поэтому набор не пустой")
  void everyMemberHasRows() throws Exception {
    String spec = """
      {"type":"set","seed":3,"asOf":"2026-11-15","output":"%s",
       "members":[{"name":"orgs","kind":"organization","count":1},
                  {"name":"people","kind":"individual","count":1,"employer":"orgs"}]}
      """.formatted(jsonPath("one.csv"));
    Path file = write("one.json", spec);
    assertThat(new CommandLine(new ValidumCli()).execute("generate", "--spec", file.toString()))
      .isZero();

    List<Map<String, String>> rows = readRows(tmp.resolve("one-individuals.csv"));
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).get("fullName")).isNotBlank();
  }
}
