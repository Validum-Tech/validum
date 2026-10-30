package ru.validum.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

@DisplayName("CLI generate documents — ДУЛ (СПДУЛ ФНС)")
class GenerateDocumentsCommandTest {

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

  @Test
  @DisplayName("Два вида документов по кодам СПДУЛ")
  void twoTypesByCode() {
    int code = run("generate", "documents", "--type", "21,22", "--seed", "1");

    assertThat(code).isZero();
    String out = capturedOut.toString(StandardCharsets.UTF_8);
    assertThat(out).contains("\"spdulCode\" : \"21\"");
    assertThat(out).contains("\"spdulCode\" : \"22\"");
    assertThat(out).contains("\"count\" : 2");
  }

  @Test
  @DisplayName("Два набора одного вида дают две строки")
  void countMultiplies() {
    int code = run("generate", "documents", "--type", "PASSPORT_RF", "-n", "2", "--seed", "2");

    assertThat(code).isZero();
    assertThat(capturedOut.toString(StandardCharsets.UTF_8)).contains("\"count\" : 2");
  }

  @Test
  @DisplayName("Неизвестный вид документа — код выхода 2")
  void unknownType() {
    int code = run("generate", "documents", "--type", "XX");

    assertThat(code).isEqualTo(2);
    assertThat(capturedErr.toString(StandardCharsets.UTF_8)).contains("Неизвестный вид документа");
  }
}
