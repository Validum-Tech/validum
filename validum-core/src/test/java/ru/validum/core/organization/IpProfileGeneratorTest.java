package ru.validum.core.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.profile.IndividualProfile;
import ru.validum.core.profile.IndividualProfileGenerator;
import ru.validum.core.random.DataGenerator;

@DisplayName("IpProfileGenerator")
class IpProfileGeneratorTest {

  private static final long SEED = 42L;

  private static IndividualProfile baseProfile() {
    return IndividualProfileGenerator.builder().seed(SEED).age(35).build().generate();
  }

  // ── Валидация ОГРНИП по алгоритму ФНС ────────────────────────────────────

  private static boolean isValidOgrnip(String ogrnip) {
    if (ogrnip == null || ogrnip.length() != 15) {
      return false;
    }
    long numeric = Long.parseLong(ogrnip);
    int control = (int) ((numeric / 10) % 13 % 10);
    return control == (ogrnip.charAt(14) - '0');
  }

  @Test
  @DisplayName("ОГРНИП — 15 цифр, контрольная цифра валидна")
  void ogrnipIsValid() {
    for (int i = 0; i < 50; i++) {
      IndividualProfile base = IndividualProfileGenerator.defaults().generate();
      IpProfile ip = IpProfileGenerator.from(base);
      assertThat(ip.ogrnip()).hasSize(15).containsOnlyDigits();
      assertThat(isValidOgrnip(ip.ogrnip())).as("ОГРНИП %s", ip.ogrnip()).isTrue();
    }
  }

  @Test
  @DisplayName("Отчество заменено на «ИП Автотеста»")
  void patronymicIsReplaced() {
    IpProfile ip = IpProfileGenerator.from(baseProfile());
    assertThat(ip.name().patronymic()).isEqualTo(IpProfileGenerator.IP_PATRONYMIC);
  }

  @Test
  @DisplayName("Фамилия и имя сохранены из base")
  void lastNameAndFirstNamePreserved() {
    IndividualProfile base = baseProfile();
    IpProfile ip = IpProfileGenerator.from(base);
    assertThat(ip.name().lastName()).isEqualTo(base.name().lastName());
    assertThat(ip.name().firstName()).isEqualTo(base.name().firstName());
    assertThat(ip.name().gender()).isEqualTo(base.name().gender());
  }

  @Test
  @DisplayName("ИНН, СНИЛС, паспорт, банк, счёт — из base без изменений")
  void baseFieldsPreserved() {
    IndividualProfile base = baseProfile();
    IpProfile ip = IpProfileGenerator.from(base);

    assertThat(ip.inn()).isEqualTo(base.inn());
    assertThat(ip.snils()).isEqualTo(base.snils());
    assertThat(ip.passport()).isEqualTo(base.passport());
    assertThat(ip.bank()).isEqualTo(base.bank());
    assertThat(ip.account()).isEqualTo(base.account());
    assertThat(ip.registrationAddress()).isEqualTo(base.registrationAddress());
  }

  @Test
  @DisplayName("Флаги terrorist / blocked пробрасываются")
  void flagsPropagate() {
    IndividualProfile base =
      IndividualProfileGenerator.builder()
        .seed(SEED)
        .terrorist(true)
        .blocked(true)
        .build()
        .generate();
    IpProfile ip = IpProfileGenerator.from(base);
    assertThat(ip.terrorist()).isTrue();
    assertThat(ip.blocked()).isTrue();
  }

  @Test
  @DisplayName("Возраст сохраняется из base")
  void agePreserved() {
    IndividualProfile base = baseProfile();
    IpProfile ip = IpProfileGenerator.from(base);
    assertThat(ip.age()).isEqualTo(base.age());
  }

  // ── Детерминизм с seed ──────────────────────────────────────────────────

  @Test
  @DisplayName("from(base, seed) — одинаковый seed → одинаковый ОГРНИП")
  void fromWithSeedDeterministic() {
    IndividualProfile base = baseProfile();
    IpProfile a = IpProfileGenerator.from(base, SEED);
    IpProfile b = IpProfileGenerator.from(base, SEED);
    assertThat(a.ogrnip()).isEqualTo(b.ogrnip());
  }

  @Test
  @DisplayName("generate(gen, seed) — одинаковый seed → одинаковый ОГРНИП")
  void generateWithSeedDeterministic() {
    IpProfile a =
      IpProfileGenerator.generate(
        IndividualProfileGenerator.builder().seed(SEED).age(35).build(), SEED);
    IpProfile b =
      IpProfileGenerator.generate(
        IndividualProfileGenerator.builder().seed(SEED).age(35).build(), SEED);
    assertThat(a.ogrnip()).isEqualTo(b.ogrnip());
    assertThat(a.inn()).isEqualTo(b.inn());
  }

  @Test
  @DisplayName("generate(gen, seed) — работа с готовым генератором")
  void generateFromGeneratorWithSeed() {
    IndividualProfileGenerator generator =
      IndividualProfileGenerator.builder().seed(SEED).age(35).build();
    IpProfile ip = IpProfileGenerator.generate(generator, SEED);
    assertThat(ip.name().patronymic()).isEqualTo(IpProfileGenerator.IP_PATRONYMIC);
    assertThat(ip.ogrnip()).hasSize(15);
  }

  @Test
  @DisplayName("Одинаковый seed → одинаковый ОГРНИП (детерминизм при явном генераторе)")
  void determinismWithExplicitGenerator() {
    IndividualProfile base = baseProfile();
    IpProfile a =
      IpProfileGenerator.from(
        base, new ru.validum.core.ogrn.OgrnGenerator(DataGenerator.seededRandom(SEED)));
    IpProfile b =
      IpProfileGenerator.from(
        base, new ru.validum.core.ogrn.OgrnGenerator(DataGenerator.seededRandom(SEED)));
    assertThat(a.ogrnip()).isEqualTo(b.ogrnip());
  }

  // ── Валидация аргументов ────────────────────────────────────────────────

  @Test
  @DisplayName("Null base бросает исключение")
  void nullBaseThrows() {
    assertThatThrownBy(() -> IpProfileGenerator.from(null))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("Null generator бросает исключение")
  void nullGeneratorThrows() {
    assertThatThrownBy(() -> IpProfileGenerator.generate(null))
      .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> IpProfileGenerator.generate(null, SEED))
      .isInstanceOf(IllegalArgumentException.class);
  }
}
