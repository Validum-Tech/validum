package ru.validum.core.card;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Генератор тестовых карт «Мир».
 *
 * <p>Номер: BIN из диапазона «Мира» (220000–220499), 9 случайных цифр и контрольная цифра Луна —
 * итого 16. Срок действия — от двух до пяти лет вперёд от даты расчёта. Исход: в девяти случаях из
 * десяти успешный, остальные — отказы, распределённые по типам.
 */
public final class MirCardGenerator {

  /** Минимальный BIN «Мира» (включительно). */
  public static final int BIN_MIN = 220_000;

  /** Максимальный BIN «Мира» (включительно). */
  public static final int BIN_MAX = 220_499;

  /** Сколько цифр номера занимает BIN. */
  private static final int BIN_DIGITS = 6;

  /** Полная длина номера карты «Мир». */
  private static final int PAN_LENGTH = 16;

  private static final long SUCCESS_WEIGHT = 90;

  private final Random random;

  public MirCardGenerator(Random random) {
    this.random = random;
  }

  /** Одна карта; срок действия считается от даты расчёта. */
  public MirCard next(LocalDate asOf) {
    return new MirCard(pan(), 1 + random.nextInt(12), asOf.getYear() + 2 + random.nextInt(4),
      outcome());
  }

  /** Несколько карт одного клиента. */
  public List<MirCard> next(LocalDate asOf, int count) {
    List<MirCard> cards = new ArrayList<>(Math.max(0, count));
    for (int i = 0; i < count; i++) {
      cards.add(next(asOf));
    }
    return cards;
  }

  /** Девять успешных из десяти; остальные — равномерно между отказами. */
  private MirCardOutcome outcome() {
    if (random.nextInt(100) < SUCCESS_WEIGHT) {
      return MirCardOutcome.SUCCESS;
    }
    MirCardOutcome[] declines = {
      MirCardOutcome.INSUFFICIENT_FUNDS,
      MirCardOutcome.LIMIT_EXCEEDED,
      MirCardOutcome.CARD_EXPIRED,
      MirCardOutcome.ISSUER_DECLINED,
      MirCardOutcome.INVALID_CARD,
    };
    return declines[random.nextInt(declines.length)];
  }

  /** 16 цифр: BIN + 9 случайных + контрольная цифра Луна. */
  private String pan() {
    StringBuilder sb = new StringBuilder(PAN_LENGTH);
    sb.append(BIN_MIN + random.nextInt(BIN_MAX - BIN_MIN + 1));
    while (sb.length() < PAN_LENGTH - 1) {
      sb.append(random.nextInt(10));
    }
    sb.append(luhnCheckDigit(sb.toString()));
    return sb.toString();
  }

  private static char luhnCheckDigit(String partial) {
    int sum = 0;
    boolean doubleDigit = true;
    for (int i = partial.length() - 1; i >= 0; i--) {
      int digit = partial.charAt(i) - '0';
      if (doubleDigit) {
        digit *= 2;
        if (digit > 9) {
          digit -= 9;
        }
      }
      sum += digit;
      doubleDigit = !doubleDigit;
    }
    return (char) ('0' + (10 - sum % 10) % 10);
  }
}
