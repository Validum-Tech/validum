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
 * Экспорт справочника участников в формате ЦБ РФ ED807 (справочник БИК, {@code InfoTypeCode=SIRR}).
 *
 * <p>Структура повторяет официальные примеры ЦБ РФ к ED807 и схему {@code cbr_ed807_v2026.09.0.xsd}.
 * Генерируем то, что знаем: БИК, наименование, тип участника, статус,
 * корсчёт и город. Атрибуты, которых у нас нет (адрес, индекс, код региона, регномер, UID), берутся
 * из {@link Defaults} — они стабильны между вызовами, чтобы файл оставался воспроизводимым.
 *
 * <p>Внимание: это структурно корректный, но не банковский по содержанию файл — он для тестов
 * парсеров и фикстур, а не для отправки в ЦБ.
 */
public final class Ed807Exporter {

  /** Namespace всех сообщений УФЭБС v2.0. */
  public static final String NAMESPACE = "urn:cbr-ru:ed:v2.0";

  /** Кодировка, которую объявляет ЦБ в своих примерах. */
  public static final Charset ENCODING = Charset.forName("windows-1251");

  private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

  private Ed807Exporter() {}

  /** Заголовок ED807 ({@code CreationDateTime} берётся текущий UTC, если не задан). */
  public record Header(
    String edAuthor,
    String edReceiver,
    LocalDate edDate,
    String edNo,
    String creationReason,
    LocalDate businessDay,
    String directoryVersion,
    Instant creationDateTime) {

    public static Header defaults(String edAuthor, String edReceiver, LocalDate edDate, String edNo) {
      return new Header(edAuthor, edReceiver, edDate, edNo, "RQST", edDate, "1", null);
    }
  }

  /** Значения атрибутов, которых нет в модели {@link Participant}. */
  public record Defaults(
    String address,
    String index,
    String locality,
    String regionCode,
    String countryCode,
    String orgForm,
    String services,
    String exchangeType,
    LocalDate dateIn) {

    /** Разумные значения по умолчанию: Москва, все услуги, электронный обмен. */
    public static Defaults moscow() {
      return new Defaults("МОСКВА", "101000", "МОСКВА", "45", "RU", "Г", "3", "1", LocalDate.of(2020, 1, 1));
    }
  }

  /** XML как строка (кодировка в декларации — WINDOWS-1251; в строке Java — UTF-16). */
  public static String toXml(List<Participant> participants, Header header, Defaults defaults) {
    StringBuilder sb = new StringBuilder(512 + participants.size() * 400);
    sb.append("<?xml version=\"1.0\" encoding=\"WINDOWS-1251\"?>\n");
    sb.append("<ED807 EDDate=\"").append(DATE.format(header.edDate()))
      .append("\" EDNo=\"").append(esc(header.edNo()))
      .append("\" CreationDateTime=\"").append(creationDateTime(header))
      .append("\" CreationReason=\"").append(esc(header.creationReason()))
      .append("\" EDReceiver=\"").append(esc(header.edReceiver()))
      .append("\" InfoTypeCode=\"SIRR\"")
      .append(" BusinessDay=\"").append(DATE.format(header.businessDay()))
      .append("\" DirectoryVersion=\"").append(esc(header.directoryVersion()))
      .append("\" EDAuthor=\"").append(esc(header.edAuthor()))
      .append("\" xmlns=\"").append(NAMESPACE).append("\">\n");

    int regNo = 1;
    for (Participant p : participants) {
      sb.append("  <BICDirectoryEntry BIC=\"").append(esc(p.bik())).append("\" ChangeType=\"NCNG\">\n");
      String regN = p.regN() != null && !p.regN().isBlank() ? p.regN() : String.valueOf(regNo);
      sb.append("    <ParticipantInfo Adr=\"").append(esc(nz(p.address(), defaults.address())))
        .append("\" CntrCd=\"").append(esc(defaults.countryCode()))
        .append("\" DateIn=\"").append(DATE.format(defaults.dateIn()))
        .append("\" Ind=\"").append(esc(nz(p.index(), defaults.index())))
        .append("\" NameP=\"").append(esc(p.name()))
        .append("\" Nnp=\"").append(esc(nz(p.city(), defaults.locality())))
        .append("\" ParticipantStatus=\"").append(esc(p.status()))
        .append("\" PtType=\"").append(esc(p.type().code()))
        .append("\" RegN=\"").append(esc(regN))
        .append("\" Rgn=\"").append(esc(nz(p.region(), defaults.regionCode())))
        .append("\" Srvcs=\"").append(esc(defaults.services()))
        .append("\" Tnp=\"").append(esc(defaults.orgForm()))
        .append("\" UID=\"").append(esc(nz(p.uid(), uid(p.bik()))))
        .append("\" XchType=\"").append(esc(defaults.exchangeType()))
        .append("\"/>\n");
      if (p.corrAccount() != null) {
        sb.append("    <Accounts Account=\"").append(esc(p.corrAccount()))
          .append("\" AccountCBRBIC=\"").append(esc(rcBic(p.bik())))
          .append("\" AccountStatus=\"ACAC\"")
          .append(" CK=\"").append(CK_PLACEHOLDER).append("\"")
          .append(" DateIn=\"").append(DATE.format(defaults.dateIn()))
          .append("\" RegulationAccountType=\"CRSA\"/>\n");
      }
      sb.append("  </BICDirectoryEntry>\n");
      regNo++;
    }

    sb.append("</ED807>\n");
    return sb.toString();
  }

  /** XML в байтах windows-1251 — то, что реально можно отдать как файл. */
  public static byte[] toBytes(List<Participant> participants, Header header, Defaults defaults) {
    return toXml(participants, header, defaults).getBytes(ENCODING);
  }

  /** БИК расчётного центра: первые шесть знаков БИК + {@code 000} (соглашение о счётах ПС БР). */
  public static String rcBic(String bik) {
    return bik.length() >= 6 ? bik.substring(0, 6) + "000" : bik;
  }

  /** UID участника — 10 цифр: БИК (9) + контрольный ноль. */
  static String uid(String bik) {
    return bik + "0";
  }

  /**
   * Заглушка контрольного ключа счёта ({@code CK}) — как в примере ЦБ.
   *
   * <p>Настоящий {@code CK} — двухсимвольный контрольный ключ счёта из «Реквизитного состава»
   * (12.5.3), алгоритм в открытых источниках не описан и из номера счёта не выводится. Атрибут
   * необязательный, но часть банковских валидаторов его ждёт — поэтому ставим значение из
   * официального примера ED807 и явно помечаем его как заглушку.
   */
  public static final String CK_PLACEHOLDER = "99";

  /** Значение из справочника, иначе значение по умолчанию. */
  static String nz(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  private static String creationDateTime(Header header) {
    Instant at = header.creationDateTime() != null ? header.creationDateTime() : Instant.now();
    return OffsetDateTime.ofInstant(at, ZoneOffset.UTC)
      .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
  }

  static String esc(String value) {
    if (value == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder(value.length() + 8);
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '&' -> sb.append("&amp;");
        case '<' -> sb.append("&lt;");
        case '>' -> sb.append("&gt;");
        case '"' -> sb.append("&quot;");
        case '\'' -> sb.append("&apos;");
        default -> sb.append(c);
      }
    }
    return sb.toString();
  }
}
