package ru.validum.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.test.web.servlet.MvcResult;
import ru.validum.core.account.AccountValidator;

@SpringBootTest
@AutoConfigureMockMvc
class GenerationControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("POST /api/v1/generate/individual возвращает 200 и заполненный профиль")
  void generateIndividualReturnsFullProfile() throws Exception {
    mockMvc
      .perform(
        post("/api/v1/generate/individual")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.fullName").isNotEmpty())
      .andExpect(jsonPath("$.gender").isNotEmpty())
      .andExpect(jsonPath("$.inn").isNotEmpty())
      .andExpect(jsonPath("$.snils").isNotEmpty())
      .andExpect(jsonPath("$.passport.series").isNotEmpty())
      .andExpect(jsonPath("$.registrationAddress.city").isNotEmpty())
      .andExpect(jsonPath("$.postAddress.city").isNotEmpty())
      .andExpect(jsonPath("$.currentAddress.city").isNotEmpty())
      .andExpect(jsonPath("$.mobile.value").isNotEmpty())
      .andExpect(jsonPath("$.email.value").isNotEmpty())
      .andExpect(jsonPath("$.bank.bik").isNotEmpty())
      .andExpect(jsonPath("$.account").isNotEmpty());
  }

  @Test
  @DisplayName("Пол в ответе согласован с запросом")
  void genderMatchesRequest() throws Exception {
    mockMvc
      .perform(
        post("/api/v1/generate/individual")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"gender\":\"MALE\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.gender").value("MALE"));

    mockMvc
      .perform(
        post("/api/v1/generate/individual")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"gender\":\"FEMALE\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.gender").value("FEMALE"));
  }

  @Test
  @DisplayName("Одинаковый seed даёт идентичный профиль")
  void sameSeedProducesSameProfile() throws Exception {
    String body = "{\"seed\":42}";
    MvcResult first =
      mockMvc
        .perform(
          post("/api/v1/generate/individual")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andReturn();
    MvcResult second =
      mockMvc
        .perform(
          post("/api/v1/generate/individual")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andReturn();
    assertThat(first.getResponse().getContentAsString())
      .isEqualTo(second.getResponse().getContentAsString());
  }

  @Test
  @DisplayName("Счёт в ответе валиден по 809-П")
  void accountInResponseIsValid() throws Exception {
    MvcResult result =
      mockMvc
        .perform(
          post("/api/v1/generate/individual")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andReturn();
    String body = result.getResponse().getContentAsString();
    String account = body.replaceAll(".*\"account\":\"(\\d{20})\".*", "$1");
    String bik = body.replaceAll(".*\"bik\":\"(\\d{9})\".*", "$1");
    assertThat(account).hasSize(20);
    assertThat(AccountValidator.isValid(account, ru.validum.core.account.AccountType.CLAC, bik))
      .isTrue();
  }

  @Test
  @DisplayName("Неверный пол возвращает 400")
  void invalidGenderReturns400() throws Exception {
    mockMvc
      .perform(
        post("/api/v1/generate/individual")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"gender\":\"UNKNOWN\"}"))
      .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("POST /api/v1/generate/individual с terrorist=true возвращает флаг")
  void terroristFlagPropagates() throws Exception {
    mockMvc
      .perform(
        post("/api/v1/generate/individual")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"seed\":42,\"terrorist\":true,\"blocked\":true}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.terrorist").value(true))
      .andExpect(jsonPath("$.blocked").value(true));
  }

  // ── ИП ────────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("POST /api/v1/generate/ip — 200, ОГРНИП 15 цифр, отчество «ИП Автотеста»")
  void generateIpReturnsProfile() throws Exception {
    mockMvc
      .perform(
        post("/api/v1/generate/ip")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"seed\":42,\"age\":35}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.ogrnip").isNotEmpty())
      .andExpect(jsonPath("$.ogrnip").value(org.hamcrest.Matchers.matchesPattern("\\d{15}")))
      .andExpect(jsonPath("$.fullName").isNotEmpty())
      .andExpect(jsonPath("$.inn").isNotEmpty())
      .andExpect(jsonPath("$.terrorist").value(false))
      .andExpect(jsonPath("$.blocked").value(false));
  }

  // ── ЮЛ ────────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("POST /api/v1/generate/organization — 200, все поля заполнены")
  void generateOrganizationReturnsProfile() throws Exception {
    mockMvc
      .perform(
        post("/api/v1/generate/organization")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"seed\":42,\"organizationType\":\"LLC\",\"region\":\"MOSCOW\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.organizationName").isNotEmpty())
      .andExpect(jsonPath("$.organizationType").value("LLC"))
      .andExpect(jsonPath("$.ogrn").value(org.hamcrest.Matchers.matchesPattern("\\d{13}")))
      .andExpect(jsonPath("$.inn").value(org.hamcrest.Matchers.matchesPattern("\\d{10}")))
      .andExpect(jsonPath("$.kpp").value(org.hamcrest.Matchers.matchesPattern("77\\d{7}")))
      .andExpect(jsonPath("$.okpo").isNotEmpty())
      .andExpect(jsonPath("$.okopf").value("65"))
      .andExpect(jsonPath("$.registrationAddress.region").value("Москва"))
      .andExpect(jsonPath("$.registrationAddress.regionType").value("г"))
      .andExpect(jsonPath("$.registrationAddress.city").isNotEmpty())
      .andExpect(jsonPath("$.bank.bik").value(org.hamcrest.Matchers.matchesPattern("\\d{9}")))
      .andExpect(jsonPath("$.bank.name").isNotEmpty())
      .andExpect(jsonPath("$.account").value(org.hamcrest.Matchers.matchesPattern("\\d{20}")))
      .andExpect(jsonPath("$.terrorist").value(false))
      .andExpect(jsonPath("$.blocked").value(false));
  }

  @Test
  @DisplayName("POST /api/v1/generate/organization с явным БИК — банк тот же")
  void organizationWithExplicitBik() throws Exception {
    mockMvc
      .perform(
        post("/api/v1/generate/organization")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"seed\":42,\"bik\":\"044525225\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.bank.bik").value("044525225"))
      .andExpect(jsonPath("$.bank.name").isNotEmpty())
      .andExpect(jsonPath("$.account").value(org.hamcrest.Matchers.matchesPattern("\\d{20}")));
  }

  @Test
  @DisplayName("POST /api/v1/generate/organization с неизвестным БИК — 400")
  void organizationWithUnknownBik() throws Exception {
    mockMvc
      .perform(
        post("/api/v1/generate/organization")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"seed\":42,\"bik\":\"999999999\"}"))
      .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("POST /api/v1/generate/organization с region=SPB — КПП на 78")
  void organizationSpbHasCorrectKpp() throws Exception {
    mockMvc
      .perform(
        post("/api/v1/generate/organization")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"seed\":42,\"region\":\"SPB\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.kpp").value(org.hamcrest.Matchers.matchesPattern("78\\d{7}")))
      .andExpect(jsonPath("$.registrationAddress.region").value("Санкт-Петербург"))
      .andExpect(jsonPath("$.registrationAddress.regionType").value("г"))
      .andExpect(jsonPath("$.registrationAddress.city").isNotEmpty());
  }

  @Test
  @DisplayName("Регион из полного набора (TATARSTAN) принимается — КПП на 16")
  void organizationTatarstanHasCorrectKpp() throws Exception {
    mockMvc
      .perform(
        post("/api/v1/generate/organization")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"seed\":42,\"region\":\"TATARSTAN\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.kpp").value(org.hamcrest.Matchers.matchesPattern("16\\d{7}")));
  }

  @Test
  @DisplayName("Регион можно задать кодом субъекта — «16» равен TATARSTAN")
  void organizationRegionByCode() throws Exception {
    mockMvc
      .perform(
        post("/api/v1/generate/organization")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"seed\":42,\"region\":\"16\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.kpp").value(org.hamcrest.Matchers.matchesPattern("16\\d{7}")));
  }

  @Test
  @DisplayName("Неизвестный регион отклоняется с 400")
  void organizationUnknownRegionRejected() throws Exception {
    mockMvc
      .perform(
        post("/api/v1/generate/organization")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"seed\":42,\"region\":\"KARELIA\"}"))
      .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Одиночный профиль ФЛ принимает ранее закрытый регион (TATARSTAN)")
  void individualAcceptsFullRegionSet() throws Exception {
    mockMvc
      .perform(
        post("/api/v1/generate/individual")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"seed\":42,\"region\":\"TATARSTAN\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.inn").isNotEmpty());
  }

  @Test
  @DisplayName("Профиль ИП принимает ранее закрытый регион (SARATOV)")
  void ipAcceptsFullRegionSet() throws Exception {
    mockMvc
      .perform(
        post("/api/v1/generate/ip")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"seed\":42,\"region\":\"SARATOV\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.inn").isNotEmpty());
  }

  @Test
  @DisplayName("POST /generate/individual с asOf: профиль воспроизводим между запросами")
  void individualAsOfIsDeterministic() throws Exception {
    String body = "{\"seed\":42,\"asOf\":\"2026-01-01\"}";

    String first =
      mockMvc
        .perform(post("/api/v1/generate/individual").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.asOf").value("2026-01-01"))
        .andReturn()
        .getResponse()
        .getContentAsString();

    String second =
      mockMvc
        .perform(post("/api/v1/generate/individual").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    assertThat(first).isEqualTo(second);
  }
}
