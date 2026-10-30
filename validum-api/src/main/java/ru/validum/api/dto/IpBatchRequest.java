package ru.validum.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import ru.validum.api.validation.ValidRegion;

@Schema(description = "Запрос на пакетную генерацию профилей ИП")
public record IpBatchRequest(
  @Schema(description = "Количество профилей", example = "50", requiredMode = Schema.RequiredMode.REQUIRED)
  @Min(value = 1, message = "Минимум 1 профиль")
  @Max(value = 10000, message = "Максимум 10000 профилей")
  Integer count,

  @Schema(description = "Базовый seed для воспроизводимости", example = "42") Long seed,
  @Schema(
    description = "Дата расчёта (ISO) для всех профилей пачки; не задана — сегодня",
    example = "2026-01-01")
  java.time.LocalDate asOf,
  @Schema(description = "Пол", example = "MALE", allowableValues = {"MALE", "FEMALE"})
  @Pattern(regexp = "MALE|FEMALE", message = "Пол должен быть MALE или FEMALE")
  String gender,
  @Schema(description = "Возраст", example = "35") Integer age,
  @Schema(description = "Регион РФ — имя enum Region или код субъекта")
  @ValidRegion
  String region,
  @Schema(description = "Категория клиента") String categoryClient,
  @Schema(description = "Пакет обслуживания") String accountPackage,
  @Schema(description = "Флаг «террорист / экстремист»") Boolean terrorist,
  @Schema(description = "Флаг «заблокирован банком»") Boolean blocked,
  @Schema(
    description = "Явный БИК банка (P3.3). Применяется ко всем профилям в пакете.",
    example = "044525225")
  @Pattern(regexp = "\\d{9}", message = "БИК должен содержать 9 цифр")
  String bik,

  @Schema(description = "Код ОКВЭД-2 (применяется ко всем профилям в пакете)", example = "62.01")
  @Pattern(
    regexp = "\\d{2}(\\.\\d{1,2}){0,2}",
    message = "ОКВЭД должен иметь вид 62, 62.0, 62.01 или 01.11.1")
  String okved
) {}
