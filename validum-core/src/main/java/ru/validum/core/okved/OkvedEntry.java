package ru.validum.core.okved;

/**
 * Запись классификатора ОКВЭД-2.
 *
 * @param code код вида деятельности (2–6 знаков: {@code 01}, {@code 01.1}, {@code 62.01},
 *     {@code 01.11.1})
 * @param name наименование вида деятельности
 * @param section раздел классификатора (латинская буква {@code A}–{@code U})
 */
public record OkvedEntry(String code, String name, String section) {}
