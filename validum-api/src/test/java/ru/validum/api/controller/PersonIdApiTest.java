package ru.validum.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.validum.core.common.PersonIds;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("personId в REST: тот же seed → тот же идентификатор, что и в core")
class PersonIdApiTest {

  @Autowired private MockMvc mockMvc;

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private String personId(long seed) throws Exception {
    MvcResult result = mockMvc
      .perform(
        post("/api/v1/generate/individual")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"seed\":" + seed + "}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.personId").exists())
      .andReturn();
    JsonNode node = MAPPER.readTree(result.getResponse().getContentAsString());
    return node.get("personId").asText();
  }

  @Test
  @DisplayName("personId детерминирован seed и совпадает с core")
  void deterministicAndConsistentWithCore() throws Exception {
    assertThat(personId(42)).isEqualTo(PersonIds.of(42));
    assertThat(personId(42)).isEqualTo(personId(42));
    assertThat(personId(43)).isNotEqualTo(personId(42));
  }
}
