package ru.validum.core.okved;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/**
 * Справочник ОКВЭД-2 (ОК 029-2014, КДЕС Ред. 2) — 3069 позиций.
 *
 * <p>Данные лежат ресурсом {@code okved.tsv} рядом с классом: код, наименование, раздел. Загрузка
 * ленивая и однократная; порядок сохраняется как в классификаторе, чтобы результат был предсказуем.
 *
 * <p>Нулевых зависимостей: только JDK.
 */
public final class OkvedRegistry {

  /** Имя ресурса со справочником. */
  static final String RESOURCE = "/okved.tsv";

  private static final class Holder {
    private static final List<OkvedEntry> ALL = load();
    private static final Map<String, OkvedEntry> BY_CODE = index(ALL);
  }

  private OkvedRegistry() {}

  /** Все позиции классификатора в порядке следования. */
  public static List<OkvedEntry> all() {
    return Holder.ALL;
  }

  /** Поиск по коду; {@code 62.01} и {@code 62.01 } эквивалентны. */
  public static Optional<OkvedEntry> byCode(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    return Optional.ofNullable(Holder.BY_CODE.get(code.trim()));
  }

  /** Требует существующий код, иначе {@link IllegalArgumentException} с подсказкой. */
  public static OkvedEntry require(String code) {
    return byCode(code)
      .orElseThrow(
        () ->
          new IllegalArgumentException(
            "ОКВЭД не найден в справочнике: '" + code + "'. Примеры: 62.01, 41.20, 47.11."));
  }

  /** Случайная позиция — из диапазона {@link Random}. */
  public static OkvedEntry random(Random random) {
    List<OkvedEntry> all = Holder.ALL;
    return all.get(random.nextInt(all.size()));
  }

  /** Количество позиций в справочнике. */
  public static int size() {
    return Holder.ALL.size();
  }

  private static Map<String, OkvedEntry> index(List<OkvedEntry> entries) {
    Map<String, OkvedEntry> map = new LinkedHashMap<>(entries.size() * 2);
    for (OkvedEntry e : entries) {
      map.put(e.code(), e);
    }
    return Collections.unmodifiableMap(map);
  }

  private static List<OkvedEntry> load() {
    InputStream in = OkvedRegistry.class.getResourceAsStream(RESOURCE);
    if (in == null) {
      throw new IllegalStateException("Ресурс справочника ОКВЭД не найден: " + RESOURCE);
    }
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
      List<OkvedEntry> entries = new ArrayList<>(3200);
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.isEmpty() || line.charAt(0) == '#') {
          continue;
        }
        String[] parts = line.split("\t", 3);
        if (parts.length < 3) {
          continue;
        }
        entries.add(new OkvedEntry(parts[0], parts[1], parts[2]));
      }
      return List.copyOf(entries);
    } catch (IOException e) {
      throw new UncheckedIOException("Не удалось прочитать справочник ОКВЭД", e);
    }
  }
}
