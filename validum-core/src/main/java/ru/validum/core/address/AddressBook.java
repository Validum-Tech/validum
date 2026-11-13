package ru.validum.core.address;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import ru.validum.core.abs.AbsDefaults;
import ru.validum.core.common.Country;
import ru.validum.core.common.Region;

/**
 * Реальный справочник адресов РФ: регионы → населённые пункты → улицы.
 *
 * <p>Источник — КЛАДР (ФНС, выгрузка {@code base.7z} от 2026-10-02, лицензия открытых данных). В jar
 * лежит компактный срез: {@code addresses/subjects.tsv} (субъекты) и по одному gzip-файлу на
 * субъект {@code addresses/XX.tsv.gz} — всего около 1,99 млн улиц и 125 тыс. населённых пунктов.
 * Дома и квартиры генерируются процедурно (КЛАДР их содержит неполно), что даёт реалистичный
 * номер без раздувания ресурсов.
 *
 * <p>Загрузка ленивая и по субъекту: файл читается при первом обращении к этому региону, поэтому
 * память занимает только то, что реально используется.
 */
public final class AddressBook {

  private static final String SUBJECTS_RESOURCE = "/addresses/subjects.tsv";
  private static final String SUBJECT_RESOURCE_TEMPLATE = "/addresses/%s.tsv.gz";

  private static final Map<String, List<Place>> CACHE = new ConcurrentHashMap<>();
  private static final List<Subject> SUBJECTS = loadSubjects();

  private AddressBook() {}

  /** Субъект РФ: код (2 цифры), короткое имя («Москва»), аббревиатура и полное имя типа. */
  public record Subject(String code, String name, String typeAbbr, String typeName) {

    /** Как печатать в адресе: {@code «Москва г»}. */
    public String display() {
      return typeAbbr.isEmpty() ? name : name + " " + typeAbbr;
    }
  }

  /** Улица: название, тип ({@code ул}, {@code пр-кт}), почтовый индекс (может быть пустым). */
  public record Street(String name, String type, String index) {

    public String display() {
      return type.isEmpty() ? name : name + " " + type;
    }
  }

  /** Населённый пункт: имя, тип ({@code г}, {@code п}), район, индекс и улицы. */
  public record Place(
    String name, String type, String district, String index, List<Street> streets) {

    public String display() {
      return type.isEmpty() ? name : name + " " + type;
    }
  }

  /** Все субъекты, для которых есть срез. */
  public static List<Subject> subjects() {
    return SUBJECTS;
  }

  /** Субъект по коду (2 цифры). */
  public static Subject subject(String code) {
    for (Subject s : SUBJECTS) {
      if (s.code().equals(code)) {
        return s;
      }
    }
    throw new IllegalArgumentException("Субъект РФ не найден в справочнике: '" + code + "'");
  }

  /** Случайный субъект с непустым срезом. */
  public static Subject randomSubject(Random random) {
    return SUBJECTS.get(random.nextInt(SUBJECTS.size()));
  }

  /** Населённые пункты субъекта — читаются один раз и кешируются. */
  public static List<Place> places(String subjectCode) {
    return CACHE.computeIfAbsent(subjectCode, AddressBook::loadPlaces);
  }

  /** Сколько населения пунктов и улиц в срезе — для диагностики и тестов. */
  public static int placeCount() {
    int total = 0;
    for (Subject s : SUBJECTS) {
      total += places(s.code()).size();
    }
    return total;
  }

  /** Количество улиц в срезе — для диагностики и тестов. */
  public static int streetCount() {
    int total = 0;
    for (Subject s : SUBJECTS) {
      for (Place p : places(s.code())) {
        total += p.streets().size();
      }
    }
    return total;
  }

  /**
   * Адрес из реального справочника: субъект → населённый пункт → улица, дом и квартира процедурно.
   *
   * @param random источник случайности (детерминизм по seed)
   * @param subjectCode код субъекта или {@code null} — тогда субъект случайный
   * @param type тип адреса (регистрация, почтовый, место рождения…)
   */
  public static Address address(Random random, String subjectCode, AddressType type) {
    Subject subject = subjectCode == null ? randomSubject(random) : subject(subjectCode);
    List<Place> places = places(subject.code());
    if (places.isEmpty()) {
      throw new IllegalStateException(
        "Срез адресов пуст для субъекта " + subject.code() + " (" + subject.name() + ")");
    }
    Place place = places.get(random.nextInt(places.size()));
    Street street = place.streets().get(random.nextInt(place.streets().size()));

    String postalCode = firstNonBlank(street.index(), place.index(), proceduralIndex(random));
    String building = String.valueOf(1 + random.nextInt(150));
    String apartment = String.valueOf(1 + random.nextInt(300));
    // Человекочитаемо: «196240, Санкт-Петербург г, г. Пушкин, ул. Госпитальная, д. 12, кв. 5».
    // Пустые части не печатаем, тип населённого пункта/улицы ставим ПЕРЕД названием,
    // как принято в русских адресах (раньше получалось «Катышка д,,1 Мая ул»).
    String formatted =
      humanAddress(
        postalCode,
        subject.display(),
        subject.name(),
        place.type(),
        place.name(),
        street.type(),
        street.name(),
        building,
        apartment);

    return new Address(
      type,
      AddressFormat.FIAS,
      "Действительный",
      true,
      Country.RU,
      postalCode,
      subject.name(),
      subject.typeAbbr(),
      place.district(),
      "",
      place.name(),
      place.type(),
      null,
      null,
      null,
      null,
      street.name(),
      street.type(),
      building,
      AbsDefaults.FIAS_BUILDING_TYPE,
      apartment,
      null,
      formatted,
      formatted,
      AbsDefaults.FIAS_IFNSFL,
      AbsDefaults.FIAS_STREET_AOGUID);
  }

