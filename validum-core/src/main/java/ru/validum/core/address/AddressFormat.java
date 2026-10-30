package ru.validum.core.address;

/** Формат адреса. */
public enum AddressFormat {
  /** Адрес из справочника ФИАС — фиксированный набор частей. */
  FIAS,

  /** Свободный формат — только строка, без разбора. */
  FREE_FORM
}
