package ru.validum.core.profile;

import java.time.LocalDate;
import java.util.List;
import ru.validum.core.address.Address;
import ru.validum.core.bank.Bank;
import ru.validum.core.contact.Contact;
import ru.validum.core.document.IdentityDocument;
import ru.validum.core.name.FamilyStatus;
import ru.validum.core.name.Gender;
import ru.validum.core.name.PersonName;
import ru.validum.core.passport.Passport;

/**
 * Связанный профиль клиента-физлица.
 *
 * <p>Все поля согласованы между собой:
 *
 * <ul>
 *   <li>ФИО и пол — согласованы;
 *   <li>паспорт выдан по правилам РФ (14/20/45) и имеет срок действия;
 *   <li>ИНН, СНИЛС — валидны по алгоритмам ФНС/ПФР;
 *   <li>счёт (если есть) — валиден по 809-П;
 *   <li>БИК и корсчёт — из справочника ЦБ.
 * </ul>
 *
 * <p>АБС-специфичные поля (salutation, riskC, crcbTypeC) в модель не входят — они применяются на
 * уровне мапперов интеграции через {@code AbsDefaults}.
 *
 * <p>Флаги {@link #terrorist} и {@link #blocked} — для тестов клиентов, на которых АБС должна
 * реагировать особым образом. По умолчанию {@code false}.
 */
public record IndividualProfile(
  // === Идентификация ===
  PersonName name,
  LocalDate birthDate,
  Address birthPlace,
  FamilyStatus familyStatus,

  // === Резидентство ===
  Residency residency,

  // === Документы ===
  Passport passport,
  String inn,
  String snils,

  // === Адреса (3 по стандарту АБС) ===
  Address registrationAddress,
  Address postAddress,
  Address currentAddress,

  // === Контакты ===
  Contact mobile,
  Contact email,

  // === Дополнительная информация ===
  String secretWord,
  String categoryClient,
  String accountPackage,
  String personalManager,

  // === Флаги ===
  boolean terrorist,
  boolean blocked,

  // === Банк и счёт ===
  Bank bank,
  String account,

  /**
   * Дополнительные документы, удостоверяющие личность (СПДУЛ ФНС). Паспорт РФ и иностранный
   * паспорт по-прежнему лежат в {@link #passport}; здесь — ВНЖ, РВП, загранпаспорт, военный билет и
   * т. п. Пустой список, если дополнительные документы не запрашивались.
   */
  List<IdentityDocument> documents,

  /**
   * Дата расчёта профиля: относительно неё считаются возраст, даты выдачи паспорта и документов.
   *
   * <p>Пустое значение трактуется как «сегодня» — так собраны профили, созданные до появления
   * поля. Задавайте явно (builder {@code .asOf(...)}), если нужна воспроизводимость между днями.
   */
  LocalDate asOf,

  /**
   * Детерминированный идентификатор человека: выводится из seed, которым собран профиль
   * (см. {@link ru.validum.core.common.PersonIds}). Пусто у профилей, собранных без seed —
   * например, при импорте CSV без колонки {@code personId}.
   */
  String personId) {

  /** Полное ФИО одной строкой. */
  public String fullName() {
    return name.fullName();
  }

  /** Пол клиента. */
  public Gender gender() {
    return name.gender();
  }

  /** Возраст на дату расчёта профиля (или на сегодня, если она не зафиксирована). */
  public int age() {
    return ageAt(asOf != null ? asOf : LocalDate.now());
  }

  /** Возраст на указанную дату. */
  public int ageAt(LocalDate at) {
    int age = at.getYear() - birthDate.getYear();
    if (birthDate.plusYears(age).isAfter(at)) {
      age--;
    }
    return age;
  }

  /** Есть ли у клиента счёт. */
  public boolean hasAccount() {
    return account != null && !account.isBlank();
  }
  /** Копия профиля с заменённым ФИО — используется в композиции (например, ИП). */
  public IndividualProfile withName(PersonName newName) {
    return new IndividualProfile(
      newName,
      birthDate,
      birthPlace,
      familyStatus,
      residency,
      passport,
      inn,
      snils,
      registrationAddress,
      postAddress,
      currentAddress,
      mobile,
      email,
      secretWord,
      categoryClient,
      accountPackage,
      personalManager,
      terrorist,
      blocked,
      bank,
      account,
      documents,
      asOf,
      personId);
  }
}
