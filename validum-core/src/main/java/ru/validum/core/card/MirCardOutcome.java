package ru.validum.core.card;

/**
 * Ожидаемый исход операции по карте «Мир» — то, что платёжный шлюз вернёт в тестовой среде.
 *
 * <p>Для большинства тестовых карт нужен успешный исход; карты с отказом позволяют проверить
 * обработку ошибок в АБС и фронте без реального терминала.
 */
public enum MirCardOutcome {

  /** Операция разрешена. */
  SUCCESS("успех"),

  /** Недостаточно средств на счёте. */
  INSUFFICIENT_FUNDS("недостаточно средств"),

  /** Превышен лимит операции. */
  LIMIT_EXCEEDED("превышен лимит"),

  /** Срок действия карты истёк. */
  CARD_EXPIRED("карта просрочена"),

  /** Эмитент отклонил операцию. */
  ISSUER_DECLINED("отказ эмитента"),

  /** Карта недействительна (закрыта, заблокирована). */
  INVALID_CARD("карта недействительна");

  private final String russianName;

  MirCardOutcome(String russianName) {
    this.russianName = russianName;
  }

  /** Человеческое название исхода для отчётов и `describe`. */
  public String russianName() {
    return russianName;
  }
}
