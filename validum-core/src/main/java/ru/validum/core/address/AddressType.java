package ru.validum.core.address;

/** Тип адреса. Соответствует `addrTypeC` в SQL АБС. */
public enum AddressType {
  /** Адрес регистрации (REG). */
  REGISTRATION,

  /** Почтовый адрес (POST). */
  POST,

  /** Адрес фактического проживания (FACT). */
  CURRENT,

  /** Адрес государственной регистрации (для юрлиц). */
  STATE_REGISTRATION,

  /** Место рождения. */
  BIRTH_PLACE,

  /** Адрес за границей. */
  FOREIGN,

  /** Адрес места работы. */
  WORK,

  /** Адрес доставки. */
  DELIVERY
}
