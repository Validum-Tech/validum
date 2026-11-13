package ru.validum.core.export;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import ru.validum.core.address.Address;
import ru.validum.core.bank.Bank;
import ru.validum.core.common.Country;
import ru.validum.core.contact.Contact;
import ru.validum.core.contact.ContactType;
import ru.validum.core.name.FamilyStatus;
import ru.validum.core.name.Gender;
import ru.validum.core.name.PersonName;
import ru.validum.core.passport.Passport;
import ru.validum.core.passport.PassportType;
import ru.validum.core.profile.IndividualProfile;
import ru.validum.core.profile.Residency;

/**
 * Собирает {@link IndividualProfile} из плоской строки CSV — обратная операция к {@link
 * IndividualProfileFlattener}.
 *
 * <p>Колонки читаются по тем же именам, что пишет флаттенер ({@code passportSeries}, {@code
 * registrationCity}, {@code bankBik}, …). Отсутствующая колонка трактуется как пустое значение.
 *
 * <p>Поля, которых нет в CSV-контракте ({@code birthPlace}, {@code residency}, срок действия
 * паспорта, признак «по умолчанию» у контактов), заполняются безопасными значениями по умолчанию —
 * на round-trip {@code flatten(parse(csv))} это не влияет, так как флаттенер их и не пишет.
 *
 * <p><b>null сохраняется.</b> Флаттенер пишет {@code null} как {@code null} в map (и как пустую
 * ячейку в CSV). Парсер возвращает ровно то, что лежит в map, без подмены {@code null → ""} — иначе
 * круговая операция на in-memory map расходилась бы на каждом незаполненном подполе.
 *
 * <p>Некорректные даты не проглатываются молча: дата бросает {@link IllegalArgumentException}.
 * Неизвестные enum-значения заменяются значением по умолчанию.
 */
public final class ProfileFromCsv {

  private ProfileFromCsv() {}

