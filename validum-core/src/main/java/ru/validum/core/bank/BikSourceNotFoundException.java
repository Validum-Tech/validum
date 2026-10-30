package ru.validum.core.bank;

/**
 * Справочник БИК не найден: файл ED807 не задан и отсутствует по умолчанию.
 *
 * <p>Отдельный тип, чтобы API отдал {@code 503} с понятным кодом, а CLI — код возврата 2 вместо
 * стектрейса. Встроенного демо-справочника в продукте нет: справочник либо есть, либо его надо
 * загрузить командой {@code validum fetch-bik}.
 */
public class BikSourceNotFoundException extends IllegalStateException {

  private static final long serialVersionUID = 1L;

  public BikSourceNotFoundException(String message) {
    super(message);
  }
}
