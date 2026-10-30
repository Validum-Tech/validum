package ru.validum.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Регион РФ: имя enum {@link ru.validum.core.common.Region} ({@code MOSCOW}) или код субъекта
 * ({@code 77}). Пустое значение считается валидным — регион необязателен.
 *
 * <p>Одна проверка на все DTO вместо шести копий {@code @Pattern}: набор регионов задаётся
 * {@link ru.validum.core.common.Region}, а не регуляркой в каждом классе.
 */
@Documented
@Constraint(validatedBy = ValidRegionValidator.class)
@Target({
  ElementType.FIELD,
  ElementType.METHOD,
  ElementType.PARAMETER,
  ElementType.ANNOTATION_TYPE,
  ElementType.RECORD_COMPONENT
})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidRegion {

  String message() default "Неизвестный регион";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
