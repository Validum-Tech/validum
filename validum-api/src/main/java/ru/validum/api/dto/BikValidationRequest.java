package ru.validum.api.dto;
import io.swagger.v3.oas.annotations.media.Schema;
public record BikValidationRequest(@Schema(example = "044525225") String bik) {}
