package ru.validum.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;
import ru.validum.core.export.CsvImporter;

@DisplayName("CLI fill individual — дозаполнение CSV (P4.1)")
class FillCommandTest {

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

  @Test
  @DisplayName("Пустые ячейки заполняются, непустые сохраняются")
  void fillsEmptyCells() throws Exception {
    Path input = tmp.resolve("in.csv");
    Path output = tmp.resolve("out.csv");
    Files.writeString(
      input, "fullName,inn\nИванов Иван Иванович,\n", StandardCharsets.UTF_8);

    int code =
      run(
        "fill", "individual",
        "--input", input.toString(),
        "--output", output.toString(),
        "--seed", "42");

    assertThat(code).isZero();
    List<Map<String, String>> rows =
      CsvImporter.parse(Files.readString(output, StandardCharsets.UTF_8));
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0)).containsEntry("fullName", "Иванов Иван Иванович");
    assertThat(rows.get(0).get("inn")).isNotBlank();
  }

  @Test
  @DisplayName("CSV без строк данных → exit 2")
  void emptyInput() throws Exception {
    Path input = tmp.resolve("empty.csv");
    Files.writeString(input, "", StandardCharsets.UTF_8);

    int code = run("fill", "individual", "--input", input.toString());

    assertThat(code).isEqualTo(2);
    assertThat(capturedErr.toString(StandardCharsets.UTF_8)).contains("нет строк");
  }
}
