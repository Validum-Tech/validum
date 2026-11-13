package ru.validum.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;
import ru.validum.core.bank.BikSources;
import ru.validum.core.export.CsvExporter;
import ru.validum.core.export.IndividualProfileFlattener;
import ru.validum.core.profile.IndividualProfile;
import ru.validum.core.profile.IndividualProfileGenerator;

@DisplayName("CLI describe: карточка человека из набора")
class DescribeCommandTest {

  @TempDir
  Path tmp;

  private record Result(int code, String out, String err) {}

  private Result run(String... args) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    var originalOut = System.out;
    var originalErr = System.err;
    System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
    System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
    try {
      int code = new CommandLine(new DescribeCommand()).execute(args);
      return new Result(code, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
    } finally {
      System.setOut(originalOut);
      System.setErr(originalErr);
    }
  }

  private IndividualProfile profile(long seed) {
    return IndividualProfileGenerator.builder()
      .bikSource(BikSources.resolve("src/test/resources/bik-sample.xml"))
      .seed(seed)
      .asOf(LocalDate.of(2026, 1, 15))
      .age(30)
      .build()
      .generate();
  }

  private Path writeSet(IndividualProfile profile) throws Exception {
    Path csv = tmp.resolve("set.csv");
    Files.writeString(
      csv,
      CsvExporter.toCsv(List.of(IndividualProfileFlattener.flatten(profile)), ';', true),
      StandardCharsets.UTF_8);
    return csv;
  }

  @Test
  @DisplayName("--row 1 — карточка с ФИО, паспортом, адресами и банком")
  void printsFullCard() throws Exception {
    IndividualProfile profile = profile(3);
    Path csv = writeSet(profile);

    Result result = run("--file", csv.toString(), "--row", "1");

    assertThat(result.code()).isZero();
    assertThat(result.out())
      .contains(profile.fullName())
      .contains("[Кто]")
      .contains("[Проверки]")
      .contains("[Паспорт]")
      .contains("[Адреса]")
      .contains("[Банк и счёт]")
      .contains(profile.passport().series())
      .contains(profile.passport().number())
      .contains("Согласованность:")
      .contains("нарушений нет");
  }

  @Test
  @DisplayName("--person находит того же человека по идентификатору")
  void findsByPersonId() throws Exception {
    IndividualProfile profile = profile(4);
    Path csv = writeSet(profile);

    Result result = run("--file", csv.toString(), "--person", profile.personId());

    assertThat(result.code()).isZero();
    assertThat(result.out()).contains(profile.fullName()).contains(profile.personId());
  }

  @Test
  @DisplayName("Без --person и --row — ошибка и код 2")
  void requiresSelector() throws Exception {
    Path csv = writeSet(profile(5));

    Result result = run("--file", csv.toString());

    assertThat(result.code()).isEqualTo(2);
    assertThat(result.err()).contains("--person").contains("--row");
  }

  @Test
  @DisplayName("Несуществующая запись — код 2 и сообщение")
  void unknownRowIsRejected() throws Exception {
    Path csv = writeSet(profile(6));

    Result result = run("--file", csv.toString(), "--row", "99");

    assertThat(result.code()).isEqualTo(2);
    assertThat(result.err()).contains("нет такой записи");
  }
}
