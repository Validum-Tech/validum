package ru.validum.api.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.validum.core.account.AccountGenerator;
import ru.validum.core.account.AccountType;
import ru.validum.core.bank.BikSources;
import ru.validum.core.bank.Participant;
import ru.validum.core.bank.ParticipantType;
import ru.validum.core.export.Ed807Exporter;
import ru.validum.core.export.Ed808Exporter;

/**
 * Экспорт в форматы ЦБ РФ: ED807 (справочник БИК) и ED808 (профиль участника).
 *
 * <p>Справочник берётся через {@link BikSources}: {@code VALIDUM_BIK_FILE} → свойство {@code
 * validum.bik.file} → {@code data/ED807.xml}. Встроенного демо-набора нет: без файла {@link
 * ru.validum.core.bank.BikSourceNotFoundException} превращается в 503 {@code bik_source_missing}.
 */
@Service
public class ExportService {

  private static final Logger log = LoggerFactory.getLogger(ExportService.class);
  private static final String ED_AUTHOR = "4525545000";

  /** Участники справочника: явный файл → env → свойство → {@code data/ED807.xml}. */
  public List<Participant> participants() {
    return BikSources.resolveParticipants(null, log::info).all();
  }

  /** ED807 в байтах windows-1251; {@code type} — код PtType (например, {@code 20}). */
  public byte[] ed807(String type, Integer limit) {
    List<Participant> list = participants();
    if (type != null && !type.isBlank()) {
      ParticipantType wanted = ParticipantType.fromCode(type.trim());
      list = list.stream().filter(p -> p.type() == wanted).toList();
    }
    if (limit != null && limit > 0 && list.size() > limit) {
      list = list.subList(0, limit);
    }
    LocalDate today = LocalDate.now();
    return Ed807Exporter.toBytes(
      list,
      Ed807Exporter.Header.defaults(ED_AUTHOR, ED_AUTHOR, today, "1"),
      Ed807Exporter.Defaults.moscow());
  }

  /** ED808 в байтах windows-1251 для участника с указанным БИК. */
  public byte[] ed808(String bik, Long seed) {
    List<Participant> list = participants();
    Participant p = list.stream()
      .filter(x -> x.bik().equals(bik))
      .findFirst()
      .orElseThrow(() -> new NoSuchElementException("Участник не найден: " + bik));

    AccountGenerator generator = seed == null ? new AccountGenerator() : new AccountGenerator(new Random(seed));
    String clientAccount = generator.generate(
      AccountGenerator.DEFAULT_BALANCE_ACCOUNT, AccountType.CLAC, p.bik());

    List<Ed808Exporter.AccountRef> accounts = new ArrayList<>();
    if (p.corrAccount() != null) {
      accounts.add(Ed808Exporter.AccountRef.correspondent(p.corrAccount()));
    }
    accounts.add(Ed808Exporter.AccountRef.bank(clientAccount));

    LocalDate today = LocalDate.now();
    return Ed808Exporter.toBytes(
      p,
      accounts,
      Ed808Exporter.Header.defaults(ED_AUTHOR, ED_AUTHOR, today, "1"),
      Ed807Exporter.Defaults.moscow());
  }
}
