package ru.validum.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

/**
 * {@code -o out/people.json} падал с {@code NoSuchFileException}, если каталога {@code out/} не
 * было: {@code Files.write} не создаёт родительские каталоги. Проверяем, что CLI создаёт их сам —
 * для профилей, для файла документов и для дозаполнения CSV.
 */
@DisplayName("CLI: запись в несуществующий каталог")
class OutputDirTest {

  @TempDir Path tmp;

  @Test
  @DisplayName("generate создаёт вложенный каталог и файл")
  void generateCreatesMissingDirectories() {
    Path out = tmp.resolve("nested/deeper/people.json");

    int code = run(
      "generate", "individual", "--seed", "42", "--count", "2",
      "--format", "json-array", "-o", out.toString());

    assertThat(code).isZero();
    assertThat(out).exists();
    assertThat(Files.isDirectory(out.getParent())).isTrue();
  }

  @Test
  @DisplayName("файл документов создаётся в том же новом каталоге")
  void documentsFileUsesCreatedDirectory() {
    Path out = tmp.resolve("a/b/people.csv");

    // Вторая таблица пишется только для CSV и только в режиме split:
    // при --format json документы уходят массивом внутри профиля.
    int code = run(
      "generate", "individual", "--seed", "42", "--count", "2", "--format", "csv",
      "--documents", "21", "--documents-mode", "split", "-o", out.toString());

    assertThat(code).isZero();
    assertThat(out).exists();
    assertThat(out.resolveSibling("people-documents.csv")).exists();
  }

  @Test
  @DisplayName("fill пишет в несуществующий каталог")
  void fillCreatesMissingDirectory() throws Exception {
    Path in = tmp.resolve("in.csv");
    Files.writeString(in, "inn,fullName\n,Тест Тестов\n", StandardCharsets.UTF_8);
    Path out = tmp.resolve("x/y/filled.csv");

    int code = run("fill", "individual", "-i", in.toString(), "-o", out.toString());

    assertThat(code).isZero();
    assertThat(out).exists();
    assertThat(Files.readString(out, StandardCharsets.UTF_8)).contains("Тест Тестов");
  }

  private static int run(String... args) {
    CommandLine cmd = ValidumCli.commandLine();
    cmd.setOut(quiet());
    cmd.setErr(quiet());
    return cmd.execute(args);
  }

  /** picocli печатает через PrintWriter, а не PrintStream. */
  private static PrintWriter quiet() {
    return new PrintWriter(
      new OutputStreamWriter(new ByteArrayOutputStream(), StandardCharsets.UTF_8), true);
  }
}
