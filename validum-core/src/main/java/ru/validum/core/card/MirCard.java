package ru.validum.core.card;

import java.time.LocalDate;

/**
 * Тестовая карта «Мир»: номер, срок действия и ожидаемый исход операции.
 *
 * <p>Номер проходит алгоритм Луна и попадает в реальный BIN-диапазон «Мира»
 * (220000–220499), поэтому карта проходит формальную проверку платёжных систем. Это не боевая
 * карта: владелец, CVV и ПИН ей не выдаются.
 *
 * @param pan         16 цифр, проходят Луна
 * @param expiryMonth месяц истечения, 1–12
 * @param expiryYear  год истечения (полный, например 2029)
 * @param outcome     ожидаемый исход операции
 */
public record MirCard(String pan, int expiryMonth, int expiryYear, MirCardOutcome outcome) {

  /** Срок действия в виде {@code ММ/ГГ} — так он печатается на карте. */
  public String expiry() {
    return String.format("%02d/%02d", expiryMonth, expiryYear % 100);
  }

  /** Просрочена ли карта на указанную дату. */
  public boolean expiredAt(LocalDate at) {
    LocalDate end = LocalDate.of(expiryYear, expiryMonth, 1).plusMonths(1).minusDays(1);
    return end.isBefore(at);
  }

  /** Проверка контрольной цифры по алгоритму Луна. */
  public boolean luhnValid() {
    int sum = 0;
    boolean doubleDigit = false;
    for (int i = pan.length() - 1; i >= 0; i--) {
      int digit = pan.charAt(i) - '0';
      if (doubleDigit) {
        digit *= 2;
        if (digit > 9) {
          digit -= 9;
        }
      }
      sum += digit;
      doubleDigit = !doubleDigit;
    }
    return sum % 10 == 0;
  }
}
