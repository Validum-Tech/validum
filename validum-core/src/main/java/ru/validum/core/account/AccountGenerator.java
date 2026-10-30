package ru.validum.core.account;

import java.util.Random;
import ru.validum.core.random.DataGenerator;

/**
 * Генератор 20-значных номеров лицевых счетов.
 *
 * <p>Структура счёта: 1-5 — балансовый счёт (код второго порядка); 6-8 — код валюты по ОКВ (810 для
 * рубля); 9 — контрольный ключ; 10-13 — код филиала; 14-20 — номер лицевого счёта.
 *
 * <p>Контрольный ключ считается по письму Банка России № 515 от 08.09.1997 (см. {@link
 * AccountValidator}), поэтому зависит от БИК банка: для клиентского счёта берутся разряды 7-9 БИК,
 * для корреспондентского — «0» + разряды 5-6 БИК подразделения Банка России.
 */
public final class AccountGenerator implements DataGenerator {

  /**
   * БИК-заглушка: условный номер {@code 000}. Счёт при этом не привязан к конкретному банку и
   * проверяется через {@link AccountValidator#isValid(String, AccountType, String)}.
   */
  public static final String NO_BANK_BIK = "000000000";

  /** Балансовый счёт расчётного счёта физлица по умолчанию. */
  public static final String DEFAULT_BALANCE_ACCOUNT = "40817";

  private final Random random;

  public AccountGenerator() {
    this(DataGenerator.defaultRandom());
  }

  public AccountGenerator(Random random) {
    this.random = random;
  }

  /**
   * Генерирует счёт указанного типа и валюты.
   *
   * @param balanceAccount балансовый счёт (5 цифр, например «40817»)
   * @param type тип счёта из классификатора УФЭБС
   * @param bik БИК банка (для клиентского счёта) или БИК подразделения Банка России (для
   *     корреспондентского)
   * @param currencyCode код валюты по ОКВ (3 цифры, 810 — рубль)
   * @return 20-значный счёт с корректным контрольным ключом
   */
  public String generate(String balanceAccount, AccountType type, String bik, String currencyCode) {
    if (balanceAccount == null || balanceAccount.length() != 5) {
      throw new IllegalArgumentException(
        "Код второго порядка должен содержать 5 цифр: " + balanceAccount);
    }
    if (type == null) {
      throw new IllegalArgumentException("Тип счёта обязателен");
    }
    if (bik == null || bik.length() != 9) {
      throw new IllegalArgumentException("БИК должен содержать 9 цифр: " + bik);
    }
    if (currencyCode == null || currencyCode.length() != 3) {
      throw new IllegalArgumentException("Код валюты должен содержать 3 цифры: " + currencyCode);
    }
    int filial = 0;
    int serial = random.nextInt(10_000_000);
    String draft = String.format("%s%s0%04d%07d", balanceAccount, currencyCode, filial, serial);
    int key = AccountValidator.calculateKey(draft, AccountValidator.prefixFor(type, bik));
    return draft.substring(0, 8) + key + draft.substring(9);
  }

  /** Генерирует рублёвый счёт указанного типа. */
  public String generate(String balanceAccount, AccountType type, String bik) {
    return generate(balanceAccount, type, bik, AccountValidator.RUB_CURRENCY);
  }
}
