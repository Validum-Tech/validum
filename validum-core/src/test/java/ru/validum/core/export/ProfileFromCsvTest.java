package ru.validum.core.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.bank.BikSource;
import ru.validum.core.bank.BikSources;
import ru.validum.core.document.DocumentType;
import ru.validum.core.name.Gender;
import ru.validum.core.profile.IndividualProfile;
import ru.validum.core.profile.IndividualProfileGenerator;

@DisplayName("Профиль из плоской строки CSV (ProfileFromCsv)")
class ProfileFromCsvTest {

  private static final BikSource BANKS =
    BikSources.resolve(Path.of("src", "test", "resources", "bik-sample.xml").toString());

  private static IndividualProfile generated() {
    return IndividualProfileGenerator.builder()
      .seed(42L)
      .bikSource(BANKS)
      .build()
      .generate();
  }

  @Test
  @DisplayName("Круговая операция: flatten → parse → flatten совпадает")
  void roundTrip() {
    IndividualProfile profile = generated();

    Map<String, String> flat = IndividualProfileFlattener.flatten(profile);
    IndividualProfile parsed = ProfileFromCsv.parse(flat);
    Map<String, String> flatAgain = IndividualProfileFlattener.flatten(parsed);

    assertThat(flatAgain).isEqualTo(flat);
  }

  @Test
  @DisplayName("Документы ДУЛ переживают круг CSV → профиль → CSV")
  void documentsRoundTrip() {
    IndividualProfile profile = IndividualProfileGenerator.builder()
      .seed(7L)
      .bikSource(BANKS)
      .documents(DocumentType.PASSPORT_RF, DocumentType.FOREIGN_PASSPORT)
      .build()
      .generate();

    Map<String, String> flat = IndividualProfileFlattener.flatten(profile);
    IndividualProfile parsed = ProfileFromCsv.parse(flat);

    assertThat(parsed.documents()).isEqualTo(profile.documents());
  }

  @Test
  @DisplayName("Строка только с ФИО даёт профиль без паспорта, адреса и банка")
  void minimalRow() {
    Map<String, String> row = new LinkedHashMap<>();
    row.put("fullName", "Иванов Иван Иванович");
    row.put("gender", "MALE");

    IndividualProfile profile = ProfileFromCsv.parse(row);

    assertThat(profile.passport()).isNull();
    assertThat(profile.registrationAddress()).isNull();
    assertThat(profile.bank()).isNull();
    assertThat(profile.fullName()).isEqualTo("Иванов Иван Иванович");
  }

  @Test
  @DisplayName("Некорректная дата бросает IllegalArgumentException")
  void badDate() {
    Map<String, String> row = new LinkedHashMap<>();
    row.put("fullName", "Иванов Иван Иванович");
    row.put("birthDate", "31.12.1990");

    assertThatThrownBy(() -> ProfileFromCsv.parse(row))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Некорректная дата");
  }

  @Test
  @DisplayName("Неизвестный пол приводит к MALE")
  void unknownGender() {
    Map<String, String> row = new LinkedHashMap<>();
    row.put("fullName", "Иванов Иван Иванович");
    row.put("gender", "UNKNOWN");

    assertThat(ProfileFromCsv.parse(row).gender()).isEqualTo(Gender.MALE);
  }

  @Test
  @DisplayName("Номер счёта сохраняется строкой, включая ведущие нули")
  void accountKeptAsString() {
    Map<String, String> row = new LinkedHashMap<>();
    row.put("fullName", "Иванов Иван Иванович");
    row.put("account", "00012345678901234567");

    assertThat(ProfileFromCsv.parse(row).account()).isEqualTo("00012345678901234567");
  }
}
