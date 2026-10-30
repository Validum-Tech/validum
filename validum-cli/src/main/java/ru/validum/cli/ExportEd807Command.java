package ru.validum.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import ru.validum.core.bank.Participant;
import ru.validum.core.bank.ParticipantType;
import ru.validum.core.export.Ed807Exporter;

/** Пишет справочник участников в XML ED807 (windows-1251). */
@Command(
  name = "ed807",
  mixinStandardHelpOptions = true,
  description = "Справочник участников платёжной системы в формате ED807 (SIRR)"
)
public final class ExportEd807Command implements Callable<Integer> {

  @Option(names = {"-f", "--file"}, description = "Файл ED807; по умолчанию data/ED807.xml")
  Path file;

  @Option(names = {"-t", "--type"}, description = "Код PtType: 20 — банк, 52 — казначейство, 10 — РКЦ")
  String type;

  @Option(names = {"-l", "--limit"}, description = "Сколько участников включить")
  Integer limit;

  @Option(names = {"-o", "--output"}, defaultValue = "ed807.xml", description = "Куда записать XML")
  Path output;

  @Override
  public Integer call() throws Exception {
    List<Participant> participants = CliBik.participants(file);
    if (type != null && !type.isBlank()) {
      ParticipantType wanted;
      try {
        wanted = ParticipantType.fromCode(type.trim());
      } catch (IllegalArgumentException e) {
        System.err.println("Неизвестный тип участника: " + type + ". Допустимо: "
          + java.util.Arrays.stream(ParticipantType.values())
            .map(ParticipantType::code)
            .collect(java.util.stream.Collectors.joining(", ")));
        return 2;
      }
      participants = participants.stream().filter(p -> p.type() == wanted).toList();
    }
    if (limit != null) {
      if (limit <= 0) {
        System.err.println("--limit должен быть положительным");
        return 2;
      }
      if (participants.size() > limit) {
        participants = participants.subList(0, limit);
      }
    }

    LocalDate today = LocalDate.now();
    byte[] xml = Ed807Exporter.toBytes(
      participants,
      Ed807Exporter.Header.defaults("4525545000", "4525545000", today, "1"),
      Ed807Exporter.Defaults.moscow());

    write(output, xml);
    System.err.println("Участников в файле: " + participants.size());
    return 0;
  }

  private static void write(Path output, byte[] bytes) throws Exception {
    Path parent = output.toAbsolutePath().getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    Files.write(output, bytes);
    System.err.println("Записано: " + output.toAbsolutePath() + " (" + bytes.length + " байт)");
  }
}
