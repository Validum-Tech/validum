package ru.validum.core.passport;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import ru.validum.core.common.Country;
import ru.validum.core.common.Region;
import ru.validum.core.random.DataGenerator;

/**
 * Генератор паспортов.
 *
 * <p>Дата выдачи рассчитывается по правилам замены паспорта РФ:
 *
 * <ul>
 *   <li>14 лет — первое получение;
 *   <li>20 лет — замена;
 *   <li>45 лет — замена (последняя, далее бессрочный).
 * </ul>
 *
 * <p>Просроченный паспорт: если {@code passportWasExchanged=false} и возраст человека больше 20 —
 * паспорт будет просрочен.
 *
 * <p>Для {@link PassportType#FOREIGN} серия/номер синтетические, шаблон «кем выдан» — иностранный.
 *
 * <p>Если задан {@code regionCode} (2 цифры, код субъекта РФ) — серия начинается с него.
 */
public final class PassportGenerator implements DataGenerator {

  /** Возраст первого получения паспорта РФ. */
  public static final int AGE_FIRST_ISSUE = 14;

  /** Возраст первой замены паспорта РФ. */
  public static final int AGE_FIRST_EXCHANGE = 20;

  /** Возраст второй замены (последняя, далее бессрочный). */
  public static final int AGE_SECOND_EXCHANGE = 45;

  private static final int INDEFINITE_VALIDITY_YEARS = 100;

  private static final DateTimeFormatter DATE_FORMATTER =
    DateTimeFormatter.ofPattern("dd.MM.yyyy");

  private static final String STATUS_VALID = "Действительный";
  private static final String STATUS_EXPIRED = "Просрочен";

  /**
   * Коды регионов для префикса серии — ровно те, что есть в {@link Region}. Раньше здесь лежал
   * отдельный список с кодами 45 и 34, которых в перечислении нет: серия «45…» получала паспорт,
   * выданный в другом регионе, и собственная проверка согласованности этого не видела.
   */
  private static final List<String> REGION_CODES =
    java.util.Arrays.stream(Region.values()).map(Region::code).toList();

  /** Как называется подразделение, выдавшее паспорт; регион подставляется отдельно. */
  private static final List<String> ISSUER_PREFIXES =
    List.of("Отделом УФМС России по ", "Отделением УФМС России по ", "ОУФМС России по ");

  private static final List<String> FOREIGN_ISSUED_BY_TEMPLATES =
    List.of("Consulate General", "Embassy", "Ministry of Foreign Affairs");

  private final Random random;

  public PassportGenerator() {
    this(DataGenerator.defaultRandom());
  }

  public PassportGenerator(Random random) {
    this.random = random;
  }

  /** Паспорт РФ, действительный на текущую дату. */
  public Passport generate(LocalDate birthDate) {
    return generate(birthDate, LocalDate.now());
  }

  /** Паспорт РФ, действительный на указанную дату. */
  public Passport generate(LocalDate birthDate, LocalDate today) {
    return generate(birthDate, today, true);
  }

  /** Паспорт РФ с указанием, менялся ли он по возрасту. */
  public Passport generate(LocalDate birthDate, LocalDate today, boolean passportWasExchanged) {
    return generate(birthDate, today, passportWasExchanged, PassportType.RF, Country.RU, null);
  }

  /** Паспорт с указанием типа и страны документа. Регион — случайный. */
  public Passport generate(
    LocalDate birthDate,
    LocalDate today,
    boolean passportWasExchanged,
    PassportType type,
    Country country) {
    return generate(birthDate, today, passportWasExchanged, type, country, null);
  }

  /**
   * Паспорт с полной кастомизацией: тип, страна документа и код региона.
   *
   * @param regionCode код субъекта РФ (2 цифры) — первые 2 цифры серии для РФ-паспорта. Если
   *     {@code null} — регион случайный из {@link #REGION_CODES}. Для {@code FOREIGN} игнорируется.
   */
  public Passport generate(
    LocalDate birthDate,
    LocalDate today,
    boolean passportWasExchanged,
    PassportType type,
    Country country,
    String regionCode) {

    LocalDate issueDate = calculateIssueDate(birthDate, today, passportWasExchanged);
    LocalDate expirationDate = calculateExpirationDate(birthDate, issueDate);
    boolean isExpired = expirationDate.isBefore(today);

    String seriesPrefix;
    if (type == PassportType.FOREIGN) {
      seriesPrefix = String.format("%02d", random.nextInt(100));
    } else if (regionCode != null && regionCode.length() == 2) {
      seriesPrefix = regionCode;
    } else {
      seriesPrefix = REGION_CODES.get(random.nextInt(REGION_CODES.size()));
    }
    String series = seriesPrefix + String.format("%02d", random.nextInt(100));

    String number = String.format("%06d", random.nextInt(1_000_000));

    String issuedBy;
    if (type == PassportType.FOREIGN) {
      String template =
        FOREIGN_ISSUED_BY_TEMPLATES.get(random.nextInt(FOREIGN_ISSUED_BY_TEMPLATES.size()));
      issuedBy = template + " of " + country.countryName();
    } else {
      // «Кем выдан» согласован с регионом прописки: Московская область — не Екатеринбург.
      Region issuer = Region.byCode(seriesPrefix);
      if (issuer == null) {
        issuer = Region.values()[random.nextInt(Region.values().length)];
      }
      issuedBy = ISSUER_PREFIXES.get(random.nextInt(ISSUER_PREFIXES.size()))
        + issuer.department().ufms();
    }

    // Код подразделения начинается с кода региона и должен согласовываться с серией:
    // Москва (77) — 77Х-ХХХ, Башкортостан (02) — 02Х-ХХХ.
    String departmentCode =
      String.format("%s%d-%03d", seriesPrefix, 1 + random.nextInt(9), 1 + random.nextInt(999));
    String status = isExpired ? STATUS_EXPIRED : STATUS_VALID;

    return new Passport(
      series, number, issuedBy, issueDate, expirationDate, departmentCode, status, type, country);
  }

  private static LocalDate calculateIssueDate(
    LocalDate birthDate, LocalDate today, boolean passportWasExchanged) {
    if (!passportWasExchanged) {
      return birthDate.plusYears(AGE_FIRST_ISSUE);
    }
    int age = Period.between(birthDate, today).getYears();
    if (age >= AGE_SECOND_EXCHANGE) {
      return birthDate.plusYears(AGE_SECOND_EXCHANGE);
    }
    if (age >= AGE_FIRST_EXCHANGE) {
      return birthDate.plusYears(AGE_FIRST_EXCHANGE);
    }
    return birthDate.plusYears(AGE_FIRST_ISSUE);
  }

  private static LocalDate calculateExpirationDate(LocalDate birthDate, LocalDate issueDate) {
    int issueAge = Period.between(birthDate, issueDate).getYears();
    if (issueAge >= AGE_SECOND_EXCHANGE) {
      return issueDate.plusYears(INDEFINITE_VALIDITY_YEARS);
    }
    if (issueAge >= AGE_FIRST_EXCHANGE) {
      return birthDate.plusYears(AGE_SECOND_EXCHANGE);
    }
    return birthDate.plusYears(AGE_FIRST_EXCHANGE);
  }

  /** Форматирование даты в {@code dd.MM.yyyy}. */
  public static String format(LocalDate date) {
    return date.format(DATE_FORMATTER);
  }
}
