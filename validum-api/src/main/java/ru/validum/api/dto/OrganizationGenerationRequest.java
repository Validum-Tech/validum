package ru.validum.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import ru.validum.api.validation.ValidRegion;

@Schema(description = "Запрос на генерацию профиля юрлица")
public record OrganizationGenerationRequest(
  @Schema(description = "Seed для воспроизводимости", example = "42") Long seed,

  @Schema(
    description = "Организационно-правовая форма",
    example = "LLC",
    allowableValues = {"LLC", "JSC", "PJSC", "NCO", "FOUNDATION", "SE", "ME"})
  @Pattern(
    regexp = "LLC|JSC|PJSC|NCO|FOUNDATION|SE|ME",
    message = "Тип должен быть LLC, JSC, PJSC, NCO, FOUNDATION, SE или ME")
  String organizationType,

  @Schema(description = "Наименование. Если не задано — генерируется автоматически")
  String organizationName,

  @Schema(
    description = "Регион РФ — влияет на адрес и КПП. Имя enum Region или код субъекта",
    example = "MOSCOW",
    allowableValues = {
      "SPB", "MOSCOW", "MO", "KRASNODAR", "SVERDLOVSK", "LENOBL", "NOVOSIBIRSK", "ROSTOV",
      "BASHKORTOSTAN", "TATARSTAN", "CHELYABINSK", "SAMARA", "NIZHNY_NOVGOROD", "KRASNOYARSK",
      "PRIMORSKY", "SARATOV"})
  @ValidRegion
  String region,

  @Schema(
    description = "Код ОКВЭД-2. Не задан — случайный из справочника (3069 позиций). "
      + "Неизвестный код → 400.",
    example = "62.01")
  @Pattern(
    regexp = "\\d{2}(\\.\\d{1,2}){0,2}",
    message = "ОКВЭД должен иметь вид 62, 62.0, 62.01 или 01.11.1")
  String okved,

  @Schema(
    description = "БИК банка компании (9 цифр). Не задан — случайный из справочника; "
      + "неизвестный → 400.",
    example = "044525225")
  @Pattern(regexp = "\\d{9}", message = "БИК должен содержать 9 цифр")
  String bik,

  @Schema(description = "Флаг «террорист / экстремист»", example = "false") Boolean terrorist,
  @Schema(description = "Флаг «заблокирован банком»", example = "false") Boolean blocked) {}
