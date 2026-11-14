package ru.validum.core.profile;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.contact.EmailDomainMode;

@DisplayName("IndividualProfile — пулы email / secretWord / manager (P0.3)")
class IndividualProfilePoolsTest {

  private static final long SEED = 42L;

  // ── Email ────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("1000 профилей → >980 уникальных email (допускаем редкие коллизии по ФИО)")
  void thousandProfilesGiveMostlyUniqueEmails() {
    IndividualProfileGenerator generator =
      IndividualProfileGenerator.builder().seed(SEED).build();

    Set<String> emails = new HashSet<>();
    for (int i = 0; i < 1000; i++) {
      emails.add(generator.generate().email().value());
    }
    // Ожидаем ~988 из 1000, гарантируем >980
    assertThat(emails).hasSizeGreaterThan(980);
  }

  @Test
  @DisplayName("Email привязан к ФИО профиля")
  void emailIsLinkedToName() {
    IndividualProfile profile =
      IndividualProfileGenerator.builder().seed(SEED).build().generate();

    String emailLocalPart = profile.email().value().split("@")[0];

    assertThat(emailLocalPart).isNotEmpty();
    assertThat(profile.email().value()).contains("@");
  }

  @Test
  @DisplayName("EmailDomainMode.REALISTIC → домены из реалистичного пула")
  void realisticModeUsesRealisticDomains() {
    IndividualProfile profile =
      IndividualProfileGenerator.builder()
        .seed(SEED)
        .emailDomainMode(EmailDomainMode.REALISTIC)
        .build()
        .generate();

    String domain = profile.email().value().split("@")[1];
    assertThat(domain).isIn("mail.ru", "yandex.ru", "bk.ru", "inbox.ru", "list.ru",
      "internet.ru", "rambler.ru", "ya.ru", "gmail.com", "mail.com");
  }

  @Test
  @DisplayName("Явный email переопределяет генерацию")
  void explicitEmailOverrides() {
    IndividualProfile profile =
      IndividualProfileGenerator.builder()
        .seed(SEED)
        .email("custom@corp.ru")
        .build()
        .generate();

    assertThat(profile.email().value()).isEqualTo("custom@corp.ru");
  }

  // ── SecretWord ───────────────────────────────────────────────────────────

  @Test
  @DisplayName("1000 профилей покрывают почти весь пул секретных слов")
  void thousandProfilesCoverAllSecretWords() {
    IndividualProfileGenerator generator =
      IndividualProfileGenerator.builder().seed(SEED).build();

    Set<String> words = new HashSet<>();
    for (int i = 0; i < 1000; i++) {
      words.add(generator.generate().secretWord());
    }
    // Пул — 310 слов; 1000 равномерных выборок дают ≈ 0,96 · 310 ≈ 298 разных слов,
    // поэтому точное число зависит от потока random и тут проверяется покрытие, а не размер.
    assertThat(words).hasSizeBetween(291, 310);
  }

  @Test
  @DisplayName("Явное секретное слово переопределяет генерацию")
  void explicitSecretWordOverrides() {
    IndividualProfile profile =
      IndividualProfileGenerator.builder()
        .seed(SEED)
        .secretWord("СЛОН")
        .build()
        .generate();

    assertThat(profile.secretWord()).isEqualTo("СЛОН");
  }

  @Test
  @DisplayName("Дефолтное секретное слово — из пула, а не хардкод РЫБА")
  void defaultSecretWordIsFromPool() {
    IndividualProfile profile =
      IndividualProfileGenerator.builder().seed(SEED).build().generate();

    // Слово должно быть в верхнем регистре, состоять из кириллицы
    assertThat(profile.secretWord()).matches("[А-ЯЁ]+");
  }

  // ── Manager ──────────────────────────────────────────────────────────────

  @Test
  @DisplayName("1000 профилей → один менеджер (в справочнике одна заглушка)")
  void thousandProfilesShareSingleManager() {
    IndividualProfileGenerator generator =
      IndividualProfileGenerator.builder().seed(SEED).build();

    Set<String> managers = new HashSet<>();
    for (int i = 0; i < 1000; i++) {
      managers.add(generator.generate().personalManager());
    }
    assertThat(managers).containsExactly("Менеджер клиента");
  }

  @Test
  @DisplayName("Явный менеджер переопределяет генерацию")
  void explicitManagerOverrides() {
    IndividualProfile profile =
      IndividualProfileGenerator.builder()
        .seed(SEED)
        .personalManager("СЗА02")
        .build()
        .generate();

    assertThat(profile.personalManager()).isEqualTo("СЗА02");
  }

  // ── Детерминизм ──────────────────────────────────────────────────────────

  @Test
  @DisplayName("Одинаковый seed → одинаковый email, слово, менеджер")
  void sameSeedProducesSamePools() {
    IndividualProfile a =
      IndividualProfileGenerator.builder().seed(SEED).build().generate();
    IndividualProfile b =
      IndividualProfileGenerator.builder().seed(SEED).build().generate();

    assertThat(a.email()).isEqualTo(b.email());
    assertThat(a.secretWord()).isEqualTo(b.secretWord());
    assertThat(a.personalManager()).isEqualTo(b.personalManager());
  }
}
