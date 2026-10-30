package ru.validum.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import ru.validum.api.validation.ValidRegion;

@Schema(description = "Запрос на пакетную генерацию профилей физлиц")
public record IndividualBatchRequest(
  @Schema(description = "Количество профилей", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
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
  @Schema(description = "Возраст", example = "23") Integer age,
  @Schema(description = "Менялся ли паспорт") Boolean passportWasExchanged,
  @Schema(description = "Страна для нерезидента") String residencyCountry,
  @Schema(description = "Тип паспорта", allowableValues = {"RF", "FOREIGN"})
  @Pattern(regexp = "RF|FOREIGN", message = "Тип паспорта должен быть RF или FOREIGN")
  String passportType,
  @Schema(description = "Алфавит ФИО", allowableValues = {"CYRILLIC", "LATIN"})
  @Pattern(regexp = "CYRILLIC|LATIN", message = "Алфавит должен быть CYRILLIC или LATIN")
  String fioScript,
  @Schema(description = "Поля с битой контрольной суммой") List<@Pattern(regexp = "INN|SNILS") String> corrupt,
  @Schema(description = "Регион РФ — имя enum Region или код субъекта")
  @ValidRegion
  String region,
  @Schema(description = "Категория клиента") String categoryClient,
  @Schema(description = "Пакет обслуживания") String accountPackage,
  @Schema(description = "Семейное положение", allowableValues = {"SINGLE", "DIVORCED", "MARRIED", "CIVIL_MARRIAGE", "WIDOWED"})
  @Pattern(regexp = "SINGLE|DIVORCED|MARRIED|CIVIL_MARRIAGE|WIDOWED", message = "Неизвестное семейное положение")
  String familyStatus,
  @Schema(description = "Флаг «террорист / экстремист»") Boolean terrorist,
  @Schema(description = "Флаг «заблокирован банком»") Boolean blocked,
  @Schema(
    description = "Явный БИК банка (P3.3). Применяется ко всем профилям в пакете. "
      + "Если не задан — каждый профиль получит случайный банк.",
    example = "044525225")
  @Pattern(regexp = "\\d{9}", message = "БИК должен содержать 9 цифр")
  String bik
) {}
