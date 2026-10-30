package ru.validum.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Ответ с пакетом профилей ЮЛ")
public record OrganizationBatchResponse(
  @Schema(example = "20") int count,
  @Schema(example = "42") Long seed,
  List<OrganizationProfileDto> profiles
) {}
