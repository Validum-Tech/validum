package ru.validum.cli;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

@DisplayName("--residency / --country: нерезидент живёт в своей стране (0.3.0)")
class ResidencyCliTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @TempDir Path tmp;

  private final ByteArrayOutputStream capturedErr = new ByteArrayOutputStream();

  private int run(String... args) {
    PrintStream originalOut = System.out;
    PrintStream originalErr = System.err;
    System.setOut(new PrintStream(OutputStream.nullOutputStream(), true, StandardCharsets.UTF_8));
    System.setErr(new PrintStream(capturedErr, true, StandardCharsets.UTF_8));
    try {
      return new CommandLine(new ValidumCli()).execute(args);
    } finally {
      System.setOut(originalOut);
      System.setErr(originalErr);
    }
  }

  @AfterEach
  void clearErrors() {
    capturedErr.reset();
  }

  @Test
  @DisplayName("Казахстан по умолчанию: адрес в Алматы, паспорт иностранный")
  void nonResidentDefaultsToKazakhstan() throws Exception {
    Path out = tmp.resolve("kz.json");
    int code = run("generate", "individual", "--seed", "11", "--residency", "non-resident",
      "--age", "30", "--documents", "auto", "--format", "json-array", "-o", out.toString());

    assertThat(code).isZero();
    JsonNode row = MAPPER.readTree(out.toFile()).get(0);
    // Адрес — конкретными колонками: банк у нерезидента может остаться российским (bankCity).
    assertThat(row.path("registrationCity").asText()).as("регистрация").isEqualTo("Алматы");
    assertThat(row.path("registrationPostalCode").asText()).as("индекс").startsWith("05");
    assertThat(row.path("isResident").asText()).as("резидентство").isEqualTo("false");
    assertThat(row.path("citizenship").asText()).as("гражданство").isEqualTo("KZ");
    assertThat(row.path("passportType").asText()).as("тип паспорта").isEqualTo("FOREIGN");
    assertThat(row.path("passportCountry").asText()).as("страна паспорта").isEqualTo("KZ");
    assertThat(row.path("postCity").asText()).as("почтовый адрес").isEqualTo("Алматы");
    assertThat(row.path("currentCity").asText()).as("фактический адрес").isEqualTo("Алматы");
  }

  @Test
  @DisplayName("Беларусь: Минск и белорусский индекс")
  void belarusCity() throws Exception {
    Path out = tmp.resolve("by.json");
    int code = run("generate", "individual", "--seed", "12", "--residency", "non-resident",
      "--country", "BY", "--age", "40", "--format", "json-array", "-o", out.toString());

    assertThat(code).isZero();
    assertThat(Files.readString(out, StandardCharsets.UTF_8)).contains("Минск").contains("\"BY\"");
  }

  @Test
  @DisplayName("Неизвестная страна — ошибка с понятным текстом")
  void unknownCountry() {
    int code = run("generate", "individual", "--seed", "13", "--residency", "non-resident",
      "--country", "XX");

    assertThat(code).isEqualTo(2);
    assertThat(capturedErr.toString(StandardCharsets.UTF_8)).contains("Неизвестная страна");
  }
}
