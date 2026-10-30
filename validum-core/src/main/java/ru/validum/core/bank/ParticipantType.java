package ru.validum.core.bank;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Тип участника перевода (PtType) справочника БИК ED807 (УФЭБС ЦБ РФ).
 *
 * <p>Источник: документ «УФЭБС 2026.09.0 — КБР Кодовые Значения», таблица 2, пункт 97.
 *
 * <p>Важно: PtType — <b>текстовый</b> тип до 2 символов ({@code Max2TextType}): ведущий ноль
 * значим, поэтому {@code "00"} — это две цифры. Аналог в BNKSEEK.DBF — {@code [BNKSEEK.PZN]}.
 *
 * <p>Семантика кодов (дословно по нормативу):
 * <ul>
 *   <li>{@code 20} — кредитная организация (единственный «банк» в смысле validum);</li>
 *   <li>{@code 10} — расчётно-кассовый центр (РКЦ), <b>не</b> {@code 90};</li>
 *   <li>{@code 90} — конкурсный управляющий (ликвидатор), <b>не</b> РКЦ;</li>
 *   <li>{@code 51}/{@code 52} — федеральное казначейство / его территориальный орган.</li>
 * </ul>
 */
public enum ParticipantType {

  BR_HEAD_OFFICE("00", "Главное управление Банка России или операционно-кассовый центр", false),
  RKC("10", "Расчетно-кассовый центр или операционно-кассовый центр", false),
  BR_BRANCH("12", "Отделение, отделение – национальный банк главного управления Банка России", false),
  BR_CENTRAL_SUBDIVISION("15", "Структурное подразделение центрального аппарата Банка России", false),
  CASH_CENTER("16", "Кассовый центр", false),
  BANK("20", "Кредитная организация", true),
  BANK_BRANCH("30", "Филиал кредитной организации", false),
  BR_FIELD_OFFICE("40", "Полевое учреждение Банка России", false),
  TREASURY("51", "Федеральное казначейство", false),
  TREASURY_REGIONAL("52", "Территориальный орган Федерального казначейства", false),
  FOREIGN_BANK("60", "Иностранный банк (иностранная кредитная организация)", false),
  FOREIGN_CENTRAL_BANK("65", "Иностранный центральный (национальный) банк", false),
  INDIRECT_PARTICIPANT("71", "Клиент кредитной организации, являющийся косвенным участником", false),
  CLEARING_ORGANIZATION("75", "Клиринговая организация", false),
  EXTERNAL_PAYMENT_SYSTEM("78", "Внешняя платежная система", false),
  LIQUIDATOR("90", "Конкурсный управляющий (ликвидатор, ликвидационная комиссия)", false),
  BR_CLIENT("99", "Клиент Банка России, не являющийся участником платежной системы", false);

  private final String code;
  private final String description;
  private final boolean bank;

  private static final Map<String, ParticipantType> BY_CODE =
    Arrays.stream(values())
      .collect(Collectors.toUnmodifiableMap(ParticipantType::code, Function.identity()));

  ParticipantType(String code, String description, boolean bank) {
    this.code = code;
    this.description = description;
    this.bank = bank;
  }

  /** Двухсимвольный код PtType (ведущий ноль значим). */
  public String code() {
    return code;
  }

  /** Русское описание из норматива. */
  public String description() {
    return description;
  }

  /** {@code true} только для {@link #BANK} (кредитная организация). */
  public boolean isBank() {
    return bank;
  }

  /** По коду PtType (например {@code "90"}). Кидает исключение на неизвестный код. */
  public static ParticipantType fromCode(String code) {
    ParticipantType type = BY_CODE.get(code);
    if (type == null) {
      throw new IllegalArgumentException("Неизвестный PtType: " + code);
    }
    return type;
  }

  /**
   * По коду PtType ({@code "10"}) или по имени константы ({@code "RKC"} или {@code "rkc"}).
   * Удобно для CLI.
   */
  public static ParticipantType fromCodeOrName(String value) {
    String v = value == null ? "" : value.trim();
    ParticipantType byCode = BY_CODE.get(v);
    if (byCode != null) {
      return byCode;
    }
    return valueOf(v.toUpperCase());
  }
}
