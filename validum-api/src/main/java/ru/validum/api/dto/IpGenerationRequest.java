package ru.validum.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import ru.validum.api.validation.ValidRegion;

@Schema(description = "Запрос на генерацию профиля ИП")
public record IpGenerationRequest(
  @Schema(description = "Seed для воспроизводимости", example = "42") Long seed,
  @Schema(
    description = "Дата расчёта (ISO). От неё считаются возраст и сроки документов. "
      + "Не задана — сегодня.",
    example = "2026-01-01")
  java.time.LocalDate asOf,
  @Schema(description = "Пол", example = "MALE", allowableValues = {"MALE", "FEMALE"})
  @Pattern(regexp = "MALE|FEMALE", message = "Пол должен быть MALE или FEMALE")
  String gender,
  @Schema(description = "Возраст", example = "35") Integer age,
  @Schema(
    description = "Регион РФ — влияет на адреса и серию паспорта. Имя enum Region или код субъекта",
    example = "MOSCOW",
    allowableValues = {
      "SPB", "MOSCOW", "MO", "KRASNODAR", "SVERDLOVSK", "LENOBL", "NOVOSIBIRSK", "ROSTOV",
      "BASHKORTOSTAN", "TATARSTAN", "CHELYABINSK", "SAMARA", "NIZHNY_NOVGOROD", "KRASNOYARSK",
      "PRIMORSKY", "SARATOV"})
  @ValidRegion
  String region,
  @Schema(description = "Категория клиента — влияет на пакет", example = "ИП") String categoryClient,
  @Schema(description = "Пакет обслуживания", example = "БАЗОВЫЙ") String accountPackage,
  @Schema(description = "Флаг «террорист / экстремист»", example = "false") Boolean terrorist,
  @Schema(description = "Флаг «заблокирован банком»", example = "false") Boolean blocked,
  @Schema(
    description = "Явный БИК банка (P3.3). Если задан — банк ищется в справочнике. "
      + "Не найден → 400. Если не задан — случайный из справочника.",
    example = "044525225")
  @Pattern(regexp = "\\d{9}", message = "БИК должен содержать 9 цифр")
  String bik,

  @Schema(
    description = "Код ОКВЭД-2. Не задан — случайный из справочника (3069 позиций). "
      + "Неизвестный код → 400.",
    example = "62.01")
  @Pattern(
    regexp = "\\d{2}(\\.\\d{1,2}){0,2}",
    message = "ОКВЭД должен иметь вид 62, 62.0, 62.01 или 01.11.1")
  String okved) {}
