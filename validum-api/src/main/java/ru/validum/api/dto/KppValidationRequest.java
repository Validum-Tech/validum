package ru.validum.api.dto;
import io.swagger.v3.oas.annotations.media.Schema;
public record KppValidationRequest(@Schema(example = "770101001") String kpp) {}
