package ru.validum.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Ответ с пакетом профилей ИП")
public record IpBatchResponse(
  @Schema(example = "50") int count,
  @Schema(example = "42") Long seed,
  List<IpProfileDto> profiles
) {}
