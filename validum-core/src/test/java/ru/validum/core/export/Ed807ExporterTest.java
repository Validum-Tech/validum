package ru.validum.core.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import ru.validum.core.bank.Participant;
import ru.validum.core.bank.ParticipantType;

@DisplayName("Экспорт ED807 (справочник БИК)")
class Ed807ExporterTest {

  private static final LocalDate DAY = LocalDate.of(2026, 1, 15);
  private static final Instant AT = Instant.parse("2026-01-15T09:30:47Z");

  private static Participant bank(String bik, String name) {
    return new Participant(bik, name, ParticipantType.BANK, "PSAC",
      "30101810" + bik.substring(6) + "0000000" + bik.substring(0, 2), "МОСКВА", "45");
  }

  private static Ed807Exporter.Header header() {
    return new Ed807Exporter.Header("4525545000", "4525545000", DAY, "1", "RQST", DAY, "1", AT);
  }

  private static Document parse(String xml) throws Exception {
    DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
    f.setNamespaceAware(true);
    return f.newDocumentBuilder()
      .parse(new ByteArrayInputStream(xml.getBytes(Ed807Exporter.ENCODING)));
  }

  @Test
  @DisplayName("XML well-formed и в кодировке ЦБ")
  void wellFormed() throws Exception {
    String xml = Ed807Exporter.toXml(List.of(bank("044525225", "ПАО СБЕРБАНК")), header(),
      Ed807Exporter.Defaults.moscow());

    Document doc = parse(xml);

    assertThat(doc.getDocumentElement().getLocalName()).isEqualTo("ED807");
    assertThat(doc.getDocumentElement().getAttribute("InfoTypeCode")).isEqualTo("SIRR");
    assertThat(doc.getDocumentElement().getNamespaceURI()).isEqualTo(Ed807Exporter.NAMESPACE);
    assertThat(doc.getDocumentElement().getAttribute("CreationReason")).isEqualTo("RQST");
  }

  @Test
  @DisplayName("Каждый участник — отдельный BICDirectoryEntry с PtType и корсчётом")
  void oneEntryPerParticipant() throws Exception {
    String xml = Ed807Exporter.toXml(
      List.of(bank("044525225", "ПАО СБЕРБАНК"), bank("044525187", "БАНК ВТБ")),
      header(), Ed807Exporter.Defaults.moscow());

    Document doc = parse(xml);
    NodeList entries = doc.getElementsByTagNameNS(Ed807Exporter.NAMESPACE, "BICDirectoryEntry");

    assertThat(entries.getLength()).isEqualTo(2);
    Element first = (Element) entries.item(0);
    assertThat(first.getAttribute("BIC")).isEqualTo("044525225");
    NodeList info = first.getElementsByTagNameNS(Ed807Exporter.NAMESPACE, "ParticipantInfo");
    assertThat(((Element) info.item(0)).getAttribute("PtType")).isEqualTo("20");
    assertThat(((Element) info.item(0)).getAttribute("NameP")).isEqualTo("ПАО СБЕРБАНК");
    NodeList accounts = first.getElementsByTagNameNS(Ed807Exporter.NAMESPACE, "Accounts");
    assertThat(accounts.getLength()).isEqualTo(1);
    assertThat(((Element) accounts.item(0)).getAttribute("RegulationAccountType")).isEqualTo("CRSA");
    assertThat(((Element) accounts.item(0)).getAttribute("CK")).isEqualTo("99");
  }

  @Test
  @DisplayName("Реквизиты участника берутся из справочника, а не из общих значений")
  void participantDetailsFromDirectory() throws Exception {
    Participant saratov = new Participant(
      "046311772", "АО \"БАНК \"АГРОРОС\"", ParticipantType.BANK, "PSAC",
      "30101810600000000772", "САРАТОВ", "63",
      "1481", "4525545000", "УЛ. ВАВИЛОВА,21", "410000");

    String xml = Ed807Exporter.toXml(List.of(saratov), header(), Ed807Exporter.Defaults.moscow());
    Element info = (Element) parse(xml)
      .getElementsByTagNameNS(Ed807Exporter.NAMESPACE, "ParticipantInfo").item(0);

    assertThat(info.getAttribute("Rgn")).isEqualTo("63");
    assertThat(info.getAttribute("Nnp")).isEqualTo("САРАТОВ");
    assertThat(info.getAttribute("Adr")).isEqualTo("УЛ. ВАВИЛОВА,21");
    assertThat(info.getAttribute("Ind")).isEqualTo("410000");
    assertThat(info.getAttribute("RegN")).isEqualTo("1481");
    assertThat(info.getAttribute("UID")).isEqualTo("4525545000");
  }

  @Test
  @DisplayName("Без реквизитов в справочнике подставляются значения по умолчанию")
  void fallsBackToDefaults() throws Exception {
    Participant bare = new Participant(
      "044525225", "ПАО СБЕРБАНК", ParticipantType.BANK, "PSAC",
      "30101810400000000225", null, null, null, null, null, null);

    String xml = Ed807Exporter.toXml(List.of(bare), header(), Ed807Exporter.Defaults.moscow());
    Element info = (Element) parse(xml)
      .getElementsByTagNameNS(Ed807Exporter.NAMESPACE, "ParticipantInfo").item(0);

    assertThat(info.getAttribute("Rgn")).isEqualTo("45");
    assertThat(info.getAttribute("RegN")).isEqualTo("1");
    assertThat(info.getAttribute("UID")).isEqualTo("0445252250");
  }

  @Test
  @DisplayName("Участник без корсчёта (казначейство) остаётся без Accounts")
  void participantWithoutAccount() throws Exception {
    Participant treasury = new Participant("016311121", "УФК", ParticipantType.TREASURY_REGIONAL,
      "PSAC", null, "МОСКВА", "45");

    String xml = Ed807Exporter.toXml(List.of(treasury), header(), Ed807Exporter.Defaults.moscow());
    Document doc = parse(xml);

    assertThat(doc.getElementsByTagNameNS(Ed807Exporter.NAMESPACE, "Accounts").getLength()).isZero();
    NodeList info = doc.getElementsByTagNameNS(Ed807Exporter.NAMESPACE, "ParticipantInfo");
    assertThat(((Element) info.item(0)).getAttribute("PtType")).isEqualTo("52");
  }

  @Test
  @DisplayName("Спецсимволы в наименовании экранируются")
  void escapesName() throws Exception {
    String xml = Ed807Exporter.toXml(List.of(bank("044525225", "ООО \"Ромашка\" & Ко")), header(),
      Ed807Exporter.Defaults.moscow());

    assertThat(xml).contains("&amp;").contains("&quot;");
    assertThat(parse(xml).getDocumentElement().getLocalName()).isEqualTo("ED807");
  }

  @Test
  @DisplayName("БИК расчётного центра — первые 6 знаков + 000")
  void rcBic() {
    assertThat(Ed807Exporter.rcBic("049133834")).isEqualTo("049133000");
    assertThat(Ed807Exporter.rcBic("044525225")).isEqualTo("044525000");
  }
}
