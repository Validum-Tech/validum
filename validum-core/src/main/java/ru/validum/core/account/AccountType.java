package ru.validum.core.account;

/**
 * Тип счёта в терминах кодовых значений УФЭБС (атрибут {@code AccountType} в ED807,
 * {@code cbr_packeteid_v2026.09.0.xsd}).
 *
 * <p>Тип определяет правило расчёта контрольного ключа (письмо Банка России № 515 от 08.09.1997):
 *
 * <ul>
 *   <li>{@link Scope#CLIENT} — счёт открыт в кредитной организации: условный номер = разряды 7-9
 *       БИК банка;
 *   <li>{@link Scope#CORRESPONDENT} и {@link Scope#BANK_OF_RUSSIA} — счёт открыт в подразделении
 *       Банка России: условный номер = «0» + разряды 5-6 БИК этого подразделения.
 * </ul>
 */
public enum AccountType {

  /** Банковский счёт. */
  BANA("BANA", "Банковский счёт", Scope.CLIENT),

  /** Счёт в Банке России. */
  CBRA("CBRA", "Счёт в Банке России", Scope.BANK_OF_RUSSIA),

  /** Корреспондентский счёт. */
  CRSA("CRSA", "Корреспондентский счёт", Scope.CORRESPONDENT),

  /** Транзитный счёт. */
  TRSA("TRSA", "Транзитный счёт", Scope.CLIENT),

  /** Клиринговый счёт. */
  CLRA("CLRA", "Клиринговый счёт", Scope.CLIENT),

  /** Счёт единого платежного гарантийного агентства (ЕПГА). */
  EPGA("EPGA", "Счёт ЕПГА", Scope.CLIENT),

  /** Специальный счёт ЕПСА. */
  EPSA("EPSA", "Счёт ЕПСА", Scope.CLIENT),

  /** Гарантийный счёт. */
  GARA("GARA", "Гарантийный счёт", Scope.CLIENT),

  /** Счёт доверительного управления. */
  TRUA("TRUA", "Счёт доверительного управления", Scope.CLIENT),

  /** Счёт доверительного управления (учётный). */
  UTRA("UTRA", "Счёт доверительного управления (учёт)", Scope.CLIENT),

  /** Клиентский счёт (расчётный/текущий счёт клиента банка). */
  CLAC("CLAC", "Клиентский счёт", Scope.CLIENT),

  /** Счёт цифрового рубля. */
  CBDC("CBDC", "Счёт цифрового рубля", Scope.CLIENT);

  /** Где открыт счёт — от этого зависит правило контрольного ключа. */
  public enum Scope {
    /** Счёт открыт в кредитной организации. */
    CLIENT,
    /** Счёт открыт в подразделении Банка России (корреспондентский). */
    CORRESPONDENT,
    /** Счёт самого Банка России. */
    BANK_OF_RUSSIA
  }

  private final String code;
  private final String title;
  private final Scope scope;

  AccountType(String code, String title, Scope scope) {
    this.code = code;
    this.title = title;
    this.scope = scope;
  }

  /** Код из классификатора УФЭБС, например {@code CRSA}. */
  public String code() {
    return code;
  }

  /** Человекочитаемое название. */
  public String title() {
    return title;
  }

  /** Область открытия счёта. */
  public Scope scope() {
    return scope;
  }

  /** Является ли счёт клиентским (условный номер берётся из разрядов 7-9 БИК). */
  public boolean isClientAccount() {
    return scope == Scope.CLIENT;
  }

  /**
   * Находит тип по коду (регистр не важен).
   *
   * @throws IllegalArgumentException если код неизвестен
   */
  public static AccountType fromCode(String code) {
    if (code != null) {
      String normalized = code.trim().toUpperCase();
      for (AccountType type : values()) {
        if (type.code.equals(normalized)) {
          return type;
        }
      }
    }
    throw new IllegalArgumentException("Неизвестный тип счёта: " + code);
  }
}
