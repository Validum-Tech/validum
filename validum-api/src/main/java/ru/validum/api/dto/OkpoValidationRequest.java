package ru.validum.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Запрос на валидацию ОКПО.
 *
 * <p>Автоопределение типа по длине: 8 цифр — ЮЛ, 10 цифр — ИП.
 */
public record OkpoValidationRequest(
  @Schema(example = "12345678", description = "ОКПО (8 или 10 цифр)")
  String okpo
) {}
