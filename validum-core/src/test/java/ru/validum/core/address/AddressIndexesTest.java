package ru.validum.core.address;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Индексы адресов — не случайные шесть цифр: они приходят из DOMA.DBF (реальные дома), а
 * аварийный путь обязан оставаться в границах своего субъекта.
 */
@DisplayName("Индексы адресов: субъект и почтовый индекс согласованы")
class AddressIndexesTest {

  @Test
  @DisplayName("В срезе индексы населённых пунктов начинаются с префикса своего субъекта")
  void placeIndexesMatchSubject() throws IOException {
    Map<String, String[]> prefixes = prefixes();
    for (String code : List.of("63", "16", "50", "02")) {
      List<AddressBook.Place> places = AddressBook.places(code);
      long checked = 0;
      for (AddressBook.Place place : places) {
        String index = place.index();
        if (index == null || index.isBlank()) {
          continue;
        }
        checked++;
        assertThat(prefixes.get(code))
          .as("для субъекта %s нет карты префиксов", code)
          .isNotNull();
        assertThat(index.substring(0, 3))
          .as("индекс %s в %s должен принадлежать субъекту %s", index, place.name(), code)
          .isIn((Object[]) prefixes.get(code));
      }
      assertThat(checked).as("в субъекте %s есть индексы", code).isGreaterThan(100);
    }
  }

  @Test
  @DisplayName("Аварийный индекс держится в границах субъекта и остаётся шестизначным")
  void proceduralIndexStaysInSubject() throws IOException {
    Map<String, String[]> prefixes = prefixes();
    Random random = new Random(7);
    for (String code : List.of("63", "77", "50")) {
      for (int i = 0; i < 200; i++) {
        String index = AddressBook.proceduralIndex(random, code);
        assertThat(index).hasSize(6).containsOnlyDigits();
        assertThat(index.substring(0, 3)).isIn((Object[]) prefixes.get(code));
      }
    }
  }

  @Test
  @DisplayName("Для неизвестного субъекта аварийный индекс остаётся случайным")
  void proceduralIndexForUnknownSubject() {
    Random random = new Random(1);
    for (int i = 0; i < 50; i++) {
      assertThat(AddressBook.proceduralIndex(random, "99")).hasSize(6).containsOnlyDigits();
    }
  }

  private static Map<String, String[]> prefixes() throws IOException {
    try (InputStream in = AddressBook.class.getResourceAsStream("/addresses/index-prefixes.tsv")) {
      assertThat(in).as("ресурс index-prefixes.tsv").isNotNull();
      Map<String, String[]> map = new HashMap<>();
      try (BufferedReader reader =
             new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          int tab = line.indexOf('\t');
          map.put(line.substring(0, tab), line.substring(tab + 1).split(","));
        }
      }
      return map;
    }
  }
}
