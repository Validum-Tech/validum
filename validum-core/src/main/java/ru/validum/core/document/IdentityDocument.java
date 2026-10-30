package ru.validum.core.document;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Документ, удостоверяющий личность.
 *
 * <p>Серия хранится в том виде, в каком печатается на бланке: у паспорта РФ — двумя группами
 * («45 10»), у военного билета — две заглавные буквы («НЛ»), у остальных — цифрами.
 *
 * @param type вид документа
 * @param series серия
 * @param number номер
 * @param issuedBy кем выдан
 * @param issueDate дата выдачи
 * @param expirationDate дата окончания срока; {@code null} — бессрочно или срок по возрасту
 * @param departmentCode код подразделения (только для паспорта РФ), иначе {@code null}
 */
public record IdentityDocument(
  DocumentType type,
  String series,
  String number,
  String issuedBy,
  LocalDate issueDate,
  LocalDate expirationDate,
  String departmentCode) {

  public IdentityDocument {
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(series, "series");
    Objects.requireNonNull(number, "number");
    Objects.requireNonNull(issueDate, "issueDate");
  }

  /** Серия и номер через пробел — как печатается в бланке. */
  public String seriesAndNumber() {
    return series + " " + number;
  }

  /** Серия без разделителей — для сравнения и хранения в БД. */
  public String seriesDigits() {
    return series.replace(" ", "");
  }

  /** Истёк ли документ на указанную дату (бессрочный — никогда). */
  public boolean isExpired(LocalDate today) {
    return expirationDate != null && expirationDate.isBefore(today);
  }
}
