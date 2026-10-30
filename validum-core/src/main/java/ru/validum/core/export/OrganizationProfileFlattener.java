package ru.validum.core.export;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ru.validum.core.address.Address;
import ru.validum.core.bank.Bank;
import ru.validum.core.contact.Contact;
import ru.validum.core.organization.OrganizationProfile;

/**
 * Разворачивает {@link OrganizationProfile} в плоскую map.
 *
 * <p>Классификаторы (ogrn, inn, kpp, okpo, okopf, okfs, okogu, okved, budget, swift) идут
 * отдельными колонками. Адреса и контакты — с префиксами, как у ФЛ.
 */
public final class OrganizationProfileFlattener {

  private OrganizationProfileFlattener() {}

  public static List<Map<String, String>> flattenAll(List<OrganizationProfile> profiles) {
    return profiles.stream().map(OrganizationProfileFlattener::flatten).toList();
  }

  public static Map<String, String> flatten(OrganizationProfile p) {
    Map<String, String> m = new LinkedHashMap<>();
    m.put("organizationName", p.organizationName());
    m.put("organizationType", p.organizationType().name());
    m.put("ogrn", p.ogrn());
    m.put("inn", p.inn());
    m.put("kpp", p.kpp());
    m.put("okpo", p.okpo());
    m.put("okopf", p.okopf());
    m.put("okfs", p.okfs());
    m.put("okogu", p.okogu());
    m.put("okved", p.okved());
    m.put("okvedName", p.okvedName());
    m.put("budget", p.budget());
    m.put("swift", p.swift());
    flattenAddress(m, "registration", p.registrationAddress());
    flattenAddress(m, "post", p.postAddress());
    flattenContact(m, "mobile", p.mobile());
    flattenContact(m, "email", p.email());
    m.put("terrorist", String.valueOf(p.terrorist()));
    m.put("blocked", String.valueOf(p.blocked()));
    flattenBank(m, p.bank());
    m.put("account", p.account());
    return m;
  }

  private static void flattenAddress(Map<String, String> m, String prefix, Address a) {
    if (a == null) return;
    m.put(prefix + "PostalCode", a.postalCode());
    m.put(prefix + "City", a.city());
    m.put(prefix + "Street", a.street());
    m.put(prefix + "Building", a.building());
    m.put(prefix + "Apartment", a.apartment());
    m.put(prefix + "Formatted", a.formatted());
  }

  private static void flattenBank(Map<String, String> m, Bank b) {
    if (b == null) return;
    m.put("bankBik", b.bik());
    m.put("bankName", b.name());
    m.put("bankCorrAccount", b.corrAccount());
    m.put("bankCity", b.city());
    m.put("bankRegion", b.region());
  }

  private static void flattenContact(Map<String, String> m, String prefix, Contact c) {
    if (c == null) return;
    m.put(prefix + "Type", c.type().name());
    m.put(prefix + "Value", c.value());
    m.put(prefix + "Description", c.description());
    m.put(prefix + "Status", c.status());
  }
}
