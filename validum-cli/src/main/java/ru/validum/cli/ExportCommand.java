package ru.validum.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/** Экспорт в форматы ЦБ РФ: ED807 (SIRR) и ED808 (FIRR). */
@Command(
  name = "export",
  mixinStandardHelpOptions = true,
  description = "Экспорт в форматы ЦБ РФ: ED807 (справочник БИК) и ED808 (профиль участника)",
  subcommands = {ExportEd807Command.class, ExportEd808Command.class}
)
public final class ExportCommand implements Callable<Integer> {

  @Spec
  CommandSpec spec;

  @Override
  public Integer call() {
    spec.commandLine().usage(System.out);
    return 0;
  }
}
