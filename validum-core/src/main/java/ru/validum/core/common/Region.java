package ru.validum.core.common;

/**
 * Регион РФ с данными для генерации согласованного адреса и серии паспорта.
 *
 * <p>Используется каскадом: если {@code region} задан — регистрация, адрес рождения и серия
 * паспорта берутся из одного региона, а не собираются случайно.
 *
 * <p>{@code code} — код субъекта РФ (2 цифры), используется как префикс серии паспорта и КПП.
 *
 * <p>Улица, дом и почтовый индекс в {@code Region} не хранятся: адреса берутся из реального
 * справочника КЛАДР через {@link ru.validum.core.address.AddressBook} (90 субъектов, ~125 тыс.
 * населённых пунктов, ~1,99 млн улиц). {@code code} по-прежнему связывает регион с серией
 * паспорта и КПП.
 */
public enum Region {

  // === Старые 5 регионов ===

  SPB(
    "78",
    "Санкт-Петербург", "кр",
    null, null,
    "Санкт-Петербург", "г"),

  MOSCOW(
    "77",
    "Москва", "г",
    null, null,
    "Москва", "г"),

  MO(
    "50",
    "Московская", "обл",
    null, null,
    "Мытищи", "г"),

  KRASNODAR(
    "23",
    "Краснодарский", "кр",
    null, null,
    "Сочи", "г"),

  SVERDLOVSK(
    "66",
    "Свердловская", "обл",
    null, null,
    "Екатеринбург", "г"),

  // === Новые 10 регионов (P0.2) ===

  LENOBL(
    "47",
    "Ленинградская", "обл",
    null, null,
    "Всеволожск", "г"),

  NOVOSIBIRSK(
    "54",
    "Новосибирская", "обл",
    null, null,
    "Новосибирск", "г"),

  ROSTOV(
    "61",
    "Ростовская", "обл",
    null, null,
    "Ростов-на-Дону", "г"),

  BASHKORTOSTAN(
    "02",
    "Башкортостан", "респ",
    null, null,
    "Уфа", "г"),

  TATARSTAN(
    "16",
    "Татарстан", "респ",
    null, null,
    "Казань", "г"),

  CHELYABINSK(
    "74",
    "Челябинская", "обл",
    null, null,
    "Челябинск", "г"),

  SAMARA(
    "63",
    "Самарская", "обл",
    null, null,
    "Самара", "г"),

  NIZHNY_NOVGOROD(
    "52",
    "Нижегородская", "обл",
    null, null,
    "Нижний Новгород", "г"),

  KRASNOYARSK(
    "24",
    "Красноярский", "кр",
    null, null,
    "Красноярск", "г"),

  PRIMORSKY(
    "25",
    "Приморский", "кр",
    null, null,
    "Владивосток", "г"),

  // === Саратов (по запросу, P0.2) ===

  SARATOV(
    "64",
    "Саратовская", "обл",
    null, null,
    "Саратов", "г");

  private final String code;
  private final String regionName;
  private final String regionType;
  private final String district;
  private final String districtType;
  private final String cityName;
  private final String cityType;

  Region(
    String code,
    String regionName, String regionType,
    String district, String districtType,
    String cityName, String cityType) {
    this.code = code;
    this.regionName = regionName;
    this.regionType = regionType;
    this.district = district;
    this.districtType = districtType;
    this.cityName = cityName;
    this.cityType = cityType;
  }

  public String code() {
    return code;
  }

  public String regionName() {
    return regionName;
  }

  public String regionType() {
    return regionType;
  }

  public String district() {
    return district;
  }

  public String districtType() {
    return districtType;
  }

  public String cityName() {
    return cityName;
  }

  public String cityType() {
    return cityType;
  }

  /** Поиск региона по коду субъекта РФ (2 цифры). {@code null}, если не найден. */
  public static Region byCode(String code) {
    if (code == null) {
      return null;
    }
    String normalized = code.trim();
    for (Region r : values()) {
      if (r.code.equals(normalized)) {
        return r;
      }
    }
    return null;
  }

  /** Поиск региона по имени enum без учёта регистра. {@code null}, если не найден. */
  public static Region byName(String name) {
    if (name == null || name.isBlank()) {
      return null;
    }
    String normalized = name.trim();
    for (Region r : values()) {
      if (r.name().equalsIgnoreCase(normalized)) {
        return r;
      }
    }
    return null;
  }

  /** Регион по имени enum ({@code MOSCOW}) или коду субъекта ({@code 77}). */
  public static Region byNameOrCode(String value) {
    Region byName = byName(value);
    return byName != null ? byName : byCode(value);
  }

  /**
   * То же, что {@link #byNameOrCode(String)}, но с исключением вместо {@code null}.
   *
   * @throws IllegalArgumentException если регион неизвестен
   */
  public static Region require(String value) {
    Region region = byNameOrCode(value);
    if (region == null) {
      throw new IllegalArgumentException("Неизвестный регион: " + value);
    }
    return region;
  }
}
