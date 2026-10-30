package ru.validum.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

@Schema(description = "Ответ с дозаполненными строками")
public record FillResponse(
  @Schema(example = "1") int count,
  List<Map<String, String>> rows
) {}
