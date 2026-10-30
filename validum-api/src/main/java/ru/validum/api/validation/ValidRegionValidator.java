package ru.validum.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import ru.validum.core.common.Region;

/** Проверяет, что строка — имя enum {@link Region} или код субъекта РФ из справочника. */
public class ValidRegionValidator implements ConstraintValidator<ValidRegion, String> {

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null || value.isBlank()) {
      return true;
    }
    return Region.byNameOrCode(value) != null;
  }
}
