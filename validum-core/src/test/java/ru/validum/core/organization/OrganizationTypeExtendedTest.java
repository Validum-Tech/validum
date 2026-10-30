package ru.validum.core.organization;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import ru.validum.core.common.OrganizationType;

@DisplayName("OrganizationType — расширенные ОПФ (P2.2)")
class OrganizationTypeExtendedTest {

  private static final long SEED = 42L;

  // ── Базовые свойства ОПФ ─────────────────────────────────────────────────

  @ParameterizedTest(name = "{0}")
  @EnumSource(OrganizationType.class)
  @DisplayName("Все ОПФ корректно генерируют okopf, okfs и budget")
  void allTypesHaveCorrectMetadata(OrganizationType type) {
    OrganizationProfile org = OrganizationProfileGenerator.builder()
      .seed(SEED)
      .organizationType(type)
      .build()
      .generate();

    assertThat(org.organizationType()).isEqualTo(type);
    assertThat(org.okopf()).isEqualTo(type.okopf());
    assertThat(org.okfs()).isEqualTo(type.okfs());
    assertThat(org.budget()).isEqualTo(type.budget());
  }

  @ParameterizedTest(name = "{0}")
  @EnumSource(OrganizationType.class)
  @DisplayName("Название начинается с правильного префикса")
  void nameStartsWithPrefix(OrganizationType type) {
    OrganizationProfile org = OrganizationProfileGenerator.builder()
      .seed(SEED)
      .organizationType(type)
      .build()
      .generate();

    assertThat(org.organizationName()).startsWith(type.prefix() + " «");
  }

  // ── Конкретные новые типы ────────────────────────────────────────────────

  @Test
  @DisplayName("NCO: НКО, OKOPF=85, OKFS=16, некоммерческая")
  void ncoMetadata() {
    OrganizationProfile org = OrganizationProfileGenerator.builder()
      .seed(SEED).organizationType(OrganizationType.NCO).build().generate();

    assertThat(org.organizationName()).startsWith("НКО «");
    assertThat(org.okopf()).isEqualTo("85");
    assertThat(org.okfs()).isEqualTo("16");
    assertThat(org.budget()).isEqualTo("Некоммерческая организация");
  }

  @Test
  @DisplayName("FOUNDATION: Фонд, OKOPF=88, OKFS=16, некоммерческая")
  void foundationMetadata() {
    OrganizationProfile org = OrganizationProfileGenerator.builder()
      .seed(SEED).organizationType(OrganizationType.FOUNDATION).build().generate();

    assertThat(org.organizationName()).startsWith("Фонд «");
    assertThat(org.okopf()).isEqualTo("88");
    assertThat(org.okfs()).isEqualTo("16");
    assertThat(org.budget()).isEqualTo("Некоммерческая организация");
  }

  @Test
  @DisplayName("SE (ГУП): OKOPF=61, OKFS=14, бюджетная")
  void seMetadata() {
    OrganizationProfile org = OrganizationProfileGenerator.builder()
      .seed(SEED).organizationType(OrganizationType.SE).build().generate();

    assertThat(org.organizationName()).startsWith("ГУП «");
    assertThat(org.okopf()).isEqualTo("61");
    assertThat(org.okfs()).isEqualTo("14");
    assertThat(org.budget()).isEqualTo("Бюджетная организация");
  }

  @Test
  @DisplayName("ME (МУП): OKOPF=62, OKFS=13, бюджетная")
  void meMetadata() {
    OrganizationProfile org = OrganizationProfileGenerator.builder()
      .seed(SEED).organizationType(OrganizationType.ME).build().generate();

    assertThat(org.organizationName()).startsWith("МУП «");
    assertThat(org.okopf()).isEqualTo("62");
    assertThat(org.okfs()).isEqualTo("13");
    assertThat(org.budget()).isEqualTo("Бюджетная организация");
  }

  // ── Совместимость с LLC ──────────────────────────────────────────────────

  @Test
  @DisplayName("LLC по умолчанию: OKFS=16, небюджетная")
  void defaultLlcMetadata() {
    OrganizationProfile org = OrganizationProfileGenerator.builder()
      .seed(SEED).build().generate();

    assertThat(org.organizationType()).isEqualTo(OrganizationType.LLC);
    assertThat(org.okopf()).isEqualTo("65");
    assertThat(org.okfs()).isEqualTo("16");
    assertThat(org.budget()).isEqualTo("Небюджетная организация");
  }

  // ── Override и детерминизм ───────────────────────────────────────────────

  @Test
  @DisplayName("Явное имя переопределяет префикс для новой ОПФ")
  void explicitNameOverridesPrefix() {
    OrganizationProfile org = OrganizationProfileGenerator.builder()
      .seed(SEED)
      .organizationType(OrganizationType.SE)
      .organizationName("ГУП «СпецСтрой»")
      .build()
      .generate();

    assertThat(org.organizationName()).isEqualTo("ГУП «СпецСтрой»");
    assertThat(org.okopf()).isEqualTo("61");
  }

  @Test
  @DisplayName("Детерминизм сохраняется для новых ОПФ")
  void determinismPreserved() {
    OrganizationProfile a = OrganizationProfileGenerator.builder()
      .seed(SEED).organizationType(OrganizationType.NCO).build().generate();
    OrganizationProfile b = OrganizationProfileGenerator.builder()
      .seed(SEED).organizationType(OrganizationType.NCO).build().generate();

    assertThat(a).isEqualTo(b);
  }
}
