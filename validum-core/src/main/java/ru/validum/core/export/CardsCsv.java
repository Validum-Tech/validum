package ru.validum.core.export;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ru.validum.core.card.MirCard;

/**
 * Таблица карт «Мир» для режима {@code --cards N} (N больше единицы): одна строка — одна карта,
 * связь с человеком через {@code personId}.
 */
public final class CardsCsv {

  /** Заголовок таблицы. */
  public static final List<String> SPLIT_HEADER =
    List.of("personId", "pan", "expiry", "outcome");

  private CardsCsv() {}

  /** Строки таблицы для одного клиента. */
  public static List<Map<String, String>> toRows(String personId, List<MirCard> cards) {
    List<Map<String, String>> rows = new ArrayList<>();
    if (cards == null) {
      return rows;
    }
    for (MirCard card : cards) {
      Map<String, String> row = new LinkedHashMap<>();
      row.put("personId", personId == null ? "" : personId);
      row.put("pan", card.pan());
      row.put("expiry", card.expiry());
      row.put("outcome", card.outcome().russianName());
      rows.add(row);
    }
    return rows;
  }
}
