package ru.validum.cli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Чтение входных файлов с понятной ошибкой вместо стектрейса.
 *
 * <p>Без этой обёртки отсутствующий файл печатал {@code java.nio.file.NoSuchFileException}
 * и десять строк стека — пользователь не понимал, что он указал не тот путь.
 */
public final class InputFiles {

  private InputFiles() {}

  /**
   * Проверяет, что файл существует.
   *
   * @return тот же путь
   * @throws IllegalArgumentException с абсолютным путём, если файла нет
   */
  public static Path require(Path file) {
    if (file == null) {
      throw new IllegalArgumentException("путь к файлу не задан");
    }
    if (!Files.exists(file)) {
      throw new IllegalArgumentException("файл не найден — " + file.toAbsolutePath());
    }
    return file;
  }

  /**
   * Читает файл как UTF-8.
   *
   * @throws IllegalArgumentException если файла нет
   * @throws IOException если чтение не удалось
   */
  public static String read(Path file) throws IOException {
    return Files.readString(require(file), StandardCharsets.UTF_8);
  }

  /**
   * Печатает ошибку в stderr и возвращает код 2 — общий вид для команд.
   */
  public static int fail(IllegalArgumentException e) {
    System.err.println("Ошибка: " + e.getMessage());
    return 2;
  }
}
