package ru.validum.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.validum.api.dto.*;
import ru.validum.api.service.GenerationService;
import ru.validum.core.export.CsvExporter;
import ru.validum.core.export.IndividualProfileFlattener;
import ru.validum.core.export.IpProfileFlattener;
import ru.validum.core.export.OrganizationProfileFlattener;
import ru.validum.core.organization.IpProfile;
import ru.validum.core.organization.OrganizationProfile;
import ru.validum.core.profile.IndividualProfile;

@RestController
@RequestMapping("/api/v1/generate")
@Tag(name = "Generation", description = "Генерация валидных данных")
public class GenerationController {

  private static final String CONTENT_TYPE_CSV = "text/csv; charset=UTF-8";

  private final GenerationService service;

  public GenerationController(GenerationService service) {
    this.service = service;
  }

  // ── Одиночные ─────────────────────────────────────────────────────────────

  @PostMapping(value = "/individual", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Сгенерировать профиль физлица")
  public IndividualProfileDto generateIndividual(@RequestBody @Valid IndividualGenerationRequest request) {
    return service.generateIndividual(request);
  }

  @PostMapping(value = "/ip", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Сгенерировать профиль ИП")
  public IpProfileDto generateIp(@RequestBody @Valid IpGenerationRequest request) {
    return service.generateIp(request);
  }

  @PostMapping(value = "/organization", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Сгенерировать профиль юрлица")
  public OrganizationProfileDto generateOrganization(@RequestBody @Valid OrganizationGenerationRequest request) {
    return service.generateOrganization(request);
  }

  // ── Батчи ─────────────────────────────────────────────────────────────────

  @PostMapping(value = "/individual/batch")
  @Operation(
    summary = "Сгенерировать пакет профилей физлиц",
    description = "Возвращает от 1 до 10 000 профилей. Форматы: json (обёртка), json-array (плоский массив), csv (для Excel / БД).")
  public ResponseEntity<?> generateIndividualBatch(
    @RequestBody @Valid IndividualBatchRequest request,
    @Parameter(description = "Формат ответа: json, json-array, csv")
    @RequestParam(required = false, defaultValue = "json") String format) {

    return switch (format.toLowerCase()) {
      case "json-array" -> ResponseEntity.ok(service.generateIndividualBatch(request).profiles());
      case "csv" -> {
        List<IndividualProfile> raw = service.generateIndividualBatchRaw(request);
        String csv = CsvExporter.toCsv(IndividualProfileFlattener.flattenAll(raw));
        yield csvResponse("validum-individual", csv);
      }
      default -> ResponseEntity.ok(service.generateIndividualBatch(request));
    };
  }

  @PostMapping(value = "/ip/batch")
  @Operation(
    summary = "Сгенерировать пакет профилей ИП",
    description = "Форматы: json, json-array, csv.")
  public ResponseEntity<?> generateIpBatch(
    @RequestBody @Valid IpBatchRequest request,
    @Parameter(description = "Формат ответа: json, json-array, csv")
    @RequestParam(required = false, defaultValue = "json") String format) {

    return switch (format.toLowerCase()) {
      case "json-array" -> ResponseEntity.ok(service.generateIpBatch(request).profiles());
      case "csv" -> {
        List<IpProfile> raw = service.generateIpBatchRaw(request);
        String csv = CsvExporter.toCsv(IpProfileFlattener.flattenAll(raw));
        yield csvResponse("validum-ip", csv);
      }
      default -> ResponseEntity.ok(service.generateIpBatch(request));
    };
  }

  @PostMapping(value = "/organization/batch")
  @Operation(
    summary = "Сгенерировать пакет профилей юрлиц",
    description = "Форматы: json, json-array, csv.")
  public ResponseEntity<?> generateOrganizationBatch(
    @RequestBody @Valid OrganizationBatchRequest request,
    @Parameter(description = "Формат ответа: json, json-array, csv")
    @RequestParam(required = false, defaultValue = "json") String format) {

    return switch (format.toLowerCase()) {
      case "json-array" -> ResponseEntity.ok(service.generateOrganizationBatch(request).profiles());
      case "csv" -> {
        List<OrganizationProfile> raw = service.generateOrganizationBatchRaw(request);
        String csv = CsvExporter.toCsv(OrganizationProfileFlattener.flattenAll(raw));
        yield csvResponse("validum-organization", csv);
      }
      default -> ResponseEntity.ok(service.generateOrganizationBatch(request));
    };
  }

  // ── Helpers ──────────────────────────────────────────────────────────────

  private static ResponseEntity<byte[]> csvResponse(String basename, String csv) {
    return ResponseEntity.ok()
      .contentType(MediaType.parseMediaType(CONTENT_TYPE_CSV))
      .header("Content-Disposition",
        "attachment; filename=\"" + basename + "-" + LocalDate.now() + ".csv\"")
      .body(CsvExporter.toBytes(csv));
  }
}
