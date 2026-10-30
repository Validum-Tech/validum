package ru.validum.core.bank;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.random.DataGenerator;

/**
 * Генератор банков проверяется на фикстуре справочника: встроенного демо-набора в продукте нет,
 * источник — файл ED807.
 */
class BikGeneratorTest {

  private static final Path FIXTURE = Path.of("src", "test", "resources", "bik-sample.xml");
  private static final BikSource SOURCE = BikSources.resolve(FIXTURE.toString());

  private final BikGenerator generator = new BikGenerator(DataGenerator.seededRandom(7L), SOURCE);

  @Test
  @DisplayName("Фикстура справочника содержит не менее трёх банков")
  void fixtureHasBanks() {
    assertThat(SOURCE.size()).isGreaterThanOrEqualTo(3);
  }

  @Test
  @DisplayName("БИК любого банка содержит 9 цифр")
  void allBiksHave9Digits() {
    for (Bank bank : SOURCE.banks()) {
      assertThat(bank.bik()).as("БИК %s", bank.name()).hasSize(9).containsOnlyDigits();
    }
  }

  @Test
  @DisplayName("Корсчёт любого банка содержит 20 цифр")
  void allCorrAccountsHave20Digits() {
    for (Bank bank : SOURCE.banks()) {
      assertThat(bank.corrAccount()).as("Корсчёт %s", bank.name()).hasSize(20).containsOnlyDigits();
    }
  }

  @Test
  @DisplayName("Корсчёт начинается с 301")
  void allCorrAccountsStartWith301() {
    for (Bank bank : SOURCE.banks()) {
      assertThat(bank.corrAccount()).as("Корсчёт %s", bank.name()).startsWith("301");
    }
  }

  @Test
  @DisplayName("БИК в справочнике уникальны")
  void biksAreUnique() {
    Set<String> biks = new HashSet<>();
    for (Bank bank : SOURCE.banks()) {
      assertThat(biks.add(bank.bik()))
        .as("Дубликат БИК: %s (%s)", bank.bik(), bank.name())
        .isTrue();
    }
  }

  @Test
  @DisplayName("Сгенерированный БИК есть в справочнике")
  void generatedBikExistsInRegistry() {
    for (int i = 0; i < 50; i++) {
      String bik = generator.generateBik();
      assertThat(SOURCE.findByBik(bik)).as("БИК %s", bik).isPresent();
    }
  }

  @Test
  @DisplayName("Одинаковый seed даёт одинаковый БИК")
  void sameSeedProducesSameBik() {
    BikGenerator first = new BikGenerator(DataGenerator.seededRandom(42L), SOURCE);
    BikGenerator second = new BikGenerator(DataGenerator.seededRandom(42L), SOURCE);
    assertThat(first.generateBik()).isEqualTo(second.generateBik());
  }

  @Test
  @DisplayName("Разные seed покрывают все банки за 500 попыток")
  void differentSeedsCoverAllBanks() {
    Set<String> biks = new HashSet<>();
    for (int i = 0; i < 500; i++) {
      biks.add(generator.generateBik());
    }
    assertThat(biks).hasSize(SOURCE.size());
  }

  @Test
  @DisplayName("Поиск по БИК находит Сбербанк")
  void findByBikReturnsSberbank() {
    Bank sber = SOURCE.findByBik("044525225").orElseThrow();
    assertThat(sber.name()).isEqualTo("ПАО Сбербанк");
    assertThat(sber.corrAccount()).isEqualTo("30101810400000000225");
  }

  @Test
  @DisplayName("Поиск по несуществующему БИК возвращает пусто")
  void findByUnknownBikIsEmpty() {
    assertThat(SOURCE.findByBik("000000000")).isEmpty();
  }

  @Test
  @DisplayName("BikGenerator можно создать со своим источником")
  void generatorAcceptsCustomSource() {
    Bank sber = SOURCE.findByBik("044525225").orElseThrow();
    BikSource onlySber =
      new BikSource() {
        @Override
        public List<Bank> banks() {
          return List.of(sber);
        }

        @Override
        public Optional<Bank> findByBik(String bik) {
          return "044525225".equals(bik) ? Optional.of(sber) : Optional.empty();
        }
      };
    BikGenerator customGen = new BikGenerator(DataGenerator.seededRandom(1L), onlySber);
    for (int i = 0; i < 20; i++) {
      assertThat(customGen.generateBik()).isEqualTo("044525225");
    }
  }
}
