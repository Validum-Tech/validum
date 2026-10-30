package ru.validum.api.dto;
import io.swagger.v3.oas.annotations.media.Schema;
public record OgrnValidationRequest(@Schema(example = "1027700123456") String ogrn) {}
