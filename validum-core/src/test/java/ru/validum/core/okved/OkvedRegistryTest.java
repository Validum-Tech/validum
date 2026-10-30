package ru.validum.core.okved;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Random;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Справочник ОКВЭД-2")
class OkvedRegistryTest {

  private static final Pattern CODE = Pattern.compile("\\d{2}(?:\\.\\d{1,2}){0,2}");

  @Test
  @DisplayName("Загружено больше 3000 позиций, все коды по формату")
  void loadsFullClassifier() {
    assertThat(OkvedRegistry.size()).isGreaterThan(3000);
    assertThat(OkvedRegistry.all()).allSatisfy(e -> {
      assertThat(e.code()).matches(CODE);
      assertThat(e.name()).isNotBlank();
      assertThat(e.section()).hasSize(1);
    });
  }

  @Test
  @DisplayName("Известный код находит наименование и раздел")
  void findsKnownCode() {
    OkvedEntry it = OkvedRegistry.require("62.01");
    assertThat(it.name()).isEqualTo("Разработка компьютерного программного обеспечения");
    assertThat(it.section()).isEqualTo("J");

    OkvedEntry building = OkvedRegistry.require(" 41.20 ");
    assertThat(building.name()).isEqualTo("Строительство жилых и нежилых зданий");
  }

  @Test
  @DisplayName("Неизвестный код — пусто, require кидает с подсказкой")
  void rejectsUnknownCode() {
    assertThat(OkvedRegistry.byCode("12.34")).isEmpty();
    assertThat(OkvedRegistry.byCode(null)).isEmpty();
    assertThat(OkvedRegistry.byCode("  ")).isEmpty();

    assertThatThrownBy(() -> OkvedRegistry.require("12.34"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("ОКВЭД не найден")
      .hasMessageContaining("62.01");
  }

  @Test
  @DisplayName("Случайная позиция детерминирована seed и берётся из справочника")
  void randomIsDeterministic() {
    OkvedEntry a = OkvedRegistry.random(new Random(42));
    OkvedEntry b = OkvedRegistry.random(new Random(42));

    assertThat(a).isEqualTo(b);
    assertThat(OkvedRegistry.byCode(a.code())).contains(a);
  }

  @Test
  @DisplayName("Генератор уважает явный код")
  void generatorRespectsExplicitCode() {
    OkvedGenerator gen = new OkvedGenerator(new Random(7));
    assertThat(gen.require("01.11.1").name()).isEqualTo("Выращивание зерновых культур");
    assertThat(OkvedRegistry.byCode(gen.generate().code())).isPresent();
  }
}
