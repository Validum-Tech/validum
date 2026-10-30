package ru.validum.core.inn;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import ru.validum.core.random.DataGenerator;

/**
 * Генератор ИНН физических и юридических лиц РФ.
 *
 * <p>Гарантирует корректность контрольных разрядов по алгоритму ФНС: две контрольные цифры для
 * ИНН физлица (12 цифр), одна — для ИНН юрлица (10 цифр).
 */
public final class InnGenerator implements DataGenerator {

  private final Random random;

  public InnGenerator() {
    this(DataGenerator.defaultRandom());
  }

  public InnGenerator(Random random) {
    this.random = random;
  }

  /** Генерирует ИНН физического лица (12 цифр). */
  public String generateIndividual() {
    List<Integer> digits = new ArrayList<>(randomInnDigits(111111, 888888, 6));
    digits.add(calculateControlDigit(digits, InnControlType.FL_11));
    digits.add(calculateControlDigit(digits, InnControlType.FL_12));
    return joinDigits(digits);
  }

  /** Генерирует ИНН юридического лица (10 цифр). */
  public String generateLegalEntity() {
    List<Integer> digits = new ArrayList<>(randomInnDigits(11111, 88888, 5));
    digits.add(calculateControlDigit(digits, InnControlType.UL_10));
    return joinDigits(digits);
  }

  private enum InnControlType {
    FL_11(List.of(7, 2, 4, 10, 3, 5, 9, 4, 6, 8)),
    FL_12(List.of(3, 7, 2, 4, 10, 3, 5, 9, 4, 6, 8)),
    UL_10(List.of(2, 4, 10, 3, 5, 9, 4, 6, 8));

    private final List<Integer> coefficients;

    InnControlType(List<Integer> coefficients) {
      this.coefficients = coefficients;
    }
  }

  private List<Integer> randomInnDigits(int recordMin, int recordMax, int recordWidth) {
    int region = 10 + random.nextInt(75);
    int inspection = 10 + random.nextInt(89);
    int record = recordMin + random.nextInt(recordMax - recordMin);
    String raw = String.format("%02d%02d%0" + recordWidth + "d", region, inspection, record);
    return toDigits(raw);
  }

  private static int calculateControlDigit(List<Integer> digits, InnControlType type) {
    int sum = 0;
    for (int i = 0; i < type.coefficients.size(); i++) {
      sum += digits.get(i) * type.coefficients.get(i);
    }
    return (sum % 11) % 10;
  }

  private static List<Integer> toDigits(String raw) {
    List<Integer> digits = new ArrayList<>(raw.length());
    for (char c : raw.toCharArray()) {
      digits.add(c - '0');
    }
    return digits;
  }

  private static String joinDigits(List<Integer> digits) {
    return digits.stream().map(String::valueOf).collect(Collectors.joining());
  }
}
