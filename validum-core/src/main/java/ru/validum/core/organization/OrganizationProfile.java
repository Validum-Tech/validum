package ru.validum.core.organization;

import ru.validum.core.address.Address;
import ru.validum.core.bank.Bank;
import ru.validum.core.common.OrganizationType;
import ru.validum.core.contact.Contact;

/**
 * Связанный профиль клиента — юридического лица.
 *
 * <p>Все поля согласованы:
 *
 * <ul>
 *   <li>ОГРН и ИНН — валидны по алгоритмам ФНС;
 *   <li>КПП начинается с кода региона регистрации;
 *   <li>OKOPF соответствует {@link OrganizationType};
 *   <li>адреса, контакты — консистентны с регионом.
 * </ul>
 *
 * <p>Флаги {@link #terrorist} и {@link #blocked} — как у ФЛ.
 */
public record OrganizationProfile(
  // === Идентификация ===
  String organizationName,
  OrganizationType organizationType,

  // === Классификаторы ===
  String ogrn,
  String inn,
  String kpp,
  String okpo,
  String okopf,
  String okfs,
  String okogu,
  String okved,
  String okvedName,
  String budget,
  String swift,

  // === Адреса ===
  Address registrationAddress,
  Address postAddress,

  // === Контакты ===
  Contact mobile,
  Contact email,

  // === Флаги ===
  boolean terrorist,
  boolean blocked,

  // === Банк и счёт (P2.4) ===
  Bank bank,
  String account) {}
