package ru.validum.cli;

import picocli.CommandLine.IVersionProvider;

/** Версия для {@code --version}: берётся из того же источника, что и {@code validum version}. */
public final class VersionProvider implements IVersionProvider {

  @Override
  public String[] getVersion() {
    return new String[] {"validum " + BuildInfo.version()};
  }
}
