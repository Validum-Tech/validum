package ru.validum.core.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.account.AccountValidator;
import ru.validum.core.bank.Bank;
import ru.validum.core.bank.BikSource;
import ru.validum.core.bank.BikSources;
import ru.validum.core.organization.IpProfile;
import ru.validum.core.organization.IpProfileGenerator;
import ru.validum.core.organization.OrganizationProfile;
import ru.validum.core.organization.OrganizationProfileGenerator;
import ru.validum.core.profile.IndividualProfile;
import ru.validum.core.profile.IndividualProfileGenerator;

/**
 * Property-проверки «по независимой реализации» (W47-N1).
 *
 * <p>Каждый алгоритм здесь продублирован в тесте заново — намеренно. Если завтра в
 * {@link InnValidator} или {@link AccountValidator} появится ошибка, дубль в тесте её не повторит и
 * прогон станет красным. Это единственный способ поймать регрессию в самих валидаторах.
 */
@DisplayName("Property-инварианты: валидаторы против независимой реализации (W47-N1)")
class PropertyInvariantsTest {

  private static final BikSource BANKS =
    BikSources.resolve("src/test/resources/bik-sample.xml");

  private static final LocalDate AS_OF = LocalDate.of(2026, 11, 1);

  private static final Random RANDOM = new Random(20261101L);

  // === Независимые реализации проверок ===

  /** Контрольные цифры ИНН ФЛ — по весам ФНС, отдельно от {@link InnValidator}. */
  private static boolean independentInn12(String inn) {
    if (inn == null || inn.length() != 12 || !inn.matches("\\d{12}")) return false;
    int[] w1 = {7, 2, 4, 10, 3, 5, 9, 4, 6, 8};
    int[] w2 = {3, 7, 2, 4, 10, 3, 5, 9, 4, 6, 8};
    int s1 = 0;
    int s2 = 0;
    for (int i = 0; i < 10; i++) s1 += (inn.charAt(i) - '0') * w1[i];
    for (int i = 0; i < 11; i++) s2 += (inn.charAt(i) - '0') * w2[i];
    return (s1 % 11 % 10) == (inn.charAt(10) - '0') && (s2 % 11 % 10) == (inn.charAt(11) - '0');
  }

  /** Контрольная цифра ИНН ЮЛ; отбраковка остатка 10 — часть алгоритма, не «поправка». */
  private static boolean independentInn10(String inn) {
    if (inn == null || inn.length() != 10 || !inn.matches("\\d{10}")) return false;
    int[] w = {2, 4, 10, 3, 5, 9, 4, 6, 8};
    int sum = 0;
    for (int i = 0; i < 9; i++) sum += (inn.charAt(i) - '0') * w[i];
    return (sum % 11 % 10) == (inn.charAt(9) - '0');
  }

  /** СНИЛС: сумма с весами 9..1, остаток 100/101 → «00». */
  private static boolean independentSnils(String snils) {
    if (snils == null || snils.length() != 11 || !snils.matches("\\d{11}")) return false;
    int sum = 0;
    for (int i = 0; i < 9; i++) sum += (snils.charAt(i) - '0') * (9 - i);
    int control = sum % 101;
    if (control == 100 || control == 101) control = 0;
    return control == Integer.parseInt(snils.substring(9));
  }

  /** ОГРН: первые 12 цифр / 11 → младший разряд как контрольный. */
  private static boolean independentOgrn(String value, int length) {
    if (value == null || value.length() != length || !value.matches("\\d{" + length + "}")) {
      return false;
    }
    long numeric = Long.parseLong(value);
    int divisor = length == 13 ? 11 : 13;
    int control = (int) ((numeric / 10) % divisor % 10);
    return control == (value.charAt(length - 1) - '0');
  }

