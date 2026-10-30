package ru.validum.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
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
@DisplayName("GenerationController — Batch (P1.2)")
class GenerationControllerBatchTest {

  @Autowired private MockMvc mockMvc;

  // ── ФЛ ────────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("POST /api/v1/generate/individual/batch — 100 профилей ФЛ")
  void individualBatchReturns100Profiles() throws Exception {
    mockMvc
      .perform(
        post("/api/v1/generate/individual/batch")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"count\":100,\"seed\":42,\"region\":\"MOSCOW\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.count").value(100))
      .andExpect(jsonPath("$.seed").value(42))
      .andExpect(jsonPath("$.profiles.length()").value(100))
      .andExpect(jsonPath("$.profiles[0].fullName").isNotEmpty())
      .andExpect(jsonPath("$.profiles[99].fullName").isNotEmpty());
  }

  @Test
  @DisplayName("Батч ФЛ детерминирован: одинаковый seed + count → идентичный ответ")
  void individualBatchIsDeterministic() throws Exception {
    String body = "{\"count\":10,\"seed\":42}";

    MvcResult first = mockMvc
      .perform(post("/api/v1/generate/individual/batch")
        .contentType(MediaType.APPLICATION_JSON).content(body))
      .andReturn();

    MvcResult second = mockMvc
      .perform(post("/api/v1/generate/individual/batch")
        .contentType(MediaType.APPLICATION_JSON).content(body))
      .andReturn();

    assertThat(first.getResponse().getContentAsString())
      .isEqualTo(second.getResponse().getContentAsString());
  }

  @Test
  @DisplayName("Все профили внутри батча уникальны (seed + i)")
  void individualBatchProfilesAreUnique() throws Exception {
    MvcResult result = mockMvc
      .perform(post("/api/v1/generate/individual/batch")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"count\":50,\"seed\":42}"))
      .andReturn();

    String json = result.getResponse().getContentAsString();
    List<String> inns = JsonPath.read(json, "$.profiles[*].inn");

    // Так как используется seed+1, все 50 ИНН должны быть разными
    assertThat(inns).hasSize(50).doesNotHaveDuplicates();
  }

  @Test
  @DisplayName("count=0 → 400 Bad Request")
  void individualBatchFailsOnZeroCount() throws Exception {
    mockMvc
      .perform(post("/api/v1/generate/individual/batch")
        .contentType(MediaType.APPLICATION_JSON).content("{\"count\":0}"))
      .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("count=10001 → 400 Bad Request")
  void individualBatchFailsOnTooLargeCount() throws Exception {
    mockMvc
      .perform(post("/api/v1/generate/individual/batch")
        .contentType(MediaType.APPLICATION_JSON).content("{\"count\":10001}"))
      .andExpect(status().isBadRequest());
  }

  // ── ИП ────────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("POST /api/v1/generate/ip/batch — 20 профилей ИП")
  void ipBatchReturns20Profiles() throws Exception {
    mockMvc
      .perform(
        post("/api/v1/generate/ip/batch")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"count\":20,\"seed\":100,\"age\":35}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.count").value(20))
      .andExpect(jsonPath("$.seed").value(100))
      .andExpect(jsonPath("$.profiles.length()").value(20))
      // ОГРНИП у всех должен быть 15 символов
      .andExpect(jsonPath("$.profiles[*].ogrnip", org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.matchesPattern("\\d{15}"))));
  }

  // ── ЮЛ ────────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("POST /api/v1/generate/organization/batch — 10 профилей ЮЛ")
  void organizationBatchReturns10Profiles() throws Exception {
    mockMvc
      .perform(
        post("/api/v1/generate/organization/batch")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"count\":10,\"seed\":500,\"organizationType\":\"LLC\",\"region\":\"SPB\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.count").value(10))
      .andExpect(jsonPath("$.seed").value(500))
      .andExpect(jsonPath("$.profiles.length()").value(10))
      // КПП должен начинаться с кода СПб (78)
      .andExpect(jsonPath("$.profiles[0].kpp").value(org.hamcrest.Matchers.matchesPattern("78\\d{7}")))
      .andExpect(jsonPath("$.profiles[9].kpp").value(org.hamcrest.Matchers.matchesPattern("78\\d{7}")));
  }
}
