package ru.validum.core.account;

import java.util.Map;

/**
 * Валидатор 20-значного номера лицевого счёта.
 *
 * <p>Алгоритм — «Порядок расчёта контрольного ключа в номере лицевого счёта» (письмо Банка России
 * № 515 от 08.09.1997), действующий и для Положения № 809-П:
 *
 * <ol>
 *   <li>Берётся 3-значный условный номер подразделения: для счёта в кредитной организации —
 *       разряды 7-9 БИК банка; для счёта в подразделении Банка России — «0» + разряды 5-6 БИК
 *       подразделения.
 *   <li>К 3 знакам условного номера и 20 знакам счёта применяются веса: {@code 7,1,3} и {@code
 *       7,1,3,7,1,3,7,1,3,7,1,3,7,1,3,7,1,3,7,1}.
 *   <li>9-й разряд счёта (контрольный ключ) при расчёте принимается равным нулю.
 *   <li>Контрольный ключ = младший разряд суммы произведений, умноженной на 3.
 * </ol>
 *
 * <p>Проверено на справочнике ED807 (2026.09.0): все 1338 счетов проходят проверку при условном
 * номере «0» + разряды 5-6 БИК подразделения ({@code AccountCBRBIC}).
 *
 * <p>Структура 20-значного счёта: 1-5 — балансовый счёт; 6-8 — код валюты по ОКВ (810 для рубля);
 * 9 — контрольный ключ; 10-13 — код филиала; 14-20 — номер лицевого счёта.
 */
public final class AccountValidator {

  /** Код рубля по ОКВ для разрядов 6-8. */
  public static final String RUB_CURRENCY = "810";

  /** Длина номера счёта. */
  public static final int LENGTH = 20;

  /** Позиция контрольного ключа (0-based). */
  private static final int KEY_POSITION = 8;

  private static final int[] PREFIX_WEIGHTS = {7, 1, 3};

  private static final int[] ACCOUNT_WEIGHTS = {
    7, 1, 3, 7, 1, 3, 7, 1, 3, 7, 1, 3, 7, 1, 3, 7, 1, 3, 7, 1
  };

  /** Буквенные значения 6-го разряда (клиринговая валюта) и их цифровые эквиваленты. */
  private static final Map<Character, Integer> LETTERS =
    Map.of('A', 0, 'B', 1, 'C', 2, 'E', 3, 'H', 4, 'K', 5, 'M', 6, 'P', 7, 'T', 8, 'X', 9);

  /** Условный номер для проверки без привязки к банку. */
  public static final String NO_PREFIX = "000";

  private AccountValidator() {}

  /** Проверяет форму: 20 знаков, цифры, в 6-м разряде допускается буква клиринговой валюты. */
  public static boolean isWellFormed(String account) {
    if (account == null || account.length() != LENGTH) {
      return false;
    }
    for (int i = 0; i < LENGTH; i++) {
      char c = account.charAt(i);
      boolean allowed = i == 5 ? Character.isDigit(c) || LETTERS.containsKey(c) : Character.isDigit(c);
      if (!allowed) {
        return false;
      }
    }
    return true;
  }

  /**
   * Вычисляет ожидаемый контрольный ключ.
   *
   * @param account 20-значный номер (значение 9-го разряда игнорируется)
   * @param prefix3 условный номер подразделения (3 цифры)
   */
  public static int calculateKey(String account, String prefix3) {
    if (account == null || account.length() != LENGTH) {
      throw new IllegalArgumentException("Номер счёта должен содержать 20 знаков: " + account);
    }
    if (prefix3 == null || prefix3.length() != 3) {
      throw new IllegalArgumentException("Условный номер должен содержать 3 знака: " + prefix3);
    }
    int sum = 0;
    for (int i = 0; i < PREFIX_WEIGHTS.length; i++) {
      sum += (prefix3.charAt(i) - '0') * PREFIX_WEIGHTS[i];
    }
    for (int i = 0; i < LENGTH; i++) {
      int digit = i == KEY_POSITION ? 0 : digitAt(account, i);
      sum += digit * ACCOUNT_WEIGHTS[i];
    }
    return (sum * 3) % 10;
  }

  /** Проверяет счёт при известном условном номере подразделения. */
  public static boolean isValid(String account, String prefix3) {
    if (!isWellFormed(account)) {
      return false;
    }
    char key = account.charAt(KEY_POSITION);
    if (!Character.isDigit(key)) {
      return false;
    }
    return calculateKey(account, prefix3) == key - '0';
  }

  /** Проверяет счёт по правилу, соответствующему типу счёта. */
  public static boolean isValid(String account, AccountType type, String bik) {
    if (!isWellFormed(account) || type == null || !hasNineDigits(bik)) {
      return false;
    }
    return isValid(account, prefixFor(type, bik));
  }

  /**
   * Проверяет счёт, пробуя оба правила — клиентское и корреспондентское. Удобно, когда тип счёта
   * неизвестен: принимается либо разряды 7-9, либо «0» + разряды 5-6 БИК.
   */
  public static boolean isValidAuto(String account, String bik) {
    if (!isWellFormed(account) || !hasNineDigits(bik)) {
      return false;
    }
    return isValid(account, prefixFor(AccountType.CLAC, bik))
      || isValid(account, prefixFor(AccountType.CRSA, bik));
  }

  /**
   * Условный номер подразделения для счёта.
   *
   * @throws IllegalArgumentException если БИК не из 9 цифр
   */
  public static String prefixFor(AccountType type, String bik) {
    if (!hasNineDigits(bik)) {
      throw new IllegalArgumentException("БИК должен содержать 9 цифр: " + bik);
    }
    if (type == null) {
      throw new IllegalArgumentException("Тип счёта обязателен");
    }
    return type.isClientAccount() ? bik.substring(6, 9) : "0" + bik.substring(4, 6);
  }

  private static int digitAt(String account, int index) {
    char c = account.charAt(index);
    Integer mapped = LETTERS.get(c);
    return mapped != null ? mapped : c - '0';
  }

  private static boolean hasNineDigits(String bik) {
    if (bik == null || bik.length() != 9) {
      return false;
    }
    for (int i = 0; i < 9; i++) {
      if (!Character.isDigit(bik.charAt(i))) {
        return false;
      }
    }
    return true;
  }
}
