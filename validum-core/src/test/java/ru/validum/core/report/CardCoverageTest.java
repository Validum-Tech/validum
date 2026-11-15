package ru.validum.core.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.TestProfiles;
import ru.validum.core.card.MirCardGenerator;
import ru.validum.core.export.IndividualProfileFlattener;

/** Проверка номера карты в отчёте покрытия. */
class CardCoverageTest {

  @Test
  @DisplayName("cardPan проверяется: Луна и BIN «Мира»")
  void cardPanIsChecked() {
    String pan = new MirCardGenerator(new Random(1))
      .next(java.time.LocalDate.of(2026, 1, 15)).pan();

    assertThat(Coverage.isChecked("cardPan")).isTrue();
    assertThat(Coverage.valid("cardPan", pan, Map.of("cardPan", pan))).isTrue();

    String broken = pan.substring(0, 15) + ((pan.charAt(15) - '0' + 1) % 10);
    assertThat(Coverage.valid("cardPan", broken, Map.of("cardPan", broken))).isFalse();
    assertThat(Coverage.valid("cardPan", "4111111111111111", Map.of()))
      .as("не «Мир»").isFalse();
  }

  @Test
  @DisplayName("В наборе колонка cardPan заполнена у всех")
  void statHasNoGaps() {
    var profile = TestProfiles.individual(9_105);
    var rows = java.util.List.of(IndividualProfileFlattener.flatten(profile));

    Coverage.FieldStat stat = Coverage.stat("cardPan", rows);

    assertThat(stat.present()).isEqualTo(1);
    assertThat(stat.invalid()).isZero();
  }
}
