package ru.validum.core.name;

/**
 * Семейное положение. Русское название — как в UI АБС, цифровой код — как в SQL-запросе.
 */
public enum FamilyStatus {
  SINGLE("Холост/Не замужем", "0"),
  DIVORCED("Разведен(на)", "1"),
  MARRIED("Женат/Замужем", "2"),
  CIVIL_MARRIAGE("Гражданский брак", "3"),
  WIDOWED("Вдова/Вдовец", "4");

  private final String russianName;
  private final String code;

  FamilyStatus(String russianName, String code) {
    this.russianName = russianName;
    this.code = code;
  }

  public String russianName() {
    return russianName;
  }

  public String code() {
    return code;
  }
}
