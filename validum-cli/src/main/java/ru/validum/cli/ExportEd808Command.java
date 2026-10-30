package ru.validum.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import ru.validum.core.account.AccountGenerator;
import ru.validum.core.account.AccountType;
import ru.validum.core.bank.Participant;
import ru.validum.core.export.Ed807Exporter;
import ru.validum.core.export.Ed808Exporter;

/** Пишет профиль участника в XML ED808 (windows-1251). */
@Command(
  name = "ed808",
  mixinStandardHelpOptions = true,
  description = "Профиль участника платёжной системы в формате ED808 (FIRR)"
)
public final class ExportEd808Command implements Callable<Integer> {

  @Option(names = {"-b", "--bik"}, required = true, description = "БИК участника")
  String bik;

  @Option(names = {"-f", "--file"}, description = "Файл ED807; по умолчанию data/ED807.xml")
  Path file;

  @Option(names = {"-s", "--seed"}, description = "Seed для клиентского счёта")
  Long seed;

  @Option(names = {"-o", "--output"}, defaultValue = "ed808.xml", description = "Куда записать XML")
  Path output;

  @Override
  public Integer call() throws Exception {
    List<Participant> participants = CliBik.participants(file);
    Participant participant;
    try {
      participant = participants.stream()
        .filter(p -> p.bik().equals(bik))
        .findFirst()
        .orElseThrow(() -> new NoSuchElementException("Участник не найден: " + bik));
    } catch (NoSuchElementException e) {
      System.err.println(e.getMessage());
      return 2;
    }

    AccountGenerator generator = seed == null ? new AccountGenerator() : new AccountGenerator(new Random(seed));
    String clientAccount = generator.generate(
      AccountGenerator.DEFAULT_BALANCE_ACCOUNT, AccountType.CLAC, participant.bik());

    List<Ed808Exporter.AccountRef> accounts = new ArrayList<>();
    if (participant.corrAccount() != null) {
      accounts.add(Ed808Exporter.AccountRef.correspondent(participant.corrAccount()));
    }
    accounts.add(Ed808Exporter.AccountRef.bank(clientAccount));

    LocalDate today = LocalDate.now();
    byte[] xml = Ed808Exporter.toBytes(
      participant,
      accounts,
      Ed808Exporter.Header.defaults("4525545000", "4525545000", today, "1"),
      Ed807Exporter.Defaults.moscow());

    Path parent = output.toAbsolutePath().getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    Files.write(output, xml);
    System.err.println("Записано: " + output.toAbsolutePath() + " (" + xml.length + " байт)");
    return 0;
  }
}
