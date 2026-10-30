package ru.validum.core.organization;

import java.util.Random;
import ru.validum.core.name.PersonName;
import ru.validum.core.ogrn.OgrnGenerator;
import ru.validum.core.okved.OkvedEntry;
import ru.validum.core.okved.OkvedGenerator;
import ru.validum.core.profile.IndividualProfile;
import ru.validum.core.profile.IndividualProfileGenerator;
import ru.validum.core.random.DataGenerator;

/**
 * Генератор профиля ИП — композиция над {@link IndividualProfileGenerator}.
 *
 * <p>Что делает:
 *
 * <ul>
 *   <li>берёт готовый {@link IndividualProfile} (через builder ФЛ);
 *   <li>подменяет отчество на {@link #IP_PATRONYMIC};
 *   <li>добавляет ОГРНИП (15 цифр, валидный по алгоритму ФНС).
 * </ul>
 *
 * <p>Для воспроизводимости используйте перегрузки с {@code long seed}: одинаковый seed даёт
 * идентичный ОГРНИП.
 *
 * <pre>{@code
 * // Из готового профиля ФЛ с явным seed
 * IndividualProfile base = IndividualProfileGenerator.builder()
 *     .seed(42L).age(35).build().generate();
 * IpProfile ip = IpProfileGenerator.from(base, 42L);
 *
 * // Сразу из генератора с seed
 * IpProfile ip = IpProfileGenerator.generate(
 *     IndividualProfileGenerator.builder().seed(42L).age(35).build(), 42L);
 * }</pre>
 */
public final class IpProfileGenerator {

  /** Фиксированное отчество ИП — узнаваемо в UI и логах. */
  public static final String IP_PATRONYMIC = "ИП Автотеста";

  private IpProfileGenerator() {}

  // ── Из готового профиля ФЛ ───────────────────────────────────────────────

  /** Из готового профиля ФЛ — ОГРНИП и ОКВЭД случайны (не воспроизводимо). */
  public static IpProfile from(IndividualProfile base) {
    Random random = DataGenerator.defaultRandom();
    return from(base, new OgrnGenerator(random), new OkvedGenerator(random));
  }

  /** Из готового профиля ФЛ с seed — ОГРНИП и ОКВЭД воспроизводимы. */
  public static IpProfile from(IndividualProfile base, long seed) {
    Random random = DataGenerator.seededRandom(seed);
    return from(base, new OgrnGenerator(random), new OkvedGenerator(random));
  }

  /** Из готового профиля ФЛ с явным генератором ОГРНИП (ОКВЭД случаен). */
  public static IpProfile from(IndividualProfile base, OgrnGenerator ogrnGen) {
    return from(base, ogrnGen, new OkvedGenerator());
  }

  /** Полный вариант: явные генераторы ОГРНИП и ОКВЭД. */
  public static IpProfile from(
      IndividualProfile base, OgrnGenerator ogrnGen, OkvedGenerator okvedGen) {
    if (okvedGen == null) {
      throw new IllegalArgumentException("OkvedGenerator не может быть null");
    }
    return from(base, ogrnGen, okvedGen.generate());
  }

  /** С явным кодом ОКВЭД (проверяется по справочнику) — ОГРНИП воспроизводим по seed. */
  public static IpProfile from(IndividualProfile base, long seed, String okvedCode) {
    Random random = DataGenerator.seededRandom(seed);
    return from(
      base, new OgrnGenerator(random), new OkvedGenerator(random).require(okvedCode));
  }

  /** Ядро сборки: явная запись ОКВЭД. */
  public static IpProfile from(
      IndividualProfile base, OgrnGenerator ogrnGen, OkvedEntry okved) {
    if (base == null) {
      throw new IllegalArgumentException("IndividualProfile не может быть null");
    }
    if (ogrnGen == null) {
      throw new IllegalArgumentException("OgrnGenerator не может быть null");
    }
    if (okved == null) {
      throw new IllegalArgumentException("OkvedEntry не может быть null");
    }
    PersonName ipName =
      new PersonName(
        base.name().lastName(),
        base.name().firstName(),
        IP_PATRONYMIC,
        base.name().gender());
    IndividualProfile withIpName = base.withName(ipName);
    String ogrnip = ogrnGen.generateIndividualEntrepreneur();
    return new IpProfile(withIpName, ogrnip, okved.code(), okved.name());
  }

  /** Из настроенного генератора с seed и явным кодом ОКВЭД. */
  public static IpProfile generate(
      IndividualProfileGenerator generator, long seed, String okvedCode) {
    if (generator == null) {
      throw new IllegalArgumentException("IndividualProfileGenerator не может быть null");
    }
    return from(generator.generate(), seed, okvedCode);
  }

  // ── Из настроенного генератора ФЛ ────────────────────────────────────────

  /** Из настроенного генератора — ОГРНИП случаен (не воспроизводимо). */
  public static IpProfile generate(IndividualProfileGenerator generator) {
    if (generator == null) {
      throw new IllegalArgumentException("IndividualProfileGenerator не может быть null");
    }
    return from(generator.generate());
  }

  /** Из настроенного генератора с seed — воспроизводимо. */
  public static IpProfile generate(IndividualProfileGenerator generator, long seed) {
    if (generator == null) {
      throw new IllegalArgumentException("IndividualProfileGenerator не может быть null");
    }
    return from(generator.generate(), seed);
  }
}
