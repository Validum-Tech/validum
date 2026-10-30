package ru.validum.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.Charset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.validum.api.service.ExportService;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("ExportController — /api/v1/export (Неделя 4)")
class ExportControllerTest {

  private static final Charset CP1251 = Charset.forName("windows-1251");

  @Autowired private MockMvc mvc;
  @Autowired private ExportService service;

  private static String body(MvcResult result) {
    return new String(result.getResponse().getContentAsByteArray(), CP1251);
  }

  @Test
  @DisplayName("ED807 с лимитом: 200, XML и ровно N участников")
  void ed807WithLimit() throws Exception {
    MvcResult result = mvc.perform(get("/api/v1/export/ed807").param("limit", "3"))
      .andExpect(status().isOk())
      .andReturn();

    String xml = body(result);
    assertThat(result.getResponse().getContentType()).contains("xml");
    assertThat(xml).startsWith("<?xml").contains("<ED807").contains("InfoTypeCode=\"SIRR\"");
    assertThat(count(xml, "<BICDirectoryEntry")).isEqualTo(3);
  }

  @Test
  @DisplayName("ED807 с фильтром по PtType: в файле только этот тип")
  void ed807WithTypeFilter() throws Exception {
    MvcResult result = mvc.perform(get("/api/v1/export/ed807")
        .param("type", "20")
        .param("limit", "2"))
      .andExpect(status().isOk())
      .andReturn();

    String xml = body(result);
    assertThat(count(xml, "<BICDirectoryEntry")).isEqualTo(2);
    assertThat(count(xml, "PtType=\"20\"")).isEqualTo(2);
  }

  @Test
  @DisplayName("ED807: неизвестный PtType → 400 bad_request")
  void ed807BadType() throws Exception {
    mvc.perform(get("/api/v1/export/ed807").param("type", "ZZ"))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.error").value("bad_request"));
  }

  @Test
  @DisplayName("ED808: 200, FIRR и ParticipantProfile")
  void ed808Ok() throws Exception {
    String bik = service.participants().get(0).bik();

    MvcResult result = mvc.perform(get("/api/v1/export/ed808").param("bik", bik))
      .andExpect(status().isOk())
      .andReturn();

    String xml = body(result);
    assertThat(xml).startsWith("<?xml")
      .contains("<ED808")
      .contains("InfoTypeCode=\"FIRR\"")
      .contains("ParticipantProfileInfo");
  }

  @Test
  @DisplayName("ED808: неизвестный БИК → 404 unknown_participant")
  void ed808Unknown() throws Exception {
    mvc.perform(get("/api/v1/export/ed808").param("bik", "000000000"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.error").value("unknown_participant"));
  }

  private static int count(String text, String needle) {
    int n = 0;
    int from = 0;
    while ((from = text.indexOf(needle, from)) >= 0) {
      n++;
      from += needle.length();
    }
    return n;
  }
}
