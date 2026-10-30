package ru.validum.core.passport;

import java.time.LocalDate;
import ru.validum.core.common.Country;

/**
 * Паспорт.
 *
 * @param series серия (4 цифры для РФ; синтетическая для иностранного)
 * @param number номер (6 цифр)
 * @param issuedBy кем выдан
 * @param issueDate дата выдачи
 * @param expirationDate дата окончания (для бессрочного — через 100 лет)
 * @param departmentCode код подразделения (формат XXX-XXX)
 * @param status «Действительный» / «Просрочен»
 * @param type тип паспорта (РФ / иностранный)
 * @param country страна документа (RU для РФ, страна из residency для иностранного)
 */
public record Passport(
  String series,
  String number,
  String issuedBy,
  LocalDate issueDate,
  LocalDate expirationDate,
  String departmentCode,
  String status,
  PassportType type,
  Country country) {}
