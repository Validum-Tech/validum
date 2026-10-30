package ru.validum.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.validum.api.service.ExportService;

/** Экспорт в форматы ЦБ РФ: ED807 (справочник БИК) и ED808 (профиль участника). */
@RestController
@RequestMapping("/api/v1/export")
@Tag(name = "Export", description = "Сообщения ЦБ РФ: ED807 (SIRR), ED808 (FIRR)")
public class ExportController {

  private final ExportService service;

  public ExportController(ExportService service) {
    this.service = service;
  }

  @GetMapping("/ed807")
  @Operation(summary = "Справочник участников ED807 (XML, windows-1251)")
  public ResponseEntity<byte[]> ed807(
    @Parameter(description = "Код PtType: 20 — банк, 52 — казначейство, 10 — РКЦ и т.д.")
    @RequestParam(required = false)
    String type,
    @Parameter(description = "Сколько участников включить (без лимита — все)")
    @RequestParam(required = false)
    Integer limit) {
    return xml(service.ed807(type, limit));
  }

  @GetMapping("/ed808")
  @Operation(summary = "Профиль участника ED808 (XML, windows-1251)")
  public ResponseEntity<byte[]> ed808(
    @Parameter(description = "БИК участника") @RequestParam String bik,
    @Parameter(description = "Seed для клиентского счёта") @RequestParam(required = false) Long seed) {
    return xml(service.ed808(bik, seed));
  }

  @ExceptionHandler(NoSuchElementException.class)
  public ResponseEntity<Map<String, String>> onUnknown(NoSuchElementException e) {
    return ResponseEntity.status(404)
      .body(Map.of("error", "unknown_participant", "message", e.getMessage()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> onBadType(IllegalArgumentException e) {
    return ResponseEntity.badRequest()
      .body(Map.of("error", "bad_request", "message", e.getMessage()));
  }

  private static ResponseEntity<byte[]> xml(byte[] body) {
    return ResponseEntity.ok()
      .contentType(new MediaType("application", "xml", java.nio.charset.Charset.forName("windows-1251")))
      .header("Content-Disposition", "inline; filename=\"export.xml\"")
      .body(body);
  }
}
