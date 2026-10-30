package ru.validum.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Результат валидации")
public record ValidationResponse(
  @Schema(example = "true") boolean valid,
  @Schema(example = "INDIVIDUAL", description = "Тип (INDIVIDUAL, LEGAL_ENTITY, BANK, RUB)") String type,
  @Schema(example = "ПАО Сбербанк", description = "Название банка для БИК или описание ошибки") String details
) {}
