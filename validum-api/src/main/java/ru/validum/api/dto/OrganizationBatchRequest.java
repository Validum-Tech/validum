package ru.validum.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import ru.validum.api.validation.ValidRegion;

@Schema(description = "Запрос на пакетную генерацию профилей юрлиц")
public record OrganizationBatchRequest(
  @Schema(description = "Количество профилей", example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
  @Min(value = 1, message = "Минимум 1 профиль")
  @Max(value = 10000, message = "Максимум 10000 профилей")
  Integer count,

  @Schema(description = "Базовый seed для воспроизводимости", example = "42") Long seed,

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

  @Schema(description = "Код ОКВЭД-2 (применяется ко всем профилям в пакете)", example = "62.01")
  @Pattern(
    regexp = "\\d{2}(\\.\\d{1,2}){0,2}",
    message = "ОКВЭД должен иметь вид 62, 62.0, 62.01 или 01.11.1")
  String okved,

  @Schema(description = "БИК банка компании (применяется ко всем профилям в пакете)", example = "044525225")
  @Pattern(regexp = "\\d{9}", message = "БИК должен содержать 9 цифр")
  String bik,

  @Schema(description = "Флаг «террорист / экстремист»", example = "false") Boolean terrorist,
  @Schema(description = "Флаг «заблокирован банком»", example = "false") Boolean blocked) {}
