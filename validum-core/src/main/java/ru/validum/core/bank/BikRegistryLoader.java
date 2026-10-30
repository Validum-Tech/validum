package ru.validum.core.bank;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Загрузчик справочника БИК ЦБ РФ из XML-файла (формат ED807).
 *
 * <p>Файл скачивается с официального сайта ЦБ РФ: https://www.cbr.ru/mcirabis/BIK/
 * (раздел «Справочник БИК РФ», файл формата ED807).
 *
 * <p>Два режима:
 * <ul>
 *   <li>{@link #loadRegistry(Path)} / {@link #loadRegistry(InputStream)} — <b>полный</b> реестр
 *       {@link ParticipantRegistry}: все участники (банки, РКЦ, казначейство, филиалы, конкурсные
 *       управляющие…) с типом {@link ParticipantType} (PtType).</li>
 *   <li>{@link #load(Path)} / {@link #load(InputStream)} — только <b>действующие банки</b>
 *       (обратная совместимость): PtType=20 + ParticipantStatus=PSAC + корсчёт CRSA/ACAC.</li>
 * </ul>
 *
 * <p>Использование:
 *
 * <pre>{@code
 * BikSource source = BikRegistryLoader.load(Path.of("ED807_full.xml"));       // только банки
 * ParticipantRegistry registry = BikRegistryLoader.loadRegistry(Path.of("ED807_full.xml")); // все
 * }</pre>
 */
public final class BikRegistryLoader {

  private static final String ENTRY = "BICDirectoryEntry";
  private static final String PARTICIPANT = "ParticipantInfo";
  private static final String ACCOUNT = "Accounts";

  private static final String ACC_TYPE_CORR = "CRSA";
  private static final String ACC_STATUS_ACTIVE = "ACAC";

  private BikRegistryLoader() {}

  /** Загружает только действующие банки (обратная совместимость). */
  public static BikSource load(Path file) throws IOException {
    try (InputStream in = Files.newInputStream(file)) {
      return load(in);
    }
  }

  /** Загружает только действующие банки (обратная совместимость). */
  public static BikSource load(InputStream in) throws IOException {
    return new InMemoryBikSource(loadRegistry(in).banks());
  }

  /** Загружает полный реестр участников (все PtType). */
  public static ParticipantRegistry loadRegistry(Path file) throws IOException {
    try (InputStream in = Files.newInputStream(file)) {
      return loadRegistry(in);
    }
  }

  /** Загружает полный реестр участников (все PtType). */
  public static ParticipantRegistry loadRegistry(InputStream in) throws IOException {
    try {
      XMLInputFactory factory = XMLInputFactory.newInstance();
      // Безопасность: отключаем DTD и внешние сущности
      factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
      factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
      // Работаем по local name, без namespace
      factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);

      // Явно указываем WINDOWS-1251 — формат ЦБ
      XMLStreamReader reader = factory.createXMLStreamReader(in, "WINDOWS-1251");
      try {
        List<Participant> participants = new ArrayList<>();
        parse(reader, participants);
        return new ParticipantRegistry(participants);
      } finally {
        reader.close();
      }
    } catch (XMLStreamException e) {
      throw new IOException("Не удалось распарсить справочник БИК: " + e.getMessage(), e);
    }
  }

  private static void parse(XMLStreamReader reader, List<Participant> participants)
    throws XMLStreamException {
    while (reader.hasNext()) {
      int event = reader.next();
      if (event == XMLStreamConstants.START_ELEMENT && ENTRY.equals(reader.getLocalName())) {
        parseEntry(reader, participants);
      }
    }
  }

  private static void parseEntry(XMLStreamReader reader, List<Participant> participants)
    throws XMLStreamException {
    String bik = reader.getAttributeValue(null, "BIC");
    if (bik == null || bik.length() != 9) {
      skipElement(reader, ENTRY);
      return;
    }

    String name = null;
    String city = null;
    String region = null;
    String corrAccount = null;
    String participantStatus = null;
    String ptTypeCode = null;
    String regN = null;
    String uid = null;
    String address = null;
    String index = null;

    int depth = 1;
    while (reader.hasNext() && depth > 0) {
      int event = reader.next();
      if (event == XMLStreamConstants.START_ELEMENT) {
        depth++;
        String localName = reader.getLocalName();
        if (PARTICIPANT.equals(localName)) {
          name = reader.getAttributeValue(null, "NameP");
          city = reader.getAttributeValue(null, "Nnp");
          region = reader.getAttributeValue(null, "Rgn");
          participantStatus = reader.getAttributeValue(null, "ParticipantStatus");
          ptTypeCode = reader.getAttributeValue(null, "PtType");
          regN = reader.getAttributeValue(null, "RegN");
          uid = reader.getAttributeValue(null, "UID");
          address = reader.getAttributeValue(null, "Adr");
          index = reader.getAttributeValue(null, "Ind");
        } else if (ACCOUNT.equals(localName)) {
          String accType = reader.getAttributeValue(null, "RegulationAccountType");
          String accStatus = reader.getAttributeValue(null, "AccountStatus");
          String acc = reader.getAttributeValue(null, "Account");
          if (ACC_TYPE_CORR.equals(accType)
            && ACC_STATUS_ACTIVE.equals(accStatus)
            && corrAccount == null) {
            corrAccount = acc;
          }
        }
      } else if (event == XMLStreamConstants.END_ELEMENT) {
        depth--;
      }
    }

    if (name == null) {
      return;
    }
    ParticipantType type;
    try {
      type = ptTypeCode == null ? null : ParticipantType.fromCode(ptTypeCode);
    } catch (IllegalArgumentException e) {
      // Неизвестный PtType — пропускаем (в реальном ED807 все 17 кодов известны).
      return;
    }
    if (type == null) {
      return;
    }
    regN = blankToNull(regN);
    uid = blankToNull(uid);
    address = blankToNull(address);
    index = blankToNull(index);
    try {
      participants.add(
        new Participant(
          bik,
          name,
          type,
          participantStatus,
          corrAccount,
          city != null ? city : "",
          region != null ? region : "",
          regN,
          uid,
          address,
          index));
    } catch (IllegalArgumentException e) {
      // Пропускаем записи с некорректным БИК или счётом.
    }
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private static void skipElement(XMLStreamReader reader, String elementName)
    throws XMLStreamException {
    int depth = 1;
    while (reader.hasNext() && depth > 0) {
      int event = reader.next();
      if (event == XMLStreamConstants.START_ELEMENT) {
        depth++;
      } else if (event == XMLStreamConstants.END_ELEMENT) {
        depth--;
      }
    }
  }

  /** Простая реализация BikSource в памяти. */
  private static final class InMemoryBikSource implements BikSource {
    private final List<Bank> banks;
    private final Map<String, Bank> byBik;

    InMemoryBikSource(List<Bank> banks) {
      this.banks = List.copyOf(banks);
      this.byBik =
        banks.stream()
          .collect(Collectors.toUnmodifiableMap(Bank::bik, Function.identity(), (a, b) -> a));
    }

    @Override
    public List<Bank> banks() {
      return banks;
    }

    @Override
    public Optional<Bank> findByBik(String bik) {
      return Optional.ofNullable(byBik.get(bik));
    }
  }
}
