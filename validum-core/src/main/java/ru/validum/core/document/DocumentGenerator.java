package ru.validum.core.document;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import ru.validum.core.common.Region;
import ru.validum.core.random.DataGenerator;

/**
 * Генератор документов, удостоверяющих личность.
 *
 * <p>Формат серии и номера — из нормативных актов о бланках, см. {@link DocumentType} и
 * {@code ПП РФ № 2267, № 298, № 789, № 356; приказы МВД № 365 и МО № 700}. Срок действия считается от даты выдачи; для паспорта
 * РФ и военного билета срок задаётся возрастом, поэтому {@code expirationDate} = {@code null}.
 *
 * <p>Даты выдачи лежат в прошлом (до 9 лет назад) — документ может быть как свежим, так и
 * близким к замене.
 */
public final class DocumentGenerator implements DataGenerator {

  /** Буквы, из которых собираются серии военных билетов (без Ё, Й, Ъ, Ы, Ь). */
  private static final String SERIES_LETTERS = "АБВГДЕЖЗИКЛМНОПРСТУФХЦЧШЭЮЯ";

  /** Линейка серий биометрических загранпаспортов (10 лет). */
  private static final List<String> FOREIGN_PASSPORT_SERIES =
    List.of("71", "72", "73", "75", "76", "79");

  private static final List<String> PASSPORT_RF_ISSUERS =
    List.of(
      "ГУ МВД России по г. Москве",
      "УМВД России по г. Санкт-Петербургу и Ленинградской области",
      "Отделом УФМС России по Московской области",
      "ОП № 3 УМВД России по г. Казани",
      "Отделением УФМС России по Свердловской области");

  private static final List<String> MID_ISSUERS =
    List.of("МИД России", "Консульский департамент МИД России", "Посольство России за рубежом");

  private static final List<String> MIGRATION_ISSUERS =
    List.of(
      "ГУ МВД России по вопросам миграции",
      "УВМ МВД России по г. Москве",
      "Отделом УФМС России по Краснодарскому краю");

  private static final List<String> MILITARY_ISSUERS =
    List.of(
      "Военным комиссариатом г. Москвы",
      "Отделом военного комиссариата Московской области",
      "Военным комиссариатом г. Екатеринбурга");

  private static final List<String> GIBDD_ISSUERS =
    List.of("ГИБДД 0215", "МРЭО ГИБДД ГУ МВД России по г. Москве", "ГИБДД 7712");

  /** Как называется подразделение, выдавшее паспорт РФ; регион подставляется отдельно. */
  private static final List<String> ISSUER_PREFIXES =
    List.of("Отделом УФМС России по ", "Отделением УФМС России по ", "ОУФМС России по ");

  private static final List<String> FOREIGN_ISSUERS =
    List.of(
      "Компетентные органы Республики Казахстан",
      "Компетентные органы Республики Таджикистан",
      "Компетентные органы Республики Узбекистан");

  private final Random random;

  public DocumentGenerator() {
    this(new Random());
  }

  public DocumentGenerator(Random random) {
    this.random = Objects.requireNonNull(random, "random");
  }

  /** Сгенерировать документ указанного вида (сроки — относительно сегодняшней даты). */
  public IdentityDocument generate(DocumentType type) {
    return generate(type, LocalDate.now());
  }

  /**
   * Сгенерировать документ указанного вида на заданную дату расчёта.
   *
   * <p>Дата выдачи лежит в пределах предыдущих девяти лет от {@code asOf}, срок действия — от даты
   * выдачи. Так профиль воспроизводим: один seed и одна дата расчёта дают один документ.
   */
  public IdentityDocument generate(DocumentType type, LocalDate asOf) {
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(asOf, "asOf");
    LocalDate issueDate = asOf.minusDays(1L + random.nextInt(365 * 9));
    Integer years = type.validityYears();
    LocalDate expirationDate = years == null ? null : issueDate.plusYears(years);
    return new IdentityDocument(
      type,
      series(type),
      number(type.numberLength()),
      issuer(type),
      issueDate,
      expirationDate,
      type.hasDepartmentCode() ? departmentCode(null) : null);
  }

  /**
   * Сгенерировать документ с привязкой к дате рождения держателя.
   *
   * <p>Дата выдачи не раньше {@code birthDate + minAge} и не позже {@code asOf}; у документов
   * со сроком действия она вдобавок не раньше {@code asOf - validityYears}, чтобы на дату расчёта
   * документ не был просрочен. Так паспорт не выдаётся пятилетнему, а права — семнадцатилетнему,
   * а у несовершеннолетнего не появляется сорокалетний стаж владения.
   */
  public IdentityDocument generate(DocumentType type, LocalDate birthDate, LocalDate asOf) {
    return generate(type, birthDate, asOf, null);
  }

