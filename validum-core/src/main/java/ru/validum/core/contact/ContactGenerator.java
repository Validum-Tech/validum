package ru.validum.core.contact;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;
import ru.validum.core.name.PersonName;
import ru.validum.core.random.DataGenerator;

/**
 * Генератор контактов клиента (мобильный телефон, email).
 */
public final class ContactGenerator implements DataGenerator {

  public static final String CLIENTS_PHONE_DESCRIPTION = "Телефон клиента из автотеста";

  private static final String EMAIL_DESCRIPTION = "Описание для email";
  private static final String STATUS_VALID = "Действительный";

  private static final List<String> DOMAINS_TEST = loadLines("/emails/domains_test.txt");
  private static final List<String> DOMAINS_REALISTIC = loadLines("/emails/domains_realistic.txt");

  private static final String[] MOBILE_PREFIXES = {
    "900", "901", "902", "903", "904", "905", "906", "908", "909",
    "910", "911", "912", "913", "914", "915", "916", "917", "918", "919",
    "920", "921", "922", "923", "924", "925", "926", "927", "928", "929",
    "930", "931", "932", "933", "934", "936", "937", "938", "939",
    "950", "951", "952", "953", "954", "955", "956", "957", "958", "959",
    "960", "961", "962", "963", "964", "965", "966", "967", "968", "969",
    "977", "978", "980", "981", "982", "983", "984", "985", "986", "987", "988", "989",
    "991", "992", "993", "994", "995", "996", "997", "999"
  };

  private final Random random;

  public ContactGenerator() {
    this(DataGenerator.defaultRandom());
  }

  public ContactGenerator(Random random) {
    this.random = random;
  }

  // ── Мобильный телефон ────────────────────────────────────────────────────

  public Contact mobile() {
    String prefix = MOBILE_PREFIXES[random.nextInt(MOBILE_PREFIXES.length)];
    StringBuilder sb = new StringBuilder(10);
    sb.append(prefix);
    for (int i = 0; i < 7; i++) {
      sb.append(random.nextInt(10));
    }
    return new Contact(
      ContactType.MOBILE, sb.toString(), CLIENTS_PHONE_DESCRIPTION, STATUS_VALID, true);
  }

  // ── Email ────────────────────────────────────────────────────────────────

  /**
   * Email без привязки к ФИО. Используется для организаций и как откат для физлиц, когда
   * транслитерация ФИО не дала результата.
   *
   * <p>Формат: {@code client12345@domain}.
   */
  public Contact email(EmailDomainMode mode) {
    String localPart = "client" + (10000 + random.nextInt(90000));
    String domain = pickDomain(mode);
    return new Contact(ContactType.EMAIL, localPart + "@" + domain, EMAIL_DESCRIPTION, STATUS_VALID, true);
  }

  /**
   * Email, привязанный к ФИО. Формат: {@code {lastName}.{fInitial}{pInitial}@domain}.
   *
   * <p>Пример: {@code Петров Сергей Иванович} → {@code petrov.si@test.ru}.
   *
   * <p>Если транслитерация дала пустой результат (например, имя состоит из символов,
   * которых нет в кириллице), происходит откат на пул {@link #email(EmailDomainMode)}.
   */
  public Contact email(PersonName name, EmailDomainMode mode) {
    String lastName = cleanTranslit(name.lastName());
    String firstName = cleanTranslit(name.firstName());
    String patronymic = cleanTranslit(name.patronymic());

    String fInit = firstName.isEmpty() ? "" : firstName.substring(0, 1);
    String pInit = patronymic.isEmpty() ? "" : patronymic.substring(0, 1);

    if (lastName.isEmpty() || fInit.isEmpty()) {
      return email(mode);
    }

    String localPart = lastName + "." + fInit + pInit;
    String domain = pickDomain(mode);

    return new Contact(ContactType.EMAIL, localPart + "@" + domain, EMAIL_DESCRIPTION, STATUS_VALID, true);
  }

  /** Случайный контакт — мобильный или email (для UI/тестов). */
  public Contact any() {
    return random.nextBoolean() ? mobile() : email(EmailDomainMode.TEST);
  }

  // ── Внутренние методы ────────────────────────────────────────────────────

  private String pickDomain(EmailDomainMode mode) {
    return switch (mode) {
      case TEST -> DOMAINS_TEST.get(random.nextInt(DOMAINS_TEST.size()));
      case REALISTIC -> DOMAINS_REALISTIC.get(random.nextInt(DOMAINS_REALISTIC.size()));
      case MIXED -> {
        boolean useTest = random.nextBoolean();
        List<String> pool = useTest ? DOMAINS_TEST : DOMAINS_REALISTIC;
        yield pool.get(random.nextInt(pool.size()));
      }
    };
  }

  private String cleanTranslit(String input) {
    String translit = Transliterator.transliterate(input).toLowerCase();
    // Оставляем только a-z и 0-9 (отсекаем пробелы, дефисы, апострофы)
    return translit.replaceAll("[^a-z0-9]", "");
  }

  private static List<String> loadLines(String resource) {
    try (InputStream in = ContactGenerator.class.getResourceAsStream(resource)) {
      if (in == null) {
        throw new IllegalStateException("Resource not found: " + resource);
      }
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
        return reader.lines()
          .map(String::trim)
          .filter(s -> !s.isEmpty())
          .toList();
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to load " + resource, e);
    }
  }
}
