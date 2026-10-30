package ru.validum.core.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.common.Region;
import ru.validum.core.random.DataGenerator;

@DisplayName("KppGenerator")
class KppGeneratorTest {

  private final KppGenerator generator = new KppGenerator(DataGenerator.seededRandom(42L));

  @Test
  @DisplayName("КПП — 9 цифр")
  void has9Digits() {
    String kpp = generator.generate(Region.MOSCOW);
    assertThat(kpp).hasSize(9).containsOnlyDigits();
  }

  @Test
  @DisplayName("Первые 2 цифры КПП = код региона")
  void startsWithRegionCode() {
    assertThat(generator.generate(Region.MOSCOW)).startsWith("77");
    assertThat(generator.generate(Region.SPB)).startsWith("78");
    assertThat(generator.generate(Region.MO)).startsWith("50");
    assertThat(generator.generate(Region.KRASNODAR)).startsWith("23");
    assertThat(generator.generate(Region.SVERDLOVSK)).startsWith("66");
  }

  @Test
  @DisplayName("Причина постановки по умолчанию — 01")
  void defaultReason() {
    String kpp = generator.generate(Region.MOSCOW);
    assertThat(kpp.substring(4, 6)).isEqualTo(KppGenerator.REASON_AT_LOCATION);
  }

  @Test
  @DisplayName("Можно задать причину 43 (филиал)")
  void customReason() {
    String kpp = generator.generate(Region.MOSCOW, KppGenerator.REASON_BRANCH);
    assertThat(kpp.substring(4, 6)).isEqualTo("43");
  }

  @Test
  @DisplayName("Null-регион бросает исключение")
  void nullRegionThrows() {
    assertThatThrownBy(() -> generator.generate(null))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("Неверная причина бросает исключение")
  void invalidReasonThrows() {
    assertThatThrownBy(() -> generator.generate(Region.MOSCOW, "1"))
      .isInstanceOf(IllegalArgumentException.class);
  }
}
