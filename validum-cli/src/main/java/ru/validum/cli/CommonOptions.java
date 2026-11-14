package ru.validum.cli;

import java.nio.file.Path;
import picocli.CommandLine.Option;

/**
 * Общие опции всех генераторов: зерно, размер набора, формат, SQL-настройки и путь вывода.
 *
 * <p>Вынесены в mixin, чтобы не держать по четыре копии в каждой команде. Значение по умолчанию
 * для {@code --table} задаёт сама команда ({@code tableName()}), потому что имя таблицы зависит
 * от типа сущности.
 */
final class CommonOptions {

  @Option(names = "--seed", description = "Базовый seed (по умолчанию — System.nanoTime())")
  Long seed;

  @Option(names = {"-n", "--count"}, defaultValue = "1",
    description = "Количество записей (1–10000, по умолчанию 1)")
  int count;

  @Option(names = "--format", defaultValue = "json",
    description = "Формат: json | json-array | csv | csv-semicolon (русский Excel) | sql")
  String format;

  @Option(names = "--dialect", description = "SQL-диалект: postgres | oracle | mysql")
  String dialect = "postgres";

  @Option(names = "--table", description = "Имя таблицы для --format sql")
  String table;

  @Option(names = "--no-ddl", description = "Без CREATE TABLE, только INSERT")
  boolean noDdl;

  @Option(names = {"-o", "--output"}, description = "Путь к файлу (по умолчанию stdout)")
  Path output;
}
