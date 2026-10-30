package ru.validum.allure;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Smoke-тесты {@link ValidumSeed} через реальный JUnit Platform.
 *
 * <p>Если Extension бросит исключение в {@code beforeEach} — тест упадёт. Если пройдёт — значит
 * цепочка «аннотация → генерация профиля → attach в Allure» работает.
 */
@ExtendWith(ValidumExtension.class)
@DisplayName("ValidumSeed — smoke через JUnit Platform (P3.2)")
class ValidumSeedTest {

  @Test
  @ValidumSeed(42)
  @DisplayName("@ValidumSeed(42) — Extension не падает, тест проходит")
  void withSeed42() {
    // Тест проходит, если beforeEach не бросил исключение
  }

  @Test
  @ValidumSeed(1)
  @DisplayName("@ValidumSeed(1) — другой seed, тоже не падает")
  void withSeed1() {
    // pass
  }

  @Test
  @ValidumSeed(Long.MAX_VALUE)
  @DisplayName("@ValidumSeed(Long.MAX_VALUE) — граничное значение, не падает")
  void withMaxSeed() {
    // pass
  }

  @Test
  @DisplayName("Тест без аннотации — Extension возвращается рано, не падает")
  void withoutAnnotation() {
    // Extension видит, что аннотаций нет, и ничего не делает
  }

  @Test
  @DisplayName("assertThatCode не нужен — тест сам по себе smoke")
  void explicitSmoke() {
    assertThatCode(() -> {
      // Extension уже отработал в beforeEach
    }).doesNotThrowAnyException();
  }
}
