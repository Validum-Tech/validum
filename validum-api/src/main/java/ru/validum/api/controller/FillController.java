package ru.validum.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.validum.api.dto.FillRequest;
import ru.validum.api.dto.FillResponse;
import ru.validum.core.bank.BikSource;
import ru.validum.core.export.CsvExporter;
import ru.validum.core.export.CsvFiller;

/**
 * Дозаполнение существующих CSV-наборов (P4.1): непустые ячейки не трогаются, пустые получают
 * валидные значения.
 */
@RestController
@RequestMapping("/api/v1/fill")
@Tag(name = "Fill", description = "Дозаполнение существующих CSV-наборов (P4.1)")
public class FillController {

  private static final String CONTENT_TYPE_CSV = "text/csv; charset=UTF-8";

  private final BikSource bikSource;

  public FillController(BikSource bikSource) {
    this.bikSource = bikSource;
  }

  @PostMapping(value = "/individual", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
    summary = "Дозаполнить строки профилей физлиц",
    description = "Непустые ячейки сохраняются как есть. Форматы ответа: json (обёртка) и csv.")
  public ResponseEntity<?> fillIndividual(
    @RequestBody @Valid FillRequest request,
    @Parameter(description = "Формат ответа: json, csv")
    @RequestParam(required = false, defaultValue = "json") String format,
    @Parameter(description = "Дата расчёта (ISO), например 2026-01-01; не задана — сегодня")
    @RequestParam(required = false) LocalDate asOf) {

    CsvFiller filler =
      new CsvFiller(
        bikSource, request.seed() == null ? null : new Random(request.seed()), asOf);
    List<Map<String, String>> filled = filler.fill(request.rows());

    return switch (format.toLowerCase()) {
      case "csv" -> csvResponse("validum-filled", CsvExporter.toCsv(filled));
      default -> ResponseEntity.ok(new FillResponse(filled.size(), filled));
    };
  }

  private static ResponseEntity<byte[]> csvResponse(String basename, String csv) {
    return ResponseEntity.ok()
      .contentType(MediaType.parseMediaType(CONTENT_TYPE_CSV))
      .header("Content-Disposition",
        "attachment; filename=\"" + basename + "-" + LocalDate.now() + ".csv\"")
      .body(CsvExporter.toBytes(csv));
  }
}
