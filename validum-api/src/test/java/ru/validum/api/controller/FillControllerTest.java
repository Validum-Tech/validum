package ru.validum.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("POST /api/v1/fill/individual — дозаполнение CSV (P4.1)")
class FillControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("Пустая ячейка заполняется, непустая сохраняется")
  void fillsEmptyKeepsFilled() throws Exception {
    mockMvc
      .perform(
        post("/api/v1/fill/individual")
          .contentType(MediaType.APPLICATION_JSON)
          .content(
            "{\"seed\":42,\"rows\":[{\"fullName\":\"Иванов Иван Иванович\",\"inn\":\"\"}]}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.count").value(1))
      .andExpect(jsonPath("$.rows[0].fullName").value("Иванов Иван Иванович"))
      .andExpect(jsonPath("$.rows[0].inn").isNotEmpty())
      .andExpect(jsonPath("$.rows[0].snils").isNotEmpty());
  }

  @Test
  @DisplayName("Заполненный ИНН не перезаписывается")
  void keepsFilledInn() throws Exception {
    mockMvc
      .perform(
        post("/api/v1/fill/individual")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"rows\":[{\"fullName\":\"Петров Пётр Петрович\",\"inn\":\"7707083893\"}]}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.rows[0].inn").value("7707083893"));
  }

  @Test
  @DisplayName("format=csv отдаёт CSV-файл")
  void csvFormat() throws Exception {
    mockMvc
      .perform(
        post("/api/v1/fill/individual")
          .param("format", "csv")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"rows\":[{\"fullName\":\"Сидоров Сидор Сидорович\"}]}"))
      .andExpect(status().isOk())
      .andExpect(content().contentTypeCompatibleWith("text/csv"))
      .andExpect(content().string(containsString("fullName")))
      .andExpect(content().string(containsString("Сидоров Сидор Сидорович")));
  }

  @Test
  @DisplayName("Пустой список строк — 400")
  void emptyRowsRejected() throws Exception {
    mockMvc
      .perform(
        post("/api/v1/fill/individual")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"rows\":[]}"))
      .andExpect(status().isBadRequest());
  }
}
