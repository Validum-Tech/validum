package ru.validum.api.mapper;

import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;
import ru.validum.api.dto.AddressDto;
import ru.validum.api.dto.BankDto;
import ru.validum.api.dto.ContactDto;
import ru.validum.api.dto.IdentityDocumentDto;
import ru.validum.api.dto.IndividualProfileDto;
import ru.validum.api.dto.IpProfileDto;
import ru.validum.api.dto.OrganizationProfileDto;
import ru.validum.api.dto.PassportDto;
import ru.validum.core.address.Address;
import ru.validum.core.bank.Bank;
import ru.validum.core.contact.Contact;
import ru.validum.core.document.IdentityDocument;
import ru.validum.core.organization.IpProfile;
import ru.validum.core.organization.OrganizationProfile;
import ru.validum.core.profile.IndividualProfile;

/** Преобразование доменных моделей в DTO для API. */
@Component
public class ProfileMapper {

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

  // ── ФЛ ────────────────────────────────────────────────────────────────────

  public IndividualProfileDto toDto(IndividualProfile profile) {
    return new IndividualProfileDto(
      profile.fullName(),
      profile.gender().name(),
      profile.birthDate().format(DATE_FORMAT),
      profile.age(),
      profile.familyStatus().russianName(),
      toDto(profile.passport()),
      profile.inn(),
      profile.snils(),
      toDto(profile.registrationAddress()),
      toDto(profile.postAddress()),
      toDto(profile.currentAddress()),
      toDto(profile.mobile()),
      toDto(profile.email()),
      profile.secretWord(),
      profile.categoryClient(),
      profile.accountPackage(),
      profile.personalManager(),
      profile.terrorist(),
      profile.blocked(),
      toDto(profile.bank()),
      profile.account(),
      profile.documents().stream().map(this::toDto).toList(),
      profile.asOf());
  }

  // ── ИП ────────────────────────────────────────────────────────────────────

  public IpProfileDto toDto(IpProfile ip) {
    IndividualProfile base = ip.base();
    return new IpProfileDto(
      ip.ogrnip(),
      base.fullName(),
      base.gender().name(),
      base.birthDate().format(DATE_FORMAT),
      base.age(),
      base.familyStatus().russianName(),
      toDto(base.passport()),
      base.inn(),
      base.snils(),
      toDto(base.registrationAddress()),
      toDto(base.postAddress()),
      toDto(base.currentAddress()),
      toDto(base.mobile()),
      toDto(base.email()),
      base.secretWord(),
      base.categoryClient(),
      base.accountPackage(),
      base.personalManager(),
      base.terrorist(),
      base.blocked(),
      toDto(base.bank()),
      base.account(),
      ip.okved(),
      ip.okvedName());
  }

  // ── ЮЛ ────────────────────────────────────────────────────────────────────

  public OrganizationProfileDto toDto(OrganizationProfile org) {
    return new OrganizationProfileDto(
      org.organizationName(),
      org.organizationType().name(),
      org.ogrn(),
      org.inn(),
      org.kpp(),
      org.okpo(),
      org.okopf(),
      org.okfs(),
      org.okogu(),
      org.okved(),
      org.okvedName(),
      org.budget(),
      org.swift(),
      toDto(org.registrationAddress()),
      toDto(org.postAddress()),
      toDto(org.mobile()),
      toDto(org.email()),
      org.terrorist(),
      org.blocked(),
      toDto(org.bank()),
      org.account());
  }

  // ── Приватные ─────────────────────────────────────────────────────────────

  private PassportDto toDto(ru.validum.core.passport.Passport passport) {
    return new PassportDto(
      passport.series(),
      passport.number(),
      passport.issuedBy(),
      passport.issueDate().format(DATE_FORMAT),
      passport.departmentCode(),
      passport.type().name(),
      passport.country().isoCode());
  }

  private AddressDto toDto(Address address) {
    return new AddressDto(
      address.postalCode(),
      address.region(),
      address.regionType(),
      address.city(),
      address.street(),
      address.building(),
      address.apartment(),
      address.formatted());
  }

  private ContactDto toDto(Contact contact) {
    return new ContactDto(
      contact.type().name(), contact.value(), contact.description(), contact.status());
  }

  private IdentityDocumentDto toDto(IdentityDocument document) {
    return new IdentityDocumentDto(
      document.type().spdulCode(),
      document.type().title(),
      document.series(),
      document.number(),
      document.issuedBy(),
      document.issueDate().format(DATE_FORMAT),
      document.expirationDate() == null ? null : document.expirationDate().format(DATE_FORMAT),
      document.departmentCode());
  }

  private BankDto toDto(Bank bank) {
    return new BankDto(bank.bik(), bank.name(), bank.corrAccount(), bank.city(), bank.region());
  }
}
