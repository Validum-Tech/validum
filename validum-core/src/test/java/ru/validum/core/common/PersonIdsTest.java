package ru.validum.core.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("personId: детерминированный идентификатор человека")
class PersonIdsTest {

  @Test
  @DisplayName("формат pd- + 16 hex")
  void format() {
    String id = PersonIds.of(42);

    assertThat(id).matches("pd-[0-9a-f]{16}");
    assertThat(PersonIds.isValid(id)).isTrue();
    assertThat(PersonIds.isValid("pd-xyz")).isFalse();
    assertThat(PersonIds.isValid(null)).isFalse();
    assertThat(PersonIds.isValid("42")).isFalse();
  }

  @Test
  @DisplayName("один seed → один и тот же идентификатор")
  void deterministic() {
    assertThat(PersonIds.of(7)).isEqualTo(PersonIds.of(7));
    assertThat(PersonIds.of(Long.MAX_VALUE)).isEqualTo(PersonIds.of(Long.MAX_VALUE));
  }

  @Test
  @DisplayName("разные seed → разные идентификаторы")
  void differentSeeds() {
    assertThat(PersonIds.of(1)).isNotEqualTo(PersonIds.of(2));
    assertThat(PersonIds.of(0)).isNotEqualTo(PersonIds.of(-1));
  }
}
