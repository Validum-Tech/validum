package ru.validum.core.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.bank.BikSource;
import ru.validum.core.bank.BikSources;

/**
 * Возраст как диапазон: набор должен покрывать разные жизненные ситуации, а не быть
 * тысячей 19-летних. Проверяем покрытие когорт, детерминизм и защиту от перевёрнутого
 * диапазона.
 */
class AgeDistributionTest {

  private static final BikSource BIK = BikSources.resolve("src/test/resources/bik-sample.xml");
  private static final LocalDate AS_OF = LocalDate.of(2026, 11, 1);

  private static List<Integer> ages(long seed, int count, int min, int max, AgeDistribution dist) {
    IndividualProfileGenerator gen =
      IndividualProfileGenerator.builder()
        .seed(seed)
        .bikSource(BIK)
        .asOf(AS_OF)
        .ageRange(min, max)
        .ageDistribution(dist)
        .build();
    List<Integer> result = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      result.add(gen.generate().ageAt(AS_OF));
    }
    return result;
  }

  @Test
  @DisplayName("Равномерный диапазон 0–100 покрывает детей и пенсионеров")
  void uniformCoversChildrenAndPensioners() {
    List<Integer> ages = ages(42, 1000, 0, 100, AgeDistribution.UNIFORM);

    assertThat(ages).allMatch(a -> a >= 0 && a <= 100);
    assertThat(ages).anyMatch(a -> a < 14);
    assertThat(ages).anyMatch(a -> a > 65);
    assertThat(ages.stream().distinct().count()).isGreaterThan(50);
  }

  @Test
  @DisplayName("Реалистичное распределение держится внутри диапазона и даёт заметную долю трудоспособных")
  void realisticKeepsWorkingAge() {
    List<Integer> ages = ages(7, 1000, 18, 70, AgeDistribution.REALISTIC);

    assertThat(ages).allMatch(a -> a >= 18 && a <= 70);
    long working = ages.stream().filter(a -> a >= 25 && a <= 54).count();
    assertThat(working).isGreaterThan(500);
  }

  @Test
  @DisplayName("Один seed и одно распределение — одинаковый набор возрастов")
  void deterministic() {
    assertThat(ages(99, 50, 18, 70, AgeDistribution.REALISTIC))
      .isEqualTo(ages(99, 50, 18, 70, AgeDistribution.REALISTIC));
  }

  @Test
  @DisplayName("Границы 0 и 110 допустимы, перевёрнутый диапазон — ошибка")
  void boundaries() {
    assertThat(ages(1, 5, 0, 0, AgeDistribution.UNIFORM)).containsOnly(0);
    assertThat(ages(1, 5, 110, 110, AgeDistribution.UNIFORM)).containsOnly(110);

    assertThatThrownBy(() -> IndividualProfileGenerator.builder().ageRange(70, 18))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("не перевёрнут");
  }
}
