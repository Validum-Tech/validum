package ru.validum.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Документ, удостоверяющий личность (классификатор СПДУЛ ФНС)")
public record IdentityDocumentDto(
  @Schema(description = "Код вида документа по СПДУЛ", example = "22") String spdulCode,
  @Schema(description = "Название вида документа", example = "Загранпаспорт РФ") String title,
  @Schema(description = "Серия", example = "71") String series,
  @Schema(description = "Номер", example = "1234567") String number,
  @Schema(description = "Кем выдан", example = "ГУ МВД России по г. Москве") String issuedBy,
  @Schema(description = "Дата выдачи", example = "2021-05-14") String issueDate,
  @Schema(description = "Дата окончания срока (null — бессрочно)", example = "2031-05-14")
  String expirationDate,
  @Schema(description = "Код подразделения", example = "770-001") String departmentCode) {}
