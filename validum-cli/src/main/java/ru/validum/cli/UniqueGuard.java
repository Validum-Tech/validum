package ru.validum.cli;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.LongFunction;

/**
 * Гарантирует отсутствие повторов ключевого поля в наборе.
 *
 * <p>Генераторы детерминированы по seed, поэтому при повторе берём следующий seed
 * ({@code profileSeed + attempt * 1_000_003}) и генерируем профиль заново. Ограничение в
 * {@link #MAX_ATTEMPTS} попыток защищает от бесконечного цикла, когда множество вариантов
 * исчерпано.
 */
public final class UniqueGuard {

  /** Сколько раз пробуем получить новое значение, прежде чем честно сказать «не вышло». */
  static final int MAX_ATTEMPTS = 1_000;

  /** Шаг seed между попытками: простое число, не кратное типичным периодам LCG. */
  private static final long SEED_STEP = 1_000_003L;

  private final String field;
  private final long capacity;
  private final Set<String> seen = new HashSet<>();

  /**
   * @param field человекочитаемое имя поля для сообщений («ИНН», «ОГРН», «ОГРНИП»)
   * @param capacity сколько различных значений поле в принципе может принять
   */
  public UniqueGuard(String field, long capacity) {
    this.field = field;
    this.capacity = capacity;
  }

  /**
   * Возвращает профиль с новым значением ключевого поля.
   *
   * @param profileSeed seed профиля (без попыток)
   * @param generator генератор, принимающий seed
   * @param keyOf функция, достающая ключевое поле
   */
  public <T> T next(long profileSeed, LongFunction<T> generator, Function<T, String> keyOf) {
    if (seen.size() >= capacity) {
      throw poolExhausted();
    }
    for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
      T value = generator.apply(profileSeed + attempt * SEED_STEP);
      String key = keyOf.apply(value);
      if (key == null || key.isBlank()) {
        throw new IllegalStateException("Пустое значение уникального поля " + field);
      }
      if (seen.add(key)) {
        return value;
      }
    }
    throw exhausted();
  }

  /** Пул исчерпан заранее: все варианты уже выданы, дальше пробовать бессмысленно. */
  private IllegalArgumentException poolExhausted() {
    return new IllegalArgumentException(
      "пул исчерпан: выдано " + seen.size() + " из " + capacity + " вариантов " + field
        + "; увеличьте --unique-pool или уменьшите -n");
  }

  /** Не повезло за отведённое число попыток. */
  private IllegalArgumentException exhausted() {
    return new IllegalArgumentException(
      "не удалось получить уникальный " + field + " за " + MAX_ATTEMPTS + " попыток из "
        + capacity + " вариантов");
  }

  /** Сколько значений уже выдано. */
  public int size() {
    return seen.size();
  }
}
