package ru.validum.core.validation;

/**
 * Валидатор ОГРН (13) и ОГРНИП (15).
 */
public final class OgrnValidator {

  private OgrnValidator() {}

  public static boolean isValid(String ogrn) {
    if (ogrn == null) return false;
    return isValidLegalEntity(ogrn) || isValidIndividualEntrepreneur(ogrn);
  }

  public static boolean isValidLegalEntity(String ogrn) {
    if (ogrn == null || ogrn.length() != 13 || !ogrn.matches("\\d{13}")) return false;
    long numeric = Long.parseLong(ogrn);
    int control = (int) ((numeric / 10) % 11 % 10);
    return control == (ogrn.charAt(12) - '0');
  }

  public static boolean isValidIndividualEntrepreneur(String ogrnip) {
    if (ogrnip == null || ogrnip.length() != 15 || !ogrnip.matches("\\d{15}")) return false;
    long numeric = Long.parseLong(ogrnip);
    int control = (int) ((numeric / 10) % 13 % 10);
    return control == (ogrnip.charAt(14) - '0');
  }
}
