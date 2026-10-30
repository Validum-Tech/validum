package ru.validum.core.organization;

import ru.validum.core.address.Address;
import ru.validum.core.bank.Bank;
import ru.validum.core.name.PersonName;
import ru.validum.core.passport.Passport;
import ru.validum.core.profile.IndividualProfile;

/**
 * Профиль индивидуального предпринимателя.
 *
 * <p>Композиция: {@link #base} — обычный профиль физлица, {@link #ogrnip} — ОГРНИП (15 цифр).
 * Все поля ФЛ (ФИО, паспорт, ИНН, СНИЛС, адреса, счёт, флаги) доступны через делегирующие методы.
 *
 * <p>Отчество ИП — фиксированное: {@link IpProfileGenerator#IP_PATRONYMIC}.
 */
public record IpProfile(
  IndividualProfile base,
  String ogrnip,
  String okved,
  String okvedName) {

  // === Делегирующие методы — удобный доступ к базовым полям ===

  public PersonName name() {
    return base.name();
  }

  public String fullName() {
    return base.fullName();
  }

  public int age() {
    return base.age();
  }

  public String inn() {
    return base.inn();
  }

  public String snils() {
    return base.snils();
  }

  public Passport passport() {
    return base.passport();
  }

  public Address registrationAddress() {
    return base.registrationAddress();
  }

  public Address postAddress() {
    return base.postAddress();
  }

  public Address currentAddress() {
    return base.currentAddress();
  }

  public String categoryClient() {
    return base.categoryClient();
  }

  public String accountPackage() {
    return base.accountPackage();
  }

  public Bank bank() {
    return base.bank();
  }

  public String account() {
    return base.account();
  }

  public boolean hasAccount() {
    return base.hasAccount();
  }

  public boolean terrorist() {
    return base.terrorist();
  }

  public boolean blocked() {
    return base.blocked();
  }
}
