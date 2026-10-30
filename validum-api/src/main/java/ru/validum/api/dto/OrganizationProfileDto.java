package ru.validum.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Связанный профиль юрлица")
public record OrganizationProfileDto(
  @Schema(example = "ООО «Ромашка»") String organizationName,
  @Schema(
    example = "LLC",
    allowableValues = {"LLC", "JSC", "PJSC", "NCO", "FOUNDATION", "SE", "ME"}
  ) String organizationType,
  @Schema(example = "1027700123456") String ogrn,
  @Schema(example = "7701234567") String inn,
  @Schema(example = "770101001") String kpp,
  @Schema(example = "12345678") String okpo,
  @Schema(example = "65") String okopf,
  @Schema(example = "16") String okfs,
  @Schema(example = "4210014") String okogu,
  @Schema(example = "62.01") String okved,
  @Schema(example = "Разработка компьютерного программного обеспечения") String okvedName,
  @Schema(example = "Небюджетная организация") String budget,
  @Schema(example = "SABRRUMMXXX") String swift,
  AddressDto registrationAddress,
  AddressDto postAddress,
  ContactDto mobile,
  ContactDto email,
  @Schema(example = "false") boolean terrorist,
  @Schema(example = "false") boolean blocked,
  @Schema(description = "Банк компании (БИК из справочника ЦБ)") BankDto bank,
  @Schema(example = "40702810400000001234", description = "Расчётный счёт ЮЛ") String account) {}
