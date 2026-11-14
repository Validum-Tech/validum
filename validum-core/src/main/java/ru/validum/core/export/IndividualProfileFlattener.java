package ru.validum.core.export;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ru.validum.core.address.Address;
import ru.validum.core.bank.Bank;
import ru.validum.core.contact.Contact;
import ru.validum.core.document.IdentityDocument;
import ru.validum.core.passport.Passport;
import ru.validum.core.profile.IndividualProfile;

/**
 * Разворачивает {@link IndividualProfile} в плоскую map — по колонке на поле.
 *
 * <p>Вложенные объекты получают префиксы: {@code passportSeries}, {@code registrationCity},
 * {@code bankBik}, {@code mobileValue}, {@code emailType}. Так CSV остаётся плоским и открывается
 * в Excel без ручной правки.
 *
 * <p>Порядок колонок фиксирован ({@link LinkedHashMap}) — контракт для downstream ETL-скриптов и
 * snapshot-тестов.
 */
public final class IndividualProfileFlattener {

  private IndividualProfileFlattener() {}

  /** Разворачивает список профилей (ДУЛ — inline-таблицей в колонке {@code documents}). */
  public static List<Map<String, String>> flattenAll(List<IndividualProfile> profiles) {
    return flattenAll(profiles, DocumentsFormat.INLINE);
  }

  /** Разворачивает список профилей в выбранном режиме сериализации ДУЛ. */
  public static List<Map<String, String>> flattenAll(
      List<IndividualProfile> profiles, DocumentsFormat format) {
    return profiles.stream().map(p -> flatten(p, format)).toList();
  }

  /** Разворачивает один профиль (ДУЛ — inline-таблицей). */
  public static Map<String, String> flatten(IndividualProfile p) {
    return flatten(p, DocumentsFormat.INLINE);
  }

  /**
   * Разворачивает один профиль в выбранном режиме сериализации ДУЛ.
   *
   * <p>{@link DocumentsFormat#SPLIT} оставляет колонку {@code documents} пустой: в этом режиме
   * документы пишутся отдельной таблицей (см. {@link DocumentsCsv#toRows}).
   */
  public static Map<String, String> flatten(IndividualProfile p, DocumentsFormat format) {
    Map<String, String> m = new LinkedHashMap<>();
    m.put("personId", p.personId() == null ? "" : p.personId());
    m.put("fullName", p.fullName());
    m.put("gender", p.gender().name());
    m.put("birthDate", formatDate(p.birthDate()));
    m.put("age", String.valueOf(p.age()));
    m.put("familyStatus", p.familyStatus().russianName());
    m.put("inn", p.inn());
    m.put("snils", p.snils());
    flattenPassport(m, p.passport());
    flattenAddress(m, "registration", p.registrationAddress());
    flattenAddress(m, "post", p.postAddress());
    flattenAddress(m, "current", p.currentAddress());
    flattenContact(m, "mobile", p.mobile());
    flattenContact(m, "email", p.email());
    m.put("secretWord", p.secretWord());
    m.put("categoryClient", p.categoryClient());
    m.put("accountPackage", p.accountPackage());
    m.put("personalManager", p.personalManager());
    m.put("terrorist", String.valueOf(p.terrorist()));
    m.put("blocked", String.valueOf(p.blocked()));
    flattenBank(m, p.bank());
    m.put("account", p.account());
    List<IdentityDocument> documents = p.documents() == null ? List.of() : p.documents();
    m.put(DocumentsCsv.COLUMN, switch (format) {
      case JSON -> DocumentsCsv.json(documents);
      case SPLIT -> "";
      case INLINE -> DocumentsCsv.inline(documents);
    });
    // Резидентство видно в плоской выдаче: по нему скрипты решают, какой набор ДУЛ ожидать.
    m.put("isResident", String.valueOf(p.residency().isResident()));
    m.put("citizenship", p.residency().citizenship().isoCode());
    m.put("asOf", formatDate(p.asOf()));
    return m;
  }

  private static void flattenPassport(Map<String, String> m, Passport p) {
    if (p == null) return;
    m.put("passportSeries", p.series());
    m.put("passportNumber", p.number());
    m.put("passportIssuedBy", p.issuedBy());
    m.put("passportIssueDate", formatDate(p.issueDate()));
    m.put("passportExpirationDate", formatDate(p.expirationDate()));
    m.put("passportDepartmentCode", p.departmentCode());
    m.put("passportType", p.type().name());
    m.put("passportCountry", p.country().isoCode());
  }

  private static void flattenAddress(Map<String, String> m, String prefix, Address a) {
    if (a == null) return;
    m.put(prefix + "Region", a.region());
    m.put(prefix + "RegionType", a.regionType());
    m.put(prefix + "PostalCode", a.postalCode());
    m.put(prefix + "City", a.city());
    m.put(prefix + "Street", a.street());
    m.put(prefix + "Building", a.building());
    m.put(prefix + "Apartment", a.apartment());
    m.put(prefix + "Formatted", a.formatted());
  }

  private static void flattenContact(Map<String, String> m, String prefix, Contact c) {
    if (c == null) return;
    m.put(prefix + "Type", c.type().name());
    m.put(prefix + "Value", c.value());
    m.put(prefix + "Description", c.description());
    m.put(prefix + "Status", c.status());
  }

  private static void flattenBank(Map<String, String> m, Bank b) {
    if (b == null) return;
    m.put("bankBik", b.bik());
    m.put("bankName", b.name());
    m.put("bankCorrAccount", b.corrAccount());
    m.put("bankCity", b.city());
    m.put("bankRegion", b.region());
  }

  private static String formatDate(LocalDate d) {
    return d == null ? "" : d.toString(); // ISO_LOCAL_DATE (yyyy-MM-dd)
  }
}
