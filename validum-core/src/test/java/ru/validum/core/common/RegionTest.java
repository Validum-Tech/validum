package ru.validum.core.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Регион: коды, имена и поиск")
class RegionTest {

  @Test
  @DisplayName("Все регионы имеют уникальный двузначный код")
  void codesAreUniqueAndTwoDigits() {
    Set<String> codes = new HashSet<>();
    for (Region region : Region.values()) {
      assertThat(region.code()).as(region.name()).matches("\\d{2}");
      assertThat(codes.add(region.code())).as("код %s повторяется", region.code()).isTrue();
    }
    assertThat(Region.values()).hasSize(16);
  }

  @Test
  @DisplayName("byCode: код, код с пробелами и неизвестный код")
  void byCode() {
    assertThat(Region.byCode("77")).isEqualTo(Region.MOSCOW);
    assertThat(Region.byCode(" 16 ")).isEqualTo(Region.TATARSTAN);
    assertThat(Region.byCode(null)).isNull();
    assertThat(Region.byCode("")).isNull();
    assertThat(Region.byCode("99")).isNull();
  }

  @Test
  @DisplayName("byName: имя enum без учёта регистра и лишних пробелов")
  void byName() {
    assertThat(Region.byName("MOSCOW")).isEqualTo(Region.MOSCOW);
    assertThat(Region.byName("moscow")).isEqualTo(Region.MOSCOW);
    assertThat(Region.byName("  Tatarstan ")).isEqualTo(Region.TATARSTAN);
    assertThat(Region.byName(null)).isNull();
    assertThat(Region.byName("   ")).isNull();
    assertThat(Region.byName("Москва")).isNull();
  }

  @Test
  @DisplayName("byNameOrCode принимает и имя, и код")
  void byNameOrCode() {
    assertThat(Region.byNameOrCode("16")).isEqualTo(Region.TATARSTAN);
    assertThat(Region.byNameOrCode("tatarstan")).isEqualTo(Region.TATARSTAN);
    assertThat(Region.byNameOrCode("Атлантида")).isNull();
  }

  @Test
  @DisplayName("require бросает исключение с понятным текстом")
  void requireThrows() {
    assertThat(Region.require("78")).isEqualTo(Region.SPB);
    assertThatThrownBy(() -> Region.require("Атлантида"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Неизвестный регион")
      .hasMessageContaining("Атлантида");
  }

  @Test
  @DisplayName("Реквизиты региона для адреса и паспорта")
  void attributes() {
    assertThat(Region.MOSCOW.regionName()).isEqualTo("Москва");
    assertThat(Region.MOSCOW.regionType()).isEqualTo("г");
    assertThat(Region.MOSCOW.district()).isNull();
    assertThat(Region.MOSCOW.districtType()).isNull();
    assertThat(Region.MOSCOW.cityName()).isEqualTo("Москва");
    assertThat(Region.MOSCOW.cityType()).isEqualTo("г");
    assertThat(Region.BASHKORTOSTAN.regionType()).isEqualTo("респ");
    assertThat(Region.BASHKORTOSTAN.code()).isEqualTo("02");
    assertThat(Arrays.stream(Region.values()).filter(r -> "02".equals(r.code())).count()).isEqualTo(1);
  }
}
