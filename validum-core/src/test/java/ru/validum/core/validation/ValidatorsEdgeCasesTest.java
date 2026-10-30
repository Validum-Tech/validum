package ru.validum.core.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.bank.Bank;
import ru.validum.core.bank.BikSource;

/**
 * Граничные случаи валидаторов: null, пустые строки, неверная длина и нецифровые символы.
 *
 * <p>Проверки формата и контрольных сумм лежат в отдельных тестах; здесь — «мусорный вход», на
 * котором валидаторы обязаны отвечать {@code false}, а не падать.
 */
@DisplayName("Валидаторы: граничные случаи")
class ValidatorsEdgeCasesTest {

  private static final String INN_LEGAL = "7707083893";
  private static final String INN_INDIVIDUAL = "500100732259";
  private static final String SNILS = "11223344595";
  private static final String OGRN = "1027700132195";
  private static final String OGRNIP = "304500116000157";
  private static final String OKPO_LEGAL = "00738545";
  private static final String OKPO_IP = "0003251672";
  private static final String KPP = "773601001";
  private static final String BIK = "044525225";

  @Test
  @DisplayName("ИНН: null и пустые строки дают false во всех трёх методах")
  void innNulls() {
    assertThat(InnValidator.isValid(null)).isFalse();
    assertThat(InnValidator.isValid("")).isFalse();
    assertThat(InnValidator.isValid("   ")).isFalse();
    assertThat(InnValidator.isValidIndividual(null)).isFalse();
    assertThat(InnValidator.isValidLegalEntity(null)).isFalse();
    assertThat(InnValidator.isValid("77070838A3")).isFalse();
  }

  @Test
  @DisplayName("ИНН: эталонные значения проходят нужный метод и не проходят чужой")
  void innValidButWrongKind() {
    assertThat(InnValidator.isValid(INN_LEGAL)).isTrue();
    assertThat(InnValidator.isValid(INN_INDIVIDUAL)).isTrue();
    assertThat(InnValidator.isValidLegalEntity(INN_INDIVIDUAL)).isFalse();
    assertThat(InnValidator.isValidIndividual(INN_LEGAL)).isFalse();
  }

  @Test
  @DisplayName("СНИЛС: null, пустая строка и неверная длина дают false")
  void snilsNulls() {
    assertThat(SnilsValidator.isValid(null)).isFalse();
    assertThat(SnilsValidator.isValid("")).isFalse();
    assertThat(SnilsValidator.isValid("1122334459")).isFalse();
    assertThat(SnilsValidator.isValid("112233445955")).isFalse();
    assertThat(SnilsValidator.isValid("1122334459A")).isFalse();
    assertThat(SnilsValidator.isValid(SNILS)).isTrue();
  }

  @Test
  @DisplayName("ОГРН: null, пустая строка и длины 12/14 дают false")
  void ogrnNulls() {
    assertThat(OgrnValidator.isValid(null)).isFalse();
    assertThat(OgrnValidator.isValid("")).isFalse();
    assertThat(OgrnValidator.isValid(OGRN.substring(1))).isFalse();
    assertThat(OgrnValidator.isValid(OGRN + "0")).isFalse();
    assertThat(OgrnValidator.isValid(OGRN)).isTrue();
  }

  @Test
  @DisplayName("ОГРНИП: null, пустая строка и длины 14/16 дают false")
  void ogrnipNulls() {
    assertThat(OgrnipValidator.isValid(null)).isFalse();
    assertThat(OgrnipValidator.isValid("")).isFalse();
    assertThat(OgrnipValidator.isValid(OGRNIP.substring(1))).isFalse();
    assertThat(OgrnipValidator.isValid(OGRNIP + "0")).isFalse();
    assertThat(OgrnipValidator.isValid(OGRNIP)).isTrue();
  }

  @Test
  @DisplayName("ОКПО: null и длины 9/11 дают false, 8 и 10 определяются автоматически")
  void okpoDispatchAndNulls() {
    assertThat(OkpoValidator.isValid(null)).isFalse();
    assertThat(OkpoValidator.isValid("")).isFalse();
    assertThat(OkpoValidator.isValid("123456789")).isFalse();
    assertThat(OkpoValidator.isValid("12345678901")).isFalse();
    assertThat(OkpoValidator.isValid(OKPO_LEGAL)).isTrue();
    assertThat(OkpoValidator.isValid(OKPO_IP)).isTrue();
    assertThat(OkpoValidator.isValidLegalEntity(OKPO_IP)).isFalse();
    assertThat(OkpoValidator.isValidIndividualEntrepreneur(OKPO_LEGAL)).isFalse();
  }

  @Test
  @DisplayName("КПП: null, пустая строка и длины 8/10 дают false")
  void kppNulls() {
    assertThat(KppValidator.isValid(null)).isFalse();
    assertThat(KppValidator.isValid("")).isFalse();
    assertThat(KppValidator.isValid("77360100")).isFalse();
    assertThat(KppValidator.isValid("7736010010")).isFalse();
    assertThat(KppValidator.isValid(KPP)).isTrue();
  }

  @Test
  @DisplayName("БИК: проверяется формат и наличие в справочнике")
  void bikFormatAndLookup() {
    Bank bank = new Bank(BIK, "ПАО Сбербанк", "30101810400000000225", "Москва", "Москва");
    BikSource source = new BikSource() {
      @Override
      public List<Bank> banks() {
        return List.of(bank);
      }

      @Override
      public Optional<Bank> findByBik(String bik) {
        return banks().stream().filter(b -> b.bik().equals(bik)).findFirst();
      }
    };
    BikValidator validator = new BikValidator(source);

    assertThat(validator.isValid(null)).isFalse();
    assertThat(validator.isValid("")).isFalse();
    assertThat(validator.isValid("44525225")).isFalse();
    assertThat(validator.isValid("0445252255")).isFalse();
    assertThat(validator.isValid("04452522A")).isFalse();
    assertThat(validator.isValid("999999999")).isFalse();
    assertThat(validator.isValid(BIK)).isTrue();
    assertThat(validator.getBank(BIK)).isEqualTo(bank);
    assertThat(validator.getBank("999999999")).isNull();
  }
}
