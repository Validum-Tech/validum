package ru.validum.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UniqueGuard — повторы и исчерпание вариантов (W47-B1)")
class UniqueGuardTest {

  @Test
  @DisplayName("Повторяющиеся значения отбрасываются: второй вызов берёт следующий seed")
  void skipsDuplicates() {
    UniqueGuard guard = new UniqueGuard("ТЕСТ", 10);

    // Генератор игнорирует seed на первых попытках: пока возвращается "A", guard пробует снова.
    String first = guard.next(1L, seed -> seed <= 3 ? "A" : "B", s -> s);
    String second = guard.next(1L, seed -> seed <= 3 ? "A" : "B", s -> s);

    assertThat(first).isEqualTo("A");
    assertThat(second).isEqualTo("B");
    assertThat(guard.size()).isEqualTo(2);
  }

  @Test
  @DisplayName("Пул исчерпан → IllegalArgumentException с попытками и ёмкостью")
  void exhaustedPool() {
    UniqueGuard guard = new UniqueGuard("ИНН", 1);

    String first = guard.next(1L, seed -> "always", s -> s);
    assertThat(first).isEqualTo("always");

    assertThatThrownBy(() -> guard.next(1L, seed -> "always", s -> s))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("уникальный ИНН")
      .hasMessageContaining("1000 попыток")
      .hasMessageContaining("из 1 вариантов");
  }

  @Test
  @DisplayName("Пустое значение ключевого поля — ошибка состояния, а не тихий дубль")
  void blankKey() {
    UniqueGuard guard = new UniqueGuard("ОГРН", 5);

    assertThatThrownBy(() -> guard.next(1L, seed -> "  ", s -> s))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("ОГРН");
  }
}
