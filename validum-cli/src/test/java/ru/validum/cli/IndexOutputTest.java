package ru.validum.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

@DisplayName("CLI generate individual --index")
class IndexOutputTest {

  @TempDir
  Path tmp;

  @Test
  @DisplayName("--index печатает таблицу с идентификаторами и подсказку describe")
  void printsIndexTable() throws Exception {
    Path cursor = tmp.resolve("set.json");
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    var originalOut = System.out;
    var originalErr = System.err;
    System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
    System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
    int code;
    try {
      code = new CommandLine(new GenerateIndividualCommand()).execute(
        "--seed", "1", "-n", "2", "--index", "--format", "json-array",
        "-o", cursor.toString());
    } finally {
      System.setOut(originalOut);
      System.setErr(originalErr);
    }

    assertThat(code).isZero();
    String errText = err.toString(StandardCharsets.UTF_8);
    assertThat(errText).contains("Идентификатор").contains("Возраст").contains("Счёт");
    assertThat(errText).contains("validum describe --file");
    // Таблица в stderr, данные — в файле: stdout не тронут.
    assertThat(out.toString(StandardCharsets.UTF_8)).isEmpty();
  }
}
