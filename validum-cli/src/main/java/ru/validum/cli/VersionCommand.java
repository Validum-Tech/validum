package ru.validum.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

@Command(
  name = "version",
  mixinStandardHelpOptions = true,
  description = "Версия Validum и окружения"
)
public final class VersionCommand implements Callable<Integer> {

  @Override
  public Integer call() {
    System.out.println("validum " + BuildInfo.version());
    System.out.println("Java:   " + System.getProperty("java.version"));
    System.out.println("Vendor: " + System.getProperty("java.vendor"));
    System.out.println("OS:     " + System.getProperty("os.name")
      + " " + System.getProperty("os.version"));
    return 0;
  }
}
