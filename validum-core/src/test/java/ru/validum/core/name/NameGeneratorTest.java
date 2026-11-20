package ru.validum.core.name;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.random.DataGenerator;

@DisplayName("NameGenerator — кириллица")
class NameGeneratorTest {

  private final NameGenerator generator = new NameGenerator();

  @Test
  @DisplayName("Мужское ФИО: отчество на -ич, фамилия — не на -а")
  void maleNameIsConsistent() {
    for (int i = 0; i < 100; i++) {
      PersonName name = generator.generate(Gender.MALE);
      assertThat(name.gender()).isEqualTo(Gender.MALE);
      assertThat(name.patronymic()).endsWith("ич");
      assertThat(name.lastName()).doesNotEndWith("а");
    }
  }

  @Test
  @DisplayName("Женское ФИО: отчество на -на, фамилия женская по типу")
  void femaleNameIsConsistent() {
    for (int i = 0; i < 100; i++) {
      PersonName name = generator.generate(Gender.FEMALE);
      assertThat(name.gender()).isEqualTo(Gender.FEMALE);
      assertThat(name.patronymic()).endsWith("на");
      // Склоняемые фамилии получают «-а/-я», неизменяемые («-ко», «-ук», «-ич», «-ян») — как есть.
      assertThat(name.lastName())
        .as("женская фамилия: %s", name.lastName())
        .matches(".*(а|я|ко|ук|юк|ич|ян|дзе|швили)$");
    }
  }

  @Test
  @DisplayName("fullName собирается из трёх частей")
  void fullNameIsCombined() {
    PersonName name = generator.generate(Gender.MALE);
    assertThat(name.fullName())
      .isEqualTo(name.lastName() + " " + name.firstName() + " " + name.patronymic());
  }

  @Test
  @DisplayName("Одинаковый seed даёт одинаковое ФИО")
  void sameSeedProducesSameName() {
    NameGenerator first = new NameGenerator(DataGenerator.seededRandom(42L));
    NameGenerator second = new NameGenerator(DataGenerator.seededRandom(42L));
    assertThat(first.generate().fullName()).isEqualTo(second.generate().fullName());
  }

  @Test
  @DisplayName("Кириллица по умолчанию — не латиница")
  void cyrillicByDefault() {
    PersonName name = generator.generate(Gender.MALE);
    assertThat(name.fullName()).containsPattern("[А-Яа-яЁё]");
    assertThat(name.fullName()).doesNotMatch("[A-Za-z]");
  }

  @Test
  @DisplayName("Отчество не привязано к собственному имени (нет «всех Иванов Ивановичей»)")
  void patronymicIsIndependentFromFirstName() {
    NameGenerator seeded = new NameGenerator(DataGenerator.seededRandom(2026L));
    int tautonyms = 0;
    int total = 500;
    for (int i = 0; i < total; i++) {
      PersonName name = seeded.generate(Gender.MALE);
      if (name.patronymic().startsWith(stem(name.firstName()))) {
        tautonyms++;
      }
    }
    assertThat(tautonyms)
      .as("доля «Имя Имя-евич» должна быть порядка 1/N, а не 100%")
      .isLessThan(total / 5);
  }

  @Test
  @DisplayName("Женская фамилия склоняется по типу, без «-ийа»")
  void femaleSurnameFollowsType() {
    assertThat(NameGenerator.femaleSurname("Смирнов")).isEqualTo("Смирнова");
    assertThat(NameGenerator.femaleSurname("Соколовский")).isEqualTo("Соколовская");
    assertThat(NameGenerator.femaleSurname("Троицкий")).isEqualTo("Троицкая");
    assertThat(NameGenerator.femaleSurname("Шевченко")).isEqualTo("Шевченко");
    assertThat(NameGenerator.femaleSurname("Ковальчук")).isEqualTo("Ковальчук");
    assertThat(NameGenerator.femaleSurname("Рабинович")).isEqualTo("Рабинович");
    assertThat(NameGenerator.femaleSurnameLatin("Smirnov")).isEqualTo("Smirnova");
    assertThat(NameGenerator.femaleSurnameLatin("Sokolovsky")).isEqualTo("Sokolovskaya");
    assertThat(NameGenerator.femaleSurnameLatin("Shevchenko")).isEqualTo("Shevchenko");
  }

  @Test
  @DisplayName("Женское ФИО из расширенного пула остаётся согласованным")
  void femaleNameWithAnySurnameTypeIsConsistent() {
    for (int i = 0; i < 300; i++) {
      PersonName name = generator.generate(Gender.FEMALE);
      assertThat(name.lastName()).doesNotEndWith("ий");
      assertThat(name.lastName()).matches(".*(а|я|ко|енко|ук|юк|ич|ян|дзе|швили)");
    }
  }

  private static String stem(String firstName) {
    return firstName.endsWith("й") || firstName.endsWith("ь")
      ? firstName.substring(0, firstName.length() - 1)
      : firstName;
  }
}
