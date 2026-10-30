package ru.validum.core.bank;

/**
 * Банк РФ с реквизитами: БИК, наименование, корсчёт, город, регион.
 *
 * <p>БИК — 9 цифр: код РФ (2) + условный номер (3) + условный номер филиала (3) + контрольная (1).
 *
 * <p>Корсчёт — 20 цифр: признак 301 + код РФ + код валюты + контрольный ключ + 6 нулей + последние
 * 5 цифр БИК (у большинства банков).
 *
 * @param bik БИК банка (9 цифр)
 * @param name полное наименование
 * @param corrAccount корреспондентский счёт в ЦБ РФ (20 цифр)
 * @param city город головного офиса
 * @param region регион (субъект РФ)
 */
public record Bank(String bik, String name, String corrAccount, String city, String region) {

  public Bank {
    if (bik == null || bik.length() != 9) {
      throw new IllegalArgumentException("БИК должен содержать 9 цифр: " + bik);
    }
    if (corrAccount == null || corrAccount.length() != 20) {
      throw new IllegalArgumentException("Корсчёт должен содержать 20 цифр: " + corrAccount);
    }
  }
}
