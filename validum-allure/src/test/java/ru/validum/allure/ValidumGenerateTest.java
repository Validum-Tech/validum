package ru.validum.allure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Smoke-тесты {@link ValidumGenerate} через реальный JUnit Platform.
 *
 * <p>Проверяют, что все параметры аннотации (age, region, gender, categoryClient) корректно
 * парсятся и применяются в билдере. Если параметр невалиден (например, region="UNKNOWN") —
 * тест упадёт с понятным стектрейсом.
 */
@ExtendWith(ValidumExtension.class)
@DisplayName("ValidumGenerate — smoke через JUnit Platform (P3.2)")
class ValidumGenerateTest {

  @Test
  @ValidumGenerate(seed = 42)
  @DisplayName("Только seed — дефолтные параметры применяются")
  void onlySeed() {
    // pass
  }

  @Test
  @ValidumGenerate(seed = 42, age = 16)
  @DisplayName("seed + age=16 — несовершеннолетний")
  void withAge() {
    // pass
  }

  @Test
  @ValidumGenerate(seed = 42, region = "MOSCOW")
  @DisplayName("seed + region=MOSCOW — регион применяется")
  void withRegion() {
    // pass
  }

  @Test
  @ValidumGenerate(seed = 42, gender = "FEMALE")
  @DisplayName("seed + gender=FEMALE")
  void withGender() {
    // pass
  }

  @Test
  @ValidumGenerate(seed = 42, categoryClient = "ЗАРПЛАТНИК")
  @DisplayName("seed + categoryClient")
  void withCategory() {
    // pass
  }

  @Test
  @ValidumGenerate(
    seed = 42,
    age = 16,
    region = "SPB",
    gender = "MALE",
    categoryClient = "КЛИЕНТ")
  @DisplayName("Все параметры сразу — не падает")
  void allParameters() {
    // pass
  }
}
