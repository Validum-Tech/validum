package ru.validum.allure;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Расширенная аннотация для JUnit 5 тестов: генерирует профиль ФЛ с указанными параметрами
 * (seed, возраст, регион, пол) и прикладывает его в Allure-отчёт.
 *
 * <p>Использование:
 *
 * <pre>{@code
 * @Test
 * @ValidumGenerate(seed = 42, age = 16, region = "MOSCOW")
 * void testMinorClient() {
 *     // Профиль с age=16, region=MOSCOW уже в отчёте
 * }
 * }</pre>
 *
 * <p>Все параметры опциональны, кроме {@link #seed()}. Если параметр не задан — используется
 * каскадный дефолт из {@code IndividualProfileGenerator}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ValidumGenerate {
  /** Seed для воспроизводимой генерации. Обязателен. */
  long seed();

  /** Возраст. Если не задан — дефолт (19). */
  int age() default -1;

  /** Регион (например, "MOSCOW", "SPB"). Если не задан — случайный. */
  String region() default "";

  /** Пол: "MALE" / "FEMALE". Если не задан — случайный. */
  String gender() default "";

  /** Категория клиента. Если не задана — дефолт (КЛИЕНТ). */
  String categoryClient() default "";
}
