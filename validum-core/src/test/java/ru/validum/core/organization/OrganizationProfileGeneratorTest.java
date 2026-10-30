package ru.validum.core.organization;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import ru.validum.core.common.OrganizationType;
import ru.validum.core.common.Region;

@DisplayName("OrganizationProfileGenerator")
class OrganizationProfileGeneratorTest {

  private static final long SEED = 42L;

  @Test
  @DisplayName("Дефолт: ООО, все поля заполнены")
  void defaultIsFullyPopulated() {
    var p = OrganizationProfileGenerator.defaults().generate();

    assertThat(p.organizationName()).startsWith("ООО «").endsWith("»");
    assertThat(p.organizationType()).isEqualTo(OrganizationType.LLC);
    assertThat(p.ogrn()).hasSize(13).containsOnlyDigits();
    assertThat(p.inn()).hasSize(10).containsOnlyDigits();
    assertThat(p.kpp()).hasSize(9).containsOnlyDigits();
    assertThat(p.okpo()).hasSize(8).containsOnlyDigits();
    assertThat(p.okopf()).isEqualTo("65");
    assertThat(p.registrationAddress()).isNotNull();
    assertThat(p.mobile()).isNotNull();
    assertThat(p.email()).isNotNull();
  }

  @Test
  @DisplayName("Флаги по умолчанию false")
  void defaultFlagsFalse() {
    var p = OrganizationProfileGenerator.builder().seed(SEED).build().generate();
    assertThat(p.terrorist()).isFalse();
    assertThat(p.blocked()).isFalse();
  }

  @Nested
  @DisplayName("Каскад region → KPP + адрес")
  class RegionCascade {

    @Test
    @DisplayName("region=MOSCOW → КПП на 77, адрес Москва")
    void moscow() {
      var p =
        OrganizationProfileGenerator.builder()
          .region(Region.MOSCOW)
          .seed(SEED)
          .build()
          .generate();
      assertThat(p.kpp()).startsWith("77");
      assertThat(p.registrationAddress().region()).isEqualTo(Region.MOSCOW.regionName());
      assertThat(p.registrationAddress().city()).isNotBlank();
    }

    @Test
    @DisplayName("region=SPB → КПП на 78, адрес СПб")
    void spb() {
      var p =
        OrganizationProfileGenerator.builder()
          .region(Region.SPB)
          .seed(SEED)
          .build()
          .generate();
      assertThat(p.kpp()).startsWith("78");
      assertThat(p.registrationAddress().region()).isEqualTo(Region.SPB.regionName());
      assertThat(p.registrationAddress().city()).isNotBlank();
    }
  }

  @Nested
  @DisplayName("Каскад organizationType → OKOPF + префикс")
  class TypeCascade {

    @Test
    @DisplayName("JSC → префикс АО, OKOPF 47")
    void jsc() {
      var p =
        OrganizationProfileGenerator.builder()
          .organizationType(OrganizationType.JSC)
          .seed(SEED)
          .build()
          .generate();
      assertThat(p.organizationName()).startsWith("АО «");
      assertThat(p.okopf()).isEqualTo("47");
    }

    @Test
    @DisplayName("PJSC → префикс ПАО, OKOPF 47")
    void pjsc() {
      var p =
        OrganizationProfileGenerator.builder()
          .organizationType(OrganizationType.PJSC)
          .seed(SEED)
          .build()
          .generate();
      assertThat(p.organizationName()).startsWith("ПАО «");
      assertThat(p.okopf()).isEqualTo("47");
    }
  }

  @Test
  @DisplayName("Явное organizationName переопределяет генерацию")
  void explicitName() {
    var p =
      OrganizationProfileGenerator.builder()
        .organizationName("ООО «Моя фирма»")
        .seed(SEED)
        .build()
        .generate();
    assertThat(p.organizationName()).isEqualTo("ООО «Моя фирма»");
  }

  @Test
  @DisplayName("Флаги terrorist/blocked выставляются явно")
  void flags() {
    var p =
      OrganizationProfileGenerator.builder()
        .terrorist(true)
        .blocked(true)
        .seed(SEED)
        .build()
        .generate();
    assertThat(p.terrorist()).isTrue();
    assertThat(p.blocked()).isTrue();
  }

  @Test
  @DisplayName("Детерминизм по seed")
  void determinism() {
    var a = OrganizationProfileGenerator.builder().seed(SEED).build().generate();
    var b = OrganizationProfileGenerator.builder().seed(SEED).build().generate();
    assertThat(a).isEqualTo(b);
  }
}
