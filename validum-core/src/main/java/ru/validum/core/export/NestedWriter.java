package ru.validum.core.export;

import java.util.List;
import ru.validum.core.address.Address;
import ru.validum.core.card.MirCard;
import ru.validum.core.contact.Contact;
import ru.validum.core.document.IdentityDocument;
import ru.validum.core.passport.Passport;
import ru.validum.core.profile.IndividualProfile;

/**
 * Вложенный JSON профиля — та же структура, что отдаёт REST ({@code IndividualProfileDto}), но для
 * файла: группы {@code passport}, {@code addresses}, {@code contacts}, {@code bank}, {@code cards},
 * {@code documents}.
 *
 * <p>Плоская выдача ({@code csv}/{@code json-array}) остаётся контрактом для колонок и SQL; вложенная
 * нужна там, где данные читают глазами или отдают в другой сервис. Своя сериализация — потому что
 * {@code validum-core} не тянет зависимостей.
 */
public final class NestedWriter {

  private NestedWriter() {}

  /** Полный JSON-объект: {@code {"count":N,"seed":S,"profiles":[…]}}. */
  public static String toJson(List<IndividualProfile> profiles, Long seed) {
    StringBuilder sb = new StringBuilder(256 * Math.max(1, profiles.size()));
    sb.append('{');
    sb.append("\"count\":").append(profiles.size());
    if (seed != null) {
      sb.append(",\"seed\":").append(seed);
    }
    sb.append(",\"profiles\":[");
    for (int i = 0; i < profiles.size(); i++) {
      if (i > 0) {
        sb.append(',');
      }
      appendProfile(sb, profiles.get(i));
    }
    sb.append("]}");
    return sb.toString();
  }

  private static void appendProfile(StringBuilder sb, IndividualProfile p) {
    sb.append('{');
    field(sb, "personId", p.personId()).append(',');
    field(sb, "employerOgrn", p.employerOgrn()).append(',');
    field(sb, "fullName", p.fullName()).append(',');
    field(sb, "gender", p.gender().name()).append(',');
    field(sb, "birthDate", String.valueOf(p.birthDate())).append(',');
    field(sb, "age", String.valueOf(p.age())).append(',');
    field(sb, "familyStatus", p.familyStatus().russianName()).append(',');
    field(sb, "inn", p.inn()).append(',');
    field(sb, "snils", p.snils()).append(',');

    sb.append("\"passport\":");
    appendPassport(sb, p.passport());
    sb.append(',');

    sb.append("\"addresses\":{");
    sb.append("\"registration\":");
    appendAddress(sb, p.registrationAddress());
    sb.append(",\"post\":");
    appendAddress(sb, p.postAddress());
    sb.append(",\"current\":");
    appendAddress(sb, p.currentAddress());
    sb.append("},\"contacts\":{");
    sb.append("\"mobile\":");
    appendContact(sb, p.mobile());
    sb.append(",\"email\":");
    appendContact(sb, p.email());
    sb.append("},\"bank\":");
    appendBank(sb, p);
    sb.append(',');
    field(sb, "account", p.account()).append(',');
    sb.append("\"cards\":[");
    List<MirCard> cards = p.cards() == null ? List.of() : p.cards();
    for (int i = 0; i < cards.size(); i++) {
      if (i > 0) {
        sb.append(',');
      }
      appendCard(sb, cards.get(i));
    }
    sb.append("],\"documents\":[");
    List<IdentityDocument> docs = p.documents() == null ? List.of() : p.documents();
    for (int i = 0; i < docs.size(); i++) {
      if (i > 0) {
        sb.append(',');
      }
      appendDocument(sb, docs.get(i));
    }
    sb.append("],");
    field(sb, "secretWord", p.secretWord()).append(',');
    field(sb, "categoryClient", p.categoryClient()).append(',');
    field(sb, "accountPackage", p.accountPackage()).append(',');
    field(sb, "personalManager", p.personalManager()).append(',');
    field(sb, "terrorist", String.valueOf(p.terrorist())).append(',');
    field(sb, "blocked", String.valueOf(p.blocked())).append(',');
    field(sb, "isResident", String.valueOf(p.residency() != null && p.residency().isResident()))
      .append(',');
    field(sb, "citizenship", p.residency() == null ? null : p.residency().citizenship().isoCode())
      .append(',');
    field(sb, "asOf", String.valueOf(p.asOf())).append('}');
  }

