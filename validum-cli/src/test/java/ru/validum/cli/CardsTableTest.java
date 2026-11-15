package ru.validum.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

/** {@code --cards N>1} пишет вторую таблицу карт рядом с профилями. */
@DisplayName("CLI: таблица карт «Мир»")
class CardsTableTest {

  @TempDir Path tmp;

  @Test
  @DisplayName("--cards 3 -o people.csv → people-cards.csv с тремя строками на клиента")
  void writesCardsTable() throws Exception {
    Path out = tmp.resolve("people.csv");

    int code = run("generate", "individual", "--seed", "4", "--count", "2",
      "--cards", "3", "--format", "csv", "-o", out.toString());

    assertThat(code).isZero();
    Path cards = tmp.resolve("people-cards.csv");
    assertThat(cards).exists();

    List<String> lines = Files.readAllLines(cards, StandardCharsets.UTF_8);
    assertThat(lines.get(0)).isEqualTo("\uFEFFpersonId,pan,expiry,outcome");
    assertThat(lines).hasSize(1 + 6);
    assertThat(lines.subList(1, lines.size()))
      .allSatisfy(line -> assertThat(line).containsPattern("^pd-[0-9a-f]{16},220\\d{13},\\d{2}/\\d{2},"));
  }

  @Test
  @DisplayName("--cards 3 без -o → отказ с кодом 2")
  void cardsWithoutOutputIsRefused() {
    int code = run("generate", "individual", "--seed", "4", "--count", "1", "--cards", "3");
    assertThat(code).isEqualTo(2);
  }

  @Test
  @DisplayName("--cards 1 не создаёт вторую таблицу")
  void singleCardHasNoTable() {
    Path out = tmp.resolve("one.csv");

    int code = run("generate", "individual", "--seed", "5", "--count", "1",
      "--cards", "1", "--format", "csv", "-o", out.toString());

    assertThat(code).isZero();
    assertThat(tmp.resolve("one-cards.csv")).doesNotExist();
    assertThat(out).exists();
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
