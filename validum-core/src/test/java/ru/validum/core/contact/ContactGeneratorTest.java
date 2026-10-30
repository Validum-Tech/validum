package ru.validum.core.contact;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.name.Gender;
import ru.validum.core.name.PersonName;
import ru.validum.core.random.DataGenerator;

@DisplayName("ContactGenerator — email по ФИО и домены (P0.3)")
class ContactGeneratorTest {

  private final ContactGenerator generator =
    new ContactGenerator(DataGenerator.seededRandom(42L));

  // ── Мобильные ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Мобильный телефон: 10 цифр, тип MOBILE")
  void mobileHas10Digits() {
    Contact mobile = generator.mobile();
    assertThat(mobile.type()).isEqualTo(ContactType.MOBILE);
    assertThat(mobile.value()).hasSize(10).containsOnlyDigits();
  }

  // ── Email из ФИО ─────────────────────────────────────────────────────────

  @Test
  @DisplayName("Email привязан к ФИО: Петров Сергей Иванович → petrov.si@...")
  void emailFromPersonName() {
    PersonName name = new PersonName("Петров", "Сергей", "Иванович", Gender.MALE);
    Contact email = generator.email(name, EmailDomainMode.TEST);

    assertThat(email.type()).isEqualTo(ContactType.EMAIL);
    assertThat(email.value()).startsWith("petrov.si@");
  }

  @Test
  @DisplayName("Дефисы и спецсимволы в фамилии удаляются: Иванов-Петров → ivanovpetrov")
  void emailCleansSpecialCharacters() {
    PersonName name = new PersonName("Иванов-Петров", "Сергей", "Иванович", Gender.MALE);
    Contact email = generator.email(name, EmailDomainMode.TEST);

    // Дефис удалён, обе части слиты
    assertThat(email.value()).startsWith("ivanovpetrov.si@");
    // Никаких дефисов в local part
    String localPart = email.value().split("@")[0];
    assertThat(localPart).matches("[a-z0-9.]+");
  }

  @Test
  @DisplayName("Транслит имени и отчества: Щербаков Юрий Александрович → shcherbakov.ya@...")
  void emailTransliteratesCorrectly() {
    PersonName name = new PersonName("Щербаков", "Юрий", "Александрович", Gender.MALE);
    Contact email = generator.email(name, EmailDomainMode.TEST);

    assertThat(email.value()).startsWith("shcherbakov.ya@");
  }

  // ── Режимы доменов ───────────────────────────────────────────────────────

  @Test
  @DisplayName("TEST: домен всегда из безопасного пула")
  void testModeUsesTestDomains() {
    Set<String> testDomains = Set.of("test.ru", "example.com", "example.org", "example.net",
      "qa.local", "test.local", "demo.local", "mock.io", "test.tech", "fake.dev");

    PersonName name = new PersonName("Петров", "Сергей", "Иванович", Gender.MALE);
    for (int i = 0; i < 50; i++) {
      Contact email = generator.email(name, EmailDomainMode.TEST);
      String domain = email.value().split("@")[1];
      assertThat(testDomains).as("Домен %s", domain).contains(domain);
    }
  }

  @Test
  @DisplayName("REALISTIC: домен всегда из реалистичного пула")
  void realisticModeUsesRealisticDomains() {
    Set<String> realisticDomains = Set.of("mail.ru", "yandex.ru", "bk.ru", "inbox.ru",
      "list.ru", "internet.ru", "rambler.ru", "ya.ru", "gmail.com", "mail.com");

    PersonName name = new PersonName("Петров", "Сергей", "Иванович", Gender.MALE);
    for (int i = 0; i < 50; i++) {
      Contact email = generator.email(name, EmailDomainMode.REALISTIC);
      String domain = email.value().split("@")[1];
      assertThat(realisticDomains).as("Домен %s", domain).contains(domain);
    }
  }

  @Test
  @DisplayName("MIXED: за 100 генераций встречаются домены из обоих пулов")
  void mixedModeUsesBothPools() {
    Set<String> testDomains = Set.of("test.ru", "example.com", "qa.local");
    Set<String> realisticDomains = Set.of("mail.ru", "yandex.ru", "bk.ru");

    PersonName name = new PersonName("Петров", "Сергей", "Иванович", Gender.MALE);
    boolean sawTest = false;
    boolean sawRealistic = false;

    for (int i = 0; i < 100; i++) {
      String domain = generator.email(name, EmailDomainMode.MIXED).value().split("@")[1];
      if (testDomains.contains(domain) || domain.endsWith(".local") || domain.endsWith(".test")) sawTest = true;
      if (realisticDomains.contains(domain)) sawRealistic = true;
    }

    assertThat(sawTest).as("Хотя бы один тестовый домен").isTrue();
    assertThat(sawRealistic).as("Хотя бы один реалистичный домен").isTrue();
  }

  // ── Fallback ─────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Если ФИО не даёт транслита → fallback на clientNNNNN@...")
  void emptyNameFallsBackToPool() {
    // Пустые строки — cleanTranslit вернёт пустую строку
    PersonName emptyName = new PersonName("", "", "", Gender.MALE);
    Contact email = generator.email(emptyName, EmailDomainMode.TEST);
    assertThat(email.value()).matches("client\\d{5}@.+");
  }

  @Test
  @DisplayName("Разные ФИО дают разные email")
  void differentNamesGiveDifferentEmails() {
    Set<String> emails = new HashSet<>();
    PersonName[] names = {
      new PersonName("Иванов", "Иван", "Иванович", Gender.MALE),
      new PersonName("Петров", "Петр", "Петрович", Gender.MALE),
      new PersonName("Сидоров", "Сидор", "Сидорович", Gender.MALE)
    };

    for (PersonName name : names) {
      emails.add(generator.email(name, EmailDomainMode.TEST).value());
    }
    assertThat(emails).hasSize(3);
  }
}
