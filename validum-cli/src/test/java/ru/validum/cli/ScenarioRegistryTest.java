package ru.validum.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.profile.ScenarioCatalog;

@DisplayName("ScenarioRegistry — тонкий фасад над каталогом (P3.1b)")
class ScenarioRegistryTest {

  @Test
  @DisplayName("Девять сценариев, порядок как в API")
  void nineScenariosInOrder() {
    List<String> slugs = ScenarioRegistry.allSlugs();
    assertThat(slugs).containsExactly(
      "adult-resident-with-account",
      "adult-resident-without-account",
      "pensioner-male",
      "pensioner-female",
      "salary-client",
      "non-resident-with-residence-permit",
      "foreigner-with-visa",
      "serviceman",
      "minor"
    );
  }

  @Test
  @DisplayName("bySlug находит каждый сценарий")
  void bySlugFindsEach() {
    for (String slug : ScenarioRegistry.allSlugs()) {
      assertThat(ScenarioRegistry.bySlug(slug)).as("slug %s", slug).isPresent();
    }
  }

  @Test
  @DisplayName("bySlug для неизвестного slug → пусто")
  void bySlugUnknown() {
    assertThat(ScenarioRegistry.bySlug("unknown-thing")).isEmpty();
    assertThat(ScenarioRegistry.bySlug(null)).isEmpty();
  }

  @Test
  @DisplayName("pensioner-male даёт возраст 65, female — 60")
  void pensionersHaveCorrectAges() {
    var male = ScenarioCatalog.PENSIONER_MALE.generate(42L);
    var female = ScenarioCatalog.PENSIONER_FEMALE.generate(42L);
    assertThat(male.age()).isEqualTo(65);
    assertThat(female.age()).isEqualTo(60);
  }

  @Test
  @DisplayName("salary-client → категория ЗАРПЛАТНИК")
  void salaryClientCategory() {
    var p = ScenarioCatalog.SALARY_CLIENT.generate(42L);
    assertThat(p.categoryClient()).isEqualTo("ЗАРПЛАТНИК");
  }

  @Test
  @DisplayName("Детерминизм: одинаковый seed → одинаковый профиль")
  void deterministic() {
    var a = ScenarioCatalog.ADULT_RESIDENT_WITH_ACCOUNT.generate(42L);
    var b = ScenarioCatalog.ADULT_RESIDENT_WITH_ACCOUNT.generate(42L);
    assertThat(a.inn()).isEqualTo(b.inn());
    assertThat(a.fullName()).isEqualTo(b.fullName());
  }
}
