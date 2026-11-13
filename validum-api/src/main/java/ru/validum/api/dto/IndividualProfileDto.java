package ru.validum.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Связанный профиль физлица")
public record IndividualProfileDto(
  @Schema(
    description = "Детерминированный идентификатор: тот же seed даёт тот же personId",
    example = "pd-9f2a6c1d0b7e4a53") String personId,
  @Schema(example = "Кузнецов Александр Владимирович") String fullName,
  @Schema(example = "MALE") String gender,
  @Schema(example = "1988-03-12") String birthDate,
  @Schema(example = "38") int age,
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
  @Schema(description = "Флаг «террорист / экстремист»", example = "false") boolean terrorist,
  @Schema(description = "Флаг «заблокирован банком»", example = "false") boolean blocked,
  BankDto bank,
  @Schema(example = "40817810123456789012") String account,
  @Schema(description = "Дополнительные документы (СПДУЛ): загранпаспорт, ВНЖ, РВП, военный билет…")
  List<IdentityDocumentDto> documents,
  @Schema(
    description = "Дата расчёта профиля (ISO), от неё посчитаны age и сроки документов",
    example = "2026-01-01")
  java.time.LocalDate asOf) {}
