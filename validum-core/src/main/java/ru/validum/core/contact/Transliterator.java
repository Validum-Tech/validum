package ru.validum.core.contact;

import java.util.Map;

/**
 * Транслитератор кириллицы в латиницу по ГОСТ Р 52535.1-2006 (паспортный стандарт).
 *
 * <p>Используется для генерации email из ФИО. Например, {@code Щербаков} → {@code Shcherbakov}.
 *
 * <p>Особенности:
 *
 * <ul>
 *   <li>Твёрдый и мягкий знаки отбрасываются (как в загранпаспорте).
 *   <li>Символы, не входящие в кириллический алфавит (цифры, дефисы, пробелы), сохраняются
 *       как есть.
 * </ul>
 */
public final class Transliterator {

  private static final Map<Character, String> MAP =
    Map.ofEntries(
      Map.entry('А', "A"), Map.entry('а', "a"),
      Map.entry('Б', "B"), Map.entry('б', "b"),
      Map.entry('В', "V"), Map.entry('в', "v"),
      Map.entry('Г', "G"), Map.entry('г', "g"),
      Map.entry('Д', "D"), Map.entry('д', "d"),
      Map.entry('Е', "E"), Map.entry('е', "e"),
      Map.entry('Ё', "E"), Map.entry('ё', "e"),
      Map.entry('Ж', "Zh"), Map.entry('ж', "zh"),
      Map.entry('З', "Z"), Map.entry('з', "z"),
      Map.entry('И', "I"), Map.entry('и', "i"),
      Map.entry('Й', "I"), Map.entry('й', "i"),
      Map.entry('К', "K"), Map.entry('к', "k"),
      Map.entry('Л', "L"), Map.entry('л', "l"),
      Map.entry('М', "M"), Map.entry('м', "m"),
      Map.entry('Н', "N"), Map.entry('н', "n"),
      Map.entry('О', "O"), Map.entry('о', "o"),
      Map.entry('П', "P"), Map.entry('п', "p"),
      Map.entry('Р', "R"), Map.entry('р', "r"),
      Map.entry('С', "S"), Map.entry('с', "s"),
      Map.entry('Т', "T"), Map.entry('т', "t"),
      Map.entry('У', "U"), Map.entry('у', "u"),
      Map.entry('Ф', "F"), Map.entry('ф', "f"),
      Map.entry('Х', "Kh"), Map.entry('х', "kh"),
      Map.entry('Ц', "Ts"), Map.entry('ц', "ts"),
      Map.entry('Ч', "Ch"), Map.entry('ч', "ch"),
      Map.entry('Ш', "Sh"), Map.entry('ш', "sh"),
      Map.entry('Щ', "Shch"), Map.entry('щ', "shch"),
      Map.entry('Ъ', ""), Map.entry('ъ', ""),
      Map.entry('Ы', "Y"), Map.entry('ы', "y"),
      Map.entry('Ь', ""), Map.entry('ь', ""),
      Map.entry('Э', "E"), Map.entry('э', "e"),
      Map.entry('Ю', "Yu"), Map.entry('ю', "yu"),
      Map.entry('Я', "Ya"), Map.entry('я', "ya"));

  private Transliterator() {}

  /**
   * Транслитерирует строку.
   *
   * @param input исходная строка (кириллица, цифры, спецсимволы)
   * @return транслитерированная строка (латиница + неизменённые символы)
   */
  public static String transliterate(String input) {
    if (input == null || input.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder(input.length() * 2);
    for (char c : input.toCharArray()) {
      String mapped = MAP.get(c);
      if (mapped != null) {
        sb.append(mapped);
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }
}
