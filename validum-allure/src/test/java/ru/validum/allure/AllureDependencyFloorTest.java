package ru.validum.allure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Страж контракта «Allure не навязываем» (C10/C11).
 *
 * <p>Правила, которые этот тест защищает:
 *
 * <ul>
 *   <li>main-код обращается только к {@code io.qameta.allure.Allure} из {@code allure-java-commons};
 *   <li>никаких {@code allure-junit5}, {@code allure-jupiter}, {@code allure-specs} в байткоде:
 *       иначе потребителю на старой версии Allure будет нечем это закрыть;
 *   <li>{@code io.qameta.allure.Allure} реально доступен в рантайме тестов — на самой старой версии
 *       из поддерживаемых (2.29.1, см. {@code testRuntimeOnly} в build.gradle.kts).
 * </ul>
 */
class AllureDependencyFloorTest {

  /** Пакеты, которых в main-байткоде быть не должно. */
  private static final List<String> FORBIDDEN =
    List.of("io/qameta/allure/junit5", "io/qameta/allure/specs", "io/qameta/allure/model", "io/qameta/allure/junit4");

  @Test
  @DisplayName("main-байткод не ссылается на allure-junit5/jupiter/specs/model")
  void mainBytecodeUsesOnlyAllureJavaCommons() throws Exception {
    Path classes = mainClassesDir();
    List<String> violations = new ArrayList<>();
    try (Stream<Path> files = Files.walk(classes)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".class")).toList()) {
        String bytes = new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1);
        for (String forbidden : FORBIDDEN) {
          if (bytes.contains(forbidden)) {
            violations.add(classes.relativize(file) + " → " + forbidden);
          }
        }
      }
    }
    assertThat(violations)
      .describedAs("main-код должен зависеть только от allure-java-commons; убрать из AllureDependencyFloor")
      .isEmpty();
  }

  @Test
  @DisplayName("Allure доступен в рантайме тестов на минимальной поддерживаемой версии")
  void allureIsAvailableAtRuntime() throws Exception {
    // Через reflection: тест-код не должен зависеть от Allure на этапе компиляции —
    // ровно этого мы и требуем от потребителя.
    Class<?> allure = Class.forName("io.qameta.allure.Allure");
    assertThat(allure.getName()).isEqualTo("io.qameta.allure.Allure");
    // Версию определяем двумя способами: pom.properties внутри артефакта, затем имя jar
    // из code source. Тесты идут против пола 2.29.1 (testRuntimeOnly в build.gradle.kts),
    // а не против той версии, что принесёт потребитель.
    String version = allureVersion();
    if (version == null) {
      Assumptions.assumeTrue(false, "не удалось определить версию allure-java-commons (класс загружен не из jar)");
    }
    assertThat(version)
      .describedAs("тесты должны идти против минимального API (2.29.x)")
      .startsWith("2.29");
  }

  /**
   * Версия allure-java-commons в тест-рантайме: сначала META-INF/maven/pom.properties,
   * затем имя jar из code source (у части артефактов pom.properties внутри нет).
   */
  private static String allureVersion() throws Exception {
    String fromPom = readPomPropertiesVersion();
    if (fromPom != null && !fromPom.isBlank()) {
      return fromPom;
    }
    var source = Class.forName("io.qameta.allure.Allure")
      .getProtectionDomain()
      .getCodeSource()
      .getLocation()
      .toURI();
    java.util.regex.Matcher m = java.util.regex.Pattern
      .compile("allure-java-commons-(\\d+\\.\\d+\\.\\d+).*")
      .matcher(Path.of(source).getFileName().toString());
    return m.find() ? m.group(1) : null;
  }

  private static String readPomPropertiesVersion() {
    String path = "META-INF/maven/io.qameta.allure/allure-java-commons/pom.properties";
    try (InputStream in = AllureDependencyFloorTest.class.getClassLoader().getResourceAsStream(path)) {
      if (in == null) {
        return null;
      }
      java.util.Properties props = new java.util.Properties();
      props.load(in);
      return props.getProperty("version", "");
    } catch (IOException e) {
      return null;
    }
  }

  /** Каталог с нашими main-классами: classes/java/main рядом с тестовым набором. */
  private static Path mainClassesDir() throws Exception {
    Path testClasses = Path.of(
      ValidumExtension.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    Path buildDir = testClasses.getParent().getParent().getParent();
    Path main = buildDir.resolve("classes/java/main/ru/validum/allure");
    if (!Files.isDirectory(main)) {
      throw new IOException("Не найден каталог main-классов: " + main);
    }
    return main;
  }
}
