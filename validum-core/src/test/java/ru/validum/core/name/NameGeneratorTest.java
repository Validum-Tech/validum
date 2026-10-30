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
  @DisplayName("Женское ФИО: отчество на -на, фамилия на -а")
  void femaleNameIsConsistent() {
    for (int i = 0; i < 100; i++) {
      PersonName name = generator.generate(Gender.FEMALE);
      assertThat(name.gender()).isEqualTo(Gender.FEMALE);
      assertThat(name.patronymic()).endsWith("на");
      assertThat(name.lastName()).endsWith("а");
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
}
