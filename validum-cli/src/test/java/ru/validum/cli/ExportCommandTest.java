package ru.validum.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

@DisplayName("CLI: export ed807 / ed808")
class ExportCommandTest {

  private static final Charset CP1251 = Charset.forName("windows-1251");

  /** БИК Сбербанка из тестовой фикстуры справочника. */
  private static final String SBER_BIK = "044525225";

  @Test
  @DisplayName("export ed807 --limit 2 пишет XML ED807 с двумя участниками")
  void exportEd807(@TempDir Path dir) throws Exception {
    Path out = dir.resolve("ed807.xml");

    int code = new CommandLine(new ExportCommand())
      .execute("ed807", "--limit", "2", "-o", out.toString());

    assertThat(code).isZero();
    String xml = Files.readString(out, CP1251);
    assertThat(xml).contains("<ED807").contains("InfoTypeCode=\"SIRR\"");
    assertThat(count(xml, "<BICDirectoryEntry")).isEqualTo(2);
  }

  @Test
  @DisplayName("export ed808 пишет XML ED808 для известного БИК")
  void exportEd808(@TempDir Path dir) throws Exception {
    Path out = dir.resolve("ed808.xml");
    String bik = SBER_BIK;

    int code = new CommandLine(new ExportCommand())
      .execute("ed808", "--bik", bik, "--seed", "42", "-o", out.toString());

    assertThat(code).isZero();
    String xml = Files.readString(out, CP1251);
    assertThat(xml).contains("<ED808").contains("InfoTypeCode=\"FIRR\"");
  }

  @Test
  @DisplayName("export ed808 с неизвестным БИК → код 2")
  void exportEd808Unknown(@TempDir Path dir) {
    Path out = dir.resolve("none.xml");

    int code = new CommandLine(new ExportCommand())
      .execute("ed808", "--bik", "000000000", "-o", out.toString());

    assertThat(code).isEqualTo(2);
    assertThat(Files.exists(out)).isFalse();
  }

  private static int count(String text, String needle) {
    int n = 0;
    int from = 0;
    while ((from = text.indexOf(needle, from)) >= 0) {
      n++;
      from += needle.length();
    }
    return n;
  }
}
