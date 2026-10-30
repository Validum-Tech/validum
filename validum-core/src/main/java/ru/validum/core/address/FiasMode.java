package ru.validum.core.address;

/**
 * Режим FIAS-разбора адреса.
 *
 * <ul>
 *   <li>{@link #FULL} — заполняются все FIAS-части (region, city, street, ...) + строка;
 *   <li>{@link #ADDRESS_ONLY} — только `expectedAddress` и `fiasAddress`, остальные FIAS-части
 *       пустые.
 * </ul>
 *
 * <p>Для АБС, которые валидируют отдельные части — используется FULL. Для тех, кто проверяет
 * только строку — ADDRESS_ONLY, чтобы не заполнять лишнее.
 */
public enum FiasMode {
  FULL,
  ADDRESS_ONLY
}
