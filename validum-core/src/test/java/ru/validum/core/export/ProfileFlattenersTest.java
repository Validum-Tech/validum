package ru.validum.core.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import ru.validum.core.common.OrganizationType;
import ru.validum.core.common.Region;
import ru.validum.core.organization.IpProfile;
import ru.validum.core.organization.IpProfileGenerator;
import ru.validum.core.organization.OrganizationProfile;
import ru.validum.core.organization.OrganizationProfileGenerator;
import ru.validum.core.profile.IndividualProfile;
import ru.validum.core.profile.IndividualProfileGenerator;

@DisplayName("Флаттенеры профилей — core-версия (P3.1a)")
class ProfileFlattenersTest {

  private static final long SEED = 42L;

  // ── Individual ───────────────────────────────────────────────────────────

  @Nested
  @DisplayName("IndividualProfileFlattener")
  class IndividualFlattener {

    @Test
    @DisplayName("Все ключевые колонки на месте, включая вложенные с префиксами")
    void hasAllKeyColumns() {
      IndividualProfile p =
        IndividualProfileGenerator.builder().seed(SEED).build().generate();
      Map<String, String> row = IndividualProfileFlattener.flatten(p);

      assertThat(row).containsKeys(
        "fullName", "gender", "birthDate", "age", "familyStatus",
        "inn", "snils",
        "passportSeries", "passportNumber", "passportIssuedBy",
        "passportIssueDate", "passportExpirationDate",
        "passportDepartmentCode", "passportType", "passportCountry",
        "registrationRegion", "registrationRegionType",
        "registrationPostalCode", "registrationCity", "registrationStreet",
        "registrationBuilding", "registrationApartment", "registrationFormatted",
        "postCity", "currentCity",
        "mobileType", "mobileValue", "mobileDescription", "mobileStatus",
        "emailType", "emailValue",
        "secretWord", "categoryClient", "accountPackage", "personalManager",
        "terrorist", "blocked",
        "bankBik", "bankName", "bankCorrAccount", "bankCity", "bankRegion",
        "account"
      );
    }

    @Test
    @DisplayName("Регион адреса виден в плоской выдаче — по CSV его нельзя было сверить")
    void addressRegionIsPresent() {
      IndividualProfile p =
        IndividualProfileGenerator.builder().seed(SEED).region(Region.MOSCOW).build().generate();
      Map<String, String> row = IndividualProfileFlattener.flatten(p);

      assertThat(row.get("registrationRegion")).isEqualTo(Region.MOSCOW.regionName());
      assertThat(row.get("postRegion")).isEqualTo(Region.MOSCOW.regionName());
      assertThat(row.get("currentRegion")).isEqualTo(Region.MOSCOW.regionName());
      assertThat(row.get("registrationRegionType")).isNotBlank();
    }

    @Test
    @DisplayName("Срок паспорта попадает в строку: без него describe печатал пусто")
    void passportExpirationIsPresent() {
      IndividualProfile p =
        IndividualProfileGenerator.builder().seed(SEED).age(25).build().generate();
      Map<String, String> row = IndividualProfileFlattener.flatten(p);

      assertThat(row.get("passportExpirationDate"))
        .as("у 25-летнего паспорт действует до 45-летия")
        .matches("\\d{4}-\\d{2}-\\d{2}");
    }

    @Test
    @DisplayName("Порядок колонок фиксирован — personId первый, asOf последний")
    void columnOrderIsStable() {
      IndividualProfile p =
        IndividualProfileGenerator.builder().seed(SEED).build().generate();
      Map<String, String> row = IndividualProfileFlattener.flatten(p);

      List<String> keys = List.copyOf(row.keySet());
      assertThat(keys.get(0)).isEqualTo("personId");
      assertThat(keys.get(1)).isEqualTo("fullName");
      assertThat(keys.get(keys.size() - 5)).isEqualTo("account");
      assertThat(keys.get(keys.size() - 4)).isEqualTo("documents");
      assertThat(keys.get(keys.size() - 3)).isEqualTo("isResident");
      assertThat(keys.get(keys.size() - 2)).isEqualTo("citizenship");
      assertThat(keys.get(keys.size() - 1)).isEqualTo("asOf");
    }

    @Test
    @DisplayName("Профиль без счёта → колонка account пустая (null → \"\")")
    void noAccountLeavesColumnEmpty() {
      IndividualProfile p =
        IndividualProfileGenerator.builder().seed(SEED).withAccount(false).build().generate();
      Map<String, String> row = IndividualProfileFlattener.flatten(p);

      // account в map отсутствует (flattenBank добавляет, но account пустой)
      assertThat(row.get("account")).isNull();
    }

