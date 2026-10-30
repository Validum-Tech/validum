package ru.validum.core.inn;

/**
 * Намеренная порча ИНН: контрольные разряды становятся неверными.
 *
 * <p>Не путать с {@link InnGenerator}: тот создаёт валидные ИНН, этот — заведомо невалидные.
 * Изменяется ровно одна цифра контрольного разряда, чтобы алгоритм ФНС не сошёлся.
 */
public final class InnCorruptor {

  private InnCorruptor() {}

  /**
   * Порча 12-значного ИНН физлица.
   *
   * @param validInn валидный ИНН (12 цифр), сгенерированный {@link InnGenerator}
   * @return ИНН с битым контрольным разрядом
   */
  public static String corruptIndividual(String validInn) {
    if (validInn == null || validInn.length() != 12) {
      throw new IllegalArgumentException("ИНН физлица должен содержать 12 цифр: " + validInn);
    }
    char last = validInn.charAt(11);
    int flipped = (last - '0' + 1) % 10;
    return validInn.substring(0, 11) + flipped;
  }

  /**
   * Порча 10-значного ИНН юрлица.
   *
   * @param validInn валидный ИНН (10 цифр)
   * @return ИНН с битым контрольным разрядом
   */
  public static String corruptLegalEntity(String validInn) {
    if (validInn == null || validInn.length() != 10) {
      throw new IllegalArgumentException("ИНН юрлица должен содержать 10 цифр: " + validInn);
    }
    char last = validInn.charAt(9);
    int flipped = (last - '0' + 1) % 10;
    return validInn.substring(0, 9) + flipped;
  }
}
