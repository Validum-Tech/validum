package ru.validum.core.document;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import ru.validum.core.name.Gender;
import ru.validum.core.profile.Residency;

/**
 * Правила, при которых набор документов у одного человека имеет смысл.
 *
 * <p>Проверки сознательно строгие и кидают {@link IllegalArgumentException} с русским текстом:
 * лучше упасть на старте генерации, чем отдать в АБС профиль, который не проходит проверку КУС
 * (например, загранпаспорт без паспорта РФ или ВНЖ у резидента РФ).
 *
 * <p>Непроверенные форматы (миграционная карта, свидетельства о рождении и смерти, паспорт моряка)
 * в {@link DocumentType} отсутствуют — см. приказы МВД № 365 и МО № 700, ПП РФ № 356 и № 789.
 */
public final class DocumentRules {

  /** Призывной возраст для военного билета рядового состава. */
  public static final int MILITARY_MAX_AGE = 27;

  /** С какого возраста выдаётся водительское удостоверение и военный билет офицера запаса. */
  public static final int ADULT_AGE = 18;

  private DocumentRules() {}

  /**
   * Проверяет совместимость набора документов с полом, возрастом и резидентством.
   *
   * @throws IllegalArgumentException если набор внутренне противоречив
   */
  public static void validate(
    Collection<DocumentType> types, Gender gender, int age, Residency residency) {
    if (types == null || types.isEmpty()) {
      return;
    }
    boolean nonResident = residency != null && !residency.isResident();

    if (types.contains(DocumentType.FOREIGN_PASSPORT) && !types.contains(DocumentType.PASSPORT_RF)) {
      throw new IllegalArgumentException(
        "Загранпаспорт РФ (22) требует паспорт РФ (21) в том же наборе документов");
    }
    if (types.contains(DocumentType.PASSPORT_FOREIGN) && !nonResident) {
      throw new IllegalArgumentException(
        "Паспорт иностранного гражданина (10) — только для нерезидента");
    }
    if (hasMigrationDocument(types) && !nonResident) {
      throw new IllegalArgumentException(
        "ВНЖ (12), РВП (15) и удостоверение беженца (13) — только для нерезидента");
    }
    boolean military = hasMilitaryId(types);
    if (military && gender != Gender.MALE) {
      throw new IllegalArgumentException("Военный билет (07/27) — только для мужчин");
    }
    if (types.contains(DocumentType.MILITARY_ID) && (age < ADULT_AGE || age > MILITARY_MAX_AGE)) {
      throw new IllegalArgumentException(
        "Военный билет (07) — призывной возраст 18–27 лет, а задан " + age);
    }
    if (types.contains(DocumentType.MILITARY_ID_RESERVE) && age < ADULT_AGE) {
      throw new IllegalArgumentException(
        "Военный билет офицера запаса (27) — с 18 лет, а задан " + age);
    }
    if (types.contains(DocumentType.DRIVER_LICENSE) && age < ADULT_AGE) {
      throw new IllegalArgumentException("Водительское удостоверение — с 18 лет, а задан " + age);
    }
    for (DocumentType type : types) {
      if (type == DocumentType.MILITARY_ID
        || type == DocumentType.MILITARY_ID_RESERVE
        || type == DocumentType.DRIVER_LICENSE) {
        continue; // окна этих документов уже проверены выше — с точными сообщениями
      }
      if (age < type.minAge()) {
        throw new IllegalArgumentException(
          type.title() + " — не выдаётся раньше " + type.minAge() + " лет, а задан " + age);
      }
      if (type.maxAge() != null && age > type.maxAge()) {
        throw new IllegalArgumentException(
          type.title() + " — не выдаётся позже " + type.maxAge() + " лет, а задан " + age);
      }
    }
  }

  /**
   * Подобрать правдоподобный набор ДУЛ по возрасту, полу и резидентству.
   *
   * <p>Правило держится на жизненных вехах: до 14 лет паспорта РФ нет, с 14 — есть; права
   * и военный билет появляются с 18; военный билет рядового состава — только у мужчин
   * призывного возраста. У нерезидента основным документом становится паспорт иностранного
   * гражданина, а с 18 лет — ещё и вид на жительство.
   *
   * <p>Возвращённый набор гарантированно проходит {@link #validate}: автонабор не должен
   * падать на собственных правилах.
   */
  public static List<DocumentType> autoSelect(int age, Gender gender, Residency residency) {
    List<DocumentType> types = new ArrayList<>(4);
    boolean nonResident = residency != null && !residency.isResident();

    if (nonResident) {
      types.add(DocumentType.PASSPORT_FOREIGN);
      if (age >= ADULT_AGE) {
        types.add(DocumentType.RESIDENCE_PERMIT);
      }
      return List.copyOf(types);
    }

    if (age < DocumentType.PASSPORT_RF.minAge()) {
      return List.of();
    }
    // Паспорт РФ сюда НЕ добавляется: он уже есть в профиле как основной документ
    // (IndividualProfile.passport), и второй такой же в списке ДУЛ — дубль.
    if (age >= ADULT_AGE) {
      types.add(DocumentType.DRIVER_LICENSE);
      if (gender == Gender.MALE && age <= MILITARY_MAX_AGE) {
        types.add(DocumentType.MILITARY_ID);
      }
    }
    return List.copyOf(types);
  }

  private static boolean hasMigrationDocument(Collection<DocumentType> types) {
    return types.contains(DocumentType.RESIDENCE_PERMIT)
      || types.contains(DocumentType.TEMPORARY_RESIDENCE)
      || types.contains(DocumentType.REFUGEE_CERTIFICATE);
  }

  private static boolean hasMilitaryId(Collection<DocumentType> types) {
    return types.contains(DocumentType.MILITARY_ID) || types.contains(DocumentType.MILITARY_ID_RESERVE);
  }
}
