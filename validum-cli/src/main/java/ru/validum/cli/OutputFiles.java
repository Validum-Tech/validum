package ru.validum.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Общие действия при записи выходного файла.
 *
 * <p>{@code -o out/people.json} раньше падал с {@code NoSuchFileException}, если каталога
 * {@code out/} не было: {@code Files.write} не создаёт родительские каталоги. Теперь каталог
 * создаётся автоматически — опечатка в пути к существующему каталогу больше не выглядит как
 * ошибка записи файла.
 */
final class OutputFiles {

  private OutputFiles() {
  }

  /** Создаёт родительские каталоги пути, если их нет. */
  static void createParent(Path path) throws IOException {
    Path parent = path.toAbsolutePath().getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
  }

  /** Создаёт каталог назначения и пишет байты. */
  static void write(Path path, byte[] bytes) throws IOException {
    createParent(path);
    Files.write(path, bytes);
  }
}