    @Test
    @DisplayName("Детерминизм: одинаковый seed → одинаковый CSV")
    void deterministicCsv() {
      IndividualProfile a =
        IndividualProfileGenerator.builder().seed(SEED).region(Region.MOSCOW).build().generate();
      IndividualProfile b =
        IndividualProfileGenerator.builder().seed(SEED).region(Region.MOSCOW).build().generate();

      String csvA = CsvExporter.toCsv(IndividualProfileFlattener.flattenAll(List.of(a)));
      String csvB = CsvExporter.toCsv(IndividualProfileFlattener.flattenAll(List.of(b)));

      assertThat(csvA).isEqualTo(csvB);
    }
  }

  // ── Ip ───────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("IpProfileFlattener")
  class IpFlattener {

    @Test
    @DisplayName("Начинается с ogrnip, затем все колонки ФЛ")
    void startsWithOgrnipThenFl() {
      IndividualProfile base =
        IndividualProfileGenerator.builder().seed(SEED).age(35).build().generate();
      IpProfile ip = IpProfileGenerator.from(base, SEED);

      Map<String, String> row = IpProfileFlattener.flatten(ip);
      List<String> keys = List.copyOf(row.keySet());

      assertThat(keys.get(0)).isEqualTo("ogrnip");
      assertThat(keys.get(1)).isEqualTo("personId");
      assertThat(keys.get(2)).isEqualTo("fullName");
      assertThat(row.get("ogrnip")).hasSize(15);
      assertThat(row.get("inn")).isEqualTo(base.inn());
    }

    @Test
    @DisplayName("Детерминизм: одинаковый seed → одинаковый CSV")
    void deterministic() {
      IpProfile a = IpProfileGenerator.generate(
        IndividualProfileGenerator.builder().seed(SEED).age(35).build(), SEED);
      IpProfile b = IpProfileGenerator.generate(
        IndividualProfileGenerator.builder().seed(SEED).age(35).build(), SEED);

      String csvA = CsvExporter.toCsv(IpProfileFlattener.flattenAll(List.of(a)));
      String csvB = CsvExporter.toCsv(IpProfileFlattener.flattenAll(List.of(b)));

      assertThat(csvA).isEqualTo(csvB);
    }
  }

  // ── Organization ─────────────────────────────────────────────────────────

  @Nested
  @DisplayName("OrganizationProfileFlattener")
  class OrganizationFlattener {

    @Test
    @DisplayName("Все классификаторы на месте")
    void hasAllClassifiers() {
      OrganizationProfile p =
        OrganizationProfileGenerator.builder().seed(SEED).build().generate();
      Map<String, String> row = OrganizationProfileFlattener.flatten(p);

      assertThat(row).containsKeys(
        "organizationName", "organizationType",
        "ogrn", "inn", "kpp", "okpo", "okopf", "okfs", "okogu", "okved", "budget", "swift",
        "registrationCity", "postCity",
        "mobileValue", "emailValue",
        "terrorist", "blocked"
      );
    }

    @Test
    @DisplayName("ГУП: префикс, OKOPF=61, OKFS=14, бюджетная — всё в колонках")
    void gupMetadataInColumns() {
      OrganizationProfile p =
        OrganizationProfileGenerator.builder()
          .seed(SEED).organizationType(OrganizationType.SE).build().generate();
      Map<String, String> row = OrganizationProfileFlattener.flatten(p);

      assertThat(row.get("organizationType")).isEqualTo("SE");
      assertThat(row.get("organizationName")).startsWith("ГУП «");
      assertThat(row.get("okopf")).isEqualTo("61");
      assertThat(row.get("okfs")).isEqualTo("14");
      assertThat(row.get("budget")).isEqualTo("Бюджетная организация");
    }

    @Test
    @DisplayName("Детерминизм: одинаковый seed → одинаковый CSV")
    void deterministic() {
      OrganizationProfile a =
        OrganizationProfileGenerator.builder().seed(SEED).region(Region.MOSCOW).build().generate();
      OrganizationProfile b =
        OrganizationProfileGenerator.builder().seed(SEED).region(Region.MOSCOW).build().generate();

      String csvA = CsvExporter.toCsv(OrganizationProfileFlattener.flattenAll(List.of(a)));
      String csvB = CsvExporter.toCsv(OrganizationProfileFlattener.flattenAll(List.of(b)));

      assertThat(csvA).isEqualTo(csvB);
    }
  }
}
