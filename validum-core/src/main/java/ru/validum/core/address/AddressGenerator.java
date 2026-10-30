package ru.validum.core.address;

import java.util.Random;
import ru.validum.core.common.Country;
import ru.validum.core.common.Region;
import ru.validum.core.random.DataGenerator;

/**
 * Генератор адресов на реальном справочнике КЛАДР (регионы, населённые пункты, улицы).
 *
 * <p>Данные — {@link AddressBook}: 90 субъектов, ~125 тыс. населённых пунктов, ~1,99 млн улиц.
 * Дом и квартира подставляются процедурно из правдоподобных диапазонов.
 *
 * <ul>
 *   <li>{@link #generateRegistration(Region)} — адрес в заданном регионе (каскад с паспортом и КПП);
 *   <li>{@link #generateRegistration()} — случайный субъект;
 *   <li>{@link #generatePost()} / {@link #generatePost(Region)} — тип {@link AddressType#POST};
 *   <li>{@link #generateCurrent()} — тип {@link AddressType#CURRENT};
 *   <li>{@link #generateBirthPlace()} / {@link #generateBirthPlace(Region)} — {@link
 *       AddressType#BIRTH_PLACE}.
 * </ul>
 *
 * <p>Для АБС, которые валидируют только строку, — {@link #generateFreeForm(AddressType)}.
 */
public final class AddressGenerator implements DataGenerator {

  private final Random random;

  public AddressGenerator() {
    this(DataGenerator.defaultRandom());
  }

  public AddressGenerator(Random random) {
    if (random == null) {
      throw new IllegalArgumentException("random не может быть null");
    }
    this.random = random;
  }

  /** Случайный адрес из случайного субъекта. */
  public Address generate() {
    return generateRegistration();
  }

  /** Адрес регистрации из случайного субъекта. */
  public Address generateRegistration() {
    return AddressBook.address(random, null, AddressType.REGISTRATION);
  }

  /** Адрес регистрации из указанного региона — детерминизм по seed. */
  public Address generateRegistration(Region region) {
    if (region == null) {
      throw new IllegalArgumentException("Регион не может быть null");
    }
    return AddressBook.address(random, region.code(), AddressType.REGISTRATION);
  }

  /** Адрес регистрации по коду субъекта РФ ({@code "77"}, {@code "02"}). */
  public Address generateRegistration(String subjectCode) {
    return AddressBook.address(random, subjectCode, AddressType.REGISTRATION);
  }

  /** Почтовый адрес из случайного субъекта (тип POST). */
  public Address generatePost() {
    return AddressBook.address(random, null, AddressType.POST);
  }

  /** Почтовый адрес из указанного региона (тип POST). */
  public Address generatePost(Region region) {
    if (region == null) {
      throw new IllegalArgumentException("Регион не может быть null");
    }
    return AddressBook.address(random, region.code(), AddressType.POST);
  }

  /** Адрес фактического проживания из случайного субъекта (тип CURRENT). */
  public Address generateCurrent() {
    return AddressBook.address(random, null, AddressType.CURRENT);
  }

  /** Адрес фактического проживания в указанном регионе (тип CURRENT). */
  public Address generateCurrent(Region region) {
    if (region == null) {
      throw new IllegalArgumentException("Регион не может быть null");
    }
    return AddressBook.address(random, region.code(), AddressType.CURRENT);
  }

  /** Место рождения из случайного субъекта (тип BIRTH_PLACE). */
  public Address generateBirthPlace() {
    return AddressBook.address(random, null, AddressType.BIRTH_PLACE).withIsDefault(false);
  }

  /** Место рождения из указанного региона (тип BIRTH_PLACE). */
  public Address generateBirthPlace(Region region) {
    if (region == null) {
      throw new IllegalArgumentException("Регион не может быть null");
    }
    return AddressBook.address(random, region.code(), AddressType.BIRTH_PLACE).withIsDefault(false);
  }

  /** Свободный формат — только строка, без FIAS-разбора. */
  public Address generateFreeForm(AddressType type) {
    return new Address(
      type,
      AddressFormat.FREE_FORM,
      "Действительный",
      true,
      Country.RU,
      null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
      null, null,
      ",,,,,,,,, ,",
      ",,,,,,,,, ,",
      null,
      null);
  }
}
