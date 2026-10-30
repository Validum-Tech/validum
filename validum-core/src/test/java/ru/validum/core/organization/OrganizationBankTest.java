package ru.validum.core.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.account.AccountValidator;
import ru.validum.core.bank.BikSource;
import ru.validum.core.bank.BikSources;
import ru.validum.core.export.OrganizationProfileFlattener;

@DisplayName("ЮЛ: банк и расчётный счёт (P2.4)")
class OrganizationBankTest {

  private static final BikSource BANKS = BikSources.resolve("src/test/resources/bik-sample.xml");
  private static final String SBER_BIK = "044525225";

  @Test
  @DisplayName("В профиле есть банк и счёт из 20 цифр")
  void bankAndAccountPresent() {
    OrganizationProfile p =
      OrganizationProfileGenerator.builder().bikSource(BANKS).seed(42L).build().generate();

    assertThat(p.bank()).isNotNull();
    assertThat(p.bank().bik()).hasSize(9);
    assertThat(p.account()).matches("\\d{20}");
  }

  @Test
  @DisplayName("Контрольный ключ счёта сходится с БИК банка")
  void accountIsValidForBankBik() {
    OrganizationProfile p =
      OrganizationProfileGenerator.builder().bikSource(BANKS).seed(7L).build().generate();

    assertThat(AccountValidator.isValidAuto(p.account(), p.bank().bik())).isTrue();
  }

  @Test
  @DisplayName("Явный БИК попадает в профиль")
  void explicitBik() {
    OrganizationProfile p =
      OrganizationProfileGenerator.builder()
        .bikSource(BANKS)
        .bik(SBER_BIK)
        .seed(1L)
        .build()
        .generate();

    assertThat(p.bank().bik()).isEqualTo(SBER_BIK);
    assertThat(p.bank().name()).contains("Сбербанк");
  }

  @Test
  @DisplayName("Неизвестный БИК — ошибка с понятным текстом")
  void unknownBik() {
    OrganizationProfileGenerator generator =
      OrganizationProfileGenerator.builder().bikSource(BANKS).bik("999999999").seed(1L).build();

    assertThatThrownBy(generator::generate)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("не найден в справочнике");
  }

  @Test
  @DisplayName("В CSV есть колонки банка и счёта")
  void csvHasBankColumns() {
    OrganizationProfile p =
      OrganizationProfileGenerator.builder().bikSource(BANKS).seed(3L).build().generate();
    Map<String, String> row = OrganizationProfileFlattener.flatten(p);

    assertThat(row).containsKeys("bankBik", "bankName", "bankCorrAccount", "account");
    assertThat(row.get("bankBik")).isEqualTo(p.bank().bik());
    assertThat(row.get("account")).isEqualTo(p.account());
  }

  @Test
  @DisplayName("Одинаковый seed → одинаковые банк и счёт")
  void deterministic() {
    OrganizationProfile a =
      OrganizationProfileGenerator.builder().bikSource(BANKS).seed(99L).build().generate();
    OrganizationProfile b =
      OrganizationProfileGenerator.builder().bikSource(BANKS).seed(99L).build().generate();

    assertThat(a.bank()).isEqualTo(b.bank());
    assertThat(a.account()).isEqualTo(b.account());
  }
}
