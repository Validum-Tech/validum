package ru.validum.core.organization;

import java.util.Random;
import ru.validum.core.random.DataGenerator;

/**
 * Генератор ОКПО (Общероссийский классификатор предприятий и организаций).
 *
 * <p>Для ЮЛ — 8 цифр (7 значащих + 1 контрольная). Для ИП — 10 цифр (9 + 1).
 *
 * <p>Контрольная цифра: сумма значащих цифр по модулю 11. Если результат равен 10 — берётся 0.
 */
public final class OkpoGenerator implements DataGenerator {

  private final Random random;

  public OkpoGenerator() {
    this(DataGenerator.defaultRandom());
  }

  public OkpoGenerator(Random random) {
    this.random = random;
  }

  /** ОКПО для ЮЛ — 8 цифр. */
  public String generateLegalEntity() {
    StringBuilder sb = new StringBuilder(7);
    int sum = 0;
    for (int i = 0; i < 7; i++) {
      int digit = random.nextInt(10);
      sb.append(digit);
      sum += digit;
    }
    int control = sum % 11;
    if (control == 10) {
      control = 0;
    }
    return sb.append(control).toString();
  }

  /** ОКПО для ИП — 10 цифр. */
  public String generateIndividualEntrepreneur() {
    StringBuilder sb = new StringBuilder(9);
    int sum = 0;
    for (int i = 0; i < 9; i++) {
      int digit = random.nextInt(10);
      sb.append(digit);
      sum += digit;
    }
    int control = sum % 11;
    if (control == 10) {
      control = 0;
    }
    return sb.append(control).toString();
  }
}
