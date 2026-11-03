package ru.validum.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.validum.api.dto.*;
import ru.validum.api.service.GenerationService;
import ru.validum.core.Version;
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
  public ResponseEntity<IndividualProfileDto> generateIndividual(
    @RequestBody @Valid IndividualGenerationRequest request) {
    return okWithMeta(service.generateIndividual(request), request.seed());
  }

  @PostMapping(value = "/ip", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Сгенерировать профиль ИП")
  public ResponseEntity<IpProfileDto> generateIp(@RequestBody @Valid IpGenerationRequest request) {
    return okWithMeta(service.generateIp(request), request.seed());
  }

  @PostMapping(value = "/organization", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Сгенерировать профиль юрлица")
  public ResponseEntity<OrganizationProfileDto> generateOrganization(
    @RequestBody @Valid OrganizationGenerationRequest request) {
    return okWithMeta(service.generateOrganization(request), request.seed());
  }

  // ── Батчи ─────────────────────────────────────────────────────────────────

  @PostMapping(value = "/individual/batch")
  @Operation(
    summary = "Сгенерировать пакет профилей физлиц",
    description = "Возвращает от 1 до 10 000 профилей. Форматы: json (обёртка), json-array (плоский массив), csv, csv-semicolon (для русского Excel: «;» и русские заголовки).")
  public ResponseEntity<?> generateIndividualBatch(
    @RequestBody @Valid IndividualBatchRequest request,
    @Parameter(description = "Формат ответа: json, json-array, csv, csv-semicolon")
    @RequestParam(required = false, defaultValue = "json") String format) {

    return switch (format.toLowerCase()) {
      case "json-array" ->
        okWithMeta(service.generateIndividualBatch(request).profiles(), request.seed());
      case "csv" ->
        csvBody(
          "validum-individual",
          IndividualProfileFlattener.flattenAll(service.generateIndividualBatchRaw(request)),
          false,
          request.seed());
      case "csv-semicolon" ->
        csvBody(
          "validum-individual",
          IndividualProfileFlattener.flattenAll(service.generateIndividualBatchRaw(request)),
          true,
          request.seed());
      default -> okWithMeta(service.generateIndividualBatch(request), request.seed());
    };
  }

  @PostMapping(value = "/ip/batch")
  @Operation(
    summary = "Сгенерировать пакет профилей ИП",
    description = "Форматы: json, json-array, csv, csv-semicolon.")
  public ResponseEntity<?> generateIpBatch(
    @RequestBody @Valid IpBatchRequest request,
    @Parameter(description = "Формат ответа: json, json-array, csv, csv-semicolon")
    @RequestParam(required = false, defaultValue = "json") String format) {

    return switch (format.toLowerCase()) {
      case "json-array" ->
        okWithMeta(service.generateIpBatch(request).profiles(), request.seed());
      case "csv" ->
        csvBody(
          "validum-ip",
          IpProfileFlattener.flattenAll(service.generateIpBatchRaw(request)),
          false,
          request.seed());
      case "csv-semicolon" ->
        csvBody(
          "validum-ip",
          IpProfileFlattener.flattenAll(service.generateIpBatchRaw(request)),
          true,
          request.seed());
      default -> okWithMeta(service.generateIpBatch(request), request.seed());
    };
  }

  @PostMapping(value = "/organization/batch")
  @Operation(
    summary = "Сгенерировать пакет профилей юрлиц",
    description = "Форматы: json, json-array, csv, csv-semicolon.")
  public ResponseEntity<?> generateOrganizationBatch(
    @RequestBody @Valid OrganizationBatchRequest request,
    @Parameter(description = "Формат ответа: json, json-array, csv, csv-semicolon")
    @RequestParam(required = false, defaultValue = "json") String format) {

    return switch (format.toLowerCase()) {
      case "json-array" ->
        okWithMeta(service.generateOrganizationBatch(request).profiles(), request.seed());
      case "csv" ->
        csvBody(
          "validum-organization",
          OrganizationProfileFlattener.flattenAll(service.generateOrganizationBatchRaw(request)),
          false,
          request.seed());
      case "csv-semicolon" ->
        csvBody(
          "validum-organization",
          OrganizationProfileFlattener.flattenAll(service.generateOrganizationBatchRaw(request)),
          true,
          request.seed());
      default -> okWithMeta(service.generateOrganizationBatch(request), request.seed());
    };
  }

  // ── Helpers ──────────────────────────────────────────────────────────────

  /**
   * Готовит CSV-ответ. Excel-режим — «;» и русские заголовки (см. CsvExporter);
   * машинный режим остаётся запятой и английскими именами колонок.
   */
  /**
   * Заголовки, по которым набор данных воспроизводим: версия генератора и фактический seed.
   * Без seed в запросе выводится {@code auto} — генератор подобрал его сам.
   */
  private static HttpHeaders metaHeaders(Long seed) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Validum-Version", Version.version());
    headers.set("X-Validum-Seed", seed == null ? "auto" : String.valueOf(seed));
    return headers;
  }

  private static <T> ResponseEntity<T> okWithMeta(T body, Long seed) {
    return ResponseEntity.ok().headers(metaHeaders(seed)).body(body);
  }

  private static ResponseEntity<byte[]> csvBody(
    String basename, List<Map<String, String>> rows, boolean excel, Long seed) {
    String csv = excel ? CsvExporter.toCsv(rows, ';', true) : CsvExporter.toCsv(rows);
    return csvResponse(basename, csv, seed);
  }

  private static ResponseEntity<byte[]> csvResponse(String basename, String csv, Long seed) {
    return ResponseEntity.ok()
      .headers(metaHeaders(seed))
      .contentType(MediaType.parseMediaType(CONTENT_TYPE_CSV))
      .header("Content-Disposition",
        "attachment; filename=\"" + basename + "-" + LocalDate.now() + ".csv\"")
      .body(CsvExporter.toBytes(csv));
  }
}