  /**
   * То же, но «кем выдан» согласован с регионом прописки держателя.
   *
   * @param regionCode код субъекта РФ; {@code null} — орган выбирается случайно из справочника
   */
  public IdentityDocument generate(
    DocumentType type, LocalDate birthDate, LocalDate asOf, String regionCode) {
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(birthDate, "birthDate");
    Objects.requireNonNull(asOf, "asOf");

    LocalDate earliest = birthDate.plusYears(type.minAge());
    Integer years = type.validityYears();
    if (years != null) {
      LocalDate notExpired = asOf.minusYears(years);
      if (notExpired.isAfter(earliest)) {
        earliest = notExpired;
      }
    }
    if (earliest.isAfter(asOf)) {
      earliest = asOf;
    }
    long days = java.time.temporal.ChronoUnit.DAYS.between(earliest, asOf);
    LocalDate issueDate = earliest.plusDays(days == 0 ? 0L : random.nextInt((int) days + 1));
    LocalDate expirationDate = years == null ? null : issueDate.plusYears(years);
    return new IdentityDocument(
      type,
      series(type),
      number(type.numberLength()),
      issuer(type, regionCode),
      issueDate,
      expirationDate,
      type.hasDepartmentCode() ? departmentCode(regionCode) : null);
  }

  private String series(DocumentType type) {
    if (type == DocumentType.FOREIGN_PASSPORT) {
      return FOREIGN_PASSPORT_SERIES.get(random.nextInt(FOREIGN_PASSPORT_SERIES.size()));
    }
    if (type == DocumentType.RESIDENCE_PERMIT) {
      return random.nextBoolean() ? "82" : "00";
    }
    if (type == DocumentType.VISA) {
      int group = random.nextInt(3);
      int base = group == 0 ? 10 : group == 1 ? 20 : 30;
      int value = group == 2 ? 30 : base + random.nextInt(10);
      return String.format("%02d", value);
    }
    if (type.seriesLetters()) {
      return letters(type.seriesLength());
    }
    String digits = digits(type.seriesLength());
    return type.groupedSeries() ? digits.substring(0, 2) + " " + digits.substring(2) : digits;
  }

  private String letters(int length) {
    StringBuilder builder = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      builder.append(SERIES_LETTERS.charAt(random.nextInt(SERIES_LETTERS.length())));
    }
    return builder.toString();
  }

  private String number(int length) {
    return digits(length);
  }

  private String digits(int length) {
    StringBuilder builder = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      builder.append(random.nextInt(10));
    }
    return builder.toString();
  }

  /**
   * Код подразделения: первые три цифры — код региона (77Х, 02Х). Без региона — просто 100–999,
   * чтобы формат оставался {@code XXX-XXX}.
   */
  private String departmentCode(String regionCode) {
    String prefix = regionCode != null && regionCode.length() == 2 ? regionCode : null;
    String head = prefix == null
      ? String.format("%03d", 100 + random.nextInt(900))
      : prefix + (1 + random.nextInt(9));
    return head + "-" + String.format("%03d", 1 + random.nextInt(999));
  }

  private String issuer(DocumentType type, String regionCode) {
    Region region = regionCode == null ? null : Region.byCode(regionCode);
    if (region != null) {
      return switch (type) {
        case PASSPORT_RF -> pick(ISSUER_PREFIXES) + region.department().ufms();
        case MILITARY_ID, MILITARY_ID_RESERVE ->
          "Военным комиссариатом " + region.department().military();
        default -> issuer(type);
      };
    }
    return issuer(type);
  }

  private String issuer(DocumentType type) {
    return switch (type) {
      case PASSPORT_RF, FOREIGN_PASSPORT -> pick(PASSPORT_RF_ISSUERS);
      case DIPLOMATIC_PASSPORT, VISA -> pick(MID_ISSUERS);
      case PASSPORT_FOREIGN -> pick(FOREIGN_ISSUERS);
      case RESIDENCE_PERMIT, TEMPORARY_RESIDENCE, REFUGEE_CERTIFICATE -> pick(MIGRATION_ISSUERS);
      case MILITARY_ID, MILITARY_ID_RESERVE -> pick(MILITARY_ISSUERS);
      case DRIVER_LICENSE -> pick(GIBDD_ISSUERS);
    };
  }

  private String pick(List<String> values) {
    return values.get(random.nextInt(values.size()));
  }
}
