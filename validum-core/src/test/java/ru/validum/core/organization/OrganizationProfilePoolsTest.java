package ru.validum.core.organization;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.contact.EmailDomainMode;

@DisplayName("OrganizationProfile — пул email (P0.3)")
class OrganizationProfilePoolsTest {

  private static final long SEED = 42L;

  @Test
  @DisplayName("Email ЮЛ генерируется в формате clientNNNNN@domain")
  void organizationEmailUsesPoolFormat() {
    OrganizationProfile org =
      OrganizationProfileGenerator.builder().seed(SEED).build().generate();

    // Формат: client + 5 цифр + @ + домен
    assertThat(org.email().value()).matches("client\\d{5}@.+");
  }

  @Test
  @DisplayName("По умолчанию (TEST) домен из безопасного пула")
  void defaultModeUsesTestDomains() {
    Set<String> testDomains = Set.of(
      "test.ru", "example.com", "example.org", "example.net",
      "qa.local", "test.local", "demo.local", "mock.io", "test.tech", "fake.dev");

    for (int i = 0; i < 20; i++) {
      OrganizationProfile org =
        OrganizationProfileGenerator.builder().seed(SEED + i).build().generate();
      String domain = org.email().value().split("@")[1];
      assertThat(testDomains).as("Домен %s", domain).contains(domain);
    }
  }

  @Test
  @DisplayName("EmailDomainMode.REALISTIC → домены из реалистичного пула")
  void realisticModeUsesRealisticDomains() {
    Set<String> realisticDomains = Set.of(
      "mail.ru", "yandex.ru", "bk.ru", "inbox.ru", "list.ru",
      "internet.ru", "rambler.ru", "ya.ru", "gmail.com", "mail.com");

    OrganizationProfile org =
      OrganizationProfileGenerator.builder()
        .seed(SEED)
        .emailDomainMode(EmailDomainMode.REALISTIC)
        .build()
        .generate();

    String domain = org.email().value().split("@")[1];
    assertThat(realisticDomains).as("Домен %s", domain).contains(domain);
  }

  @Test
  @DisplayName("Явный email переопределяет генерацию")
  void explicitEmailOverrides() {
    OrganizationProfile org =
      OrganizationProfileGenerator.builder()
        .seed(SEED)
        .email("info@company.ru")
        .build()
        .generate();

    assertThat(org.email().value()).isEqualTo("info@company.ru");
  }

  @Test
  @DisplayName("Одинаковый seed → одинаковый email")
  void sameSeedProducesSameEmail() {
    OrganizationProfile a =
      OrganizationProfileGenerator.builder().seed(SEED).build().generate();
    OrganizationProfile b =
      OrganizationProfileGenerator.builder().seed(SEED).build().generate();

    assertThat(a.email()).isEqualTo(b.email());
  }
}
