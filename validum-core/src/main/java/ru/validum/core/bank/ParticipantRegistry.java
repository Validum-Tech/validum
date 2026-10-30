package ru.validum.core.bank;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Полный реестр участников платёжной системы Банка России из справочника ED807.
 *
 * <p>В отличие от {@link BikSource} (только банки), хранит <b>все</b> записи справочника —
 * банки, РКЦ, казначейство, филиалы, конкурсных управляющих и т.д. — с их типом (PtType).
 *
 * <p>Неизменяемый: строится один раз из {@link BikRegistryLoader#loadRegistry(java.nio.file.Path)}
 * и отдаёт неизменяемые списки.
 */
public final class ParticipantRegistry {

  private final List<Participant> participants;
  private final Map<String, Participant> byBik;

  public ParticipantRegistry(List<Participant> participants) {
    this.participants = List.copyOf(participants);
    this.byBik =
      participants.stream()
        .collect(
          Collectors.toUnmodifiableMap(Participant::bik, Function.identity(), (a, b) -> a));
  }

  /** Все участники реестра. */
  public List<Participant> all() {
    return participants;
  }

  /** Участники заданного типа (PtType). */
  public List<Participant> byType(ParticipantType type) {
    return participants.stream().filter(p -> p.type() == type).toList();
  }

  /** Только действующие банки (PtType=20, PSAC, с корсчётом) — как {@link BikSource#banks()}. */
  public List<Bank> banks() {
    return participants.stream()
      .filter(Participant::isActiveBank)
      .map(Participant::toBank)
      .toList();
  }

  /** Поиск участника по БИК. */
  public Optional<Participant> findByBik(String bik) {
    return Optional.ofNullable(byBik.get(bik));
  }

  /** Количество участников. */
  public int size() {
    return participants.size();
  }
}
