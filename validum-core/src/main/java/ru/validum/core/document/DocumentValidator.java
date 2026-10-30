package ru.validum.core.document;

import java.util.regex.Pattern;

/**
 * Проверка документа на соответствие маске своего вида.
 *
 * <p>Проверяет только формат (длина, алфавит, код подразделения, порядок дат) — не подлинность:
 * для синтетических данных этого достаточно, а сверка с реестром бланков не входит в задачу.
 */
public final class DocumentValidator {

  private static final Pattern DIGITS = Pattern.compile("\\d+");
  private static final Pattern LETTERS = Pattern.compile("[А-ЯЁ]+");
  private static final Pattern DEPARTMENT_CODE = Pattern.compile("\\d{3}-\\d{3}");

  private DocumentValidator() {}

  /** Соответствует ли документ маске своего вида. */
  public static boolean isValid(IdentityDocument document) {
    if (document == null || document.type() == null) {
      return false;
    }
    DocumentType type = document.type();
    if (!isValidSeries(type, document.series())) {
      return false;
    }
    if (!isFixedLengthDigits(document.number(), type.numberLength())) {
      return false;
    }
    if (document.issueDate() == null) {
      return false;
    }
    if (document.expirationDate() != null
      && !document.expirationDate().isAfter(document.issueDate())) {
      return false;
    }
    return !type.hasDepartmentCode()
      || (document.departmentCode() != null
        && DEPARTMENT_CODE.matcher(document.departmentCode()).matches());
  }

  /** Соответствует ли серия маске вида (с разделителем или без). */
  public static boolean isValidSeries(DocumentType type, String series) {
    if (type == null || series == null || series.isEmpty()) {
      return false;
    }
    String compact = series.replace(" ", "");
    if (compact.length() != type.seriesLength()) {
      return false;
    }
    if (type.seriesLetters()) {
      return LETTERS.matcher(compact).matches();
    }
    if (!DIGITS.matcher(compact).matches()) {
      return false;
    }
    if (type.groupedSeries()) {
      return series.equals(compact)
        || series.matches("\\d{2} \\d{" + (type.seriesLength() - 2) + "}");
    }
    return series.equals(compact);
  }

  private static boolean isFixedLengthDigits(String value, int length) {
    return value != null && value.length() == length && DIGITS.matcher(value).matches();
  }
}
