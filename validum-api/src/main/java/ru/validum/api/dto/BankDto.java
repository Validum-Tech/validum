package ru.validum.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Банк")
public record BankDto(
  @Schema(example = "044525225") String bik,
  @Schema(example = "ПАО Сбербанк") String name,
  @Schema(example = "30101810400000000225") String corrAccount,
  @Schema(example = "Москва") String city,
  @Schema(example = "Москва") String region) {}
