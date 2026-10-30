package ru.validum.api.controller;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.validum.core.bank.BikSourceNotFoundException;

/**
 * Глобальный обработчик бизнес-ошибок.
 *
 * <p>P3.3: {@link IllegalArgumentException} от {@code .bik(unknownBik)} (или от любых {@code
 * valueOf(...)} в сервисе, если клиент обошёл Bean Validation) → 400 с понятным сообщением вместо
 * 500.
 *
 * <p>Справочник БИК обязателен: если файла нет — 503 {@code bik_source_missing}, потому что это
 * ошибка конфигурации окружения, а не запроса.
 *
 * <p>Формат ответа: {@code {"error":"invalid_argument","message":"..."}}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> onIllegalArgument(IllegalArgumentException e) {
    String message = e.getMessage() != null ? e.getMessage() : "Invalid argument";
    return ResponseEntity.badRequest()
      .body(Map.of("error", "invalid_argument", "message", message));
  }

  @ExceptionHandler(BikSourceNotFoundException.class)
  public ResponseEntity<Map<String, String>> onBikSourceMissing(BikSourceNotFoundException e) {
    String message = e.getMessage() != null ? e.getMessage() : "Bik source is not configured";
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
      .body(Map.of("error", "bik_source_missing", "message", message));
  }
}
