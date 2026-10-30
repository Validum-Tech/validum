package ru.validum.core.contact;

/**
 * Контакт клиента.
 *
 * @param type тип (мобильный, email)
 * @param value значение (номер телефона или email)
 * @param description описание для UI и логов
 * @param status статус («Действительный»)
 * @param isDefault является ли контактом по умолчанию
 */
public record Contact(
  ContactType type, String value, String description, String status, boolean isDefault) {}
