package ru.validum.core.address;

import java.util.List;
import java.util.Random;
import ru.validum.core.common.Country;

/**
 * Адреса за пределами России — для нерезидентов.
 *
 * <p>Улицы и города взяты как типовые для страны: это синтетические данные, а не выгрузка из
 * национального реестра. Индекс собирается по маске страны так, чтобы он выглядел правдоподобно
 * (у Казахстана и Узбекистана — шестизначный, у Германии и США — пятизначный), но не выдаётся за
 * реальный.
 *
 * <p>Страна без своих данных (например, Таджикистан) получает нейтральный набор: столицу в
 * качестве города и шестизначный индекс.
 */
final class ForeignAddresses {

  /** Маска индекса: {@code #} заменяется цифрой, остальное — как есть. */
  private record Locality(String city, String zipMask, List<String> streets) {}

  private static final Locality DEFAULT =
    new Locality("Столица", "######", List.of("Центральная ул.", "ул. Мира"));

  private static final List<String> KZ_STREETS =
    List.of("пр. Достык", "ул. Абая", "ул. Сатпаева", "пр. Аль-Фараби");
  private static final List<String> BY_STREETS =
    List.of("пр. Независимости", "ул. Немига", "ул. Ленина", "ул. Куйбышева");
  private static final List<String> UZ_STREETS =
    List.of("ул. Амира Темура", "ул. Навои", "ул. Шота Руставели");
  private static final List<String> KG_STREETS = List.of("пр. Манаса", "ул. Чуй", "ул. Киевская");
  private static final List<String> AM_STREETS =
    List.of("ул. Абовяна", "пр. Маштоца", "ул. Туманяна");
  private static final List<String> GE_STREETS =
    List.of("пр. Руставели", "ул. Бараташвили", "ул. Чавчавадзе");
  private static final List<String> UA_STREETS =
    List.of("ул. Хрещатик", "ул. Сагайдачного", "пр. Свободы");
  private static final List<String> DE_STREETS =
    List.of("Hauptstraße", "Berliner Straße", "Lindenallee");
  private static final List<String> US_STREETS =
    List.of("Main Street", "5th Avenue", "Oak Lane");

  private static Locality locality(Country country) {
    return switch (country) {
      case KZ -> new Locality("Алматы", "05####", KZ_STREETS);
      case BY -> new Locality("Минск", "22####", BY_STREETS);
      case UZ -> new Locality("Ташкент", "10####", UZ_STREETS);
      case KG -> new Locality("Бишкек", "72####", KG_STREETS);
      case AM -> new Locality("Ереван", "00####", AM_STREETS);
      case GE -> new Locality("Тбилиси", "01####", GE_STREETS);
      case UA -> new Locality("Киев", "01####", UA_STREETS);
      case DE -> new Locality("Берлин", "10###", DE_STREETS);
      case US -> new Locality("Нью-Йорк", "10###", US_STREETS);
      default -> DEFAULT;
    };
  }

  private ForeignAddresses() {}

  /**
   * Адрес в стране нерезидента.
   *
   * @param country страна проживания
   * @param type тип адреса (регистрация, почтовый, фактический)
   */
  static Address of(Country country, AddressType type, Random random) {
    Locality locality = locality(country);
    String city = locality.city();
    String street = locality.streets().get(random.nextInt(locality.streets().size()));
    String postalCode = zip(locality.zipMask(), random);
    String building = String.valueOf(1 + random.nextInt(120));
    String apartment = String.valueOf(1 + random.nextInt(300));

    String expected =
      country.countryName() + ", г. " + city + ", " + street + ", д. " + building
        + ", кв. " + apartment + ", " + postalCode;

    return new Address(
      type,
      AddressFormat.FREE_FORM,
      "Действующий",
      false,
      country,
      postalCode,
      country.countryName(), null,
      null, null,
      city, "г.",
      null, null,
      null, null,
      street, "ул.",
      building, "д.",
      apartment, "кв.",
      expected,
      null,
      null,
      null);
  }

  private static String zip(String mask, Random random) {
    StringBuilder sb = new StringBuilder(mask.length());
    for (char c : mask.toCharArray()) {
      sb.append(c == '#' ? (char) ('0' + random.nextInt(10)) : c);
    }
    return sb.toString();
  }
}
