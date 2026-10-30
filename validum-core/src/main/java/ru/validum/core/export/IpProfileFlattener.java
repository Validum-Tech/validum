package ru.validum.core.export;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ru.validum.core.organization.IpProfile;

/**
 * Разворачивает {@link IpProfile} в плоскую map.
 *
 * <p>ИП = {@link IndividualProfile} + ОГРНИП. Начинается с колонки {@code ogrnip}, дальше — все
 * колонки ФЛ в том же порядке (переиспользуется {@link IndividualProfileFlattener#flatten}).
 */
public final class IpProfileFlattener {

  private IpProfileFlattener() {}

  public static List<Map<String, String>> flattenAll(List<IpProfile> profiles) {
    return profiles.stream().map(IpProfileFlattener::flatten).toList();
  }

  public static Map<String, String> flatten(IpProfile p) {
    Map<String, String> m = new LinkedHashMap<>();
    m.put("ogrnip", p.ogrnip());
    m.putAll(IndividualProfileFlattener.flatten(p.base()));
    m.put("okved", p.okved());
    m.put("okvedName", p.okvedName());
    return m;
  }
}
