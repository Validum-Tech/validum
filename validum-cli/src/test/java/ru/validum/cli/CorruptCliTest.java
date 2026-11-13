package ru.validum.cli;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;
import ru.validum.core.validation.InnValidator;
import ru.validum.core.validation.SnilsValidator;

@DisplayName("CLI --corrupt: намеренная порча полей")
class CorruptCliTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

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
      int code = new CommandLine(new GenerateCommand()).execute(args);
      return new Result(code, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
    } finally {
      System.setOut(originalOut);
      System.setErr(originalErr);
    }
  }

  @Test
  @DisplayName("--corrupt inn,snils → суммы не сходятся, в stderr отметка")
  void corruptsInnAndSnils() throws Exception {
    Path out = tmp.resolve("bad.json");
    Result result = run(
      "individual", "--seed", "1", "-n", "2", "--corrupt", "inn,snils",
      "--format", "json-array", "-o", out.toString());

    assertThat(result.code()).isZero();
    assertThat(result.err()).contains("# испорчено: inn,snils");
    JsonNode array = MAPPER.readTree(out.toFile());
    assertThat(array).hasSize(2);
    for (JsonNode profile : array) {
      assertThat(InnValidator.isValid(profile.get("inn").asText())).isFalse();
      assertThat(SnilsValidator.isValid(profile.get("snils").asText())).isFalse();
    }
  }

  @Test
  @DisplayName("--corrupt address → пустой индекс и сломанная строка адреса")
  void corruptsAddress() throws Exception {
    Path out = tmp.resolve("addr.json");
    Result result = run(
      "individual", "--seed", "2", "-n", "1", "--corrupt", "address",
      "--format", "json-array", "-o", out.toString());

    assertThat(result.code()).isZero();
    JsonNode profile = MAPPER.readTree(out.toFile()).get(0);
    assertThat(profile.get("registrationPostalCode").asText()).isEmpty();
    assertThat(profile.get("registrationFormatted").asText()).isEqualTo("ул. , д. , кв. ");
    // ИНН и СНИЛС не тронуты.
    assertThat(InnValidator.isValid(profile.get("inn").asText())).isTrue();
  }

  @Test
  @DisplayName("Опечатка в поле порчи — понятная ошибка и код 2")
  void unknownFieldIsRejected() {
    Result result = run("individual", "--seed", "1", "--corrupt", "inns");

    assertThat(result.code()).isEqualTo(2);
    assertThat(result.err()).contains("Неизвестное поле для --corrupt: 'inns'")
      .contains("inn, snils, address");
  }

  @Test
  @DisplayName("Спецификация: \"corrupt\": [\"inn\"] работает так же")
  void specCorrupt() throws Exception {
    Path spec = tmp.resolve("bad.validum.json");
    Path out = tmp.resolve("spec.json");
    Files.writeString(spec, """
      {"type": "individual", "seed": 5, "count": 1, "corrupt": ["inn"],
       "format": "json-array", "output": "%s"}
      """.formatted(out.toString().replace("\\", "\\\\")), StandardCharsets.UTF_8);

    Result result = run("--spec", spec.toString());

    assertThat(result.code()).isZero();
    JsonNode profile = MAPPER.readTree(out.toFile()).get(0);
    assertThat(InnValidator.isValid(profile.get("inn").asText())).isFalse();
    assertThat(result.err()).contains("# испорчено: inn");
  }
}
