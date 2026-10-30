package ru.validum.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Запрос на валидацию ОГРНИП (15 цифр) — отдельный эндпоинт от ОГРН ЮЛ.
 */
public record OgrnipValidationRequest(
  @Schema(example = "304500116000157", description = "ОГРНИП (15 цифр)")
  String ogrnip
) {
  /** Явный accessor для читаемости — record генерирует такой же неявно. */
  @Override
  public String ogrnip() {
    return ogrnip;
  }
}
