package ru.validum.api.dto;
import io.swagger.v3.oas.annotations.media.Schema;
public record InnValidationRequest(@Schema(example = "500100732259") String inn) {}
