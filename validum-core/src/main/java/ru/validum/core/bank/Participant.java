package ru.validum.core.bank;

/**
 * Участник справочника БИК ED807 — кредитная организация или небанковский участник платёжной
 * системы Банка России (РКЦ, казначейство, филиал, конкурсный управляющий и т.д.).
 *
 * <p>Более общая модель, чем {@link Bank}: корсчёт не обязателен — у небанковских участников
 * его нет (значение {@code null}), поэтому {@link Bank#corrAccount()} сюда не натягивается.
 *
 * @param bik БИК участника (9 цифр)
 * @param name полное наименование
 * @param type тип участника (PtType)
 * @param status статус участника (PSAC/PSDL)
 * @param corrAccount корсчёт (CRSA + ACAC); {@code null}, если у участника его нет
 * @param city город (может быть пустым)
 * @param region код региона (может быть пустым)
 * @param regN регистрационный номер участника в ЦБ (может быть {@code null})
 * @param uid идентификатор участника UID, 10 цифр (может быть {@code null})
 * @param address почтовый адрес {@code Adr} из справочника (может быть {@code null})
 * @param index почтовый индекс {@code Ind} (может быть {@code null})
 */
public record Participant(
    String bik,
    String name,
    ParticipantType type,
    String status,
    String corrAccount,
    String city,
    String region,
    String regN,
    String uid,
    String address,
    String index) {

  /** Участник без реквизитов ЦБ: БИК, наименование, тип, статус, корсчёт, город, регион. */
  public Participant(
      String bik,
      String name,
      ParticipantType type,
      String status,
      String corrAccount,
      String city,
      String region) {
    this(bik, name, type, status, corrAccount, city, region, null, null, null, null);
  }

  public Participant {
    if (bik == null || bik.length() != 9) {
      throw new IllegalArgumentException("БИК должен содержать 9 цифр: " + bik);
    }
    if (corrAccount != null && corrAccount.length() != 20) {
      throw new IllegalArgumentException("Корсчёт должен содержать 20 цифр: " + corrAccount);
    }
  }

  /** Действующий банк с корсчётом: PtType=20, статус PSAC, есть корсчёт. */
  public boolean isActiveBank() {
    return type == ParticipantType.BANK && "PSAC".equals(status) && corrAccount != null;
  }

  /** Преобразует в {@link Bank}; корсчёт обязателен. */
  public Bank toBank() {
    if (corrAccount == null) {
      throw new IllegalStateException(
        "Участник " + bik + " не является банком с корсчётом (PtType=" + type.code() + ")");
    }
    return new Bank(bik, name, corrAccount, city, region);
  }
}
