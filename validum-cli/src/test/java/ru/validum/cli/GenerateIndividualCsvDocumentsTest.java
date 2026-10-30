package ru.validum.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

@DisplayName("CLI generate individual — ДУЛ в CSV")
class GenerateIndividualCsvDocumentsTest {

  @Test
  @DisplayName("inline: документы попадают в колонку documents")
  void inlineDocuments(@TempDir Path dir) throws Exception {
    Path out = dir.resolve("profiles.csv");

    int code = new CommandLine(new ValidumCli()).execute(
      "generate", "individual", "--seed", "1", "--format", "csv",
      "--documents", "21", "-o", out.toString());

    assertThat(code).isZero();
    String csv = Files.readString(out, StandardCharsets.UTF_8);
    assertThat(csv).contains("documents").contains("21|");
  }

  @Test
  @DisplayName("split: рядом с профилями появляется profiles-documents.csv")
  void splitDocuments(@TempDir Path dir) throws Exception {
    Path out = dir.resolve("profiles.csv");

    int code = new CommandLine(new ValidumCli()).execute(
      "generate", "individual", "--seed", "1", "--format", "csv",
      "--documents", "21", "--documents-mode", "split", "-o", out.toString());

    assertThat(code).isZero();
    Path docs = dir.resolve("profiles-documents.csv");
    assertThat(docs).exists();
    String table = Files.readString(docs, StandardCharsets.UTF_8);
    assertThat(table).contains("profileKey").contains("spdulCode").contains("21");
  }

  @Test
  @DisplayName("split без -o — код выхода 2")
  void splitRequiresOutput() {
    int code = new CommandLine(new ValidumCli()).execute(
      "generate", "individual", "--format", "csv",
      "--documents", "21", "--documents-mode", "split");

    assertThat(code).isEqualTo(2);
  }
}
