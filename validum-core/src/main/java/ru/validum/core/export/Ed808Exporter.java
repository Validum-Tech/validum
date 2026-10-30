package ru.validum.core.export;

import java.nio.charset.Charset;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import ru.validum.core.bank.Participant;

/**
 * Экспорт профиля участника в формате ЦБ РФ ED808 ({@code InfoTypeCode=FIRR}).
 *
 * <p>Отличия от ED807, которые учтены: {@code CreationReason} — элемент (а не атрибут), нет
 * {@code DirectoryVersion}, есть {@code ParticipantProfile} с {@code AccountsInfo}. Шаблон —
 * {@code официальные примеры ЦБ (ED808)}.
 *
 * <p>Как и ED807, файл структурно корректен и воспроизводим, но не является банковским документом.
 */
public final class Ed808Exporter {

  public static final String NAMESPACE = Ed807Exporter.NAMESPACE;
  public static final Charset ENCODING = Ed807Exporter.ENCODING;

  private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

  private Ed808Exporter() {}

  public record Header(
    String edAuthor,
    String edReceiver,
    LocalDate edDate,
    String edNo,
    LocalDate businessDay,
    Instant creationDateTime) {

    public static Header defaults(String edAuthor, String edReceiver, LocalDate edDate, String edNo) {
      return new Header(edAuthor, edReceiver, edDate, edNo, edDate, null);
    }
  }

  /** Счёт участника в профиле: номер, тип по {@code AccountType} и статус. */
  public record AccountRef(String account, String accountType, String accountStatus) {

    public static AccountRef correspondent(String account) {
      return new AccountRef(account, "CRSA", "ACAC");
    }

    public static AccountRef bank(String account) {
      return new AccountRef(account, "BANA", "ACAC");
    }
  }

  public static String toXml(
    Participant participant,
    List<AccountRef> accounts,
    Header header,
    Ed807Exporter.Defaults defaults) {

    String rcBic = Ed807Exporter.rcBic(participant.bik());

    StringBuilder sb = new StringBuilder(1024);
    sb.append("<?xml version=\"1.0\" encoding=\"WINDOWS-1251\"?>\n");
    sb.append("<ED808 EDDate=\"").append(DATE.format(header.edDate()))
      .append("\" EDNo=\"").append(Ed807Exporter.esc(header.edNo()))
      .append("\" CreationDateTime=\"").append(creationDateTime(header))
      .append("\" EDReceiver=\"").append(Ed807Exporter.esc(header.edReceiver()))
      .append("\" InfoTypeCode=\"FIRR\"")
      .append(" BusinessDay=\"").append(DATE.format(header.businessDay()))
      .append("\" EDAuthor=\"").append(Ed807Exporter.esc(header.edAuthor()))
      .append("\" xmlns=\"").append(NAMESPACE).append("\">\n");

    sb.append("  <CreationReason>\n");
    sb.append("    <CreationReasonCode>RQST</CreationReasonCode>\n");
    sb.append("  </CreationReason>\n");

    sb.append("  <BICDirectoryEntry BIC=\"").append(Ed807Exporter.esc(participant.bik()))
      .append("\" ChangeType=\"NCNG\">\n");
    sb.append("    <ParticipantInfo Adr=\"")
      .append(Ed807Exporter.esc(Ed807Exporter.nz(participant.address(), defaults.address())))
      .append("\" CntrCd=\"").append(Ed807Exporter.esc(defaults.countryCode()))
      .append("\" DateIn=\"").append(DATE.format(defaults.dateIn()))
      .append("\" Ind=\"")
      .append(Ed807Exporter.esc(Ed807Exporter.nz(participant.index(), defaults.index())))
      .append("\" NameP=\"").append(Ed807Exporter.esc(participant.name()))
      .append("\" Nnp=\"")
      .append(Ed807Exporter.esc(Ed807Exporter.nz(participant.city(), defaults.locality())))
      .append("\" ParticipantStatus=\"").append(Ed807Exporter.esc(participant.status()))
      .append("\" PtType=\"").append(Ed807Exporter.esc(participant.type().code()))
      .append("\" RegN=\"").append(Ed807Exporter.esc(Ed807Exporter.nz(participant.regN(), "1")))
      .append("\" Rgn=\"")
      .append(Ed807Exporter.esc(Ed807Exporter.nz(participant.region(), defaults.regionCode())))
      .append("\" Srvcs=\"").append(Ed807Exporter.esc(defaults.services()))
      .append("\" Tnp=\"").append(Ed807Exporter.esc(defaults.orgForm()))
      .append("\" UID=\"")
      .append(Ed807Exporter.esc(Ed807Exporter.nz(participant.uid(), Ed807Exporter.uid(participant.bik()))))
      .append("\" XchType=\"").append(Ed807Exporter.esc(defaults.exchangeType()))
      .append("\"/>\n");

    for (AccountRef a : accounts) {
      sb.append("    <Accounts Account=\"").append(Ed807Exporter.esc(a.account()))
        .append("\" AccountCBRBIC=\"").append(Ed807Exporter.esc(rcBic))
        .append("\" AccountStatus=\"").append(Ed807Exporter.esc(a.accountStatus()))
        .append("\" CK=\"").append(Ed807Exporter.CK_PLACEHOLDER).append("\"")
        .append(" DateIn=\"").append(DATE.format(defaults.dateIn()))
        .append("\" RegulationAccountType=\"").append(Ed807Exporter.esc(a.accountType()))
        .append("\"/>\n");
    }
    sb.append("  </BICDirectoryEntry>\n");

    sb.append("  <ParticipantProfile>\n");
    sb.append("    <ParticipantProfileInfo ServCBRBIC=\"").append(Ed807Exporter.esc(rcBic))
      .append("\" ChannelMode=\"3\" PtType=\"").append(Ed807Exporter.esc(participant.type().code()))
      .append("\">\n");
    sb.append("      <ParticipantAttributes CanSendDD=\"1\" CanBeAuthorOfCover=\"1\"")
      .append(" CanBeAuthorOfConfLiqRsrv=\"1\" CanBeAuthorOfLiqRsrv=\"1\"/>\n");
    sb.append("    </ParticipantProfileInfo>\n");
    for (AccountRef a : accounts) {
      sb.append("    <AccountsInfo AccountType=\"").append(Ed807Exporter.esc(a.accountType()))
        .append("\" Account=\"").append(Ed807Exporter.esc(a.account()))
        .append("\"/>\n");
    }
    sb.append("  </ParticipantProfile>\n");

    sb.append("</ED808>\n");
    return sb.toString();
  }

  public static byte[] toBytes(
    Participant participant,
    List<AccountRef> accounts,
    Header header,
    Ed807Exporter.Defaults defaults) {
    return toXml(participant, accounts, header, defaults).getBytes(ENCODING);
  }

  private static String creationDateTime(Header header) {
    Instant at = header.creationDateTime() != null ? header.creationDateTime() : Instant.now();
    return OffsetDateTime.ofInstant(at, ZoneOffset.UTC)
      .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
  }
}
