package ru.validum.cli;

import ru.validum.core.Version;

/**
 * Версия сборки Validum для CLI.
 *
 * <p>Источник один — {@link Version}: системное свойство {@code validum.version} (его ставит
 * Gradle) → манифест jar → {@code dev}.
 */
public final class BuildInfo {

  private BuildInfo() {}

  /** Версия вида {@code 0.2.0} или {@code dev} вне сборки. */
  public static String version() {
    return Version.version();
  }
}
