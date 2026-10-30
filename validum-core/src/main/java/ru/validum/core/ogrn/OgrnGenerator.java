package ru.validum.core.ogrn;

import java.util.Random;
import ru.validum.core.random.DataGenerator;

/**
 * Генератор ОГРН (13 цифр) и ОГРНИП (15 цифр).
 *
 * <p>Контрольная цифра: остаток от деления числа без контрольной на 11 (ОГРН) или 13 (ОГРНИП),
 * последняя цифра результата.
 */
public final class OgrnGenerator implements DataGenerator {

  private final Random random;

  public OgrnGenerator() {
    this(DataGenerator.defaultRandom());
  }

  public OgrnGenerator(Random random) {
    this.random = random;
  }

  /** Генерирует ОГРН юридического лица (13 цифр). */
  public String generateLegalEntity() {
    int sign = random.nextBoolean() ? 1 : 5;
    int year = 20 + random.nextInt(5);
    int region = 10 + random.nextInt(89);
    int inspection = 10 + random.nextInt(89);
    int record = 10_000 + random.nextInt(90_000);
    String base = String.format("%d%02d%02d%02d%05d", sign, year, region, inspection, record);
    long numeric = Long.parseLong(base);
    int control = (int) (numeric % 11 % 10);
    return base + control;
  }

  /** Генерирует ОГРНИП индивидуального предпринимателя (15 цифр). */
  public String generateIndividualEntrepreneur() {
    int sign = 3;
    int year = 20 + random.nextInt(5);
    int region = 10 + random.nextInt(89);
    int inspection = 10 + random.nextInt(89);
    int record = 1_000_000 + random.nextInt(9_000_000);
    String base = String.format("%d%02d%02d%02d%07d", sign, year, region, inspection, record);
    long numeric = Long.parseLong(base);
    int control = (int) (numeric % 13 % 10);
    return base + control;
  }
}
