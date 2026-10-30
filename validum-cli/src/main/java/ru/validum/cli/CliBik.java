package ru.validum.cli;

import java.nio.file.Path;
import java.util.List;
import ru.validum.core.bank.BikSource;
import ru.validum.core.bank.BikSources;
import ru.validum.core.bank.Participant;

/**
 * Единственная точка выбора справочника БИК для CLI.
 *
 * <p>Вся логика поиска файла живёт в core {@link BikSources}; здесь только приёмник замечаний (stderr)
 * и форма ответа, которая нужна командам: {@link BikSource} или список {@link Participant}.
 */
final class CliBik {

  private CliBik() {}

  /** Справочник БИК по цепочке core (явный файл → env → свойство → data/ED807.xml). */
  static BikSource source() {
    return BikSources.resolve(null, System.err::println);
  }

  /**
   * Участники справочника для команд экспорта.
   *
   * @param file путь из {@code --file}, может быть {@code null}
   */
  static List<Participant> participants(Path file) {
    return BikSources
      .resolveParticipants(file == null ? null : file.toString(), System.err::println)
      .all();
  }
}
