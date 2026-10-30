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

@DisplayName("Экспорт ED808 (профиль участника)")
class Ed808ExporterTest {

  private static final LocalDate DAY = LocalDate.of(2026, 1, 15);
  private static final Instant AT = Instant.parse("2026-01-15T10:30:47Z");

  private static Participant bank() {
    return new Participant("044525545", "АО ЮНИКРЕДИТ БАНК", ParticipantType.BANK, "PSAC",
      "30101810300000000545", "МОСКВА", "45");
  }

  private static Ed808Exporter.Header header() {
    return new Ed808Exporter.Header("4583001999", "4525545000", DAY, "1", DAY, AT);
  }

  private static Document parse(String xml) throws Exception {
    DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
    f.setNamespaceAware(true);
    return f.newDocumentBuilder()
      .parse(new ByteArrayInputStream(xml.getBytes(Ed808Exporter.ENCODING)));
  }

  @Test
  @DisplayName("InfoTypeCode=FIRR, CreationReason — элемент")
  void firrAndCreationReasonElement() throws Exception {
    String xml = Ed808Exporter.toXml(bank(),
      List.of(Ed808Exporter.AccountRef.correspondent("30101810300000000545")),
      header(), Ed807Exporter.Defaults.moscow());

    Document doc = parse(xml);
    Element root = doc.getDocumentElement();

    assertThat(root.getLocalName()).isEqualTo("ED808");
    assertThat(root.getAttribute("InfoTypeCode")).isEqualTo("FIRR");
    assertThat(root.hasAttribute("CreationReason")).isFalse();
    NodeList reasons = doc.getElementsByTagNameNS(Ed808Exporter.NAMESPACE, "CreationReasonCode");
    assertThat(reasons.getLength()).isEqualTo(1);
    assertThat(reasons.item(0).getTextContent()).isEqualTo("RQST");
  }

  @Test
  @DisplayName("Счета идут и в BICDirectoryEntry, и в ParticipantProfile/AccountsInfo")
  void accountsInBothPlaces() throws Exception {
    String xml = Ed808Exporter.toXml(bank(),
      List.of(
        Ed808Exporter.AccountRef.correspondent("30101810300000000545"),
        Ed808Exporter.AccountRef.bank("40817810000000000545")),
      header(), Ed807Exporter.Defaults.moscow());

    Document doc = parse(xml);

    assertThat(doc.getElementsByTagNameNS(Ed808Exporter.NAMESPACE, "Accounts").getLength())
      .isEqualTo(2);
    NodeList info = doc.getElementsByTagNameNS(Ed808Exporter.NAMESPACE, "AccountsInfo");
    assertThat(info.getLength()).isEqualTo(2);
    assertThat(((Element) info.item(0)).getAttribute("AccountType")).isEqualTo("CRSA");
    assertThat(((Element) info.item(1)).getAttribute("AccountType")).isEqualTo("BANA");
    NodeList accounts = doc.getElementsByTagNameNS(Ed808Exporter.NAMESPACE, "Accounts");
    assertThat(((Element) accounts.item(0)).getAttribute("CK")).isEqualTo("99");
  }

  @Test
  @DisplayName("ParticipantProfileInfo несёт ServCBRBIC, ChannelMode и PtType")
  void participantProfile() throws Exception {
    String xml = Ed808Exporter.toXml(bank(),
      List.of(Ed808Exporter.AccountRef.correspondent("30101810300000000545")),
      header(), Ed807Exporter.Defaults.moscow());

    NodeList profiles = parse(xml)
      .getElementsByTagNameNS(Ed808Exporter.NAMESPACE, "ParticipantProfileInfo");
    Element p = (Element) profiles.item(0);

    assertThat(p.getAttribute("ServCBRBIC")).isEqualTo("044525000");
    assertThat(p.getAttribute("ChannelMode")).isEqualTo("3");
    assertThat(p.getAttribute("PtType")).isEqualTo("20");
  }
}
