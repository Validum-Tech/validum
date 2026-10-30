package ru.validum.core.name;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.random.DataGenerator;

@DisplayName("NameGenerator — латиница")
class NameGeneratorLatinTest {

  private final NameGenerator generator = new NameGenerator();

  @Test
  @DisplayName("Мужское ФИО на латинице: только ASCII, отчество на -vich")
  void maleLatinIsAscii() {
    for (int i = 0; i < 50; i++) {
      PersonName name = generator.generate(Gender.MALE, FioScript.LATIN);
      assertThat(name.fullName()).matches("[A-Za-z ]+");
      assertThat(name.patronymic()).endsWith("vich");
      assertThat(name.lastName()).doesNotEndWith("a");
    }
  }

  @Test
  @DisplayName("Женское ФИО на латинице: фамилия на -a, отчество на -vna")
  void femaleLatinIsAscii() {
    for (int i = 0; i < 50; i++) {
      PersonName name = generator.generate(Gender.FEMALE, FioScript.LATIN);
      assertThat(name.fullName()).matches("[A-Za-z ]+");
      assertThat(name.patronymic()).endsWith("vna");
      assertThat(name.lastName()).endsWith("a");
    }
  }

  @Test
  @DisplayName("Одинаковый seed + LATIN → одинаковое ФИО")
  void deterministicLatin() {
    NameGenerator a = new NameGenerator(DataGenerator.seededRandom(42L));
    NameGenerator b = new NameGenerator(DataGenerator.seededRandom(42L));
    assertThat(a.generate(Gender.MALE, FioScript.LATIN).fullName())
      .isEqualTo(b.generate(Gender.MALE, FioScript.LATIN).fullName());
  }
}
