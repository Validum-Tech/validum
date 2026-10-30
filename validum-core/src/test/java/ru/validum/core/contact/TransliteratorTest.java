package ru.validum.core.contact;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Transliterator — ГОСТ Р 52535.1-2006")
class TransliteratorTest {

  @ParameterizedTest(name = "{0} → {1}")
  @CsvSource({
    "Петров, Petrov",
    "Щербаков, Shcherbakov",
    "Емельянов, Emelyanov",
    "Юрьев, Yurev",
    "Юрьевич, Yurevich",
    "Иванов, Ivanov",
    "Александрович, Aleksandrovich",
    "Подъездный, Podezdnyi",
    "ИВАНОВ, IVANOV",
    "иванов, ivanov"
  })
  @DisplayName("Транслитерация фамилий по паспортному стандарту")
  void transliterateStandardNames(String input, String expected) {
    assertThat(Transliterator.transliterate(input)).isEqualTo(expected);
  }

  @Test
  @DisplayName("Твёрдый и мягкий знаки отбрасываются")
  void dropHardAndSoftSigns() {
    assertThat(Transliterator.transliterate("Подъезд")).isEqualTo("Podezd");
    assertThat(Transliterator.transliterate("Мельник")).isEqualTo("Melnik");
    assertThat(Transliterator.transliterate("ЪЬъь")).isEmpty();
  }

  @Test
  @DisplayName("Некириллические символы сохраняются без изменений")
  void keepNonCyrillicCharacters() {
    assertThat(Transliterator.transliterate("Иванов-Петров")).isEqualTo("Ivanov-Petrov");
    assertThat(Transliterator.transliterate("Тест 123")).isEqualTo("Test 123");
  }

  @Test
  @DisplayName("Null и пустая строка → пустая строка")
  void nullAndEmptyReturnEmptyString() {
    assertThat(Transliterator.transliterate(null)).isEmpty();
    assertThat(Transliterator.transliterate("")).isEmpty();
  }
}
