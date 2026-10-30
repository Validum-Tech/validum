package ru.validum.core.common;

/**
 * Страна с ISO-кодом и кодом ОКСМ (Общероссийский классификатор стран мира).
 *
 * <p>ОКСМ используется в АБС для полей страны рождения, гражданства, резидентства.
 */
public enum Country {
  RU("RU", "Российская Федерация", "643"),
  TJ("TJ", "Таджикистан", "762"),
  KZ("KZ", "Казахстан", "398"),
  UZ("UZ", "Узбекистан", "860"),
  KG("KG", "Киргизия", "417"),
  BY("BY", "Беларусь", "112"),
  UA("UA", "Украина", "804"),
  AM("AM", "Армения", "051"),
  GE("GE", "Грузия", "268"),
  DE("DE", "Германия", "276"),
  US("US", "США", "840");

  private final String isoCode;
  private final String name;
  private final String oksmCode;

  Country(String isoCode, String name, String oksmCode) {
    this.isoCode = isoCode;
    this.name = name;
    this.oksmCode = oksmCode;
  }

  public String isoCode() {
    return isoCode;
  }

  public String countryName() {
    return name;
  }

  public String oksmCode() {
    return oksmCode;
  }

  @Override
  public String toString() {
    return isoCode;
  }
}
