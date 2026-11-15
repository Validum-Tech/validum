package ru.validum.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

/** {@code --format json-nested}: вложенная структура в файл и отказ для --fields. */
@DisplayName("CLI: вложенный JSON")
class NestedFormatTest {

  @TempDir Path tmp;

  @Test
  @DisplayName("json-nested пишет профили с группами")
  void writesNested() throws Exception {
    Path out = tmp.resolve("people-nested.json");

    int code = run("generate", "individual", "--seed", "11", "--count", "2",
      "--format", "json-nested", "-o", out.toString());

    assertThat(code).isZero();
    String json = Files.readString(out, StandardCharsets.UTF_8);
    assertThat(json).startsWith("{\"count\":2,\"seed\":11,\"profiles\":[{");
    assertThat(json).contains("\"cards\":[{");
    assertThat(json).contains("\"passport\":{");
  }

  @Test
  @DisplayName("--fields с json-nested — отказ с кодом 2")
  void fieldsRefused() {
    Path out = tmp.resolve("x.json");
    int code = run("generate", "individual", "--seed", "11", "--count", "1",
      "--format", "json-nested", "--fields", "inn,fullName", "-o", out.toString());
    assertThat(code).isEqualTo(2);
    assertThat(out).doesNotExist();
  }

  private static int run(String... args) {
    CommandLine cmd = ValidumCli.commandLine();
    cmd.setOut(quiet());
    cmd.setErr(quiet());
    return cmd.execute(args);
  }

  private static PrintWriter quiet() {
    return new PrintWriter(Writer.nullWriter());
  }
}
