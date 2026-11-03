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
@DisplayName("Батч в CSV для Excel: «;» и русские заголовки")
class BatchCsvForExcelTest {

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("?format=csv-semicolon отдаёт «;», русские заголовки и BOM")
  void semicolonBatch() throws Exception {
    MvcResult result = mockMvc
      .perform(
        post("/api/v1/generate/individual/batch?format=csv-semicolon")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"count\":2,\"seed\":42}"))
      .andExpect(status().isOk())
      .andReturn();

    String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    assertThat(body).startsWith("\uFEFF");
    assertThat(body).contains("ФИО;Пол;Дата рождения");
    assertThat(body).contains("ИНН;СНИЛС");
    assertThat(result.getResponse().getHeader("Content-Disposition")).contains(".csv");
  }

  @Test
  @DisplayName("?format=csv остаётся машинным: «,» и английские заголовки")
  void machineBatch() throws Exception {
    MvcResult result = mockMvc
      .perform(
        post("/api/v1/generate/ip/batch?format=csv")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"count\":1,\"seed\":7}"))
      .andExpect(status().isOk())
      .andReturn();

    String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    assertThat(body).contains("fullName,gender,birthDate");
    assertThat(body).doesNotContain("ФИО");
  }
}
