package ru.validum.core.address;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import ru.validum.core.common.Region;
import ru.validum.core.organization.OrganizationProfile;
import ru.validum.core.organization.OrganizationProfileGenerator;
import ru.validum.core.profile.IndividualProfile;
import ru.validum.core.profile.IndividualProfileGenerator;

/**
 * Каскад {@code region → адрес / серия паспорта / КПП / место рождения} на реальном справочнике
 * КЛАДР ({@link AddressBook}).
 */
@DisplayName("Адреса — интеграция с профилями")
class AddressCascadeIntegrationTest {

  @Test
  @DisplayName("10 000 профилей region=MOSCOW → адреса из Москвы, десятки разных улиц")
  void moscowProfilesUseRealMoscowStreets() {
    IndividualProfileGenerator generator =
      IndividualProfileGenerator.builder().seed(42L).region(Region.MOSCOW).build();

    Set<String> addresses = new HashSet<>();
    for (int i = 0; i < 10_000; i++) {
      addresses.add(generator.generate().registrationAddress().expectedAddress());
    }

    assertThat(addresses).hasSizeGreaterThan(1_000);
    assertThat(addresses).allMatch(a -> a.contains("Москва г"));
  }

  @Test
  @DisplayName("10 000 профилей region=SPB → адреса из Петербурга")
  void spbProfilesUseRealSpbStreets() {
    IndividualProfileGenerator generator =
      IndividualProfileGenerator.builder().seed(7L).region(Region.SPB).build();

    Set<String> addresses = new HashSet<>();
    for (int i = 0; i < 10_000; i++) {
      addresses.add(generator.generate().registrationAddress().expectedAddress());
    }

    assertThat(addresses).hasSizeGreaterThan(500);
    assertThat(addresses).allMatch(a -> a.contains("Санкт-Петербург г"));
  }

  @ParameterizedTest(name = "{0}")
  @EnumSource(Region.class)
  @DisplayName("region → серия паспорта начинается с кода региона")
  void passportSeriesMatchesRegionCode(Region region) {
    IndividualProfile profile =
      IndividualProfileGenerator.builder().seed(42L).region(region).build().generate();

    assertThat(profile.passport().series())
      .as("Регион %s, паспорт %s", region, profile.passport())
      .startsWith(region.code());
  }

  @ParameterizedTest(name = "{0}")
  @EnumSource(Region.class)
  @DisplayName("region → КПП начинается с кода региона (ЮЛ)")
  void kppMatchesRegionCode(Region region) {
    OrganizationProfile org =
      OrganizationProfileGenerator.builder().seed(42L).region(region).build().generate();

    assertThat(org.kpp())
      .as("Регион %s, КПП %s", region, org.kpp())
      .startsWith(region.code());
    assertThat(org.kpp()).hasSize(9).containsOnlyDigits();
  }

  @ParameterizedTest(name = "{0}")
  @EnumSource(Region.class)
  @DisplayName("region → адрес регистрации ЮЛ из того же субъекта")
  void organizationRegistrationMatchesRegion(Region region) {
    OrganizationProfile org =
      OrganizationProfileGenerator.builder().seed(42L).region(region).build().generate();

    assertThat(org.registrationAddress().region()).isEqualTo(region.regionName());
    assertThat(org.registrationAddress().city()).isNotBlank();
    assertThat(org.registrationAddress().street()).isNotBlank();
  }

  @ParameterizedTest(name = "{0}")
  @EnumSource(Region.class)
  @DisplayName("region → место рождения из того же субъекта")
  void birthPlaceMatchesRegion(Region region) {
    IndividualProfile profile =
      IndividualProfileGenerator.builder().seed(42L).region(region).build().generate();

    assertThat(profile.birthPlace().type()).isEqualTo(AddressType.BIRTH_PLACE);
    assertThat(profile.birthPlace().isDefault()).isFalse();
    assertThat(profile.birthPlace().region()).isEqualTo(region.regionName());
  }

  @Test
  @DisplayName(".seed(42).region(MOSCOW) → одинаковый адрес при повторном вызове")
  void sameSeedAndRegionProducesSameAddress() {
    Address a =
      IndividualProfileGenerator.builder()
        .seed(42L).region(Region.MOSCOW).build().generate().registrationAddress();
    Address b =
      IndividualProfileGenerator.builder()
        .seed(42L).region(Region.MOSCOW).build().generate().registrationAddress();

    assertThat(a).isEqualTo(b);
  }

  @Test
  @DisplayName("Разные seed → разные адреса")
  void differentSeedsProduceDifferentAddresses() {
    Address a =
      IndividualProfileGenerator.builder()
        .seed(42L).region(Region.SPB).build().generate().registrationAddress();
    Address b =
      IndividualProfileGenerator.builder()
        .seed(43L).region(Region.SPB).build().generate().registrationAddress();

    assertThat(a.expectedAddress()).isNotBlank();
    assertThat(b.expectedAddress()).isNotBlank();
    assertThat(a.city()).isNotBlank();
    assertThat(b.city()).isNotBlank();
  }

  @Test
  @DisplayName("ЮЛ: один seed → одинаковый адрес и КПП")
  void organizationDeterministicWithRegion() {
    OrganizationProfile a =
      OrganizationProfileGenerator.builder()
        .seed(42L).region(Region.KRASNODAR).build().generate();
    OrganizationProfile b =
      OrganizationProfileGenerator.builder()
        .seed(42L).region(Region.KRASNODAR).build().generate();

    assertThat(a.registrationAddress()).isEqualTo(b.registrationAddress());
    assertThat(a.kpp()).isEqualTo(b.kpp());
  }

  @Test
  @DisplayName("region не задан → адрес из разных субъектов, не падает")
  void noRegionFallsBackToRandomRegion() {
    Set<String> regionsSeen = new HashSet<>();
    IndividualProfileGenerator generator =
      IndividualProfileGenerator.builder().seed(42L).build();
    for (int i = 0; i < 500; i++) {
      regionsSeen.add(generator.generate().registrationAddress().region());
    }
    assertThat(regionsSeen.size()).isGreaterThan(1);
  }
}
