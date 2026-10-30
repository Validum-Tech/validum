package ru.validum.core.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.common.Region;
import ru.validum.core.ogrn.OgrnGenerator;
import ru.validum.core.organization.KppGenerator;
import ru.validum.core.organization.OkpoGenerator;
import ru.validum.core.random.DataGenerator;

/**
 * Round-trip: генераторы → валидаторы.
 *
 * <p>Гарантирует, что всё, что сгенерировал валидный генератор, проходит соответствующий
 * валидатор. Это фундаментальная инвариант для QA: сгенерированный профиль всегда проходит
 * validation API без доработок.
 */
@DisplayName("Round-trip: генераторы → валидаторы (P2.3)")
class ValidatorRoundTripTest {

  private static final long SEED = 42L;

  @Test
  @DisplayName("ОКПО ЮЛ: генератор → валидатор")
  void okpoLegalEntityRoundTrip() {
    OkpoGenerator gen = new OkpoGenerator(DataGenerator.seededRandom(SEED));
    for (int i = 0; i < 200; i++) {
      String okpo = gen.generateLegalEntity();
      assertThat(OkpoValidator.isValid(okpo)).as("ОКПО %s", okpo).isTrue();
    }
  }

  @Test
  @DisplayName("ОКПО ИП: генератор → валидатор")
  void okpoIpRoundTrip() {
    OkpoGenerator gen = new OkpoGenerator(DataGenerator.seededRandom(SEED));
    for (int i = 0; i < 200; i++) {
      String okpo = gen.generateIndividualEntrepreneur();
      assertThat(OkpoValidator.isValid(okpo)).as("ОКПО %s", okpo).isTrue();
    }
  }

  @Test
  @DisplayName("ОГРНИП: генератор → валидатор")
  void ogrnipRoundTrip() {
    OgrnGenerator gen = new OgrnGenerator(DataGenerator.seededRandom(SEED));
    for (int i = 0; i < 200; i++) {
      String ogrnip = gen.generateIndividualEntrepreneur();
      assertThat(OgrnipValidator.isValid(ogrnip)).as("ОГРНИП %s", ogrnip).isTrue();
    }
  }

  @Test
  @DisplayName("КПП: генератор → валидатор для всех 16 регионов")
  void kppRoundTrip() {
    KppGenerator gen = new KppGenerator(DataGenerator.seededRandom(SEED));
    for (Region region : Region.values()) {
      for (int i = 0; i < 20; i++) {
        String kpp = gen.generate(region);
        assertThat(KppValidator.isValid(kpp))
          .as("Регион %s, КПП %s", region, kpp)
          .isTrue();
      }
    }
  }
}
