package ru.validum.api.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("ScenarioController — /api/v1/scenarios")
class ScenarioControllerTest {

  @Autowired private MockMvc mvc;

  @Test
  @DisplayName("GET /api/v1/scenarios — 9 slug'ов, порядок стабилен")
  void listScenarios() throws Exception {
    mvc.perform(get("/api/v1/scenarios"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$", hasSize(9)))
      .andExpect(jsonPath("$[0]", is("adult-resident-with-account")))
      .andExpect(jsonPath("$[4]", is("salary-client")))
      .andExpect(jsonPath("$[8]", is("minor")));
  }

  @Test
  @DisplayName("POST /scenarios/pensioner-male?seed=42 — 200 + профиль")
  void pensionerMale() throws Exception {
    mvc.perform(post("/api/v1/scenarios/pensioner-male").param("seed", "42"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.fullName").isNotEmpty())
      .andExpect(jsonPath("$.categoryClient", is("ПЕНСИОНЕР")))
      .andExpect(jsonPath("$.account").isNotEmpty())
      .andExpect(jsonPath("$.terrorist", is(false)))
      .andExpect(jsonPath("$.blocked", is(false)));
  }

  @Test
  @DisplayName("POST /scenarios/salary-client — категория ЗАРПЛАТНИК, пакет ЗАРПЛАТНЫЙ")
  void salaryClient() throws Exception {
    mvc.perform(post("/api/v1/scenarios/salary-client").param("seed", "1"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.categoryClient", is("ЗАРПЛАТНИК")))
      .andExpect(jsonPath("$.accountPackage", is("ЗАРПЛАТНЫЙ")));
  }

  @Test
  @DisplayName("POST /scenarios/serviceman — паспорт РФ + военный билет в documents")
  void serviceman() throws Exception {
    mvc.perform(post("/api/v1/scenarios/serviceman").param("seed", "42"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.documents", hasSize(2)))
      .andExpect(jsonPath("$.documents[0].spdulCode", is("21")))
      .andExpect(jsonPath("$.documents[1].spdulCode", is("07")));
  }

  @Test
  @DisplayName("POST /scenarios/minor — 16 лет, паспорт РФ")
  void minor() throws Exception {
    mvc.perform(post("/api/v1/scenarios/minor").param("seed", "42"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.age", is(16)))
      .andExpect(jsonPath("$.documents[0].spdulCode", is("21")));
  }

  @Test
  @DisplayName("POST /scenarios/unknown — 404 + error")
  void unknownScenario() throws Exception {
    mvc.perform(post("/api/v1/scenarios/unknown-thing"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.error", is("unknown_scenario")));
  }

  @Test
  @DisplayName("без seed — работает (случайный профиль)")
  void withoutSeed() throws Exception {
    mvc.perform(post("/api/v1/scenarios/adult-resident-with-account"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.fullName").isNotEmpty());
  }
}
