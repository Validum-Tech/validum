package ru.validum.core.organization;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.common.OrganizationType;
import ru.validum.core.random.DataGenerator;

@DisplayName("OrganizationNameGenerator")
class OrganizationNameGeneratorTest {

  private final OrganizationNameGenerator generator =
    new OrganizationNameGenerator(DataGenerator.seededRandom(42L));

  @Test
  @DisplayName("ООО по умолчанию — префикс ООО")
  void defaultIsLLC() {
    String name = generator.generate();
    assertThat(name).startsWith("ООО «").endsWith("»");
  }

  @Test
  @DisplayName("АО → префикс АО")
  void jscHasPrefix() {
    assertThat(generator.generate(OrganizationType.JSC)).startsWith("АО «");
  }

  @Test
  @DisplayName("ПАО → префикс ПАО")
  void pjscHasPrefix() {
    assertThat(generator.generate(OrganizationType.PJSC)).startsWith("ПАО «");
  }

  @Test
  @DisplayName("Детерминизм по seed")
  void determinism() {
    OrganizationNameGenerator a = new OrganizationNameGenerator(DataGenerator.seededRandom(42L));
    OrganizationNameGenerator b = new OrganizationNameGenerator(DataGenerator.seededRandom(42L));
    assertThat(a.generate()).isEqualTo(b.generate());
  }
}
