package ru.validum.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

@Schema(description = "Запрос на дозаполнение CSV-строк (P4.1): непустые ячейки сохраняются, пустые заполняются")
public record FillRequest(
  @Schema(
    description = "Строки CSV как карты «колонка → значение». Пустые ячейки заполняются валидными значениями.",
    requiredMode = Schema.RequiredMode.REQUIRED)
  @NotEmpty(message = "Нужна хотя бы одна строка")
  @Size(max = 10000, message = "Максимум 10000 строк")
  List<Map<String, String>> rows,

  @Schema(description = "Базовый seed для воспроизводимости", example = "42")
  Long seed
) {}
