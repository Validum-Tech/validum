package ru.validum.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.validum.core.Version;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("GenerationController — заголовки версии и seed (W47-A1)")
class GenerationMetaHeadersTest {

  private static final String VERSION_HEADER = "X-Validum-Version";
  private static final String SEED_HEADER = "X-Validum-Seed";

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("Батч ФЛ отдаёт версию генератора и фактический seed")
  void batchReportsVersionAndSeed() throws Exception {
    mockMvc
      .perform(post("/api/v1/generate/individual/batch")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"count\":3,\"seed\":7}"))
      .andExpect(status().isOk())
      .andExpect(header().string(SEED_HEADER, "7"))
      .andExpect(header().exists(VERSION_HEADER));
  }

  @Test
  @DisplayName("Без seed в запросе заголовок говорит auto")
  void batchWithoutSeedReportsAuto() throws Exception {
    mockMvc
      .perform(post("/api/v1/generate/individual/batch")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"count\":2}"))
      .andExpect(status().isOk())
      .andExpect(header().string(SEED_HEADER, "auto"));
  }

  @Test
  @DisplayName("CSV для Excel тоже отдаёт мету и русские заголовки")
  void excelCsvReportsMeta() throws Exception {
    MvcResult result = mockMvc
      .perform(post("/api/v1/generate/individual/batch")
        .param("format", "csv-semicolon")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"count\":2,\"seed\":9}"))
      .andExpect(status().isOk())
      .andExpect(header().string(SEED_HEADER, "9"))
      .andReturn();

    assertThat(result.getResponse().getContentAsString()).contains("ФИО;Пол");
  }

  @Test
  @DisplayName("Одиночный ФЛ отдаёт ту же версию, что и батч")
  void singleIndividualReportsVersion() throws Exception {
    MvcResult result = mockMvc
      .perform(post("/api/v1/generate/individual")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"seed\":42}"))
      .andExpect(status().isOk())
      .andExpect(header().string(SEED_HEADER, "42"))
      .andReturn();

    assertThat(result.getResponse().getHeader(VERSION_HEADER)).isEqualTo(Version.version());
  }

  @Test
  @DisplayName("Одиночный ЮЛ без seed отдаёт auto")
  void singleOrganizationWithoutSeed() throws Exception {
    mockMvc
      .perform(post("/api/v1/generate/organization")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"type\":\"SE\"}"))
      .andExpect(status().isOk())
      .andExpect(header().string(SEED_HEADER, "auto"));
  }
}
