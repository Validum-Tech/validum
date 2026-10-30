package ru.validum.core.passport;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.random.DataGenerator;

class PassportGeneratorTest {

  private final PassportGenerator generator = new PassportGenerator();

  @Test
  @DisplayName("Серия паспорта — 4 цифры, номер — 6 цифр")
  void seriesAndNumberHaveCorrectLength() {
    Passport passport = generator.generate(LocalDate.of(1990, 5, 15));
    assertThat(passport.series()).hasSize(4).containsOnlyDigits();
    assertThat(passport.number()).hasSize(6).containsOnlyDigits();
  }

  @Test
  @DisplayName("Код подразделения соответствует формату XXX-XXX")
  void departmentCodeMatchesFormat() {
    Passport passport = generator.generate(LocalDate.of(1990, 5, 15));
    assertThat(passport.departmentCode()).matches("\\d{3}-\\d{3}");
  }

  @Test
  @DisplayName("Для человека 30 лет паспорт выдан в 20 лет")
  void passportFor30YearOldIssuedAt20() {
    LocalDate today = LocalDate.of(2026, 10, 14);
    LocalDate birth = LocalDate.of(1996, 1, 1);
    Passport passport = generator.generate(birth, today);
    assertThat(passport.issueDate()).isEqualTo(birth.plusYears(20));
  }

  @Test
  @DisplayName("Для человека 50 лет паспорт выдан в 45 лет")
  void passportFor50YearOldIssuedAt45() {
    LocalDate today = LocalDate.of(2026, 10, 14);
    LocalDate birth = LocalDate.of(1976, 1, 1);
    Passport passport = generator.generate(birth, today);
    assertThat(passport.issueDate()).isEqualTo(birth.plusYears(45));
  }

  @Test
  @DisplayName("Для человека 18 лет паспорт выдан в 14 лет")
  void passportFor18YearOldIssuedAt14() {
    LocalDate today = LocalDate.of(2026, 10, 14);
    LocalDate birth = LocalDate.of(2008, 1, 1);
    Passport passport = generator.generate(birth, today);
    assertThat(passport.issueDate()).isEqualTo(birth.plusYears(14));
  }

  @Test
  @DisplayName("Действительный паспорт: expirationDate в будущем, status = Действительный")
  void validPassportHasCorrectStatus() {
    LocalDate today = LocalDate.of(2026, 10, 14);
    LocalDate birth = LocalDate.of(1990, 5, 15);
    Passport passport = generator.generate(birth, today);
    assertThat(passport.expirationDate()).isAfter(today);
    assertThat(passport.status()).isEqualTo("Действительный");
  }

  @Test
  @DisplayName("Просроченный паспорт: passportWasExchanged=false, возраст 25 → статус Просрочен")
  void expiredPassportHasCorrectStatus() {
    LocalDate today = LocalDate.of(2026, 10, 14);
    LocalDate birth = LocalDate.of(2001, 1, 1); // 25 лет
    Passport passport = generator.generate(birth, today, false);
    assertThat(passport.issueDate()).isEqualTo(birth.plusYears(14));
    assertThat(passport.expirationDate()).isEqualTo(birth.plusYears(20));
    assertThat(passport.status()).isEqualTo("Просрочен");
  }

  @Test
  @DisplayName("Бессрочный паспорт для человека 45+ выдан в 45, действует +100 лет")
  void passportFor45PlusIsIndefinite() {
    LocalDate today = LocalDate.of(2026, 10, 14);
    LocalDate birth = LocalDate.of(1976, 1, 1);
    Passport passport = generator.generate(birth, today, true);
    assertThat(passport.issueDate()).isEqualTo(birth.plusYears(45));
    assertThat(passport.expirationDate()).isEqualTo(birth.plusYears(145));
    assertThat(passport.status()).isEqualTo("Действительный");
  }

  @Test
  @DisplayName("Одинаковый seed даёт одинаковый паспорт")
  void sameSeedProducesSamePassport() {
    LocalDate birth = LocalDate.of(1990, 5, 15);
    PassportGenerator first = new PassportGenerator(DataGenerator.seededRandom(42L));
    PassportGenerator second = new PassportGenerator(DataGenerator.seededRandom(42L));
    Passport p1 = first.generate(birth, LocalDate.of(2026, 10, 14));
    Passport p2 = second.generate(birth, LocalDate.of(2026, 10, 14));
    assertThat(p1).isEqualTo(p2);
  }

  @Test
  @DisplayName("Формат даты — dd.MM.yyyy")
  void formatReturnsRussianDate() {
    assertThat(PassportGenerator.format(LocalDate.of(2026, 10, 14))).isEqualTo("14.10.2026");
  }
}
