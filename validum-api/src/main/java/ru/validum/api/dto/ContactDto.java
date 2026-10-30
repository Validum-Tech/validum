package ru.validum.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Контакт клиента")
public record ContactDto(
  @Schema(example = "MOBILE") String type,
  @Schema(example = "9991234567") String value,
  @Schema(example = "Телефон клиента из автотеста") String description,
  @Schema(example = "Действительный") String status) {}
