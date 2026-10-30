package ru.validum.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.validum.api.dto.IndividualProfileDto;
import ru.validum.api.service.ScenarioService;

@RestController
@RequestMapping("/api/v1/scenarios")
@Tag(name = "Scenarios", description = "Именованные сценарии генерации физлиц")
public class ScenarioController {

  private final ScenarioService service;

  public ScenarioController(ScenarioService service) {
    this.service = service;
  }

  @GetMapping
  @Operation(summary = "Список доступных сценариев")
  public List<String> list() {
    return service.listSlugs();
  }

  @PostMapping("/{name}")
  @Operation(summary = "Сгенерировать профиль по имени сценария")
  public IndividualProfileDto generate(
    @PathVariable String name,
    @Parameter(description = "Seed для воспроизводимости; без него — случайный")
    @RequestParam(required = false)
    Long seed) {
    return service.generate(name, seed);
  }

  @ExceptionHandler(NoSuchElementException.class)
  public ResponseEntity<Map<String, String>> onUnknown(NoSuchElementException e) {
    return ResponseEntity.status(404)
      .body(Map.of("error", "unknown_scenario", "message", e.getMessage()));
  }
}
