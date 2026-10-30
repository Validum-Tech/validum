package ru.validum.core.name;

/**
 * ФИО человека.
 *
 * @param lastName фамилия
 * @param firstName имя
 * @param patronymic отчество
 * @param gender пол
 */
public record PersonName(String lastName, String firstName, String patronymic, Gender gender) {

  /** Полное ФИО в формате «Фамилия Имя Отчество». */
  public String fullName() {
    return lastName + " " + firstName + " " + patronymic;
  }
}
