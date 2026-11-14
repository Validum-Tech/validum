package ru.validum.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("Чтение входных файлов")
class InputFilesTest {

  @TempDir
  Path tmp;

  @Test
  @DisplayName("Отсутствующий файл — ошибка с абсолютным путём, а не стектрейс")
  void missingFileGivesClearError() {
    Path missing = tmp.resolve("nope.csv");
    assertThatThrownBy(() -> InputFiles.read(missing))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("файл не найден")
      .hasMessageContaining(missing.toAbsolutePath().toString());
  }

  @Test
  @DisplayName("Существующий файл читается как UTF-8")
  void readsExistingFile() throws Exception {
    Path file = tmp.resolve("people.csv");
    Files.writeString(file, "ФИО,ИНН\nИванов,7707083893\n");
    assertThat(InputFiles.read(file)).startsWith("ФИО,ИНН");
  }

  @Test
  @DisplayName("Код возврата при ошибке файла — 2")
  void failReturnsTwo() {
    int code = InputFiles.fail(new IllegalArgumentException("файл не найден — x"));
    assertThat(code).isEqualTo(2);
  }
}
