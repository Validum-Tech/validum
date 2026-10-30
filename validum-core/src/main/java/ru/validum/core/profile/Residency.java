package ru.validum.core.profile;

import ru.validum.core.common.Country;

/**
 * Резидентство и гражданство клиента.
 *
 * @param isResident резидент страны пребывания?
 * @param residentCountry страна проживания
 * @param citizenship гражданство
 * @param taxResidentCountry страна налогового резидентства
 */
public record Residency(
  boolean isResident, Country residentCountry, Country citizenship, Country taxResidentCountry) {

  /** Дефолт для MVP — резидент РФ по всем полям. */
  public static Residency residentRU() {
    return new Residency(true, Country.RU, Country.RU, Country.RU);
  }

  /** Нерезидент — все поля совпадают с указанной страной. */
  public static Residency nonResident(Country country) {
    return new Residency(false, country, country, country);
  }
}
