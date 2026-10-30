package ru.validum.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

@DisplayName("CLI fill individual — соединение ДУЛ из второй таблицы")
class FillIndividualDocumentsTest {

  @Test
  @DisplayName("Документы из documents.csv подклеиваются по ИНН")
  void joinsDocumentsByInn(@TempDir Path dir) throws Exception {
    Path input = dir.resolve("profiles.csv");
    Files.writeString(input,
      "\uFEFFfullName,inn\r\nИванов Иван Иванович,500100732259\r\n",
      StandardCharsets.UTF_8);

    Path documents = dir.resolve("documents.csv");
    Files.writeString(documents,
      "\uFEFFprofileKey,spdulCode,title,series,number,issuedBy,issueDate,expirationDate,"
        + "departmentCode\r\n"
        + "500100732259,21,Паспорт гражданина РФ,45 10,123456,ГУ МВД,2015-06-01,,770-001\r\n",
      StandardCharsets.UTF_8);

    Path output = dir.resolve("filled.csv");
    int code = new CommandLine(new ValidumCli()).execute(
      "fill", "individual",
      "--input", input.toString(),
      "--documents-file", documents.toString(),
      "-o", output.toString(),
      "--seed", "1");

    assertThat(code).isZero();
    String csv = Files.readString(output, StandardCharsets.UTF_8);
    assertThat(csv).contains("documents").contains("21|45 10|123456");
  }
}
