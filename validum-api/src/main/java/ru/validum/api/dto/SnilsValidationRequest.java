package ru.validum.api.dto;
import io.swagger.v3.oas.annotations.media.Schema;
public record SnilsValidationRequest(@Schema(example = "12345678900") String snils) {}
