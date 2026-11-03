package ru.validum.core.export;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Генератор CSV из плоских строк.
 *
 * <p>Работает с {@code List<Map<String,String>>}: каждая строка — это уже развёрнутый профиль
 * (см. {@link IndividualProfileFlattener}, {@link IpProfileFlattener}, {@link
 * OrganizationProfileFlattener}). Так CSV-формат живёт в core, а не в API-модуле — его
 * используют и REST-контроллер, и CLI.
 *
 * <p>По умолчанию пишет машинный CSV: UTF-8 с BOM, разделитель «,», английские заголовки —
 * его читают скрипты и импортёры. Для человека есть {@link #toCsv(List, char, boolean)} с
 * русскими заголовками и «;» — так русский Excel открывает файл без «всё в одной колонке».
 * Экранирование по RFC 4180: кавычки, разделитель и переносы строк.
 */
public final class CsvExporter {

  private static final String BOM = "\uFEFF";
  private static final String NEWLINE = "\r\n";

  /** Русские заголовки для колонок профиля; неизвестная колонка остаётся как есть. */
  private static final Map<String, String> RUS_HEADERS = rusHeadersMap();

  private CsvExporter() {}

  /** Машинный CSV: BOM, «,», английские заголовки. Для пустого списка — только BOM. */
  public static String toCsv(List<Map<String, String>> rows) {
    return toCsv(rows, ',', false);
  }

  /** CSV с выбранным разделителем и английскими заголовками. */
  public static String toCsv(List<Map<String, String>> rows, char delimiter) {
    return toCsv(rows, delimiter, false);
  }

  /**
   * CSV с выбранным разделителем и, по желанию, русскими заголовками.
   *
   * @param rows строки в порядке колонок (LinkedHashMap)
   * @param delimiter «,» для машин, «;» для русского Excel
   * @param russianHeaders переводить ли имена колонок на русский
   */
  public static String toCsv(
    List<Map<String, String>> rows, char delimiter, boolean russianHeaders) {
    if (rows.isEmpty()) {
      return BOM;
    }
    String delim = String.valueOf(delimiter);
    StringBuilder sb = new StringBuilder(BOM);
    List<String> headers = List.copyOf(rows.get(0).keySet());
    sb.append(headers.stream()
        .map(h -> russianHeaders ? escape(russianHeader(h), delimiter) : escape(h, delimiter))
        .collect(Collectors.joining(delim)))
      .append(NEWLINE);
    for (Map<String, String> row : rows) {
      sb.append(headers.stream()
          .map(h -> escape(row.getOrDefault(h, ""), delimiter))
          .collect(Collectors.joining(delim)))
        .append(NEWLINE);
    }
    return sb.toString();
  }

  /** Русское имя колонки; незнакомая колонка возвращается без изменений. */
  public static String russianHeader(String key) {
    return RUS_HEADERS.getOrDefault(key, key);
  }

  /** Экранирует значение для разделителя «,» (совместимость). */
  public static String escape(String value) {
    return escape(value, ',');
  }

  /**
   * Экранирует одно значение по RFC 4180: оборачивает в кавычки, если внутри есть разделитель,
   * кавычка или перенос строки; внутренние кавычки удваиваются.
   */
  public static String escape(String value, char delimiter) {
    if (value == null || value.isEmpty()) {
      return "";
    }
    if (value.indexOf(delimiter) >= 0
      || value.contains("\"")
      || value.contains("\n")
      || value.contains("\r")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }

  /** Конвертирует строку в UTF-8 байты (для ResponseEntity и file output). */
  public static byte[] toBytes(String csv) {
    return csv.getBytes(StandardCharsets.UTF_8);
  }

  /** UTF-8 байты; {@code withBom=false} срезает BOM, если он уже есть в строке. */
  public static byte[] toBytes(String csv, boolean withBom) {
    if (withBom || !csv.startsWith(BOM)) {
      return csv.getBytes(StandardCharsets.UTF_8);
    }
    return csv.substring(1).getBytes(StandardCharsets.UTF_8);
  }

  private static Map<String, String> rusHeadersMap() {
    Map<String, String> m = new LinkedHashMap<>();
    m.put("fullName", "ФИО");
    m.put("gender", "Пол");
    m.put("birthDate", "Дата рождения");
    m.put("birthPlace", "Место рождения");
    m.put("age", "Возраст");
    m.put("familyStatus", "Семейное положение");
    m.put("inn", "ИНН");
    m.put("snils", "СНИЛС");
    m.put("passportSeries", "Серия паспорта");
    m.put("passportNumber", "Номер паспорта");
    m.put("passportIssuedBy", "Кем выдан");
    m.put("passportIssueDate", "Дата выдачи");
    m.put("passportDepartmentCode", "Код подразделения");
    m.put("passportType", "Тип паспорта");
    m.put("passportCountry", "Страна паспорта");
    m.put("registrationPostalCode", "Индекс регистрации");
    m.put("registrationCity", "Город регистрации");
    m.put("registrationStreet", "Улица регистрации");
    m.put("registrationBuilding", "Дом регистрации");
    m.put("registrationApartment", "Квартира регистрации");
    m.put("registrationFormatted", "Адрес регистрации");
    m.put("postPostalCode", "Индекс почтовый");
    m.put("postCity", "Город почтовый");
    m.put("postStreet", "Улица почтовая");
    m.put("postBuilding", "Дом почтовый");
    m.put("postApartment", "Квартира почтовая");
    m.put("postFormatted", "Адрес почтовый");
    m.put("currentPostalCode", "Индекс фактический");
    m.put("currentCity", "Город фактический");
    m.put("currentStreet", "Улица фактическая");
    m.put("currentBuilding", "Дом фактический");
    m.put("currentApartment", "Квартира фактическая");
    m.put("currentFormatted", "Адрес фактический");
    m.put("mobileType", "Тип телефона");
    m.put("mobileValue", "Телефон");
    m.put("mobileDescription", "Описание телефона");
    m.put("mobileStatus", "Статус телефона");
    m.put("emailType", "Тип email");
    m.put("emailValue", "Email");
    m.put("emailDescription", "Описание email");
    m.put("emailStatus", "Статус email");
    m.put("secretWord", "Кодовое слово");
    m.put("categoryClient", "Категория клиента");
    m.put("accountPackage", "Пакет услуг");
    m.put("personalManager", "Персональный менеджер");
    m.put("terrorist", "Террорист");
    m.put("blocked", "Заблокирован");
    m.put("bankBik", "БИК");
    m.put("bankName", "Банк");
    m.put("bankCorrAccount", "Корсчёт");
    m.put("bankCity", "Город банка");
    m.put("bankRegion", "Регион банка");
    m.put("account", "Счёт");
    m.put("documents", "Документы");
    m.put("asOf", "Дата расчёта");
    m.put("type", "Тип");
    m.put("name", "Наименование");
    m.put("ogrn", "ОГРН");
    m.put("kpp", "КПП");
    m.put("okpo", "ОКПО");
    m.put("okved", "ОКВЭД");
    m.put("okvedName", "Вид деятельности");
    m.put("ogrnip", "ОГРНИП");
    m.put("region", "Регион");
    m.put("regionType", "Тип региона");
    m.put("city", "Город");
    m.put("street", "Улица");
    m.put("building", "Дом");
    m.put("apartment", "Квартира");
    m.put("postalCode", "Индекс");
    m.put("formatted", "Адрес");
    m.put("spdulCode", "Код СПДУЛ");
    m.put("title", "Название");
    m.put("series", "Серия");
    m.put("number", "Номер");
    m.put("issuedBy", "Кем выдан");
    m.put("issueDate", "Дата выдачи");
    m.put("expirationDate", "Действителен до");
    m.put("departmentCode", "Код подразделения");
    m.put("profileKey", "Ключ профиля");
    return m;
  }
}
