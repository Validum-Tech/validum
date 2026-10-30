package ru.validum.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import ru.validum.api.validation.ValidRegion;

@Schema(description = "Запрос на генерацию профиля физлица")
public record IndividualGenerationRequest(
  @Schema(description = "Пол", example = "MALE", allowableValues = {"MALE", "FEMALE"})
  @Pattern(regexp = "MALE|FEMALE", message = "Пол должен быть MALE или FEMALE")
  String gender,
  @Schema(description = "Seed для воспроизводимости", example = "42") Long seed,
  @Schema(
    description = "Дата расчёта (ISO). От неё считаются возраст и сроки документов; "
      + "один seed + одна дата = один профиль в любой день. Не задана — сегодня.",
    example = "2026-01-01")
  java.time.LocalDate asOf,
  @Schema(description = "Возраст", example = "23") Integer age,
  @Schema(
    description = "Менялся ли паспорт. Не задан — каскад вычислит по возрасту (age >= 20 → true)",
    example = "true")
  Boolean passportWasExchanged,
  @Schema(
    description = "Страна для нерезидента (ISO). Если задана — профиль становится нерезидентом",
    example = "TJ")
  String residencyCountry,
  @Schema(description = "Тип паспорта", example = "RF", allowableValues = {"RF", "FOREIGN"})
  @Pattern(regexp = "RF|FOREIGN", message = "Тип паспорта должен быть RF или FOREIGN")
  String passportType,
  @Schema(description = "Алфавит ФИО", example = "CYRILLIC", allowableValues = {"CYRILLIC", "LATIN"})
  @Pattern(regexp = "CYRILLIC|LATIN", message = "Алфавит должен быть CYRILLIC или LATIN")
  String fioScript,
  @Schema(description = "Поля с битой контрольной суммой", example = "[\"INN\"]")
  List<@Pattern(regexp = "INN|SNILS") String> corrupt,
  @Schema(
    description = "Регион РФ — влияет на адреса и серию паспорта. Имя enum Region или код субъекта",
    example = "MOSCOW",
    allowableValues = {
      "SPB", "MOSCOW", "MO", "KRASNODAR", "SVERDLOVSK", "LENOBL", "NOVOSIBIRSK", "ROSTOV",
      "BASHKORTOSTAN", "TATARSTAN", "CHELYABINSK", "SAMARA", "NIZHNY_NOVGOROD", "KRASNOYARSK",
      "PRIMORSKY", "SARATOV"})
  @ValidRegion
  String region,
  @Schema(
    description = "Категория клиента. Влияет на пакет обслуживания (каскад)",
    example = "ЗАРПЛАТНИК")
  String categoryClient,
  @Schema(description = "Пакет обслуживания. Если не задан — по категории", example = "ЗАРПЛАТНЫЙ")
  String accountPackage,
  @Schema(
    description = "Семейное положение. Если не задано — по возрасту (age<18 → SINGLE)",
    example = "MARRIED",
    allowableValues = {"SINGLE", "DIVORCED", "MARRIED", "CIVIL_MARRIAGE", "WIDOWED"})
  @Pattern(
    regexp = "SINGLE|DIVORCED|MARRIED|CIVIL_MARRIAGE|WIDOWED",
    message = "Неизвестное семейное положение")
  String familyStatus,
  @Schema(description = "Флаг «террорист / экстремист»", example = "false") Boolean terrorist,
  @Schema(description = "Флаг «заблокирован банком»", example = "false") Boolean blocked,
  @Schema(
    description = "Явный БИК банка (P3.3). Если задан — банк ищется в справочнике. "
      + "Не найден → 400. Если не задан — случайный из справочника.",
    example = "044525225")
  @Pattern(regexp = "\\d{9}", message = "БИК должен содержать 9 цифр")
  String bik,
  @Schema(
    description = "Дополнительные документы (СПДУЛ): PASSPORT_RF, FOREIGN_PASSPORT, "
      + "RESIDENCE_PERMIT, TEMPORARY_RESIDENCE, REFUGEE_CERTIFICATE, MILITARY_ID, "
      + "MILITARY_ID_RESERVE, DIPLOMATIC_PASSPORT, VISA, DRIVER_LICENSE, PASSPORT_FOREIGN",
    example = "[\"PASSPORT_RF\", \"FOREIGN_PASSPORT\"]")
  List<String> documents) {}
