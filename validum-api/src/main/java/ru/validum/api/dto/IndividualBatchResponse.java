package ru.validum.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Ответ с пакетом профилей ФЛ")
public record IndividualBatchResponse(
  @Schema(example = "100") int count,
  @Schema(example = "42") Long seed,
  List<IndividualProfileDto> profiles
) {}
