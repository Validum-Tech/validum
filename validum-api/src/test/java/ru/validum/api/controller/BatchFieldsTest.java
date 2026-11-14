package ru.validum.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Батч CSV: ?fields= оставляет выбранные колонки")
class BatchFieldsTest {

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("?fields=inn,fullName — в заголовке только две колонки")
  void explicitFields() throws Exception {
    String body = batch("?format=csv&fields=inn,fullName");
    String header = headerOf(body);
    assertThat(header.split(",", -1)).containsExactly("fullName", "inn");
  }

  @Test
  @DisplayName("?fields=short — узкий «человеческий» набор, служебных полей нет")
  void shortPreset() throws Exception {
    String body = batch("?format=csv&fields=short");
    String header = headerOf(body);
    assertThat(header).contains("fullName").contains("inn").contains("age");
    assertThat(header).doesNotContain("secretWord").doesNotContain("terrorist");
  }

  @Test
  @DisplayName("Без ?fields= выдача полная — регрессия не затронута")
  void withoutFields() throws Exception {
    String header = headerOf(batch("?format=csv"));
    assertThat(header).contains("secretWord").contains("terrorist").contains("asOf");
  }

  @Test
  @DisplayName("?fields= с json отбивается 400 — молча игнорировать нельзя")
  void fieldsRequireCsv() throws Exception {
    MvcResult result = mockMvc
      .perform(
        post("/api/v1/generate/individual/batch?format=json-array&fields=short")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"count\":2,\"seed\":42}"))
      .andExpect(status().isBadRequest())
      .andReturn();
    String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    assertThat(body).contains("invalid_argument").contains("format=csv");
  }

  /** REST-CSV отдаётся с BOM (как и файловый) — для сверки заголовка его снимаем. */
  private static String headerOf(String body) {
    return body.lines().findFirst().orElse("").replace("\uFEFF", "");
  }

  private String batch(String query) throws Exception {
    MvcResult result = mockMvc
      .perform(
        post("/api/v1/generate/individual/batch" + query)
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"count\":2,\"seed\":42}"))
      .andExpect(status().isOk())
      .andReturn();
    return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
  }
}
