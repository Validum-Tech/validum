package ru.validum.cli;

import picocli.CommandLine.Command;

@Command(
  name = "fill",
  mixinStandardHelpOptions = true,
  description = "Дозаполнение CSV: пустые ячейки заполняются, непустые не трогаются",
  subcommands = {FillIndividualCommand.class}
)
public final class FillCommand implements Runnable {
  @Override
  public void run() {
    // Если подкоманда не задана — picocli напечатает help
  }
}
