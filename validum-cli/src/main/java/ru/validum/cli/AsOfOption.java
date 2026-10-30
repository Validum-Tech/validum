package ru.validum.cli;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Разбор опции {@code --as-of} — даты расчёта профиля.
 *
 * <p>Пустое значение означает «сегодня»: так CLI ведёт себя как API без поля {@code asOf}.
 */
final class AsOfOption {

  private AsOfOption() {}

  /**
   * @param value значение опции (может быть {@code null})
   * @return дата расчёта или {@code null}, если опция не задана
   * @throws IllegalArgumentException если дата не в формате ISO
   */
  static LocalDate parse(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(value.trim());
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException(
        "Неверная дата в --as-of: '" + value + "' (ожидается ISO, например 2026-01-01)");
    }
  }
}
