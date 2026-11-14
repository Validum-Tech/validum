package ru.validum.cli;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

@DisplayName("CLI --unique — без повторов ключевого поля (W47-B1)")
class UniqueCliTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @TempDir Path tmp;

  private final ByteArrayOutputStream capturedErr = new ByteArrayOutputStream();
  private PrintStream originalOut;
  private PrintStream originalErr;

  private int run(String... args) {
    originalOut = System.out;
    originalErr = System.err;
    System.setOut(new PrintStream(OutputStream.nullOutputStream(), true, StandardCharsets.UTF_8));
    System.setErr(new PrintStream(capturedErr, true, StandardCharsets.UTF_8));
    try {
      return new CommandLine(new ValidumCli()).execute(args);
    } finally {
      System.setOut(originalOut);
      System.setErr(originalErr);
    }
  }

  @AfterEach
  void restoreStreams() {
    if (originalOut != null) System.setOut(originalOut);
    if (originalErr != null) System.setErr(originalErr);
  }

  private Path out(String name) {
    return tmp.resolve(name);
  }

  /** Список значений поля из плоского массива профилей. */
  private static List<String> fields(Path file, String name) throws Exception {
    JsonNode array = MAPPER.readTree(Files.readString(file));
    List<String> values = new java.util.ArrayList<>(array.size());
    for (JsonNode node : array) {
      values.add(node.get(name).asText());
    }
    return values;
  }

  @Test
  @DisplayName("20 профилей ФЛ с --unique: все ИНН разные")
  void individualUniqueInns() throws Exception {
    Path file = out("unique.json");
    int code = run("generate", "individual", "--unique", "--seed", "1", "-n", "20",
      "--format", "json-array", "-o", file.toString());

    assertThat(code).isZero();
    List<String> inns = fields(file, "inn");
    assertThat(inns).hasSize(20).doesNotHaveDuplicates();
  }

  @Test
  @DisplayName("Повторный прогон с тем же seed даёт тот же набор")
  void uniqueIsReproducible() throws Exception {
    Path first = out("a.json");
    Path second = out("b.json");
    assertThat(run("generate", "individual", "--unique", "--seed", "7", "-n", "10",
      "--format", "json-array", "-o", first.toString())).isZero();
    assertThat(run("generate", "individual", "--unique", "--seed", "7", "-n", "10",
      "--format", "json-array", "-o", second.toString())).isZero();

    assertThat(Files.readString(first)).isEqualTo(Files.readString(second));
  }

  @Test
  @DisplayName("--unique без --seed → exit 2 и понятное сообщение")
  void uniqueRequiresSeed() {
    int code = run("generate", "individual", "--unique", "-n", "5");

    assertThat(code).isEqualTo(2);
    assertThat(capturedErr.toString(StandardCharsets.UTF_8))
      .contains("--unique требует --seed");
  }

  @Test
  @DisplayName("--unique-pool 10 и 11 строк → exit 2, честный текст про исчерпанный пул")
  void exhaustionIsReportedWithPoolSize() {
    int code = run(
      "generate", "individual", "--seed", "1", "-n", "11",
      "--unique", "--unique-pool", "10");

    assertThat(code).isEqualTo(2);
    assertThat(capturedErr.toString(StandardCharsets.UTF_8))
      .contains("пул исчерпан")
      .contains("из 10 вариантов")
      .contains("--unique-pool");
  }

  @Test
  @DisplayName("--unique работает для ИП (ОГРНИП)")
  void ipUniqueOgrnips() throws Exception {
    Path file = out("ip.json");
    int code = run("generate", "ip", "--unique", "--seed", "3", "-n", "10",
      "--format", "json-array", "-o", file.toString());

    assertThat(code).isZero();
    List<String> ogrnips = fields(file, "ogrnip");
    assertThat(ogrnips).hasSize(10).doesNotHaveDuplicates();
  }

  @Test
  @DisplayName("--unique работает для ЮЛ (ОГРН)")
  void organizationUniqueOgrns() throws Exception {
    Path file = out("org.json");
    int code = run("generate", "organization", "--unique", "--seed", "5", "-n", "10",
      "--format", "json-array", "-o", file.toString());

    assertThat(code).isZero();
    List<String> ogrns = fields(file, "ogrn");
    assertThat(ogrns).hasSize(10).doesNotHaveDuplicates();
  }
}
