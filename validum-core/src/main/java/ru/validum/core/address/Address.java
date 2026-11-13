package ru.validum.core.address;

import ru.validum.core.common.Country;

/**
 * Адрес.
 *
 * <p>Один record на все случаи жизни: регистрация, почтовый, фактический, место рождения, адрес
 * за границей. Тип определяется полем {@link #type}, формат — {@link #format}.
 *
 * <p>В режиме {@link AddressFormat#FIAS} заполнены все FIAS-части. В режиме {@link
 * AddressFormat#FREE_FORM} — только {@link #expectedAddress}.
 */
public record Address(
  AddressType type,
  AddressFormat format,
  String status,
  boolean isDefault,

  // Страна и индекс
  Country country,
  String postalCode,

  // Регион
  String region,
  String regionType,

  // Район
  String district,
  String districtType,

  // Город
  String city,
  String cityType,

  // Населённый пункт
  String locality,
  String localityType,

  // Планировочная структура (для Сочи, зон и т.п.)
  String planElement,
  String planElementType,

  // Улица
  String street,
  String streetType,

  // Дом
  String building,
  String buildingType,

  // Квартира
  String apartment,
  String apartmentType,

  // Готовые строки
  String expectedAddress,
  String fiasAddress,

  // Служебные FIAS
  String fiasIfnsfl,
  String fiasStreetAoguid) {

  /** Удобный доступ к полной строке адреса. */
  public String formatted() {
    return expectedAddress;
  }

  /** Копия адреса с другим типом. */
  public Address withType(AddressType newType) {
    return new Address(
      newType, format, status, isDefault, country, postalCode,
      region, regionType, district, districtType, city, cityType,
      locality, localityType, planElement, planElementType,
      street, streetType, building, buildingType, apartment, apartmentType,
      expectedAddress, fiasAddress, fiasIfnsfl, fiasStreetAoguid);
  }

  /** Копия адреса с другой готовой строкой и индексом (нужно для намеренной порчи формата). */
  public Address withExpected(String newPostalCode, String newExpectedAddress) {
    return new Address(
      type, format, status, isDefault, country, newPostalCode,
      region, regionType, district, districtType, city, cityType,
      locality, localityType, planElement, planElementType,
      street, streetType, building, buildingType, apartment, apartmentType,
      newExpectedAddress, fiasAddress, fiasIfnsfl, fiasStreetAoguid);
  }

  /** Копия адреса с другим флагом «по умолчанию». */
  public Address withIsDefault(boolean newIsDefault) {
    return new Address(
      type, format, status, newIsDefault, country, postalCode,
      region, regionType, district, districtType, city, cityType,
      locality, localityType, planElement, planElementType,
      street, streetType, building, buildingType, apartment, apartmentType,
      expectedAddress, fiasAddress, fiasIfnsfl, fiasStreetAoguid);
  }
}
