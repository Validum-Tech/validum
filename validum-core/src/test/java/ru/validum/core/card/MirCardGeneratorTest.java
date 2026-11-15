package ru.validum.core.card;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Карта «Мир»: BIN, Луна, срок действия и распределение исходов. */
class MirCardGeneratorTest {

  private static final LocalDate AS_OF = LocalDate.of(2026, 1, 15);

  @Test
  @DisplayName("Номер в BIN «Мира», 16 цифр и проходит Луна")
  void pansAreValid() {
    MirCardGenerator gen = new MirCardGenerator(new Random(42));

    for (int i = 0; i < 1_000; i++) {
      MirCard card = gen.next(AS_OF);
      assertThat(card.pan()).hasSize(16).startsWith("220");
      int bin = Integer.parseInt(card.pan().substring(0, 6));
      assertThat(bin).isBetween(MirCardGenerator.BIN_MIN, MirCardGenerator.BIN_MAX);
      assertThat(card.luhnValid()).as("Луна для %s", card.pan()).isTrue();
    }
  }

  @Test
  @DisplayName("Срок действия в будущем относительно даты расчёта")
  void expiryIsInFuture() {
    MirCardGenerator gen = new MirCardGenerator(new Random(7));

    for (int i = 0; i < 200; i++) {
      MirCard card = gen.next(AS_OF);
      assertThat(card.expiryMonth()).isBetween(1, 12);
      assertThat(card.expiryYear()).isBetween(AS_OF.getYear() + 2, AS_OF.getYear() + 5);
      assertThat(card.expiredAt(AS_OF)).isFalse();
      assertThat(card.expiry()).matches("\\d{2}/\\d{2}");
    }
  }

  @Test
  @DisplayName("Исходы: успех доминирует, отказы тоже встречаются")
  void outcomesAreMixed() {
    MirCardGenerator gen = new MirCardGenerator(new Random(11));
    List<MirCard> cards = new ArrayList<>();
    for (int i = 0; i < 1_000; i++) {
      cards.add(gen.next(AS_OF));
    }

    long success = cards.stream().filter(c -> c.outcome() == MirCardOutcome.SUCCESS).count();
    EnumSet<MirCardOutcome> seen = EnumSet.noneOf(MirCardOutcome.class);
    cards.forEach(c -> seen.add(c.outcome()));

    assertThat(success).isBetween(800L, 960L);
    assertThat(seen.size()).isGreaterThan(1);
    assertThat(MirCardOutcome.INSUFFICIENT_FUNDS.russianName()).isEqualTo("недостаточно средств");
  }

  @Test
  @DisplayName("Пачка карт одного клиента")
  void generatesBatch() {
    MirCardGenerator gen = new MirCardGenerator(new Random(3));
    List<MirCard> cards = gen.next(AS_OF, 3);
    assertThat(cards).hasSize(3).allSatisfy(c -> assertThat(c.luhnValid()).isTrue());
    assertThat(gen.next(AS_OF, 0)).isEmpty();
  }
}
