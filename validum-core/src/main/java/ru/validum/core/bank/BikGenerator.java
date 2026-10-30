package ru.validum.core.bank;

import java.util.List;
import java.util.Random;
import ru.validum.core.random.DataGenerator;

/**
 * Генератор банковских реквизитов из справочника.
 *
 * <p>Работает с любым источником {@link BikSource}. По умолчанию — справочник из
 * {@link BikSources#defaultSource()} (файл ED807 из {@code --file}, {@code VALIDUM_BIK_FILE},
 * свойства {@code validum.bik.file} или {@code data/ED807.xml}); если файла нет, генерация
 * бросает {@link BikSourceNotFoundException}. Встроенного демо-набора в продукте нет.
 */
public final class BikGenerator implements DataGenerator {

  private final Random random;
  private final BikSource source;

  public BikGenerator() {
    this(DataGenerator.defaultRandom());
  }

  public BikGenerator(Random random) {
    this(random, BikSources.defaultSource());
  }

  public BikGenerator(Random random, BikSource source) {
    this.random = random;
    this.source = source;
  }

  /** Случайный банк из справочника. */
  public Bank generateBank() {
    List<Bank> banks = source.banks();
    return banks.get(random.nextInt(banks.size()));
  }

  /** Случайный БИК из справочника. */
  public String generateBik() {
    return generateBank().bik();
  }

  /** Случайный корсчёт из справочника. */
  public String generateCorrAccount() {
    return generateBank().corrAccount();
  }
}
