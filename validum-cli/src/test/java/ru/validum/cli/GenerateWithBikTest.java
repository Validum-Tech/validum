package ru.validum.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

/**
 * CLI-тесты на {@code --bik} (P3.3).
 *
 * <p>Проверяют: явный БИК → банк в CSV; несуществующий БИК → exit 2 + сообщение в stderr;
 * без {@code --bik} → случайный банк (backward compat).
 */
@DisplayName("CLI generate --bik (P3.3)")
class GenerateWithBikTest {

  private static final String SBER_BIK = "044525225";
  private static final String VTB_BIK = "044525187";
  private static final String UNKNOWN_BIK = "000000000";

  @TempDir Path tmp;

  private final ByteArrayOutputStream capturedOut = new ByteArrayOutputStream();
  private final ByteArrayOutputStream capturedErr = new ByteArrayOutputStream();
  private PrintStream originalOut;
  private PrintStream originalErr;

  private int run(String... args) {
    originalOut = System.out;
    originalErr = System.err;
    System.setOut(new PrintStream(capturedOut, true, StandardCharsets.UTF_8));
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

  // ── generate individual --bik ────────────────────────────────────────────

  @Test
  @DisplayName("generate individual --bik SBER_BIK → CSV содержит bankBik=SBER_BIK")
  void individualWithSberBik() throws Exception {
    Path out = tmp.resolve("profiles.csv");
    int code = run("generate", "individual",
      "--seed", "42", "--count", "3",
      "--bik", SBER_BIK,
      "--format", "csv", "-o", out.toString());

    assertThat(code).isZero();
    String csv = Files.readString(out, StandardCharsets.UTF_8);
    // bankBik колонка должна содержать SBER_BIK во всех строках данных
    String[] lines = csv.split("\r\n");
    assertThat(lines.length).isEqualTo(4); // header + 3 data
    // Bank BIK есть в каждой строке данных
    for (int i = 1; i < lines.length; i++) {
      assertThat(lines[i]).contains(SBER_BIK);
    }
  }

  @Test
  @DisplayName("generate individual --bik VTB_BIK → CSV содержит VTB_BIK")
  void individualWithVtbBik() throws Exception {
    Path out = tmp.resolve("profiles.csv");
    int code = run("generate", "individual",
      "--seed", "42", "--count", "2",
      "--bik", VTB_BIK,
      "--format", "csv", "-o", out.toString());

    assertThat(code).isZero();
    String csv = Files.readString(out, StandardCharsets.UTF_8);
    assertThat(csv).contains(VTB_BIK);
    assertThat(csv).doesNotContain(SBER_BIK);
  }

  @Test
  @DisplayName("generate individual --bik UNKNOWN_BIK → exit 2 + сообщение в stderr")
  void individualWithUnknownBikFails() {
    int code = run("generate", "individual",
      "--seed", "42",
      "--bik", UNKNOWN_BIK,
      "--format", "csv");

    assertThat(code).isEqualTo(2);
    assertThat(capturedErr.toString(StandardCharsets.UTF_8))
      .contains("БИК не найден")
      .contains(UNKNOWN_BIK);
    // stdout пустой (ничего не сгенерировали)
    assertThat(capturedOut.toString(StandardCharsets.UTF_8)).isEmpty();
  }

  @Test
  @DisplayName("generate individual без --bik → работает (backward compat)")
  void individualWithoutBikWorks() throws Exception {
    Path out = tmp.resolve("profiles.csv");
    int code = run("generate", "individual",
      "--seed", "42", "--count", "2",
      "--format", "csv", "-o", out.toString());

    assertThat(code).isZero();
    String csv = Files.readString(out, StandardCharsets.UTF_8);
    assertThat(csv).isNotEmpty();
    // bankBik есть, но не прибит гвоздями к одному значению — просто проверим, что колонка заполнена
    assertThat(csv).contains("bankBik");
  }

  @Test
  @DisplayName("generate individual --bik --format json-array → bankBik в JSON")
  void individualWithBikJsonArray() throws Exception {
    Path out = tmp.resolve("profiles.json");
    int code = run("generate", "individual",
      "--seed", "42", "--count", "1",
      "--bik", SBER_BIK,
      "--format", "json-array", "-o", out.toString());

    assertThat(code).isZero();
    String json = Files.readString(out, StandardCharsets.UTF_8);
    assertThat(json).contains("\"bankBik\"");
    assertThat(json).contains(SBER_BIK);
    assertThat(json).contains("ПАО Сбербанк");
  }

  // ── generate ip --bik ─────────────────────────────────────────────────────

  @Test
  @DisplayName("generate ip --bik SBER_BIK → CSV содержит SBER_BIK")
  void ipWithSberBik() throws Exception {
    Path out = tmp.resolve("ip.csv");
    int code = run("generate", "ip",
      "--seed", "42", "--count", "2",
      "--bik", SBER_BIK,
      "--format", "csv", "-o", out.toString());

    assertThat(code).isZero();
    String csv = Files.readString(out, StandardCharsets.UTF_8);
    String[] lines = csv.split("\r\n");
    assertThat(lines.length).isEqualTo(3); // header + 2 data
    for (int i = 1; i < lines.length; i++) {
      assertThat(lines[i]).contains(SBER_BIK);
    }
  }

  @Test
  @DisplayName("generate ip --bik UNKNOWN_BIK → exit 2")
  void ipWithUnknownBikFails() {
    int code = run("generate", "ip",
      "--seed", "42",
      "--bik", UNKNOWN_BIK,
      "--format", "csv");

    assertThat(code).isEqualTo(2);
    assertThat(capturedErr.toString(StandardCharsets.UTF_8))
      .contains("БИК не найден");
  }

  @Test
  @DisplayName("generate ip без --bik → работает")
  void ipWithoutBikWorks() throws Exception {
    Path out = tmp.resolve("ip.csv");
    int code = run("generate", "ip",
      "--seed", "42", "--count", "1",
      "--format", "csv", "-o", out.toString());

    assertThat(code).isZero();
    assertThat(Files.readString(out, StandardCharsets.UTF_8)).contains("ogrnip");
  }

  // ── generate organization — без --bik ─────────────────────────────────────

  @Test
  @DisplayName("generate organization не знает --bik (P3.3 не трогает ЮЛ) → exit != 0")
  void organizationDoesNotSupportBik() {
    // Picocli вернёт ошибку парсинга (unknown option) — exit code != 0
    int code = run("generate", "organization",
      "--seed", "42", "--bik", SBER_BIK, "--format", "csv");

    assertThat(code).isNotZero();
  }
}
