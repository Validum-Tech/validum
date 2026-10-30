package ru.validum.core.validation;

import ru.validum.core.bank.Bank;
import ru.validum.core.bank.BikSource;

/**
 * Валидатор БИК. Проверяет формат и наличие банка в справочнике.
 */
public final class BikValidator {

  private final BikSource source;

  public BikValidator(BikSource source) {
    this.source = source;
  }

  public boolean isValid(String bik) {
    if (bik == null || !bik.matches("\\d{9}")) return false;
    return source.findByBik(bik).isPresent();
  }

  public Bank getBank(String bik) {
    if (!isValid(bik)) return null;
    return source.findByBik(bik).orElse(null);
  }
}
