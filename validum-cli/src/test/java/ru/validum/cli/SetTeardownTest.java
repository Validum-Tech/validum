package ru.validum.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

/** Скрипт отката набора: DELETE в обратном порядке, отключается ключом spec. */
class SetTeardownTest {

  @TempDir
  Path tmp;

  private String jsonPath(String name) {
    return tmp.resolve(name).toAbsolutePath().toString().replace("\\", "/");
  }

  private Path spec(String name, String json) throws Exception {
    Path file = tmp.resolve(name);
    Files.writeString(file, json, StandardCharsets.UTF_8);
    return file;
  }

  @Test
  @DisplayName("Набор пишет teardown.sql: сначала люди, потом организации")
  void teardownIsWritten() throws Exception {
    Path file = spec("t.json", """
      {"type":"set","seed":9,"asOf":"2026-11-15","output":"%s",
       "members":[{"name":"orgs","kind":"organization","count":1},
                  {"name":"people","kind":"individual","count":2,"employer":"orgs"}]}
      """.formatted(jsonPath("t.csv")));

    assertThat(new CommandLine(new ValidumCli()).execute("generate", "--spec", file.toString()))
      .isZero();

    Path sql = tmp.resolve("t-teardown.sql");
    assertThat(sql).exists();
    String text = Files.readString(sql, StandardCharsets.UTF_8);
    assertThat(text).startsWith("-- validum ");
    assertThat(text.indexOf("DELETE FROM validum_individual"))
      .isLessThan(text.indexOf("DELETE FROM validum_organization"));
    assertThat(text).doesNotContain("DROP", "TRUNCATE");
  }

  @Test
  @DisplayName("\"teardown\": false — файла нет")
  void teardownCanBeDisabled() throws Exception {
    Path file = spec("off.json", """
      {"type":"set","seed":9,"asOf":"2026-11-15","teardown":false,"output":"%s",
       "members":[{"name":"people","kind":"individual","count":1}]}
      """.formatted(jsonPath("off.csv")));

    assertThat(new CommandLine(new ValidumCli()).execute("generate", "--spec", file.toString()))
      .isZero();

    assertThat(tmp.resolve("off-teardown.sql")).doesNotExist();
  }
}
