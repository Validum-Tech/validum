package ru.validum.core.sql;

import java.util.ArrayList;
import java.util.List;

/**
 * Сценарий отката: {@code DELETE} по уже вставленным строкам, в порядке, обратном загрузке.
 *
 * <p>Никакого DDL: таблицы создаются и удаляются снаружи, скрипт лишь убирает строки, которые
 * создал прогон, — чтобы после проверки стенд остался в том же состоянии, что был до загрузки.
 */
public final class TeardownSql {

  /** Одна таблица: имя, колонка-ключ и значения ключей из этого прогона. */
  public record Table(String table, String keyColumn, List<String> keys) {

    public Table {
      keys = List.copyOf(keys);
    }
  }

  /** Сколько ключей попадает в один {@code DELETE … IN (…)}. */
  public static final int CHUNK = 200;

  private TeardownSql() {}

  /** Будущие строки: порядок как при загрузке (родители раньше детей). */
  public static String build(String header, List<Table> tables) {
    StringBuilder sb = new StringBuilder();
    sb.append("-- ").append(header).append('\n');
    sb.append("-- Порядок удаления — обратный порядку загрузки.\n\n");
    for (int i = tables.size() - 1; i >= 0; i--) {
      Table table = tables.get(i);
      for (List<String> chunk : chunks(table.keys())) {
        sb.append("DELETE FROM ").append(table.table())
          .append(" WHERE ").append(table.keyColumn()).append(" IN (")
          .append(String.join(", ", chunk.stream().map(TeardownSql::quote).toList()))
          .append(");\n");
      }
    }
    return sb.toString();
  }

  private static List<List<String>> chunks(List<String> keys) {
    List<List<String>> result = new ArrayList<>();
    for (int start = 0; start < keys.size(); start += CHUNK) {
      result.add(keys.subList(start, Math.min(start + CHUNK, keys.size())));
    }
    return result;
  }

  private static String quote(String value) {
    return "'" + value.replace("'", "''") + "'";
  }
}
