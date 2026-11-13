package ru.validum.core.profile;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import ru.validum.core.TestProfiles;
import org.junit.jupiter.api.Test;

@DisplayName("Намеренная порча адреса (негативные сценарии)")
class AddressCorruptionTest {

  private static final long SEED = 4242L;

  @Test
  @DisplayName("corrupt=ADDRESS → индекс пустой, строка адреса сломана")
  void addressIsBroken() {
    var p = TestProfiles.builder(SEED).corrupt(CorruptField.ADDRESS).build().generate();

    assertThat(p.registrationAddress().postalCode()).isEmpty();
    assertThat(p.registrationAddress().formatted()).isEqualTo("ул. , д. , кв. ");
    assertThat(p.postAddress().postalCode()).isEmpty();
    assertThat(p.currentAddress().postalCode()).isEmpty();
  }

  @Test
  @DisplayName("corrupt=ADDRESS → остальные поля профиля валидны")
  void otherFieldsStayValid() {
    var p = TestProfiles.builder(SEED).corrupt(CorruptField.ADDRESS).build().generate();

    assertThat(p.inn()).hasSize(12).containsOnlyDigits();
    assertThat(p.snils()).hasSize(11).containsOnlyDigits();
    assertThat(p.passport().series()).matches("\\d{4}");
  }

  @Test
  @DisplayName("без corrupt адрес нормальный")
  void noCorruptionByDefault() {
    var p = TestProfiles.individual(SEED);

    assertThat(p.registrationAddress().postalCode()).hasSize(6).containsOnlyDigits();
    assertThat(p.registrationAddress().formatted())
      .isNotBlank()
      .isNotEqualTo("ул. , д. , кв. ");
  }
}
