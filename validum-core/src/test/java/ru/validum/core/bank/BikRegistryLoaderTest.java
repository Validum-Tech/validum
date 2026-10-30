package ru.validum.core.bank;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.random.DataGenerator;

class BikRegistryLoaderTest {

  @Test
  @DisplayName("Парсер оставляет только действующие банки с корсчётом")
  void parsesOnlyActiveBanksWithCorrAccount() throws Exception {
    try (InputStream in = getClass().getResourceAsStream("/bik-sample.xml")) {
      assertThat(in).as("Тестовый файл bik-sample.xml не найден").isNotNull();
      BikSource source = BikRegistryLoader.load(in);

      // Из 6 записей ожидаем 3 (Сбер, ВТБ, Тинькофф)
      assertThat(source.banks()).hasSize(3);
      assertThat(source.findByBik("044525225")).isPresent();
      assertThat(source.findByBik("044525187")).isPresent();
      assertThat(source.findByBik("044525974")).isPresent();
      // УФК пропущен
      assertThat(source.findByBik("016311121")).isEmpty();
      // Закрытый банк пропущен
      assertThat(source.findByBik("999999999")).isEmpty();
      // ОКЦ без корсчёта пропущен
      assertThat(source.findByBik("044525000")).isEmpty();
    }
  }

  @Test
  @DisplayName("Данные банка разобраны корректно")
  void parsesBankFieldsCorrectly() throws Exception {
    try (InputStream in = getClass().getResourceAsStream("/bik-sample.xml")) {
      BikSource source = BikRegistryLoader.load(in);
      Bank sber = source.findByBik("044525225").orElseThrow();

      assertThat(sber.bik()).isEqualTo("044525225");
      assertThat(sber.name()).isEqualTo("ПАО Сбербанк");
      assertThat(sber.corrAccount()).isEqualTo("30101810400000000225");
      assertThat(sber.city()).isEqualTo("Москва");
      assertThat(sber.region()).isEqualTo("45");
    }
  }

  @Test
  @DisplayName("Регистрационный номер, UID, адрес и индекс читаются из справочника")
  void parsesDirectoryDetails() throws Exception {
    try (InputStream in = getClass().getResourceAsStream("/bik-sample.xml")) {
      ParticipantRegistry registry = BikRegistryLoader.loadRegistry(in);
      Participant sber = registry.findByBik("044525225").orElseThrow();

      assertThat(sber.regN()).isEqualTo("1481");
      assertThat(sber.uid()).isEqualTo("4525545000");
      assertThat(sber.address()).isEqualTo("УЛ. ВАВИЛОВА,21");
      assertThat(sber.index()).isEqualTo("117997");
      assertThat(sber.region()).isEqualTo("45");
    }
  }

  @Test
  @DisplayName("BikGenerator работает с загруженным справочником")
  void generatorWorksWithLoadedSource() throws Exception {
    try (InputStream in = getClass().getResourceAsStream("/bik-sample.xml")) {
      BikSource source = BikRegistryLoader.load(in);
      BikGenerator generator = new BikGenerator(DataGenerator.seededRandom(42L), source);

      for (int i = 0; i < 50; i++) {
        String bik = generator.generateBik();
        assertThat(source.findByBik(bik)).as("БИК %s", bik).isPresent();
      }
    }
  }

  @Test
  @DisplayName("Одинаковый seed даёт одинаковый результат с загруженным справочником")
  void sameSeedProducesSameBik() throws Exception {
    try (InputStream in = getClass().getResourceAsStream("/bik-sample.xml")) {
      BikSource source = BikRegistryLoader.load(in);
      BikGenerator first = new BikGenerator(DataGenerator.seededRandom(42L), source);
      BikGenerator second = new BikGenerator(DataGenerator.seededRandom(42L), source);
      assertThat(first.generateBik()).isEqualTo(second.generateBik());
    }
  }
}
