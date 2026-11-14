package ru.validum.cli;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Корневая команда {@code generate}: либо явная подкоманда, либо набор из спецификации.
 *
 * <p>{@code validum generate --spec clients.validum.json} читает декларацию (W47-C1) и исполняет её
 * ровно тем же кодом, что и вызов с флагами: единая логика, никаких «вторых» путей генерации.
 */
@Command(
  name = "generate",
  mixinStandardHelpOptions = true,
  description = "Генерация профилей: подкоманда или набор по спецификации --spec",
  subcommands = {
    GenerateIndividualCommand.class,
    GenerateIpCommand.class,
    GenerateOrganizationCommand.class,
    GenerateDocumentsCommand.class,
  }
)
public final class GenerateCommand implements Callable<Integer> {

  @Option(
    names = "--spec",
    paramLabel = "FILE",
    description = "Спецификация набора (.validum.json): type, count, seed, asOf, format, options")
  Path spec;

  @Option(
    names = {"-o", "--output"},
    description = "Переопределить путь вывода из спецификации")
  Path output;

  @Override
  public Integer call() throws Exception {
    if (spec == null) {
      new CommandLine(this).usage(System.err);
      return 2;
    }

    GenerateSpec specification;
    try {
      specification = GenerateSpec.load(spec);
    } catch (IllegalArgumentException e) {
      System.err.println("Ошибка в " + spec + ": " + e.getMessage());
      return 2;
    }
    if (output != null) {
      specification = specification.withOutput(output);
    }

    try {
      return switch (specification.type()) {
        case "individual" -> individual(specification).call();
        case "ip" -> ip(specification).call();
        case "organization" -> organization(specification).call();
        case "documents" -> documents(specification).call();
        default -> {
          System.err.println("Неизвестный type в спецификации: " + specification.type());
          yield 2;
        }
      };
    } catch (IllegalArgumentException e) {
      System.err.println("Ошибка: " + e.getMessage());
      return 2;
    }
  }

  private GenerateIndividualCommand individual(GenerateSpec s) {
    GenerateIndividualCommand command = new GenerateIndividualCommand();
    command.common.count = count(s);
    command.common.seed = s.seed();
    command.asOf = s.asOf();
    command.common.format = format(s);
    command.common.output = path(s);
    command.unique = Boolean.TRUE.equals(s.unique());
    command.terrorist = Boolean.TRUE.equals(s.terrorist());
    command.blocked = Boolean.TRUE.equals(s.blocked());
    command.noAccount = Boolean.TRUE.equals(s.noAccount());
    command.region = s.region();
    command.age = s.age();
    command.ageRange = s.ageRange();
    if (s.ageDistribution() != null) command.ageDistribution = s.ageDistribution();
    command.gender = s.gender();
    command.bik = s.bik();
    command.documents = s.documents() == null ? null : s.documents().split(",");
    command.corrupt = s.corrupt() == null ? null : s.corrupt().split(",");
    command.documentsMode = s.documentsMode() == null ? "inline" : s.documentsMode();
    command.residency = s.residency() == null ? "resident" : s.residency();
    command.country = s.country();
    command.common.dialect = s.dialect() == null ? "postgres" : s.dialect();
    command.common.table = s.table() == null ? "validum_individual" : s.table();
    command.common.noDdl = Boolean.TRUE.equals(s.noDdl());
    command.fields = s.fields();
    return command;
  }

  private GenerateIpCommand ip(GenerateSpec s) {
    GenerateIpCommand command = new GenerateIpCommand();
    command.common.count = count(s);
    command.common.seed = s.seed();
    command.asOf = s.asOf();
    command.common.format = format(s);
    command.common.output = path(s);
    command.unique = Boolean.TRUE.equals(s.unique());
    command.terrorist = Boolean.TRUE.equals(s.terrorist());
    command.blocked = Boolean.TRUE.equals(s.blocked());
    command.region = s.region();
    command.age = s.age();
    command.ageRange = s.ageRange();
    if (s.ageDistribution() != null) command.ageDistribution = s.ageDistribution();
    command.gender = s.gender();
    command.bik = s.bik();
    command.okved = s.okved();
    command.common.dialect = s.dialect() == null ? "postgres" : s.dialect();
    command.common.table = s.table() == null ? "validum_ip" : s.table();
    command.common.noDdl = Boolean.TRUE.equals(s.noDdl());
    command.fields = s.fields();
    return command;
  }

  private GenerateOrganizationCommand organization(GenerateSpec s) {
    GenerateOrganizationCommand command = new GenerateOrganizationCommand();
    command.common.count = count(s);
    command.common.seed = s.seed();
    command.common.format = format(s);
    command.common.output = path(s);
    command.unique = Boolean.TRUE.equals(s.unique());
    command.terrorist = Boolean.TRUE.equals(s.terrorist());
    command.blocked = Boolean.TRUE.equals(s.blocked());
    command.type = s.organizationType();
    command.name = s.organizationName();
    command.region = s.region();
    command.okved = s.okved();
    command.bankBik = s.bik();
    command.common.dialect = s.dialect() == null ? "postgres" : s.dialect();
    command.common.table = s.table() == null ? "validum_organization" : s.table();
    command.fields = s.fields();
    command.common.noDdl = Boolean.TRUE.equals(s.noDdl());
    return command;
  }

  private GenerateDocumentsCommand documents(GenerateSpec s) {
    if (s.documents() == null || s.documents().isBlank()) {
      throw new IllegalArgumentException(
        "Для type=documents нужно поле \"documents\" (коды через запятую)");
    }
    GenerateDocumentsCommand command = new GenerateDocumentsCommand();
    command.type = s.documents();
    command.common.count = count(s);
    command.common.seed = s.seed();
    command.asOf = s.asOf();
    command.common.format = format(s);
    command.common.output = path(s);
    command.common.dialect = s.dialect() == null ? "postgres" : s.dialect();
    command.common.table = s.table() == null ? "validum_documents" : s.table();
    command.common.noDdl = Boolean.TRUE.equals(s.noDdl());
    return command;
  }

  private static int count(GenerateSpec s) {
    return s.count() == null ? 1 : s.count();
  }

  private static String format(GenerateSpec s) {
    return s.format() == null ? "json" : s.format();
  }

  private static Path path(GenerateSpec s) {
    return s.output() == null ? null : Path.of(s.output());
  }
}
