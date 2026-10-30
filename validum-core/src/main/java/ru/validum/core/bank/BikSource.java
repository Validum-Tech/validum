package ru.validum.core.bank;

import java.util.List;
import java.util.Optional;

/**
 * Источник данных о банках РФ.
 *
 * <p>Абстракция позволяет:
 *
 * <ul>
 *   <li>читать справочник из файла ED807 через {@link BikSources};
 *   <li>подключить полный справочник ЦБ РФ из файла {@code bnkseek} (в будущих версиях);
 *   <li>подменить справочник на свой в тестах.
 * </ul>
 */
public interface BikSource {

  /** Все банки источника. */
  List<Bank> banks();

  /** Поиск банка по БИК. */
  Optional<Bank> findByBik(String bik);

  /** Количество банков. */
  default int size() {
    return banks().size();
  }
}
