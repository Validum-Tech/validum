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
@DisplayName("GenerationController — .bik (P3.3)")
class GenerationControllerWithBikTest {

  private static final String SBER_BIK = "044525225";
  private static final String VTB_BIK = "044525187";
  private static final String UNKNOWN_BIK = "000000000";

  @Autowired private MockMvc mvc;

  // ── Individual ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("POST /generate/individual с bik=SBER_BIK → банк Сбербанк")
  void individualWithBik() throws Exception {
    mvc.perform(post("/api/v1/generate/individual")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"seed\":42,\"bik\":\"" + SBER_BIK + "\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.bank.bik").value(SBER_BIK))
      .andExpect(jsonPath("$.bank.name").value("ПАО Сбербанк"));
  }

  @Test
  @DisplayName("POST /generate/individual с bik=UNKNOWN_BIK → 400 + invalid_argument")
  void individualWithUnknownBikReturns400() throws Exception {
    mvc.perform(post("/api/v1/generate/individual")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"seed\":42,\"bik\":\"" + UNKNOWN_BIK + "\"}"))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.error").value("invalid_argument"))
      .andExpect(jsonPath("$.message").exists());
  }

  @Test
  @DisplayName("POST /generate/individual с bik неверной длины → 400 (Bean Validation)")
  void individualWithWrongLengthBik() throws Exception {
    mvc.perform(post("/api/v1/generate/individual")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"seed\":42,\"bik\":\"12345\"}"))
      .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("POST /generate/individual без bik → случайный банк (backward compat)")
  void individualWithoutBik() throws Exception {
    mvc.perform(post("/api/v1/generate/individual")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"seed\":42}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.bank.bik").isNotEmpty());
  }

  // ── IP ────────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("POST /generate/ip с bik=VTB_BIK → банк ВТБ")
  void ipWithBik() throws Exception {
    mvc.perform(post("/api/v1/generate/ip")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"seed\":42,\"bik\":\"" + VTB_BIK + "\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.bank.bik").value(VTB_BIK))
      .andExpect(jsonPath("$.bank.name").value("Банк ВТБ (ПАО)"));
  }

  @Test
  @DisplayName("POST /generate/ip с bik=UNKNOWN_BIK → 400")
  void ipWithUnknownBikReturns400() throws Exception {
    mvc.perform(post("/api/v1/generate/ip")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"seed\":42,\"bik\":\"" + UNKNOWN_BIK + "\"}"))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.error").value("invalid_argument"));
  }

  @Test
  @DisplayName("POST /generate/ip без bik → случайный банк (backward compat)")
  void ipWithoutBik() throws Exception {
    mvc.perform(post("/api/v1/generate/ip")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"seed\":42}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.bank.bik").isNotEmpty());
  }

  // ── Batch ─────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Individual batch с bik → все профили с этим банком")
  void individualBatchWithBik() throws Exception {
    MvcResult result = mvc.perform(post("/api/v1/generate/individual/batch")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"count\":10,\"seed\":42,\"bik\":\"" + SBER_BIK + "\"}"))
      .andExpect(status().isOk())
      .andReturn();

    String json = result.getResponse().getContentAsString();
    List<String> biks = JsonPath.read(json, "$.profiles[*].bank.bik");
    assertThat(biks).hasSize(10).allMatch(SBER_BIK::equals);
  }

  @Test
  @DisplayName("Individual batch с unknown bik → 400")
  void individualBatchWithUnknownBikReturns400() throws Exception {
    mvc.perform(post("/api/v1/generate/individual/batch")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"count\":5,\"seed\":42,\"bik\":\"" + UNKNOWN_BIK + "\"}"))
      .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("IP batch с bik → все профили с этим банком")
  void ipBatchWithBik() throws Exception {
    MvcResult result = mvc.perform(post("/api/v1/generate/ip/batch")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"count\":5,\"seed\":42,\"bik\":\"" + SBER_BIK + "\"}"))
      .andExpect(status().isOk())
      .andReturn();

    String json = result.getResponse().getContentAsString();
    List<String> biks = JsonPath.read(json, "$.profiles[*].bank.bik");
    assertThat(biks).hasSize(5).allMatch(SBER_BIK::equals);
  }

  @Test
  @DisplayName("Batch без bik → у профилей разные банки (не прибиты гвоздями)")
  void batchWithoutBikGivesVariedBanks() throws Exception {
    MvcResult result = mvc.perform(post("/api/v1/generate/individual/batch")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"count\":30,\"seed\":42}"))
      .andExpect(status().isOk())
      .andReturn();

    String json = result.getResponse().getContentAsString();
    List<String> biks = JsonPath.read(json, "$.profiles[*].bank.bik");
    // 30 профилей → должно быть > 1 уникального банка
    assertThat(biks.stream().distinct().count()).isGreaterThan(1);
  }
}
