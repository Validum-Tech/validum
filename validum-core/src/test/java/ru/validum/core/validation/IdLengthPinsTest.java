package ru.validum.core.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Пины граничных длин по XSD ЦБ (схемы УФЭБС, версия 2026.09.0): у ИНН/КПП/ОГРН в схемах нет масок,
 * только длины — поэтому проверяем именно границы.
 */
@DisplayName("Длины реквизитов по XSD ЦБ (граничные значения)")
class IdLengthPinsTest {

  private static final String INN_LEGAL = "7707083893";
  private static final String INN_INDIVIDUAL = "500100732259";
  private static final String OGRN = "1027700132195";

  @Test
  @DisplayName("ИНН: 10 знаков — ЮЛ, 12 — ФЛ")
  void innLengths() {
    assertThat(InnValidator.isValidLegalEntity(INN_LEGAL)).isTrue();
    assertThat(InnValidator.isValidIndividual(INN_LEGAL)).isFalse();
    assertThat(InnValidator.isValidIndividual(INN_INDIVIDUAL)).isTrue();
    assertThat(InnValidator.isValidLegalEntity(INN_INDIVIDUAL)).isFalse();
  }

  @Test
  @DisplayName("ИНН 13 знаков (длиннее предела XSD) — невалиден")
  void innLongerThanTwelveIsInvalid() {
    assertThat(InnValidator.isValidIndividual(INN_INDIVIDUAL + "9")).isFalse();
    assertThat(InnValidator.isValidLegalEntity(INN_LEGAL + "999")).isFalse();
  }

  @Test
  @DisplayName("ИНН 9 и 11 знаков — невалидны")
  void innShortOrElevenIsInvalid() {
    assertThat(InnValidator.isValidIndividual("50010073225")).isFalse();
    assertThat(InnValidator.isValidLegalEntity("770708389")).isFalse();
    assertThat(InnValidator.isValidIndividual("")).isFalse();
  }

  @Test
  @DisplayName("ОГРН: 13 знаков валиден, 12 и 14 — нет")
  void ogrnLengths() {
    assertThat(OgrnValidator.isValidLegalEntity(OGRN)).isTrue();
    assertThat(OgrnValidator.isValidLegalEntity(OGRN.substring(0, 12))).isFalse();
    assertThat(OgrnValidator.isValidLegalEntity(OGRN + "0")).isFalse();
    assertThat(OgrnValidator.isValidLegalEntity("")).isFalse();
  }

  @Test
  @DisplayName("ОГРН из 13 знаков не проходит как ОГРНИП (15)")
  void ogrnIsNotOgrnip() {
    assertThat(OgrnipValidator.isValid(OGRN)).isFalse();
  }
}
