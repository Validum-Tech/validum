package ru.validum.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Адрес")
public record AddressDto(
  @Schema(example = "196247") String postalCode,
  @Schema(example = "Санкт-Петербург", description = "Субъект РФ") String region,
  @Schema(example = "г", description = "Тип субъекта (г, обл, кр, Респ)") String regionType,
  @Schema(example = "Санкт-Петербург") String city,
  @Schema(example = "Бассейная") String street,
  @Schema(example = "15") String building,
  @Schema(example = "42") String apartment,
  @Schema(example = "196247,Санкт-Петербург г,,,,,,Бассейная ул,15, ,") String formatted) {}