  /**
   * Русский порядок частей: «индекс, субъект, г. НП, ул. Улица, д. N, кв. M». Пустое — пропускаем,
   * чтобы строка не выглядела битой ни в Excel, ни в отчёте {@code describe}.
   */
  static String humanAddress(
    String postalCode,
    String subject,
    String subjectName,
    String placeType,
    String placeName,
    String streetType,
    String streetName,
    String building,
    String apartment) {
    List<String> parts = new ArrayList<>(6);
    add(parts, postalCode);
    add(parts, subject);
    // «Москва г» + «г. Москва» — один и тот же город, второй раз не печатаем.
    if (placeName != null
      && !placeName.isBlank()
      && (subjectName == null || !placeName.equalsIgnoreCase(subjectName))) {
      add(parts, withType(placeType, placeName));
    }
    if (streetName != null && !streetName.isBlank()) {
      add(parts, withType(streetType, streetName));
    }
    if (building != null && !building.isBlank()) {
      add(parts, "д. " + building);
    }
    if (apartment != null && !apartment.isBlank()) {
      add(parts, "кв. " + apartment);
    }
    return String.join(", ", parts);
  }

  private static void add(List<String> parts, String value) {
    if (value != null && !value.isBlank()) {
      parts.add(value);
    }
  }

  private static String withType(String type, String name) {
    if (type == null || type.isBlank()) {
      return name;
    }
    // В справочнике КЛАДР тип иногда уже с точкой («кп.», «с.») — вторую не добавляем,
    // иначе получается «кп.. Горные Ключи».
    String prefix = type.endsWith(".") ? type : type + ".";
    return prefix + " " + name;
  }

  // ── Внутреннее ───────────────────────────────────────────────────────────

  private static String firstNonBlank(String... values) {
    for (String v : values) {
      if (v != null && !v.isBlank()) {
        return v;
      }
    }
    return "";
  }

  private static String proceduralIndex(Random random) {
    // В срезе индексы есть не у всех улиц; правдоподобный шестизначный индекс для остальных.
    return String.format("%06d", 100000 + random.nextInt(899999));
  }

  private static List<Subject> loadSubjects() {
    try (InputStream in = AddressBook.class.getResourceAsStream(SUBJECTS_RESOURCE)) {
      if (in == null) {
        throw new IllegalStateException("Ресурс справочника адресов не найден: " + SUBJECTS_RESOURCE);
      }
      List<Subject> result = new ArrayList<>();
      try (BufferedReader reader =
             new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          if (line.isBlank()) {
            continue;
          }
          String[] f = line.split("\t", -1);
          if (f.length < 4) {
            throw new IllegalStateException("Битая строка subjects.tsv: " + line);
          }
          Region region = Region.byCode(f[0]);
          if (region != null) {
            // КЛАДР хранит исторические названия («Ленинград», «Горьковская») — берём из Region.
            result.add(new Subject(f[0], region.regionName(), f[2], f[3]));
          } else {
            result.add(new Subject(f[0], f[1], f[2], f[3]));
          }
        }
      }
      if (result.isEmpty()) {
        throw new IllegalStateException("Справочник адресов пуст: " + SUBJECTS_RESOURCE);
      }
      return List.copyOf(result);
    } catch (IOException e) {
      throw new UncheckedIOException("Не удалось прочитать " + SUBJECTS_RESOURCE, e);
    }
  }

  private static List<Place> loadPlaces(String subjectCode) {
    String resource = String.format(SUBJECT_RESOURCE_TEMPLATE, subjectCode);
    try (InputStream raw = AddressBook.class.getResourceAsStream(resource)) {
      if (raw == null) {
        return List.of();
      }
      List<Place> places = new ArrayList<>();
      String placeName = null;
      String placeType = null;
      String district = null;
      String index = null;
      List<Street> streets = null;
      try (BufferedReader reader =
             new BufferedReader(
               new InputStreamReader(new GZIPInputStream(raw), StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          if (line.isEmpty()) {
            continue;
          }
          String[] f = line.split("\t", -1);
          if ("L".equals(f[0])) {
            if (placeName != null) {
              places.add(
                new Place(placeName, placeType, district, index, List.copyOf(streets)));
            }
            placeName = f[1];
            placeType = f[2];
            district = f[3];
            index = f[4];
            streets = new ArrayList<>();
          } else if ("S".equals(f[0]) && streets != null) {
            streets.add(new Street(f[1], f[2], f[3]));
          }
        }
        if (placeName != null) {
          places.add(new Place(placeName, placeType, district, index, List.copyOf(streets)));
        }
      }
      return List.copyOf(places);
    } catch (IOException e) {
      throw new UncheckedIOException("Не удалось прочитать " + resource, e);
    }
  }
}
