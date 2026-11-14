package ru.validum.core.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.bank.BikSource;
import ru.validum.core.bank.BikSources;
import ru.validum.core.organization.OrganizationProfile;
import ru.validum.core.organization.OrganizationProfileGenerator;
import ru.validum.core.profile.IndividualProfile;
import ru.validum.core.profile.IndividualProfileGenerator;

/**
 * Golden-фикстуры: {@code seed + asOf → SHA-256 набора} (W47-N1).
 *
 * <p>Идея: одно и то же зерно и одна и та же дата расчёта обязаны давать байт-в-байт одинаковый
 * набор не только в рамках одного процесса (это ловит {@code DeterminismTest}), но и между
 * версиями. Любое осознанное изменение вывода обновляет эталон: {@code
 * $env:VALIDUM_GOLDEN_UPDATE="1"; ./gradlew :validum-core:test}.
 *
 * <p>Эталон пишется в {@code src/test/resources/golden} при первом запуске — после этого прогон
 * ловит случайный дрейф справочников, форматов и порядка колонок.
 */
@DisplayName("Golden-фикстуры: seed+asOf → SHA-256 (W47-N1)")
class GoldenFixturesTest {

  private static final Path GOLDEN_DIR = Path.of("src/test/resources/golden");

  private static final String UPDATE_FLAG = "validum.golden.update";
  private static final boolean UPDATE =
    Boolean.getBoolean(UPDATE_FLAG) || "1".equals(System.getenv("VALIDUM_GOLDEN_UPDATE"));

  private static final LocalDate AS_OF = LocalDate.of(2026, 11, 1);

  private static final BikSource BANKS =
    BikSources.resolve("src/test/resources/bik-sample.xml");

  @Test
  @DisplayName("ФЛ: пять зёрен, по два профиля — стабильный SHA-256")
  void individualGolden() throws IOException {
    Map<Long, String> digests = new LinkedHashMap<>();
    Map<String, String> preview = null;
    for (long seed : List.of(0L, 1L, 42L, 1234L, 99_999L)) {
      List<IndividualProfile> profiles = new ArrayList<>();
      for (int i = 0; i < 2; i++) {
        profiles.add(IndividualProfileGenerator.builder()
          .seed(seed + i)
          .bikSource(BANKS)
          .asOf(AS_OF)
          .build()
          .generate());
      }
      digests.put(seed, digest(IndividualProfileFlattener.flattenAll(profiles)));
      if (seed == 0L) preview = IndividualProfileFlattener.flatten(profiles.get(0));
    }
    verify("individual", digests, preview);
  }

  @Test
  @DisplayName("ЮЛ: три зерна, по два профиля — стабильный SHA-256")
  void organizationGolden() throws IOException {
    Map<Long, String> digests = new LinkedHashMap<>();
    Map<String, String> preview = null;
    for (long seed : List.of(0L, 1L, 42L)) {
      List<OrganizationProfile> profiles = new ArrayList<>();
      for (int i = 0; i < 2; i++) {
        profiles.add(OrganizationProfileGenerator.builder()
          .seed(seed + i)
          .bikSource(BANKS)
          .build()
          .generate());
      }
      digests.put(seed, digest(OrganizationProfileFlattener.flattenAll(profiles)));
      if (seed == 0L) preview = OrganizationProfileFlattener.flatten(profiles.get(0));
    }
    verify("organization", digests, preview);
  }

  private void verify(String name, Map<Long, String> digests, Map<String, String> preview)
    throws IOException {
    Path file = GOLDEN_DIR.resolve(name + ".txt");
    Path previewFile = GOLDEN_DIR.resolve(name + "-preview.txt");
    Map<Long, String> expected = Files.exists(file) && !UPDATE
      ? read(file)
      : Map.of();

    StringBuilder fresh = new StringBuilder();
    for (Map.Entry<Long, String> entry : digests.entrySet()) {
      fresh.append(entry.getKey()).append('|').append(entry.getValue()).append('\n');
    }

    if (expected.isEmpty()) {
      Files.createDirectories(GOLDEN_DIR);
      Files.writeString(file, fresh.toString(), StandardCharsets.UTF_8);
      writePreview(previewFile, preview);
      return; // первая запись эталона: сверять пока не с чем
    }

    // Читаемый снимок сверяется построчно: при смене контракта git diff показывает,
    // какие именно поля изменились, а не только «хеш другой».
    assertThat(Files.exists(previewFile))
      .as("%s: нет читаемого эталона %s — пересоберите осознанно: "
        + "$env:VALIDUM_GOLDEN_UPDATE=\"1\"; ./gradlew :validum-core:test", name, previewFile.getFileName())
      .isTrue();
    assertThat(Files.readAllLines(previewFile, StandardCharsets.UTF_8))
      .as("%s: читаемый эталон разошёлся (см. %s)", name, previewFile.getFileName())
      .isEqualTo(previewLines(preview));

    for (Map.Entry<Long, String> entry : digests.entrySet()) {
      assertThat(expected.get(entry.getKey()))
        .as("%s: golden для seed=%d отсутствует в %s", name, entry.getKey(), file)
        .isNotNull();
      assertThat(entry.getValue())
        .as("%s: набор для seed=%d отклонился от эталона", name, entry.getKey())
        .isEqualTo(expected.get(entry.getKey()));
    }
  }

  private static List<String> previewLines(Map<String, String> row) {
    List<String> lines = new ArrayList<>();
    new TreeMap<>(row).forEach((key, value) -> lines.add(key + "=" + value));
    return lines;
  }

  private static void writePreview(Path file, Map<String, String> row) throws IOException {
    Files.createDirectories(file.getParent());
    Files.write(file, previewLines(row), StandardCharsets.UTF_8);
  }

  private static Map<Long, String> read(Path file) throws IOException {
    Map<Long, String> result = new TreeMap<>();
    for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
      if (line.isBlank()) continue;
      int separator = line.indexOf('|');
      result.put(Long.parseLong(line.substring(0, separator)), line.substring(separator + 1));
    }
    return result;
  }

  /** Каноничный вид набора: ключи по алфавиту, строки через {@code \\n}, SHA-256 в hex. */
  private static String digest(List<Map<String, String>> rows) {
    StringBuilder canonical = new StringBuilder();
    for (Map<String, String> row : rows) {
      TreeMap<String, String> sorted = new TreeMap<>(row);
      // personId выводится из seed и проверяется отдельно (PersonIdsTest, SeedDeterminismTest):
      // в каноничный вид его не кладём, чтобы golden-эталон описывал только данные профиля.
      sorted.remove("personId");
      sorted.forEach((key, value) ->
        canonical.append(key).append('=').append(value).append('&'));
      canonical.append('\n');
    }
    try {
      byte[] hash = MessageDigest.getInstance("SHA-256")
        .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (Exception e) {
      throw new IllegalStateException("SHA-256 недоступен", e);
    }
  }
}
