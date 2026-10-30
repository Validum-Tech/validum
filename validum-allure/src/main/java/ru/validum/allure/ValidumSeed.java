package ru.validum.allure;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация для JUnit 5 тестов: генерирует профиль ФЛ с указанным seed и прикладывает его
 * в Allure-отчёт как JSON-attachment.
 *
 * <p>Использование:
 *
 * <pre>{@code
 * @Test
 * @ValidumSeed(42)
 * void testCreateClient() {
 *     // Профиль с seed=42 уже в отчёте
 * }
 * }</pre>
 *
 * <p>Для расширенной конфигурации (возраст, регион, пол) — {@link ValidumGenerate}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ValidumSeed {
  /** Seed для воспроизводимой генерации профиля. */
  long value();
}
