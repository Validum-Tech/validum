package ru.validum.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import ru.validum.core.bank.BikRegistryLoader;
import ru.validum.core.bank.Participant;
import ru.validum.core.bank.ParticipantRegistry;
import ru.validum.core.bank.ParticipantType;

/**
 * Выводит участников платёжной системы Банка России из справочника ED807.
 *
 * <p>В отличие от команды {@code generate}, показывает <b>все</b> участников (PtType),
 * а не только действующие банки: РКЦ, казначейство, филиалы, конкурсных управляющих и т.д.
 */
@Command(
  name = "participants",
  mixinStandardHelpOptions = true,
  description = "Участники платёжной системы Банка России из справочника ED807 (все PtType)"
)
public final class ParticipantsCommand implements Callable<Integer> {

  @Option(names = {"-f", "--file"}, required = true,
    description = "Файл справочника ED807 (XML)")
  Path file;

  @Option(names = {"-t", "--type"},
    description = "Фильтр по типу: код PtType (\"20\") или имя (\"RKC\"). Без фильтра — все участники.")
  String type;

  @Override
  public Integer call() {
    if (!Files.exists(file)) {
      System.err.println("Ошибка: файл не найден: " + file.toAbsolutePath());
      return 1;
    }

    ParticipantRegistry registry;
    try {
      registry = BikRegistryLoader.loadRegistry(file);
    } catch (IOException e) {
      System.err.println("Ошибка чтения справочника: " + e.getMessage());
      return 1;
    }

    List<Participant> participants;
    if (type != null && !type.isBlank()) {
      ParticipantType filter;
      try {
        filter = ParticipantType.fromCodeOrName(type);
      } catch (IllegalArgumentException e) {
        String codes =
          java.util.Arrays.stream(ParticipantType.values())
            .map(ParticipantType::code)
            .collect(Collectors.joining(", "));
        System.err.println("Ошибка: неизвестный тип участника: " + type);
        System.err.println("Допустимые коды PtType: " + codes);
        return 1;
      }
      participants = registry.byType(filter);
    } else {
      participants = registry.all();
    }

    System.out.println("БИК\tPtType\tТип\tКорсчёт\tНаименование");
    for (Participant p : participants) {
      System.out.println(
        p.bik() + "\t"
          + p.type().code() + "\t"
          + p.type().description() + "\t"
          + (p.corrAccount() == null ? "-" : p.corrAccount()) + "\t"
          + p.name());
    }
    System.out.println();
    System.out.println("Всего выведено: " + participants.size()
      + " (в справочнике: " + registry.size() + ")");
    return 0;
  }
}
