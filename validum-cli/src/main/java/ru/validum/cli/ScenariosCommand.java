package ru.validum.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

@Command(
  name = "scenarios",
  mixinStandardHelpOptions = true,
  description = "Список доступных сценариев генерации ФЛ"
)
public final class ScenariosCommand implements Callable<Integer> {

  @Override
  public Integer call() {
    ScenarioRegistry.allSlugs().forEach(System.out::println);
    return 0;
  }
}
