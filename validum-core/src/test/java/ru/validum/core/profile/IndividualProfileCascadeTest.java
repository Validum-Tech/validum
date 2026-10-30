package ru.validum.core.profile;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Period;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import ru.validum.core.common.Country;
import ru.validum.core.name.FioScript;
import ru.validum.core.passport.PassportType;

@DisplayName("Каскадные дефолты IndividualProfileGenerator")
class IndividualProfileCascadeTest {

  private static final long SEED = 42L;

  private static int ageOf(IndividualProfile p) {
    return Period.between(p.birthDate(), LocalDate.now()).getYears();
  }

  @Nested
  @DisplayName("Возраст + паспорт")
  class AgeAndPassport {

    @Test
    @DisplayName("age=23 без passportWasExchanged → валидный паспорт, выдан в 20")
    void age23_DefaultValidPassport() {
      var p = IndividualProfileGenerator.builder().age(23).seed(SEED).build().generate();
      assertThat(ageOf(p)).isEqualTo(23);
      assertThat(p.passport().status()).isEqualTo("Действительный");
      assertThat(p.passport().issueDate()).isEqualTo(p.birthDate().plusYears(20));
    }

    @Test
    @DisplayName("age=23 + passportWasExchanged=false → просроченный паспорт, выдан в 14")
    void age23_NotExchanged_Expired() {
      var p =
        IndividualProfileGenerator.builder()
          .age(23)
          .passportWasExchanged(false)
          .seed(SEED)
          .build()
          .generate();
      assertThat(p.passport().status()).isEqualTo("Просрочен");
      assertThat(p.passport().issueDate()).isEqualTo(p.birthDate().plusYears(14));
    }

    @Test
    @DisplayName("age=16 → несовершеннолетний, паспорт выдан в 14 и действителен")
    void age16_MinorWithValidPassport() {
      var p = IndividualProfileGenerator.builder().age(16).seed(SEED).build().generate();
      assertThat(ageOf(p)).isEqualTo(16);
      assertThat(p.passport().status()).isEqualTo("Действительный");
      assertThat(p.passport().issueDate()).isEqualTo(p.birthDate().plusYears(14));
    }

    @Test
    @DisplayName("age=16 + passportWasExchanged=true → всё равно выдан в 14 (каскад защищает)")
    void age16_ExchangedTrue_StillIssuedAt14() {
      var p =
        IndividualProfileGenerator.builder()
          .age(16)
          .passportWasExchanged(true)
          .seed(SEED)
          .build()
          .generate();
      assertThat(p.passport().issueDate()).isEqualTo(p.birthDate().plusYears(14));
    }

    @Test
    @DisplayName("age=50 → паспорт бессрочный, выдан в 45")
    void age50_Indefinite() {
      var p = IndividualProfileGenerator.builder().age(50).seed(SEED).build().generate();
      assertThat(p.passport().issueDate()).isEqualTo(p.birthDate().plusYears(45));
      assertThat(p.passport().status()).isEqualTo("Действительный");
    }
  }

  @Nested
  @DisplayName("Нерезидент + паспорт")
  class ResidencyAndPassport {

    @Test
    @DisplayName("passportType=FOREIGN без passportCountry → страна из residency")
    void foreignPassport_CountryFromResidency() {
      var p =
        IndividualProfileGenerator.builder()
          .residency(Residency.nonResident(Country.TJ))
          .passportType(PassportType.FOREIGN)
          .seed(SEED)
          .build()
          .generate();
      assertThat(p.passport().type()).isEqualTo(PassportType.FOREIGN);
      assertThat(p.passport().country()).isEqualTo(Country.TJ);
      assertThat(p.residency().isResident()).isFalse();
    }

    @Test
    @DisplayName("passportCountry задан явно — переопределяет residency")
    void explicitPassportCountryWins() {
      var p =
        IndividualProfileGenerator.builder()
          .residency(Residency.nonResident(Country.TJ))
          .passportType(PassportType.FOREIGN)
          .passportCountry(Country.KZ)
          .seed(SEED)
          .build()
          .generate();
      assertThat(p.passport().country()).isEqualTo(Country.KZ);
    }

    @Test
    @DisplayName("passportType не задан → RF, страна RU")
    void defaultRFPassport() {
      var p = IndividualProfileGenerator.builder().seed(SEED).build().generate();
      assertThat(p.passport().type()).isEqualTo(PassportType.RF);
      assertThat(p.passport().country()).isEqualTo(Country.RU);
    }
  }

  @Nested
  @DisplayName("ФИО")
  class Fio {

    @Test
    @DisplayName("fioScript не задан → кириллица")
    void defaultCyrillic() {
      var p = IndividualProfileGenerator.builder().seed(SEED).build().generate();
      assertThat(p.fullName()).containsPattern("[А-Яа-яЁё]");
    }

    @Test
    @DisplayName("fioScript=LATIN → только ASCII")
    void latinFio() {
      var p =
        IndividualProfileGenerator.builder()
          .fioScript(FioScript.LATIN)
          .seed(SEED)
          .build()
          .generate();
      assertThat(p.fullName()).matches("[A-Za-z ]+");
    }
  }

  @Nested
  @DisplayName("Порча контрольных сумм")
  class Corruption {

    @Test
    @DisplayName("corrupt=INN → ИНН битый, СНИЛС валидный")
    void corruptInnOnly() {
      var p =
        IndividualProfileGenerator.builder()
          .corrupt(CorruptField.INN)
          .seed(SEED)
          .build()
          .generate();
      assertThat(p.inn()).hasSize(12);
      assertThat(p.snils()).hasSize(11);
      // СНИЛС не порчен — длина и цифры ок
      assertThat(p.snils()).containsOnlyDigits();
    }

    @Test
    @DisplayName("corrupt=INN,SNILS → оба поля изменены")
    void corruptBoth() {
      var p =
        IndividualProfileGenerator.builder()
          .corrupt(CorruptField.INN, CorruptField.SNILS)
          .seed(SEED)
          .build()
          .generate();
      assertThat(p.inn()).hasSize(12);
      assertThat(p.snils()).hasSize(11);
    }

    @Test
    @DisplayName("corrupt не задан → всё валидно")
    void noCorruptionByDefault() {
      var p = IndividualProfileGenerator.builder().seed(SEED).build().generate();
      assertThat(p.inn()).hasSize(12).containsOnlyDigits();
      assertThat(p.snils()).hasSize(11).containsOnlyDigits();
    }
  }

  @Nested
  @DisplayName("Композиция параметров")
  class Composition {

    @Test
    @DisplayName("age=16 + LATIN + corrupt(INN) + нерезидент → всё применяется согласованно")
    void fullCombination() {
      var p =
        IndividualProfileGenerator.builder()
          .age(16)
          .fioScript(FioScript.LATIN)
          .corrupt(CorruptField.INN)
          .residency(Residency.nonResident(Country.KZ))
          .passportType(PassportType.FOREIGN)
          .seed(SEED)
          .build()
          .generate();

      assertThat(ageOf(p)).isEqualTo(16);
      assertThat(p.fullName()).matches("[A-Za-z ]+");
      assertThat(p.inn()).hasSize(12);
      assertThat(p.passport().type()).isEqualTo(PassportType.FOREIGN);
      assertThat(p.passport().country()).isEqualTo(Country.KZ);
      assertThat(p.passport().status()).isEqualTo("Действительный");
      assertThat(p.registrationAddress()).isNotNull();
    }
  }
}
