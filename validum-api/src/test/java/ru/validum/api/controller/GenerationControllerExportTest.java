package ru.validum.api.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("GenerationController — Экспорт (P1.4)")
class GenerationControllerExportTest {

  @Autowired private MockMvc mvc;

  private static final String BATCH_BODY = "{\"count\":3,\"seed\":42}";

  @Test
  @DisplayName("ФЛ: format=json-array → плоский массив без обёртки")
  void individualBatchJsonArray() throws Exception {
    mvc.perform(post("/api/v1/generate/individual/batch?format=json-array")
        .contentType(MediaType.APPLICATION_JSON)
        .content(BATCH_BODY))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$").isArray())
      .andExpect(jsonPath("$.length()").value(3))
      .andExpect(jsonPath("$[0].fullName").isNotEmpty());
  }

  @Test
  @DisplayName("ФЛ: format=csv → Content-Type, Content-Disposition, BOM")
  void individualBatchCsv() throws Exception {
    mvc.perform(post("/api/v1/generate/individual/batch?format=csv")
        .contentType(MediaType.APPLICATION_JSON)
        .content(BATCH_BODY))
      .andExpect(status().isOk())
      .andExpect(content().contentType(new MediaType("text", "csv", StandardCharsets.UTF_8)))
      .andExpect(header().string("Content-Disposition", containsString("attachment; filename=\"validum-individual-")))
      .andExpect(content().string(startsWith("\uFEFF"))) // BOM
      .andExpect(content().string(containsString("fullName,gender,birthDate")));
  }

  @Test
  @DisplayName("ИП: format=csv → корректный ответ")
  void ipBatchCsv() throws Exception {
    mvc.perform(post("/api/v1/generate/ip/batch?format=csv")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"count\":2,\"seed\":42}"))
      .andExpect(status().isOk())
      .andExpect(content().contentType(new MediaType("text", "csv", StandardCharsets.UTF_8)))
      .andExpect(content().string(startsWith("\uFEFF")))
      .andExpect(content().string(containsString("ogrnip,personId,fullName")));
  }

  @Test
  @DisplayName("ЮЛ: format=csv → корректный ответ")
  void organizationBatchCsv() throws Exception {
    mvc.perform(post("/api/v1/generate/organization/batch?format=csv")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"count\":2,\"seed\":42}"))
      .andExpect(status().isOk())
      .andExpect(content().contentType(new MediaType("text", "csv", StandardCharsets.UTF_8)))
      .andExpect(content().string(startsWith("\uFEFF")))
      .andExpect(content().string(containsString("organizationName,organizationType,ogrn")));
  }

  @Test
  @DisplayName("ФЛ: неизвестный format → fallback на json (обёртка)")
  void individualBatchUnknownFormatFallsBackToJson() throws Exception {
    mvc.perform(post("/api/v1/generate/individual/batch?format=unknown")
        .contentType(MediaType.APPLICATION_JSON)
        .content(BATCH_BODY))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.count").value(3))
      .andExpect(jsonPath("$.profiles").isArray());
  }
}
