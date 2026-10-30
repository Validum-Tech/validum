package ru.validum.api.dto;
import io.swagger.v3.oas.annotations.media.Schema;
public record AccountValidationRequest(
  @Schema(example = "30103810345374525078") String account,
  @Schema(example = "044525000") String bik
) {}
