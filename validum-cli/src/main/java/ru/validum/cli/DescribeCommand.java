package ru.validum.cli;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import ru.validum.core.account.AccountValidator;
import ru.validum.core.document.IdentityDocument;
import ru.validum.core.export.DocumentsCsv;
import ru.validum.core.report.Consistency;
import ru.validum.core.validation.InnValidator;
import ru.validum.core.validation.SnilsValidator;

/**
 * Карточка одного человека из готового набора: чем он документирован, где живёт, в каком банке и
 * проходит ли проверки. Нужна, когда QA нашёл «странную строку в CSV» и хочет понять, это данные
 * такие или генератор сломался.
 */
@Command(
  name = "describe",
  description = "Карточка человека из набора: документы, адреса, банк и проверки",
  mixinStandardHelpOptions = true)
public final class DescribeCommand implements Callable<Integer> {

  @Option(names = "--file", required = true, paramLabel = "<csv|json>",
    description = "Файл набора: CSV или JSON")
  Path file;

  @Option(names = "--format", paramLabel = "<fmt>",
    description = "csv|csv-semicolon|json|json-array (по умолчанию — по расширению)")
  String format;

  @Option(names = "--person", paramLabel = "<personId>",
    description = "Идентификатор человека (колонка personId)")
  String person;

  @Option(names = "--row", paramLabel = "<N>", description = "Номер записи в наборе, с 1")
  Integer rowNumber;

  @Override
  public Integer call() {
    if ((person == null || person.isBlank()) && rowNumber == null) {
      System.err.println("Ошибка: укажите --person <personId> или --row <N>");
      return 2;
    }
    if (person != null && !person.isBlank() && rowNumber != null) {
      System.err.println("Ошибка: --person и --row взаимоисключающие");
      return 2;
    }
    try {
      List<Map<String, String>> rows = DatasetReader.read(file, format);
      if (rows.isEmpty()) {
        System.err.println("Ошибка: во входном файле нет ни одной записи");
        return 2;
      }
      Map<String, String> found = find(rows);
      if (found == null) {
        System.err.println("Ошибка: в наборе нет такой записи — " + (person != null ? person : "строка " + rowNumber));
        return 2;
      }
      print(found);
      return 0;
    } catch (java.nio.file.NoSuchFileException e) {
      System.err.println("Ошибка: файл не найден — " + file.toAbsolutePath());
      return 2;
    } catch (IOException e) {
      System.err.println("Ошибка чтения " + file + ": " + e.getMessage());
      return 2;
    }
  }

  private Map<String, String> find(List<Map<String, String>> rows) {
    if (person != null && !person.isBlank()) {
      for (Map<String, String> row : rows) {
        if (person.equals(row.get("personId"))) {
          return row;
        }
      }
      return null;
    }
    if (rowNumber == null || rowNumber < 1 || rowNumber > rows.size()) {
      return null;
    }
    return rows.get(rowNumber - 1);
  }

  private void print(Map<String, String> row) {
    String who = row.getOrDefault("fullName", "—");
    System.out.println("# validum " + BuildInfo.version() + " · человек " + who
      + (row.get("personId") == null ? "" : " (" + row.get("personId") + ")"));
    System.out.println();
    section("Кто");
    line("ФИО", who);
    line("Пол", row.get("gender"));
    line("Дата рождения", row.get("birthDate"));
    line("Возраст", row.get("age"));
    line("Дата расчёта", row.get("asOf"));
    line("Семейное положение", row.get("familyStatus"));

    section("Проверки");
    line("ИНН", row.get("inn"), check(InnValidator.isValid(row.get("inn"))));
    line("СНИЛС", row.get("snils"), check(SnilsValidator.isValid(row.get("snils"))));
    String account = row.get("account");
    String bik = row.get("bankBik");
    line("Счёт", account, check(account != null && AccountValidator.isValidAuto(account, bik)));
    List<Consistency.Violation> violations = Consistency.check(List.of(row));
    if (violations.isEmpty()) {
      line("Согласованность", "нарушений нет", "ok");
    } else {
      for (Consistency.Violation violation : violations) {
        line("Согласованность", violation.rule() + " — " + violation.detail(), "ошибка");
      }
    }

    section("Паспорт");
    line("Тип", row.get("passportType"));
    line("Страна", row.get("passportCountry"));
    line("Серия и номер", join(row.get("passportSeries"), row.get("passportNumber")));
    line("Кем выдан", row.get("passportIssuedBy"));
    line("Дата выдачи", row.get("passportIssueDate"));
    line("Действителен до", row.get("passportExpirationDate"));
    line("Код подразделения", row.get("passportDepartmentCode"));

    List<IdentityDocument> documents = DocumentsCsv.parseInline(row.get("documents"));
    if (!documents.isEmpty()) {
      section("Дополнительные документы");
      for (IdentityDocument document : documents) {
        String validity = document.expirationDate() == null
          ? "бессрочно"
          : "до " + document.expirationDate();
        System.out.println("  " + document.type().title() + ": " + document.seriesAndNumber()
          + ", " + document.issuedBy() + ", " + document.issueDate() + ", " + validity);
      }
    }

    section("Адреса");
    line("Регистрация", row.get("registrationFormatted"), addressNote(row, "registration"));
    line("Почтовый", row.get("postFormatted"), addressNote(row, "post"));
    line("Фактический", row.get("currentFormatted"), addressNote(row, "current"));

    section("Банк и счёт");
    // bank.region() — это КОД субъекта ("45"), для человека он бесполезен: печатаем город.
    line("Банк", row.get("bankName"), row.get("bankCity"));
    line("БИК", bik);
    line("Корсчёт", row.get("bankCorrAccount"));
    line("Счёт", account);
    line("Пакет услуг", row.get("accountPackage"));
    line("Персональный менеджер", row.get("personalManager"));

    section("Флаги");
    line("Резидент", row.get("isResident"));
    line("Гражданство", row.get("citizenship"));
    line("Террорист", row.get("terrorist"));
    line("Заблокирован", row.get("blocked"));
    line("Телефон", row.get("mobileValue"), row.get("mobileType"));
    line("Email", row.get("emailValue"), row.get("emailType"));
  }

  private static String check(boolean ok) {
    return ok ? "ok" : "ошибка";
  }

  private static String join(String a, String b) {
    if (a == null || a.isBlank()) {
      return b;
    }
    if (b == null || b.isBlank()) {
      return a;
    }
    return a + " · " + b;
  }

  private static void section(String title) {
    System.out.println();
    System.out.println("[" + title + "]");
  }

  private static void line(String label, String value) {
    line(label, value, null);
  }

  /** «регион, индекс» — то, что видно в CSV отдельными колонками. */
  private static String addressNote(Map<String, String> row, String prefix) {
    String region = row.get(prefix + "Region");
    String postal = row.get(prefix + "PostalCode");
    if (region == null && postal == null) return null;
    if (region == null) return "индекс " + postal;
    return postal == null ? region : region + ", индекс " + postal;
  }

  private static void line(String label, String value, String note) {
    if (value == null || value.isBlank()) {
      return;
    }
    String tail = note == null || note.isBlank() ? "" : " (" + note + ")";
    System.out.printf("  %-22s %s%s%n", label + ":", value, tail);
  }
}
