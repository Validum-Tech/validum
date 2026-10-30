package ru.validum.core.okved;

import java.util.Random;
import ru.validum.core.random.DataGenerator;

/**
 * Генератор кода ОКВЭД-2 с наименованием из справочника.
 *
 * <p>Возвращает пару «код + наименование», поэтому в профиле не бывает расхождения между {@code
 * okved} и {@code okvedName}.
 *
 * <pre>{@code
 * OkvedGenerator gen = new OkvedGenerator(DataGenerator.seededRandom(42));
 * OkvedEntry e = gen.generate();      // случайный вид деятельности
 * OkvedEntry fixed = gen.require("62.01"); // явный код, проверенный по справочнику
 * }</pre>
 */
public final class OkvedGenerator {

  private final Random random;

  /** Случайный источник — для обычной генерации. */
  public OkvedGenerator() {
    this(DataGenerator.defaultRandom());
  }

  public OkvedGenerator(Random random) {
    if (random == null) {
      throw new IllegalArgumentException("Random не может быть null");
    }
    this.random = random;
  }

  /** Случайная позиция справочника. */
  public OkvedEntry generate() {
    return OkvedRegistry.random(random);
  }

  /** Явный код — проверяется по справочнику. */
  public OkvedEntry require(String code) {
    return OkvedRegistry.require(code);
  }
}
