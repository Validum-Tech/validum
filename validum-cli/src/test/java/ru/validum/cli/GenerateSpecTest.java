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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

@DisplayName("generate --spec: набор из .validum.json (W47-C1)")
class GenerateSpecTest {

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
    capturedErr.reset();
  }

  private Path spec(String json) throws Exception {
    Path file = tmp.resolve("set.validum.json");
    Files.writeString(file, json, StandardCharsets.UTF_8);
    return file;
  }

  @Test
  @DisplayName("ФЛ: count/seed/format/asOf из файла — ровно count записей")
  void individualsFromSpec() throws Exception {
    Path set = spec("""
      {"type": "individual", "count": 3, "seed": 42, "asOf": "2026-01-01", "format": "json-array"}
      """);
    Path out = tmp.resolve("out.json");
    int code = run("generate", "--spec", set.toString(), "-o", out.toString());

    assertThat(code).isZero();
    JsonNode rows = MAPPER.readTree(out.toFile());
    assertThat(rows.size()).isEqualTo(3);
    assertThat(rows.get(0).get("inn").asText()).hasSize(12);
  }

  @Test
  @DisplayName("csv-semicolon из спецификации: русский заголовок и BOM")
  void excelCsvFromSpec() throws Exception {
    Path set = spec("""
      {"type": "individual", "count": 2, "seed": 7, "format": "csv-semicolon"}
      """);
    Path out = tmp.resolve("out.csv");
    assertThat(run("generate", "--spec", set.toString(), "-o", out.toString())).isZero();

    byte[] bytes = Files.readAllBytes(out);
    assertThat(bytes[0] & 0xFF).isEqualTo(0xEF);
    assertThat(bytes[1] & 0xFF).isEqualTo(0xBB);
    assertThat(bytes[2] & 0xFF).isEqualTo(0xBF);
    assertThat(Files.readString(out)).startsWith("\uFEFFИдентификатор;ФИО;");
  }

  @Test
  @DisplayName("Вложенный options и русский синоним типа")
  void nestedOptionsAndAliases() throws Exception {
    Path set = spec("""
      {"type": "фл", "count": 1, "seed": 1, "format": "json-array", "options": {"region": "77"}}
      """);
    Path out = tmp.resolve("out.json");
    assertThat(run("generate", "--spec", set.toString(), "-o", out.toString())).isZero();
    assertThat(MAPPER.readTree(out.toFile()).size()).isEqualTo(1);
  }

  @Test
  @DisplayName("unique без seed — отказ с понятным текстом")
  void uniqueRequiresSeed() throws Exception {
    Path set = spec("""
      {"type": "individual", "count": 5, "unique": true}
      """);
    assertThat(run("generate", "--spec", set.toString())).isEqualTo(2);
    assertThat(capturedErr.toString(StandardCharsets.UTF_8)).contains("seed");
  }

  @Test
  @DisplayName("Опечатка в ключе — ошибка, а не тихий пропуск")
  void unknownKeyIsRejected() throws Exception {
    Path set = spec("""
      {"type": "individual", "formt": "csv"}
      """);
    assertThat(run("generate", "--spec", set.toString())).isEqualTo(2);
    assertThat(capturedErr.toString(StandardCharsets.UTF_8)).contains("formt");
  }

  @Test
  @DisplayName("type=documents без списка документов — отказ")
  void documentsNeedCodes() throws Exception {
    Path set = spec("""
      {"type": "documents", "count": 1, "seed": 1}
      """);
    assertThat(run("generate", "--spec", set.toString())).isEqualTo(2);
    assertThat(capturedErr.toString(StandardCharsets.UTF_8)).contains("documents");
  }

  @Test
  @DisplayName("Без --spec и без подкоманды — код 2")
  void missingSpecShowsUsage() {
    assertThat(run("generate")).isEqualTo(2);
  }

  @Test
  @DisplayName("Спецификация с ageRange и без ageDistribution не падает")
  void ageRangeWithoutDistribution() throws Exception {
    Path set = spec("""
      {"type": "individual", "count": 3, "seed": 5, "format": "csv", "ageRange": "25-54"}
      """);
    Path out = tmp.resolve("range-only.csv");
    int code = run("generate", "--spec", set.toString(), "-o", out.toString());

    assertThat(code).isZero();
    assertThat(Files.readAllLines(out, StandardCharsets.UTF_8)).hasSize(4);
    assertThat(capturedErr.toString(StandardCharsets.UTF_8)).doesNotContain("Exception");
  }
}
