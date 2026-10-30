package ru.validum.cli;

import picocli.CommandLine.Command;

@Command(
  name = "generate",
  mixinStandardHelpOptions = true,
  description = "Генерация профилей (ФЛ / ИП / ЮЛ)",
  subcommands = {
    GenerateIndividualCommand.class,
    GenerateIpCommand.class,
    GenerateOrganizationCommand.class,
    GenerateDocumentsCommand.class,
  }
)
public final class GenerateCommand implements Runnable {
  @Override
  public void run() {
    // Если подкоманда не задана — picocli напечатает help
  }
}
