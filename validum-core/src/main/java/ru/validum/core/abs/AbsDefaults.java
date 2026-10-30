package ru.validum.core.abs;

/**
 * АБС-специфичные константы.
 *
 * <p>Все «магические» значения, которые нужны конкретной банковской системе, но не имеют
 * доменного смысла, собраны здесь. При переходе на другую АБС — заменяется весь класс, а не
 * десятки файлов в ядре.
 */
public final class AbsDefaults {

  // === clientInfo ===
  public static final String SALUTATION = "СашКа";
  public static final String RISK_C = "НИЗ";
  public static final String RISK_COUNTRY = "RU";
  public static final String CRCB_TYPE_INDIVIDUAL = "14";
  public static final String FRGN_STRUCT_NO_JUR_COND = "A";
  public static final String TAX_RESIDENT_C = "Y";

  // === Категории клиента ===
  public static final String CATEGORY_CLIENT    = "КЛИЕНТ";
  public static final String CATEGORY_PENSIONER = "ПЕНСИОНЕР";
  public static final String CATEGORY_SALARY    = "ЗАРПЛАТНИК";

  // === Пакеты услуг ===
  public static final String PACKAGE_BASE     = "БАЗОВЫЙ";
  public static final String PACKAGE_SALARY   = "ЗАРПЛАТНЫЙ";
  public static final String PACKAGE_PENSIONER = "ПЕНСИОННЫЙ";

  // === FIAS-разбор адресов ===
  public static final String FIAS_IFNSFL = "222";
  public static final String FIAS_REGION_TYPE = "кр";
  public static final String FIAS_CITY_TYPE = "г";
  public static final String FIAS_STREET_TYPE = "ул";
  public static final String FIAS_BUILDING_TYPE = "д.";
  public static final String FIAS_PLAN_TYPE_REG = "зона";
  public static final String FIAS_PLAN_TYPE_FACT = "р-н";
  public static final String FIAS_STREET_AOGUID =
    "2ff78308-f866-4b05-88f0-e9f39c4fec65";

  // === CRS / FATCA ===
  public static final String STATUS_CRS_RESIDENT = "2";
  public static final String STATUS_CRS_NON_RESIDENT = "1";
  public static final String STATUS_FATCA = "2";
  public static final String CONFIRM_STATUS = "N";
  public static final String RCR_CODE_CRS = "01";
  public static final String IDENT_SERTIFICATE = "0";
  public static final String W8BEN = "0";

  // === Идентификаторы и согласия ===
  public static final String INTERNET_BANK = "Y";
  public static final String EXT_SOURCE = "СОЗДАНО ЧЕРЕЗ АВТОТЕСТЫ";
  public static final String AGREE_PERS = "Y";
  public static final String AGREE_ADV = "Y";
  public static final String AGREE_FATCA = "Y";
  public static final String AGREE_OFFER = "Y";

  private AbsDefaults() {}
}
