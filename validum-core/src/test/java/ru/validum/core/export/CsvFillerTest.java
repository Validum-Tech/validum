package ru.validum.core.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.bank.Bank;
import ru.validum.core.bank.BikSource;
import ru.validum.core.bank.BikSources;
import ru.validum.core.name.Gender;
import ru.validum.core.profile.IndividualProfile;

@DisplayName("Дозаполнение CSV (CsvFiller)")
class CsvFillerTest {

  private static final BikSource BANKS =
    BikSources.resolve(Path.of("src", "test", "resources", "bik-sample.xml").toString());

  private static final CsvFiller FILLER = new CsvFiller(BANKS, new Random(7L));

  @Test
  @DisplayName("Пустые ячейки заполняются, строка становится полным профилем")
  void fillsEmptyCells() {
    Map<String, String> row = new LinkedHashMap<>();
    row.put("fullName", "Иванов Иван Иванович");
    row.put("inn", "");
    row.put("snils", "");

    Map<String, String> filled = FILLER.fillRow(row);

    assertThat(filled.get("inn")).isNotBlank();
    assertThat(filled.get("snils")).isNotBlank();
    assertThat(filled).containsKey("passportSeries");
    assertThat(filled).containsKey("bankBik");
  }

  @Test
  @DisplayName("Непустые ячейки не перезаписываются")
  void keepsFilledCells() {
    Map<String, String> row = new LinkedHashMap<>();
    row.put("fullName", "Петров Пётр Петрович");
    row.put("inn", "7707083893");
    row.put("account", "40817810099910004312");

    Map<String, String> filled = FILLER.fillRow(row);

    assertThat(filled).containsEntry("fullName", "Петров Пётр Петрович");
    assertThat(filled).containsEntry("inn", "7707083893");
    assertThat(filled).containsEntry("account", "40817810099910004312");
  }

  @Test
  @DisplayName("Пол из подсказки сохраняется в сгенерированном профиле")
  void genderHint() {
    Map<String, String> row = new LinkedHashMap<>();
    row.put("gender", "FEMALE");

    Map<String, String> filled = FILLER.fillRow(row);

    assertThat(ProfileFromCsv.parse(filled).gender()).isEqualTo(Gender.FEMALE);
  }

  @Test
  @DisplayName("БИК из подсказки задаёт банк и его реквизиты")
  void bankHint() {
    Bank bank = BANKS.banks().get(0);

    Map<String, String> row = new LinkedHashMap<>();
    row.put("bankBik", bank.bik());

    Map<String, String> filled = FILLER.fillRow(row);

    assertThat(filled).containsEntry("bankBik", bank.bik());
    assertThat(filled).containsEntry("bankName", bank.name());
    assertThat(filled).containsEntry("bankCorrAccount", bank.corrAccount());
  }

  @Test
  @DisplayName("Неизвестный БИК не подмешивает чужой банк")
  void unknownBikLeavesBankColumnsEmpty() {
    Map<String, String> row = new LinkedHashMap<>();
    row.put("bankBik", "000000000");

    Map<String, String> filled = FILLER.fillRow(row);

    assertThat(filled).containsEntry("bankBik", "000000000");
    assertThat(filled).doesNotContainKey("bankName");
    assertThat(filled).doesNotContainKey("bankCorrAccount");
  }

  @Test
  @DisplayName("После дозаполнения flatten → parse → flatten устойчив")
  void roundTripAfterFill() {
    Map<String, String> row = new LinkedHashMap<>();
    row.put("fullName", "Сидоров Сидор Сидорович");

    Map<String, String> filled = FILLER.fillRow(row);
    IndividualProfile parsed = ProfileFromCsv.parse(filled);
    Map<String, String> again = IndividualProfileFlattener.flatten(parsed);

    assertThat(again).isEqualTo(filled);
  }

  @Test
  @DisplayName("fill() обрабатывает список строк")
  void fillList() {
    Map<String, String> a = new LinkedHashMap<>();
    a.put("fullName", "Иванов Иван Иванович");
    Map<String, String> b = new LinkedHashMap<>();
    b.put("fullName", "Петров Пётр Петрович");

    List<Map<String, String>> filled = FILLER.fill(List.of(a, b));

    assertThat(filled).hasSize(2);
    assertThat(filled.get(0)).containsEntry("fullName", "Иванов Иван Иванович");
    assertThat(filled.get(1)).containsEntry("fullName", "Петров Пётр Петрович");
  }
}
