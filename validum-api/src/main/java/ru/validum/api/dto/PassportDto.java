package ru.validum.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Паспорт")
public record PassportDto(
  @Schema(example = "4515") String series,
  @Schema(example = "123456") String number,
  @Schema(example = "Отделением УФМС России по г. Москве") String issuedBy,
  @Schema(example = "2015-05-20") String issueDate,
  @Schema(example = "770-001") String departmentCode,
  @Schema(example = "RF", allowableValues = {"RF", "FOREIGN"}) String type,
  @Schema(example = "RU") String country) {}
