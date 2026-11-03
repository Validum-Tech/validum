package ru.validum.cli;

import java.io.PrintStream;
import java.time.LocalDate;

/**
 * Строка «чем сгенерировано» — версия, seed и дата расчёта.
 *
 * <p>Пишется в stderr, чтобы не ломать stdout с данными. При автоматическом seed (когда
 * пользователь не передал {@code --seed}) печатается готовая команда для повторного прогона:
 * без неё упавший тест невозможно воспроизвести.
 */
final class OutputMeta {

  private OutputMeta() {}

  static void print(PrintStream err, long seed, LocalDate asOf, boolean seedWasGenerated) {
    StringBuilder line = new StringBuilder("# validum ")
      .append(BuildInfo.version())
      .append(" · seed=")
      .append(seed);
    if (asOf != null) {
      line.append(" · asOf=").append(asOf);
    }
    err.println(line);
    if (seedWasGenerated) {
      err.println("# seed сгенерирован автоматически; повторить прогон: --seed " + seed);
    }
  }
}
