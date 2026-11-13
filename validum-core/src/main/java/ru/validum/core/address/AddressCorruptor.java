package ru.validum.core.address;

/**
 * Намеренная порча адреса для негативных сценариев.
 *
 * <p>Ломается именно формат: индекс пустой, а готовая строка — «ул. , д. , кв. ». Такой адрес
 * отбрасывает валидатор АБС, при этом остальные поля профиля остаются согласованными — тест
 * падает именно на адресе, а не на «всё сломалось».
 */
public final class AddressCorruptor {

  private static final String BROKEN_ADDRESS = "ул. , д. , кв. ";

  private AddressCorruptor() {}

  public static Address corrupt(Address address) {
    if (address == null) {
      return null;
    }
    return address.withExpected("", BROKEN_ADDRESS);
  }
}
