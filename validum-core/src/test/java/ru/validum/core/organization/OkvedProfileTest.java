package ru.validum.core.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.okved.OkvedRegistry;
import ru.validum.core.profile.IndividualProfile;
import ru.validum.core.profile.IndividualProfileGenerator;

@DisplayName("ОКВЭД в профилях ЮЛ и ИП")
class OkvedProfileTest {

  private static final long SEED = 42L;

  @Test
  @DisplayName("ЮЛ: явный код уважается, наименование из справочника")
  void organizationExplicitCode() {
    OrganizationProfile p =
      OrganizationProfileGenerator.builder().seed(SEED).okved("41.20").build().generate();

    assertThat(p.okved()).isEqualTo("41.20");
    assertThat(p.okvedName()).isEqualTo("Строительство жилых и нежилых зданий");
  }

  @Test
  @DisplayName("ЮЛ: по умолчанию код случайный, но всегда есть в справочнике")
  void organizationRandomCodeIsKnown() {
    OrganizationProfile p = OrganizationProfileGenerator.builder().seed(SEED).build().generate();

    assertThat(OkvedRegistry.byCode(p.okved())).isPresent();
    assertThat(p.okvedName()).isEqualTo(OkvedRegistry.require(p.okved()).name());
  }

  @Test
  @DisplayName("ЮЛ: неизвестный код — IllegalArgumentException")
  void organizationUnknownCode() {
    assertThatThrownBy(
        () -> OrganizationProfileGenerator.builder().seed(SEED).okved("12.34").build().generate())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("ОКВЭД не найден");
  }

  @Test
  @DisplayName("ИП: код и наименование согласованы, seed воспроизводим")
  void ipOkvedIsConsistentAndDeterministic() {
    IndividualProfile base =
      IndividualProfileGenerator.builder().seed(SEED).age(35).build().generate();

    IpProfile a = IpProfileGenerator.from(base, SEED);
    IpProfile b = IpProfileGenerator.from(base, SEED);

    assertThat(a.okved()).isEqualTo(b.okved());
    assertThat(a.okvedName()).isEqualTo(b.okvedName());
    assertThat(OkvedRegistry.require(a.okved()).name()).isEqualTo(a.okvedName());
  }

  @Test
  @DisplayName("ИП: явный код через generate(..., seed, code)")
  void ipExplicitCode() {
    IpProfile ip =
      IpProfileGenerator.generate(
        IndividualProfileGenerator.builder().seed(SEED).age(35).build(), SEED, "62.01");

    assertThat(ip.okved()).isEqualTo("62.01");
    assertThat(ip.okvedName()).isEqualTo("Разработка компьютерного программного обеспечения");
  }

  @Test
  @DisplayName("CSV: у ЮЛ и ИП появились okved и okvedName")
  void csvHasColumn() {
    OrganizationProfile org =
      OrganizationProfileGenerator.builder().seed(SEED).okved("62.01").build().generate();
    IpProfile ip =
      IpProfileGenerator.generate(
        IndividualProfileGenerator.builder().seed(SEED).age(35).build(), SEED, "62.01");

    assertThat(ru.validum.core.export.OrganizationProfileFlattener.flatten(org))
      .containsEntry("okved", "62.01")
      .containsEntry("okvedName", "Разработка компьютерного программного обеспечения");
    assertThat(ru.validum.core.export.IpProfileFlattener.flatten(ip))
      .containsEntry("okved", "62.01")
      .containsEntry("okvedName", "Разработка компьютерного программного обеспечения");
  }
}
