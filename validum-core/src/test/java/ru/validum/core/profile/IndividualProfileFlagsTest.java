package ru.validum.core.profile;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Флаги terrorist и blocked в профиле")
class IndividualProfileFlagsTest {

  private static final long SEED = 42L;

  @Test
  @DisplayName("По умолчанию оба флага false")
  void defaultFlagsAreFalse() {
    var p = IndividualProfileGenerator.builder().seed(SEED).build().generate();
    assertThat(p.terrorist()).isFalse();
    assertThat(p.blocked()).isFalse();
  }

  @Test
  @DisplayName("terrorist=true ставится явно, blocked остаётся false")
  void terroristFlag() {
    var p =
      IndividualProfileGenerator.builder()
        .seed(SEED)
        .terrorist(true)
        .build()
        .generate();
    assertThat(p.terrorist()).isTrue();
    assertThat(p.blocked()).isFalse();
  }

  @Test
  @DisplayName("blocked=true ставится явно, terrorist остаётся false")
  void blockedFlag() {
    var p =
      IndividualProfileGenerator.builder()
        .seed(SEED)
        .blocked(true)
        .build()
        .generate();
    assertThat(p.blocked()).isTrue();
    assertThat(p.terrorist()).isFalse();
  }

  @Test
  @DisplayName("Оба флага одновременно true")
  void bothFlags() {
    var p =
      IndividualProfileGenerator.builder()
        .seed(SEED)
        .terrorist(true)
        .blocked(true)
        .build()
        .generate();
    assertThat(p.terrorist()).isTrue();
    assertThat(p.blocked()).isTrue();
  }

  @Test
  @DisplayName("Флаги не влияют на детерминизм остальных полей")
  void flagsDoNotBreakDeterminism() {
    var withFlags =
      IndividualProfileGenerator.builder()
        .seed(SEED)
        .terrorist(true)
        .blocked(true)
        .build()
        .generate();
    var withoutFlags = IndividualProfileGenerator.builder().seed(SEED).build().generate();

    assertThat(withFlags.fullName()).isEqualTo(withoutFlags.fullName());
    assertThat(withFlags.inn()).isEqualTo(withoutFlags.inn());
    assertThat(withFlags.snils()).isEqualTo(withoutFlags.snils());
    assertThat(withFlags.bank().bik()).isEqualTo(withoutFlags.bank().bik());
    assertThat(withFlags.account()).isEqualTo(withoutFlags.account());
  }

  @Test
  @DisplayName("Флаги комбинируются с каскадами (age=16 + terrorist)")
  void flagsCombineWithCascades() {
    var p =
      IndividualProfileGenerator.builder()
        .seed(SEED)
        .age(16)
        .terrorist(true)
        .build()
        .generate();
    assertThat(p.terrorist()).isTrue();
    assertThat(p.familyStatus()).isEqualTo(ru.validum.core.name.FamilyStatus.SINGLE);
    assertThat(p.passport().status()).isEqualTo("Действительный");
  }
}