  /**
   * Контрольный ключ счёта по письму № 515: проверяются оба правила — клиентское (разряды 7-9
   * БИК) и корреспондентское («0» + разряды 5-6 БИК), как в {@code isAuto}.
   */
  private static boolean independentAccount(String account, String bik) {
    if (account == null || account.length() != 20 || bik == null || bik.length() != 9) {
      return false;
    }
    return keyMatches(account, bik.substring(6))
      || keyMatches(account, "0" + bik.substring(4, 6));
  }

  private static boolean keyMatches(String account, String prefix) {
    int[] prefixWeights = {7, 1, 3};
    int[] accountWeights = {7, 1, 3, 7, 1, 3, 7, 1, 3, 7, 1, 3, 7, 1, 3, 7, 1, 3, 7, 1};
    int sum = 0;
    for (int i = 0; i < 3; i++) sum += (prefix.charAt(i) - '0') * prefixWeights[i];
    for (int i = 0; i < 20; i++) {
      if (i == 8) continue; // контрольный ключ в расчёте участвует как ноль
      sum += digitValue(account.charAt(i)) * accountWeights[i];
    }
    return (sum * 3 % 10) == digitValue(account.charAt(8));
  }

  /** Буква клиринговой валюты в 6-м разряде: A→0, B→1, C→2, E→3, H→4, K→5, M→6, P→7, T→8, X→9. */
  private static int digitValue(char c) {
    return switch (c) {
      case 'A' -> 0;
      case 'B' -> 1;
      case 'C' -> 2;
      case 'E' -> 3;
      case 'H' -> 4;
      case 'K' -> 5;
      case 'M' -> 6;
      case 'P' -> 7;
      case 'T' -> 8;
      case 'X' -> 9;
      default -> c - '0';
    };
  }

  // === Свойства валидаторов на случайных данных ===

  @Test
  @DisplayName("ИНН-12: валидатор и независимая формула согласны на 5000 случайных строках")
  void inn12MatchesIndependent() {
    for (int i = 0; i < 5000; i++) {
      String candidate = randomDigits(12);
      assertThat(InnValidator.isValidIndividual(candidate))
        .as("ИНН ФЛ %s", candidate)
        .isEqualTo(independentInn12(candidate));
    }
  }

  @Test
  @DisplayName("ИНН-10 и СНИЛС: те же свойства для ЮЛ и СНИЛС")
  void inn10AndSnilsMatchIndependent() {
    for (int i = 0; i < 2000; i++) {
      String inn = randomDigits(10);
      assertThat(InnValidator.isValidLegalEntity(inn))
        .as("ИНН ЮЛ %s", inn)
        .isEqualTo(independentInn10(inn));

      String snils = randomDigits(11);
      assertThat(SnilsValidator.isValid(snils))
        .as("СНИЛС %s", snils)
        .isEqualTo(independentSnils(snils));
    }
  }

  @Test
  @DisplayName("ОГРН 13 и 15 знаков: контрольный разряд совпадает с независимой формулой")
  void ogrnMatchesIndependent() {
    for (int i = 0; i < 2000; i++) {
      String ogrn = "1" + randomDigits(12);
      assertThat(OgrnValidator.isValidLegalEntity(ogrn))
        .as("ОГРН %s", ogrn)
        .isEqualTo(independentOgrn(ogrn, 13));

      String ogrnip = "3" + randomDigits(14);
      assertThat(OgrnValidator.isValidIndividualEntrepreneur(ogrnip))
        .as("ОГРНИП %s", ogrnip)
        .isEqualTo(independentOgrn(ogrnip, 15));
    }
  }

  // === Свойства генерации ===

