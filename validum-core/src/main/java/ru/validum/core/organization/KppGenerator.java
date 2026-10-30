package ru.validum.core.organization;

import java.util.Random;
import ru.validum.core.common.Region;
import ru.validum.core.random.DataGenerator;

/**
 * Генератор КПП (код причины постановки на учёт).
 *
 * <p>Формат: {@code NNNNPPZZZ} — 9 цифр.
 *
 * <ul>
 *   <li>{@code NNNN} — код налогового органа: 2 цифры код региона + 2 цифры код ИФНС;
 *   <li>{@code PP} — причина постановки (01 — по месту нахождения, 43 — филиал, ...);
 *   <li>{@code ZZZ} — порядковый номер постановки (001–999).
 * </ul>
 *
 * <p>Первые 2 цифры КПП всегда совпадают с кодом региона из адреса регистрации.
 */
public final class KppGenerator implements DataGenerator {

  /** Код причины: постановка по месту нахождения ЮЛ. */
  public static final String REASON_AT_LOCATION = "01";

  /** Код причины: постановка по месту нахождения обособленного подразделения. */
  public static final String REASON_BRANCH = "43";

  private final Random random;

  public KppGenerator() {
    this(DataGenerator.defaultRandom());
  }

  public KppGenerator(Random random) {
    this.random = random;
  }

  /** КПП для указанного региона, причина «по месту нахождения». */
  public String generate(Region region) {
    return generate(region, REASON_AT_LOCATION);
  }

  /** КПП с указанной причиной постановки. */
  public String generate(Region region, String reason) {
    if (region == null) {
      throw new IllegalArgumentException("Регион не может быть null");
    }
    if (reason == null || reason.length() != 2) {
      throw new IllegalArgumentException("Причина постановки должна быть 2 цифры: " + reason);
    }

    // NNNN: код региона (2) + код ИФНС (2). Код ИФНС — от 01 до 99.
    String inspectionCode = String.format("%02d", 1 + random.nextInt(99));
    String nnnn = region.code() + inspectionCode;

    // ZZZ: порядковый номер, 001-999
    String serial = String.format("%03d", 1 + random.nextInt(999));

    return nnnn + reason + serial;
  }
}
