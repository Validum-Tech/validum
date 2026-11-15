package ru.validum.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Спецификация набора из нескольких сущностей: организации, физические лица и ИП в одном прогоне,
 * со связью «человек → работодатель».
 *
 * <pre>
 * {
 *   "type": "set",
 *   "seed": 42,
 *   "asOf": "2026-11-15",
 *   "output": "out/demo-set.csv",
 *   "members": [
 *     {"name": "orgs",   "kind": "organization", "count": 2},
 *     {"name": "people", "kind": "individual",   "count": 10, "employer": "orgs"},
 *     {"name": "ip",     "kind": "ip",           "count": 2,  "employer": "orgs"}
 *   ]
 * }
 * </pre>
 */
public record SetSpec(Long seed, String asOf, String output, Boolean teardown, List<Member> members) {

  /** Писать ли рядом с набором скрипт отката ({@code <output>-teardown.sql}); по умолчанию — да. */
  public boolean teardownWanted() {
    return teardown == null || teardown;
  }

  /** Один участник набора. */
  public record Member(String name, String kind, int count, String employer) {}

  private static final ObjectMapper MAPPER = new ObjectMapper();

  /** Разбор файла; любая проблема — {@link IllegalArgumentException} с понятным текстом. */
  public static SetSpec load(Path file) {
    JsonNode root;
    try {
      root = MAPPER.readTree(file.toFile());
    } catch (Exception e) {
      throw new IllegalArgumentException("не удалось прочитать " + file + ": " + e.getMessage());
    }
    if (root == null || !root.isObject()) {
      throw new IllegalArgumentException("ожидался JSON-объект");
    }
    Long seed = root.hasNonNull("seed") ? root.get("seed").asLong() : null;
    String asOf = text(root, "asOf");
    String output = text(root, "output");
    Boolean teardown = root.hasNonNull("teardown") ? root.get("teardown").asBoolean() : null;

    JsonNode rawMembers = root.get("members");
    if (rawMembers == null || !rawMembers.isArray() || rawMembers.isEmpty()) {
      throw new IllegalArgumentException("нужен непустой массив \"members\"");
    }
    List<Member> members = new ArrayList<>();
    Set<String> names = new HashSet<>();
    for (JsonNode raw : rawMembers) {
      String name = text(raw, "name");
      if (name == null || name.isBlank()) {
        throw new IllegalArgumentException("у каждого участника набора должно быть имя \"name\"");
      }
      if (!names.add(name)) {
        throw new IllegalArgumentException("имя участника повторяется: " + name);
      }
      String kind = kind(text(raw, "kind"));
      int count = raw.hasNonNull("count") ? raw.get("count").asInt() : 1;
      if (count < 1) {
        throw new IllegalArgumentException("count у «" + name + "» должен быть хотя бы 1");
      }
      members.add(new Member(name, kind, count, text(raw, "employer")));
    }

    for (Member member : members) {
      if (member.employer() == null) {
        continue;
      }
      Member target = members.stream()
        .filter(m -> m.name().equals(member.employer()))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(
          "участник «" + member.name() + "» ссылается на несуществующего работодателя: "
            + member.employer()));
      if (!"organization".equals(target.kind())) {
        throw new IllegalArgumentException(
          "работодатель «" + target.name() + "» должен быть организацией, а он " + target.kind());
      }
    }
    return new SetSpec(seed, asOf, output, teardown, List.copyOf(members));
  }

  private static String kind(String raw) {
    if (raw == null) {
      throw new IllegalArgumentException("у участника набора нужен \"kind\"");
    }
    return switch (raw.toLowerCase(Locale.ROOT).trim()) {
      case "organization", "org", "юл" -> "organization";
      case "individual", "person", "фл" -> "individual";
      case "ip", "ип" -> "ip";
      default -> throw new IllegalArgumentException(
        "неизвестный kind «" + raw + "»: organization | individual | ip");
    };
  }

  private static String text(JsonNode node, String field) {
    JsonNode value = node.get(field);
    if (value == null || value.isNull()) {
      return null;
    }
    String text = value.asText();
    return text == null || text.isBlank() ? null : text.trim();
  }
}