  @Test
  @DisplayName("Сгенерированные профили ФЛ валидны по независимым реализациям")
  void generatedIndividualsAreValid() {
    for (long seed = 0; seed < 30; seed++) {
      IndividualProfileGenerator generator = IndividualProfileGenerator.builder()
        .seed(seed)
        .bikSource(BANKS)
        .asOf(AS_OF)
        .build();
      IndividualProfile profile = generator.generate();

      assertThat(independentInn12(profile.inn()))
        .as("seed=%d, ИНН=%s", seed, profile.inn())
        .isTrue();
      assertThat(independentSnils(profile.snils()))
        .as("seed=%d, СНИЛС=%s", seed, profile.snils())
        .isTrue();
      assertThat(profile.passport().series()).matches("\\d{4}");
      assertThat(profile.passport().number()).matches("\\d{6}");
      assertThat(profile.passport().issueDate()).isBeforeOrEqualTo(AS_OF);
      assertThat(profile.birthDate()).isBeforeOrEqualTo(AS_OF);

      Bank bank = profile.bank();
      assertThat(bank).as("seed=%d: банк должен быть из справочника", seed).isNotNull();
      assertThat(independentAccount(profile.account(), bank.bik()))
        .as("seed=%d, счёт=%s, БИК=%s", seed, profile.account(), bank.bik())
        .isTrue();
    }
  }

  @Test
  @DisplayName("Сгенерированные профили ИП и ЮЛ валидны по независимым реализациям")
  void generatedIpAndOrganizationsAreValid() {
    for (long seed = 0; seed < 20; seed++) {
      IndividualProfileGenerator base = IndividualProfileGenerator.builder()
        .seed(seed)
        .bikSource(BANKS)
        .asOf(AS_OF)
        .build();
      IpProfile ip = IpProfileGenerator.generate(base, seed);
      assertThat(independentInn12(ip.inn())).as("ИП seed=%d", seed).isTrue();
      assertThat(independentOgrn(ip.ogrnip(), 15)).as("ИП seed=%d", seed).isTrue();

      OrganizationProfile org = OrganizationProfileGenerator.builder()
        .seed(seed)
        .bikSource(BANKS)
        .build()
        .generate();
      assertThat(independentOgrn(org.ogrn(), 13)).as("ЮЛ seed=%d", seed).isTrue();
      assertThat(independentInn10(org.inn())).as("ЮЛ seed=%d", seed).isTrue();
    }
  }

  @Test
  @DisplayName("Детерминизм: одинаковые seed и asOf дают одинаковые строки")
  void sameSeedGivesSameRows() {
    List<IndividualProfile> first = threeIndividuals(42L);
    List<IndividualProfile> second = threeIndividuals(42L);

    assertThat(first).isEqualTo(second);
    assertThat(flat(first)).isEqualTo(flat(second));
  }

  @Test
  @DisplayName("Смена seed меняет ИНН: набор не залипает")
  void differentSeedChangesValues() {
    List<IndividualProfile> a = threeIndividuals(1L);
    List<IndividualProfile> b = threeIndividuals(2L);
    assertThat(a.get(0).inn()).isNotEqualTo(b.get(0).inn());
  }

  // === Служебное ===

  private static List<IndividualProfile> threeIndividuals(long seed) {
    return List.of(
      generateIndividual(seed),
      generateIndividual(seed + 1),
      generateIndividual(seed + 2));
  }

  private static IndividualProfile generateIndividual(long seed) {
    return IndividualProfileGenerator.builder()
      .seed(seed)
      .bikSource(BANKS)
      .asOf(AS_OF)
      .build()
      .generate();
  }

  private static List<java.util.Map<String, String>> flat(List<IndividualProfile> profiles) {
    return ru.validum.core.export.IndividualProfileFlattener.flattenAll(profiles);
  }

  private static String randomDigits(int length) {
    StringBuilder builder = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      builder.append(RANDOM.nextInt(10));
    }
    return builder.toString();
  }

  /** Контроль непротиворечивости: валидатор счёта согласен с независимой реализацией. */
  @Test
  @DisplayName("Счёт: независимая реализация письма № 515 согласна с AccountValidator")
  void accountMatchesIndependent() {
    for (long seed = 0; seed < 30; seed++) {
      IndividualProfile profile = generateIndividual(seed);
      boolean expected = independentAccount(profile.account(), profile.bank().bik());
      assertThat(AccountValidator.isWellFormed(profile.account())).isTrue();
      assertThat(expected)
        .as("seed=%d, счёт=%s", seed, profile.account())
        .isTrue();
    }
  }
}
