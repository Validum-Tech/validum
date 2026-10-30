package ru.validum.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Связанный профиль индивидуального предпринимателя")
public record IpProfileDto(
  @Schema(example = "304500000000001") String ogrnip,
  @Schema(example = "Кузнецов Александр ИП Автотеста") String fullName,
  @Schema(example = "MALE") String gender,
  @Schema(example = "1988-03-12") String birthDate,
  @Schema(example = "35") int age,
  @Schema(example = "Женат/Замужем") String familyStatus,
  PassportDto passport,
  @Schema(example = "500100732259") String inn,
  @Schema(example = "12345678900") String snils,
  AddressDto registrationAddress,
  AddressDto postAddress,
  AddressDto currentAddress,
  ContactDto mobile,
  ContactDto email,
  @Schema(example = "РЫБА") String secretWord,
  @Schema(example = "КЛИЕНТ") String categoryClient,
  @Schema(example = "БАЗОВЫЙ") String accountPackage,
  @Schema(example = "ААА") String personalManager,
  @Schema(example = "false") boolean terrorist,
  @Schema(example = "false") boolean blocked,
  BankDto bank,
  @Schema(example = "40802810123456789012") String account,
  @Schema(example = "62.01") String okved,
  @Schema(example = "Разработка компьютерного программного обеспечения") String okvedName) {}
