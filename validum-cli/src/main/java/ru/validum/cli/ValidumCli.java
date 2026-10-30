package ru.validum.cli;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import ru.validum.core.bank.BikSourceNotFoundException;

@Command(
  name = "validum",
  mixinStandardHelpOptions = true,
  version = "validum 0.1.0",
  description = "Validum — генератор валидных данных для российского финтеха и госсектора",
  subcommands = {
    GenerateCommand.class,
    FillCommand.class,
    ValidateCommand.class,
    FetchBikCommand.class,
    ParticipantsCommand.class,
    ScenariosCommand.class,
    ExportCommand.class,
    VersionCommand.class,
  }
)
public final class ValidumCli implements Runnable {

  @Override
  public void run() {
    CommandLine.usage(this, System.out);
  }

  public static void main(String[] args) {
    // Windows: консоль по умолчанию — cp866/cp1251, кириллица ломается.
    // Форсим UTF-8 для stdout и stderr. Внешний PrintStream пишет байты
    // через write(byte[]) — кодировка обёрнутого System.out не применяется.
    System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
    System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));

    int exitCode = commandLine().execute(args);
    System.exit(exitCode);
  }

  /**
   * Готовая командная строка — общая для {@link #main} и тестов.
   *
   * <p>Отсутствие справочника БИК превращается в понятное сообщение и код возврата 2, а не в
   * стектрейс: справочник обязателен, встроенного демо-набора в продукте нет.
   */
  public static CommandLine commandLine() {
    CommandLine commandLine = new CommandLine(new ValidumCli());
    commandLine.setExecutionExceptionHandler(
      (ex, cmd, parseResult) -> {
        if (ex instanceof BikSourceNotFoundException missing) {
          cmd.getErr().println(missing.getMessage());
          return 2;
        }
        throw ex;
      });
    return commandLine;
  }
}
