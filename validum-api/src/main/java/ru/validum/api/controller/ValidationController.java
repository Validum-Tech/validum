package ru.validum.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.validum.api.dto.*;
import ru.validum.api.service.ValidationService;

@RestController
@RequestMapping("/api/v1/validate")
@Tag(name = "Validation", description = "Валидация реквизитов и контрольных сумм")
public class ValidationController {

  private final ValidationService service;

  public ValidationController(ValidationService service) {
    this.service = service;
  }

  @PostMapping(value = "/inn", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Валидация ИНН", description = "Автоматически определяет тип (ФЛ или ЮЛ).")
  public ValidationResponse validateInn(@RequestBody InnValidationRequest req) {
    return service.validateInn(req);
  }

  @PostMapping(value = "/snils", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Валидация СНИЛС")
  public ValidationResponse validateSnils(@RequestBody SnilsValidationRequest req) {
    return service.validateSnils(req);
  }

  @PostMapping(value = "/ogrn", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Валидация ОГРН/ОГРНИП",
    description = "Автоопределение по длине: 13 — ОГРН ЮЛ, 15 — ОГРНИП.")
  public ValidationResponse validateOgrn(@RequestBody OgrnValidationRequest req) {
    return service.validateOgrn(req);
  }

  @PostMapping(value = "/ogrnip", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Валидация ОГРНИП (P2.3)",
    description = "Только ОГРНИП — 15 цифр. Явный контракт для QA, чтобы не смешивать с ОГРН ЮЛ.")
  public ValidationResponse validateOgrnip(@RequestBody OgrnipValidationRequest req) {
    return service.validateOgrnip(req);
  }

  @PostMapping(value = "/account", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Валидация счёта по 809-П")
  public ValidationResponse validateAccount(@RequestBody AccountValidationRequest req) {
    return service.validateAccount(req);
  }

  @PostMapping(value = "/kpp", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Валидация КПП")
  public ValidationResponse validateKpp(@RequestBody KppValidationRequest req) {
    return service.validateKpp(req);
  }

  @PostMapping(value = "/okpo", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Валидация ОКПО (P2.3)",
    description = "Автоопределение по длине: 8 — ЮЛ, 10 — ИП.")
  public ValidationResponse validateOkpo(@RequestBody OkpoValidationRequest req) {
    return service.validateOkpo(req);
  }

  @PostMapping(value = "/bik", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Валидация БИК", description = "Возвращает название банка при успехе.")
  public ValidationResponse validateBik(@RequestBody BikValidationRequest req) {
    return service.validateBik(req);
  }
}
