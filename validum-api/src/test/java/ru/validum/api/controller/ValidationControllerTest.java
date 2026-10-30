package ru.validum.api.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
@DisplayName("ValidationController — /api/v1/validate (P1.3 + P2.3)")
class ValidationControllerTest {

  @Autowired private MockMvc mvc;

  // ── ИНН ───────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Валидный ИНН ФЛ → true, type=INDIVIDUAL")
  void validIndividualInn() throws Exception {
    mvc.perform(post("/api/v1/validate/inn")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"inn\":\"500100732259\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.valid", is(true)))
      .andExpect(jsonPath("$.type", is("INDIVIDUAL")));
  }

  @Test
  @DisplayName("Валидный ИНН ЮЛ → true, type=LEGAL_ENTITY")
  void validLegalEntityInn() throws Exception {
    mvc.perform(post("/api/v1/validate/inn")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"inn\":\"7707083893\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.valid", is(true)))
      .andExpect(jsonPath("$.type", is("LEGAL_ENTITY")));
  }

  @Test
  @DisplayName("Битый ИНН → false")
  void invalidInn() throws Exception {
    mvc.perform(post("/api/v1/validate/inn")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"inn\":\"500100732258\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.valid", is(false)));
  }

  // ── СНИЛС ─────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Валидный СНИЛС → true")
  void validSnils() throws Exception {
    mvc.perform(post("/api/v1/validate/snils")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"snils\":\"11223344595\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.valid", is(true)));
  }

  @Test
  @DisplayName("Битый СНИЛС → false")
  void invalidSnils() throws Exception {
    mvc.perform(post("/api/v1/validate/snils")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"snils\":\"11223344594\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.valid", is(false)));
  }

  // ── ОГРН ──────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Валидный ОГРН ЮЛ → true, type=LEGAL_ENTITY")
  void validOgrnLegalEntity() throws Exception {
    mvc.perform(post("/api/v1/validate/ogrn")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"ogrn\":\"1027700132195\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.valid", is(true)))
      .andExpect(jsonPath("$.type", is("LEGAL_ENTITY")));
  }

  @Test
  @DisplayName("Валидный ОГРНИП → true, type=INDIVIDUAL_ENTREPRENEUR")
  void validOgrnip() throws Exception {
    mvc.perform(post("/api/v1/validate/ogrn")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"ogrn\":\"304500116000157\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.valid", is(true)))
      .andExpect(jsonPath("$.type", is("INDIVIDUAL_ENTREPRENEUR")));
  }

  @Test
  @DisplayName("Битый ОГРН → false")
  void invalidOgrn() throws Exception {
    mvc.perform(post("/api/v1/validate/ogrn")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"ogrn\":\"1027700132194\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.valid", is(false)));
  }

  // ── ОГРНИП (P2.3, отдельный эндпоинт) ────────────────────────────────────

  @Test
  @DisplayName("P2.3: /ogrnip — валидный ОГРНИП → true, type=INDIVIDUAL_ENTREPRENEUR")
  void validOgrnipEndpoint() throws Exception {
    mvc.perform(post("/api/v1/validate/ogrnip")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"ogrnip\":\"304500116000157\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.valid", is(true)))
      .andExpect(jsonPath("$.type", is("INDIVIDUAL_ENTREPRENEUR")));
  }

  @Test
  @DisplayName("P2.3: /ogrnip — ОГРН ЮЛ (13 цифр) не проходит как ОГРНИП → false")
  void ogrnFailsAsOgrnip() throws Exception {
    mvc.perform(post("/api/v1/validate/ogrnip")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"ogrnip\":\"1027700132195\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.valid", is(false)));
  }

  @Test
  @DisplayName("P2.3: /ogrnip — битый ОГРНИП → false")
  void invalidOgrnipEndpoint() throws Exception {
    mvc.perform(post("/api/v1/validate/ogrnip")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"ogrnip\":\"304500116000158\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.valid", is(false)));
  }

  // ── Счёт (809-П) ──────────────────────────────────────────────────────────

  @Test
  @DisplayName("Валидный корреспондентский счёт (ED807) → true")
  void validAccount() throws Exception {
    mvc.perform(post("/api/v1/validate/account")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"account\":\"30103810345374525078\",\"bik\":\"044525000\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.valid", is(true)))
      .andExpect(jsonPath("$.type", is("RUB")));
  }

  @Test
  @DisplayName("Битый контрольный ключ → false")
  void invalidAccount() throws Exception {
    mvc.perform(post("/api/v1/validate/account")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"account\":\"30103810445374525078\",\"bik\":\"044525000\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.valid", is(false)));
  }

  @Test
  @DisplayName("Без БИК ключ не проверить → false")
  void accountWithoutBikIsNotValidated() throws Exception {
    mvc.perform(post("/api/v1/validate/account")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"account\":\"30103810345374525078\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.valid", is(false)));
  }

  // ── КПП ───────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Валидный КПП → true")
  void validKpp() throws Exception {
    mvc.perform(post("/api/v1/validate/kpp")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"kpp\":\"773601001\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.valid", is(true)));
  }

  @Test
  @DisplayName("Битый КПП (неверный код региона) → false")
  void invalidKpp() throws Exception {
    mvc.perform(post("/api/v1/validate/kpp")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"kpp\":\"003601001\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.valid", is(false)));
  }

  @Test
  @DisplayName("P2.3: КПП с кодом ИФНС 00 → false (расширенная проверка)")
  void kppWithZeroInspectionFails() throws Exception {
    mvc.perform(post("/api/v1/validate/kpp")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"kpp\":\"770001001\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.valid", is(false)));
  }

  @Test
  @DisplayName("P2.3: КПП с причиной 00 → false")
  void kppWithZeroReasonFails() throws Exception {
    mvc.perform(post("/api/v1/validate/kpp")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"kpp\":\"773600001\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.valid", is(false)));
  }

  // ── ОКПО (P2.3) ──────────────────────────────────────────────────────────

  @Test
  @DisplayName("P2.3: /okpo — валидный ОКПО ЮЛ (8 цифр) → true, type=LEGAL_ENTITY")
  void validOkpoLegalEntity() throws Exception {
    // Сгенерируем валидный через OkpoGenerator с фиксированным seed для детерминизма
    var gen = new ru.validum.core.organization.OkpoGenerator(
      ru.validum.core.random.DataGenerator.seededRandom(42L));
    String okpo = gen.generateLegalEntity();

    mvc.perform(post("/api/v1/validate/okpo")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"okpo\":\"" + okpo + "\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.valid", is(true)))
      .andExpect(jsonPath("$.type", is("LEGAL_ENTITY")));
  }

  @Test
  @DisplayName("P2.3: /okpo — валидный ОКПО ИП (10 цифр) → true, type=INDIVIDUAL_ENTREPRENEUR")
  void validOkpoIp() throws Exception {
    var gen = new ru.validum.core.organization.OkpoGenerator(
      ru.validum.core.random.DataGenerator.seededRandom(42L));
    String okpo = gen.generateIndividualEntrepreneur();

    mvc.perform(post("/api/v1/validate/okpo")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"okpo\":\"" + okpo + "\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.valid", is(true)))
      .andExpect(jsonPath("$.type", is("INDIVIDUAL_ENTREPRENEUR")));
  }

  @Test
  @DisplayName("P2.3: /okpo — битый ОКПО → false")
  void invalidOkpo() throws Exception {
    // 12345678 — последняя 8 не сходится по контрольной (1+2+3+4+5+6+7=28, 28%11=6, значит валидный был бы 12345676)
    mvc.perform(post("/api/v1/validate/okpo")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"okpo\":\"12345678\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.valid", is(false)));
  }

  @Test
  @DisplayName("P2.3: /okpo — неверная длина (9 цифр) → false")
  void okpoWrongLength() throws Exception {
    mvc.perform(post("/api/v1/validate/okpo")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"okpo\":\"123456789\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.valid", is(false)));
  }

  // ── БИК ───────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Валидный БИК Сбербанка → true, возвращает имя банка")
  void validBik() throws Exception {
    mvc.perform(post("/api/v1/validate/bik")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"bik\":\"044525225\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.valid", is(true)))
      .andExpect(jsonPath("$.type", is("BANK")))
      .andExpect(jsonPath("$.details", is("ПАО Сбербанк")));
  }

  @Test
  @DisplayName("Несуществующий БИК → false")
  void invalidBik() throws Exception {
    mvc.perform(post("/api/v1/validate/bik")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"bik\":\"000000000\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.valid", is(false)));
  }
}
