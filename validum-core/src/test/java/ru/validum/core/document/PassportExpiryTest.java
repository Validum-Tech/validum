package ru.validum.core.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Срок паспорта РФ — возрастной: 20 и 45 лет, после 45 бессрочно.
 *
 * <p>Раньше {@code expirationDate} был {@code null} и в {@code describe} печаталось «бессрочно»
 * даже у 25-летнего. Это вводило в заблуждение: паспорт, выданный в 20, действует до 45.
 */
@DisplayName("Паспорт РФ: срок замены 20/45")
class PassportExpiryTest {

  private static final LocalDate AS_OF = LocalDate.of(2026, 11, 13);

  private final DocumentGenerator generator = new DocumentGenerator(new Random(42));

  @Test
  @DisplayName("срок — ближайший возрастной рубеж после выдачи")
  void expiryIsTheNextAgeMilestone() {
    LocalDate birth = LocalDate.of(2001, 10, 31);

    IdentityDocument document =
      generator.generate(DocumentType.PASSPORT_RF, birth, AS_OF, "52");

    int issueAge = (int) java.time.temporal.ChronoUnit.YEARS
      .between(birth, document.issueDate());
    LocalDate expected = issueAge < 20 ? birth.plusYears(20) : birth.plusYears(45);
    assertThat(document.expirationDate())
      .as("выдан в %s лет — срок %s", issueAge, expected)
      .isEqualTo(expected);
  }

  @Test
  @DisplayName("выдан до 20 — срок в 20")
  void issuedBeforeTwentiethExpiresAtTwenty() {
    LocalDate birth = LocalDate.of(2010, 3, 24);

    IdentityDocument document =
      generator.generate(DocumentType.PASSPORT_RF, birth, LocalDate.of(2025, 6, 16), "50");

    assertThat(document.expirationDate()).isEqualTo(birth.plusYears(20));
  }

  @Test
  @DisplayName("выдан после 45 — бессрочный")
  void issuedAfterFortyFiveHasNoExpiry() {
    LocalDate birth = LocalDate.of(1960, 5, 5);
    LocalDate asOf = LocalDate.of(2026, 11, 13);

    IdentityDocument document =
      generator.generate(DocumentType.PASSPORT_RF, birth, asOf, "77");

    assertThat(document.issueDate()).isAfterOrEqualTo(birth.plusYears(45));
    assertThat(document.expirationDate()).isNull();
  }

  @Test
  @DisplayName("у военного билета срок остаётся пустым — это не ошибка")
  void militaryIdStaysWithoutExpiry() {
    IdentityDocument document =
      generator.generate(DocumentType.MILITARY_ID, LocalDate.of(2001, 10, 31), AS_OF);

    assertThat(document.expirationDate()).isNull();
  }
}
