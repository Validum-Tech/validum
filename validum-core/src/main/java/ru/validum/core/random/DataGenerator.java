package ru.validum.core.random;

import java.util.Random;

/**
 * Базовый контракт для всех генераторов Validum.
 *
 * <p>Каждый генератор принимает {@link Random} через конструктор — это даёт два преимущества:
 *
 * <ul>
 *   <li>воспроизводимость: с фиксированным seed один и тот же вызов даёт идентичный результат;
 *   <li>тестируемость: в тестах можно подсунуть предсказуемый генератор.
 * </ul>
 */
public interface DataGenerator {

  /** Случайный seed — для продакшена и обычной генерации. */
  static Random defaultRandom() {
    return new Random();
  }

  /** Фиксированный seed — для воспроизводимых тестов и отладки. */
  static Random seededRandom(long seed) {
    return new Random(seed);
  }
}
