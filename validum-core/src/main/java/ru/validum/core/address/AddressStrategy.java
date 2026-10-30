package ru.validum.core.address;

/**
 * Стратегия генерации трёх адресов (регистрация, почтовый, фактический).
 *
 * <ul>
 *   <li>{@link #SAME_FOR_ALL} — один адрес копируется во все три поля (дефолт, как в АБС);
 *   <li>{@link #DISTINCT} — три независимых адреса (для сценариев «клиент уехал»).
 * </ul>
 */
public enum AddressStrategy {
  SAME_FOR_ALL,
  DISTINCT
}