  private static void appendPassport(StringBuilder sb, Passport passport) {
    if (passport == null) {
      sb.append("null");
      return;
    }
    sb.append('{');
    field(sb, "series", passport.series()).append(',');
    field(sb, "number", passport.number()).append(',');
    field(sb, "issuedBy", passport.issuedBy()).append(',');
    field(sb, "issueDate", String.valueOf(passport.issueDate())).append(',');
    field(sb, "expirationDate", String.valueOf(passport.expirationDate())).append(',');
    field(sb, "departmentCode", passport.departmentCode()).append(',');
    field(sb, "status", passport.status()).append(',');
    field(sb, "type", passport.type() == null ? null : passport.type().name()).append(',');
    field(sb, "country", passport.country() == null ? null : passport.country().isoCode())
      .append('}');
  }

  private static void appendAddress(StringBuilder sb, Address a) {
    if (a == null) {
      sb.append("null");
      return;
    }
    sb.append('{');
    field(sb, "postalCode", a.postalCode()).append(',');
    field(sb, "region", a.region()).append(',');
    field(sb, "regionType", a.regionType()).append(',');
    field(sb, "district", a.district()).append(',');
    field(sb, "city", a.city()).append(',');
    field(sb, "street", a.street()).append(',');
    field(sb, "building", a.building()).append(',');
    field(sb, "apartment", a.apartment()).append(',');
    field(sb, "expectedAddress", a.expectedAddress()).append('}');
  }

  private static void appendContact(StringBuilder sb, Contact c) {
    if (c == null) {
      sb.append("null");
      return;
    }
    sb.append('{');
    field(sb, "value", c.value()).append(',');
    field(sb, "type", c.type() == null ? null : c.type().name()).append('}');
  }

  private static void appendBank(StringBuilder sb, IndividualProfile p) {
    if (p.bank() == null) {
      sb.append("null");
      return;
    }
    sb.append('{');
    field(sb, "bik", p.bank().bik()).append(',');
    field(sb, "name", p.bank().name()).append(',');
    field(sb, "corrAccount", p.bank().corrAccount()).append(',');
    field(sb, "city", p.bank().city()).append('}');
  }

  private static void appendCard(StringBuilder sb, MirCard card) {
    sb.append('{');
    field(sb, "pan", card.pan()).append(',');
    field(sb, "expiry", card.expiry()).append(',');
    field(sb, "outcome", card.outcome().russianName()).append('}');
  }

  private static void appendDocument(StringBuilder sb, IdentityDocument d) {
    sb.append('{');
    field(sb, "type", d.type() == null ? null : d.type().name()).append(',');
    field(sb, "series", d.series()).append(',');
    field(sb, "number", d.number()).append(',');
    field(sb, "issuedBy", d.issuedBy()).append(',');
    field(sb, "issueDate", String.valueOf(d.issueDate())).append(',');
    field(sb, "expirationDate", String.valueOf(d.expirationDate())).append(',');
    field(sb, "departmentCode", d.departmentCode()).append('}');
  }

  private static StringBuilder field(StringBuilder sb, String name, String value) {
    sb.append('"').append(name).append("\":");
    if (value == null) {
      sb.append("null");
    } else {
      sb.append('"').append(escape(value)).append('"');
    }
    return sb;
  }

  private static String escape(String value) {
    StringBuilder sb = new StringBuilder(value.length() + 8);
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '"' -> sb.append("\\\"");
        case '\\' -> sb.append("\\\\");
        case '\n' -> sb.append("\\n");
        case '\r' -> sb.append("\\r");
        case '\t' -> sb.append("\\t");
        default -> {
          if (c < 0x20) {
            sb.append(String.format("\\u%04x", (int) c));
          } else {
            sb.append(c);
          }
        }
      }
    }
    return sb.toString();
  }
}
