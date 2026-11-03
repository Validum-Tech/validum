package ru.validum.core;

/**
 * Версия ядра в одном месте.
 *
 * <p>Порядок источников: системное свойство {@code validum.version} (его выставляет Gradle во
 * время сборки и тестов) → {@code Implementation-Version} из манифеста jar → {@code dev}.
 *
 * <p>Никаких зависимостей: класс живёт в core, чтобы CLI и REST отвечали одну и ту же версию.
 */
public final class Version {

  /** Значение, когда версию определить не удалось (запуск из классов без манифеста). */
  public static final String DEV = "dev";

  private Version() {
    throw new AssertionError("Не инстанцируется");
  }

  /** Версия в виде строки, пригодной для печати. */
  public static String version() {
    String override = System.getProperty("validum.version");
    if (override != null && !override.isBlank()) {
      return override.trim();
    }
    Package pkg = Version.class.getPackage();
    String fromManifest = pkg == null ? null : pkg.getImplementationVersion();
    return fromManifest == null || fromManifest.isBlank() ? DEV : fromManifest.trim();
  }
}
