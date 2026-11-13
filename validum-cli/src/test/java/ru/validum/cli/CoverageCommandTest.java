package ru.validum.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;
import ru.validum.core.bank.BikSources;
import ru.validum.core.export.CsvExporter;
import ru.validum.core.export.IndividualProfileFlattener;
import ru.validum.core.profile.IndividualProfile;
import ru.validum.core.profile.IndividualProfileGenerator;

class CoverageCommandTest {

  @TempDir
  Path tmp;

  @Test
  void reportsCoverageOfCsv() throws Exception {
    IndividualProfile profile = IndividualProfileGenerator.builder()
      .bikSource(BikSources.resolve("src/test/resources/bik-sample.xml"))
      .seed(3)
      .build()
      .generate();
    Path csv = tmp.resolve("set.csv");
    Files.writeString(
      csv,
      "\uFEFFfullName,inn,snils\n"
        + "Иванов Иван Иванович," + profile.inn() + "," + profile.snils() + "\n"
        + "Петров Пётр Петрович,123456789012," + profile.snils() + "\n");

    String out = run("coverage", "--file", csv.toString());

    assertThat(out).contains("rows=2");
    assertThat(out).contains("inn");
    assertThat(out).contains("snils");
    // ИНН Петрова не проходит контрольную сумму — в отчёте должна быть цифра, а не «—».
    assertThat(out).containsPattern("inn\\s+100% \\(2/2\\)\\s+2\\s+1");
  }

  @Test
  void unwrapsExcelFormulaCells() throws Exception {
    IndividualProfile profile = IndividualProfileGenerator.builder()
      .bikSource(BikSources.resolve("src/test/resources/bik-sample.xml"))
      .seed(4)
      .build()
      .generate();
    Path csv = tmp.resolve("excel.csv");
    Files.writeString(
      csv,
      "\uFEFFfullName;inn;age\n"
        + "Иванов;\"=\"\"" + profile.inn() + "\"\"\";30\n");

    String out = run("coverage", "--file", csv.toString());

    assertThat(out).contains("rows=1");
    assertThat(out).contains("Согласованность: нарушений нет");
    // Обёртка ="…" снята: ИНН проверяется и валиден, в колонке «невалидных» — 0.
    assertThat(out).containsPattern("inn\\s+100% \\(1/1\\)\\s+1\\s+0");
  }

  @Test
  void checksFriendlyCsvWithRussianHeaders() throws Exception {
    IndividualProfile profile = IndividualProfileGenerator.builder()
      .bikSource(BikSources.resolve("src/test/resources/bik-sample.xml"))
      .seed(9)
      .asOf(LocalDate.of(2026, 1, 15))
      .age(30)
      .build()
      .generate();
    Path csv = tmp.resolve("friendly.csv");
    Files.writeString(
      csv,
      CsvExporter.toCsv(
        List.of(IndividualProfileFlattener.flatten(profile)), ';', true),
      StandardCharsets.UTF_8);

    String out = run("coverage", "--file", csv.toString());

    // Главное: русские заголовки переведены в ключи, поля реально проверены.
    assertThat(out).contains("Согласованность: нарушений нет");
    assertThat(out).containsPattern("inn\\s+100% \\(1/1\\)\\s+1\\s+0");
    assertThat(out).containsPattern("snils\\s+100% \\(1/1\\)\\s+1\\s+0");
    assertThat(out).contains("Распределения");
  }

  @Test
  void reportsCoverageOfJsonArray() throws Exception {
    Path json = tmp.resolve("set.json");
    Files.writeString(
      json,
      "[{\"fullName\":\"Иванов\",\"inn\":\"500100732259\"},{\"fullName\":\"Петров\",\"inn\":\"500100732258\"}]");

    String out = run("coverage", "--file", json.toString(), "--format", "json-array");

    assertThat(out).contains("rows=2");
    assertThat(out).contains("невалидными");
  }

  @Test
  void emptyFileIsError() throws Exception {
    Path csv = tmp.resolve("empty.csv");
    Files.writeString(csv, "fullName,inn\n");

    ByteArrayOutputStream err = new ByteArrayOutputStream();
    PrintStream original = System.err;
    System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
    int code;
    try {
      code = new CommandLine(new ValidumCli()).execute("coverage", "--file", csv.toString());
    } finally {
      System.setErr(original);
    }

    assertThat(code).isEqualTo(2);
    assertThat(err.toString(StandardCharsets.UTF_8)).contains("нет ни одной записи");
  }

  private String run(String... args) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream original = System.out;
    System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
    try {
      new CommandLine(new ValidumCli()).execute(args);
    } finally {
      System.setOut(original);
    }
    return out.toString(StandardCharsets.UTF_8);
  }
}
