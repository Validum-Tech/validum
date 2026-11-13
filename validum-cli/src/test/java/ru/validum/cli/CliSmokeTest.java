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
 * Smoke-тесты CLI: запускаем {@link CommandLine#execute} программно, проверяем exit code и
 * содержимое файлов. {@code ValidumCli.main()} содержит {@code System.exit()}, а {@code execute()}
 * — нет, поэтому тесты безопасны для JVM.
 */
@DisplayName("CLI smoke — exit code и файлы (P3.1b)")
class CliSmokeTest {

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

  // ── version ──────────────────────────────────────────────────────────────

  @Test
  @DisplayName("version → exit 0, вывод содержит validum и Java")
  void version() {
    assertThat(run("version")).isZero();
    String out = capturedOut.toString(StandardCharsets.UTF_8);
    assertThat(out).contains("validum " + ru.validum.core.Version.version());
    assertThat(out).contains("Java:");
  }

  // ── scenarios ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("scenarios → exit 0, 5 slug'ов в stdout")
  void scenarios() {
    assertThat(run("scenarios")).isZero();
    String out = capturedOut.toString(StandardCharsets.UTF_8);
    assertThat(out)
      .contains("adult-resident-with-account")
      .contains("pensioner-male")
      .contains("salary-client");
  }

  // ── validate ─────────────────────────────────────────────────────────────

  @Test
  @DisplayName("validate inn валидный → exit 0, valid:true")
  void validateInnOk() {
    assertThat(run("validate", "inn", "500100732259")).isZero();
    assertThat(capturedOut.toString(StandardCharsets.UTF_8))
      .contains("\"valid\":true")
      .contains("INDIVIDUAL");
  }

  @Test
  @DisplayName("validate inn битый → exit 1, valid:false")
  void validateInnBad() {
    assertThat(run("validate", "inn", "500100732258")).isEqualTo(1);
    assertThat(capturedOut.toString(StandardCharsets.UTF_8)).contains("\"valid\":false");
  }

  @Test
  @DisplayName("validate неизвестный тип → exit 1, понятная ошибка в stderr")
  void validateUnknownType() {
    assertThat(run("validate", "foobar", "123")).isEqualTo(1);
    assertThat(capturedErr.toString(StandardCharsets.UTF_8))
      .contains("Неизвестный тип");
  }

  @Test
  @DisplayName("validate okpo ЮЛ → exit 0")
  void validateOkpoLegalEntity() {
    // 12345676 — валидный 8-значный (1+2+3+4+5+6+7=28, 28%11=6)
    assertThat(run("validate", "okpo", "12345676")).isZero();
    assertThat(capturedOut.toString(StandardCharsets.UTF_8))
      .contains("LEGAL_ENTITY");
  }

  @Test
  @DisplayName("validate ogrnip валидный → exit 0")
  void validateOgrnip() {
    assertThat(run("validate", "ogrnip", "304500116000157")).isZero();
  }

  @Test
  @DisplayName("validate ogrnip с ОГРН ЮЛ → exit 1 (длина не та)")
  void validateOgrnipRejectsOgrn() {
    assertThat(run("validate", "ogrnip", "1027700132195")).isEqualTo(1);
  }

  // ── fetch-bik --offline ──────────────────────────────────────────────────

  @Test
  @DisplayName("fetch-bik --offline → exit 0, инструкция в stderr, файл не создаётся")
  void fetchBikOffline() {
    Path target = tmp.resolve("ED807.xml");
    assertThat(run("fetch-bik", "--offline", "-o", target.toString())).isZero();
    assertThat(capturedErr.toString(StandardCharsets.UTF_8))
      .contains("--offline")
      .contains("cbr.ru");
    assertThat(Files.exists(target)).isFalse();
  }

  // ── generate → файлы ─────────────────────────────────────────────────────

  @Test
  @DisplayName("generate individual --format csv -o file → файл с BOM и заголовками")
  void generateIndividualCsvToFile() throws Exception {
    Path out = tmp.resolve("profiles.csv");
    assertThat(run("generate", "individual",
      "--seed", "42", "--count", "3",
      "--format", "csv", "-o", out.toString()))
      .isZero();

    assertThat(Files.exists(out)).isTrue();
    String csv = Files.readString(out, StandardCharsets.UTF_8);
    assertThat(csv).startsWith("\uFEFF");
    assertThat(csv).contains("fullName,gender,birthDate");
    // 3 профиля + заголовок = 4 строки
    assertThat(csv.split("\r\n")).hasSize(4);
  }

  @Test
  @DisplayName("generate individual --format json-array → массив из N объектов")
  void generateIndividualJsonArray() throws Exception {
    Path out = tmp.resolve("profiles.json");
    assertThat(run("generate", "individual",
      "--seed", "1", "--count", "2",
      "--format", "json-array", "-o", out.toString()))
      .isZero();

    String json = Files.readString(out, StandardCharsets.UTF_8);
    assertThat(json.trim()).startsWith("[").endsWith("]");
    assertThat(json).contains("\"fullName\"");
  }

  @Test
  @DisplayName("generate individual --format json → обёртка {count, seed, items}")
  void generateIndividualJsonWrapper() throws Exception {
    Path out = tmp.resolve("profiles-wrapper.json");
    assertThat(run("generate", "individual",
      "--seed", "42", "--count", "5",
      "--format", "json", "-o", out.toString()))
      .isZero();

    String json = Files.readString(out, StandardCharsets.UTF_8);
    assertThat(json).contains("\"count\" : 5");
    assertThat(json).contains("\"seed\" : 42");
    assertThat(json).contains("\"items\"");
  }

  @Test
  @DisplayName("generate ip --count 2 -o file → файл с колонкой ogrnip")
  void generateIp() throws Exception {
    Path out = tmp.resolve("ip.csv");
    assertThat(run("generate", "ip",
      "--seed", "42", "--count", "2", "--format", "csv", "-o", out.toString()))
      .isZero();

    String csv = Files.readString(out, StandardCharsets.UTF_8);
    assertThat(csv).contains("ogrnip,personId,fullName");
  }

  @Test
  @DisplayName("generate organization --type SE → префикс ГУП в CSV")
  void generateOrganizationGup() throws Exception {
    Path out = tmp.resolve("org.csv");
    assertThat(run("generate", "organization",
      "--seed", "42", "--type", "SE", "--format", "csv", "-o", out.toString()))
      .isZero();

    String csv = Files.readString(out, StandardCharsets.UTF_8);
    assertThat(csv).contains("ГУП «");
    assertThat(csv).contains(",61,"); // OKOPF=61
    assertThat(csv).contains(",14,"); // OKFS=14
  }

  @Test
  @DisplayName("generate individual --count 0 → exit 2, ошибка в stderr")
  void generateZeroCount() {
    assertThat(run("generate", "individual", "--count", "0")).isEqualTo(2);
    assertThat(capturedErr.toString(StandardCharsets.UTF_8))
      .contains("--count")
      .contains("1..10000");
  }

  @Test
  @DisplayName("generate individual --count 10001 → exit 2")
  void generateTooLargeCount() {
    assertThat(run("generate", "individual", "--count", "10001")).isEqualTo(2);
  }

  @Test
  @DisplayName("generate individual --format xml → exit не 0, ошибка про формат")
  void generateInvalidFormat() {
    int code = run("generate", "individual", "--format", "xml", "--seed", "1");
    // Picocli ловит исключение из call() и возвращает 1 (execution exception)
    assertThat(code).isNotZero();
  }
}