  /** Собирает профиль из одной строки. */
  public static IndividualProfile parse(Map<String, String> row) {
    return new IndividualProfile(
      name(row),
      date(row.get("birthDate")),
      null, // место рождения в CSV-контракт не входит
      familyStatus(row.get("familyStatus")),
      Residency.residentRU(),
      passport(row),
      row.get("inn"),
      row.get("snils"),
      address(row, "registration"),
      address(row, "post"),
      address(row, "current"),
      contact(row, "mobile", ContactType.MOBILE),
      contact(row, "email", ContactType.EMAIL),
      row.get("secretWord"),
      row.get("categoryClient"),
      row.get("accountPackage"),
      row.get("personalManager"),
      Boolean.parseBoolean(row.get("terrorist")),
      Boolean.parseBoolean(row.get("blocked")),
      bank(row),
      row.get("account"),
      DocumentsCsv.parse(row.get(DocumentsCsv.COLUMN)),
      asOf(row),
      blankToNull(row.get("personId")));
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  /** Дата расчёта из CSV; пустая ячейка — «сегодня» (как и у профилей без asOf). */
  private static LocalDate asOf(Map<String, String> row) {
    String value = row.get("asOf");
    if (value == null || value.isBlank()) {
      return LocalDate.now();
    }
    return date(value);
  }

  private static PersonName name(Map<String, String> row) {
    String full = row.get("fullName");
    if (full == null) {
      full = "";
    }
    String[] parts = full.isEmpty() ? new String[0] : full.split(" ", -1);
    String last = parts.length > 0 ? parts[0] : "";
    String first = parts.length > 1 ? parts[1] : "";
    String patronymic = parts.length > 2 ? parts[2] : "";
    return new PersonName(last, first, patronymic, gender(row.get("gender")));
  }

  private static Gender gender(String value) {
    if (value != null && !value.isBlank()) {
      try {
        return Gender.valueOf(value.trim());
      } catch (IllegalArgumentException ignored) {
        // неизвестный пол — вернём дефолт ниже
      }
    }
    return Gender.MALE;
  }

  private static FamilyStatus familyStatus(String value) {
    if (value != null && !value.isBlank()) {
      String v = value.trim();
      for (FamilyStatus status : FamilyStatus.values()) {
        if (status.russianName().equals(v) || status.name().equals(v)) {
          return status;
        }
      }
    }
    return FamilyStatus.SINGLE;
  }

  private static Passport passport(Map<String, String> row) {
    String series = row.get("passportSeries");
    String number = row.get("passportNumber");
    String issuedBy = row.get("passportIssuedBy");
    String issueDate = row.get("passportIssueDate");
    String departmentCode = row.get("passportDepartmentCode");
    String type = row.get("passportType");
    String country = row.get("passportCountry");

    boolean absent =
      !row.containsKey("passportSeries")
        && !row.containsKey("passportNumber")
        && !row.containsKey("passportIssuedBy")
        && !row.containsKey("passportIssueDate")
        && !row.containsKey("passportDepartmentCode")
        && !row.containsKey("passportType")
        && !row.containsKey("passportCountry");
    if (absent || (isBlank(series) && isBlank(number) && isBlank(issuedBy) && isBlank(issueDate))) {
      return null;
    }

    return new Passport(
      series,
      number,
      issuedBy,
      date(issueDate),
      null, // срок действия в CSV-контракт не входит
      departmentCode,
      null, // статус паспорта в CSV-контракт не входит
      passportType(type),
      country(country));
  }

  private static PassportType passportType(String value) {
    if (value != null && !value.isBlank()) {
      try {
        return PassportType.valueOf(value.trim());
      } catch (IllegalArgumentException ignored) {
        // неизвестный тип — дефолт ниже
      }
    }
    return PassportType.RF;
  }

  private static Country country(String isoCode) {
    if (isoCode != null && !isoCode.isBlank()) {
      String iso = isoCode.trim();
      for (Country c : Country.values()) {
        if (c.isoCode().equals(iso)) {
          return c;
        }
      }
    }
    return Country.RU;
  }

  private static Address address(Map<String, String> row, String prefix) {
    boolean absent =
      !row.containsKey(prefix + "PostalCode")
        && !row.containsKey(prefix + "City")
        && !row.containsKey(prefix + "Street")
        && !row.containsKey(prefix + "Formatted");
    if (absent) {
      return null;
    }

    return new Address(
      null, // type
      null, // format
      null, // status
      false, // isDefault
      Country.RU,
      row.get(prefix + "PostalCode"),
      null, null, // region, regionType
      null, null, // district, districtType
      row.get(prefix + "City"), null, // city, cityType
      null, null, // locality, localityType
      null, null, // planElement, planElementType
      row.get(prefix + "Street"), null, // street, streetType
      row.get(prefix + "Building"), null, // building, buildingType
      row.get(prefix + "Apartment"), null, // apartment, apartmentType
      row.get(prefix + "Formatted"), null, // expectedAddress, fiasAddress
      null, null); // fiasIfnsfl, fiasStreetAoguid
  }

  private static Contact contact(Map<String, String> row, String prefix, ContactType fallback) {
    boolean absent = !row.containsKey(prefix + "Type") && !row.containsKey(prefix + "Value");
    if (absent) {
      return null;
    }

    return new Contact(
      contactType(row.get(prefix + "Type"), fallback),
      row.get(prefix + "Value"),
      row.get(prefix + "Description"),
      row.get(prefix + "Status"),
      false);
  }

  private static ContactType contactType(String value, ContactType fallback) {
    if (value != null && !value.isBlank()) {
      try {
        return ContactType.valueOf(value.trim());
      } catch (IllegalArgumentException ignored) {
        // неизвестный тип — дефолт ниже
      }
    }
    return fallback;
  }

  private static Bank bank(Map<String, String> row) {
    String bik = row.get("bankBik");
    String corrAccount = row.get("bankCorrAccount");
    if (isBlank(bik) || isBlank(corrAccount)) {
      return null;
    }
    return new Bank(
      bik,
      row.get("bankName"),
      corrAccount,
      row.get("bankCity"),
      row.get("bankRegion"));
  }

  private static LocalDate date(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(value.trim());
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException("Некорректная дата в CSV: " + value, e);
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
