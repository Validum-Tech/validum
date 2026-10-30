package ru.validum.core.contact;

/**
 * Режим выбора доменов для генерации email.
 *
 * <ul>
 *   <li>{@link #TEST} — безопасные тестовые домены (test.ru, example.com, qa.local).
 *       Дефолт. Гарантирует, что при случайной отправке письма не уйдёт реальному получателю.
 *   <li>{@link #REALISTIC} — реалистичные российские домены (mail.ru, yandex.ru).
 *       Использовать только когда нужна визуальная реалистичность.
 *   <li>{@link #MIXED} — 50/50 из обоих пулов.
 * </ul>
 */
public enum EmailDomainMode {
  TEST,
  REALISTIC,
  MIXED
}
