package ru.validum.core;

import java.time.LocalDate;
import ru.validum.core.bank.BikSource;
import ru.validum.core.bank.BikSources;
import ru.validum.core.profile.IndividualProfile;
import ru.validum.core.profile.IndividualProfileGenerator;

/**
 * Фабрика тестовых профилей: один seed, одна фикстура БИК, одна дата расчёта на весь модуль.
 *
 * <p>Без неё каждый тест повторяет пять строк настройки ({@code seed}, {@code bikSource},
 * {@code asOf}), и расхождение в них превращается в «тест зелёный, а продукт ведёт себя иначе».
 * Для тестов, где нужны особые опции, — {@link #builder(long)} + свои вызовы билдера.
 */
public final class TestProfiles {

  /** Дата расчёта по умолчанию: фиксированная, чтобы golden-фикстуры не зависели от сегодня. */
  public static final LocalDate AS_OF = LocalDate.of(2026, 1, 15);

  private static final BikSource BIK = BikSources.resolve("src/test/resources/bik-sample.xml");

  private TestProfiles() {}

  /** Справочник БИК из фикстуры — для тестов, которым нужен банк или счёт. */
  public static BikSource bik() {
    return BIK;
  }

  /** Билдер с уже выставленными seed, справочником БИК и датой расчёта. */
  public static IndividualProfileGenerator.Builder builder(long seed) {
    return IndividualProfileGenerator.builder()
      .seed(seed)
      .bikSource(BIK)
      .asOf(AS_OF);
  }

  /** Готовый профиль ФЛ на дату {@link #AS_OF}. */
  public static IndividualProfile individual(long seed) {
    return builder(seed).build().generate();
  }

  /** Готовый профиль ФЛ на указанную дату расчёта. */
  public static IndividualProfile individual(long seed, LocalDate asOf) {
    return IndividualProfileGenerator.builder()
      .seed(seed)
      .bikSource(BIK)
      .asOf(asOf)
      .build()
      .generate();
  }
}
