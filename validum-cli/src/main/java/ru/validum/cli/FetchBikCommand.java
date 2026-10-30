package ru.validum.cli;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Скачивает справочник ЦБ РФ (ED807) с официального сайта и распаковывает в указанную папку.
 *
 * <p>URL: http://www.cbr.ru/s/newbik → ZIP с XML в кодировке WINDOWS-1251.
 *
 * <p>Флаг {@code --offline} — не пытается скачать (для банковских контуров, где cbr.ru
 * заблокирован). Просто печатает инструкцию и выходит с кодом 0.
 */
@Command(
  name = "fetch-bik",
  mixinStandardHelpOptions = true,
  description = "Скачать справочник БИК ЦБ РФ (ED807)"
)
public final class FetchBikCommand implements Callable<Integer> {

  private static final String URL = "http://www.cbr.ru/s/newbik";

  @Option(names = {"-o", "--output"}, defaultValue = "data/ED807.xml",
    description = "Куда сохранить (по умолчанию data/ED807.xml)")
  Path output;

  @Option(names = "--offline",
    description = "Не скачивать (для банковских контуров без доступа к cbr.ru)")
  boolean offline;

  @Override
  public Integer call() throws Exception {
    if (offline) {
      System.err.println("Режим --offline: скачивание не выполняется.");
      System.err.println("Скачайте вручную: " + URL);
      System.err.println("Справочник обязателен: генерация банковских реквизитов без него не работает.");
      return 0;
    }

    System.err.println("Скачивание справочника БИК: " + URL);
    HttpClient client = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(10))
      .followRedirects(HttpClient.Redirect.NORMAL)
      .build();
    HttpRequest request = HttpRequest.newBuilder(URI.create(URL))
      .timeout(Duration.ofSeconds(60))
      .GET().build();

    HttpResponse<InputStream> response;
    try {
      response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
    } catch (Exception e) {
      System.err.println("Ошибка сети: не удалось подключиться к " + URL);
      System.err.println("Причина: " + e.getMessage());
      System.err.println();
      System.err.println("Возможные причины:");
      System.err.println("  1. Нет доступа в интернет или cbr.ru заблокирован firewall'ом.");
      System.err.println("  2. Прокси-сервер не настроен (укажите -Dhttps.proxyHost=...).");
      System.err.println();
      System.err.println("Используйте --offline для проверки без скачивания,");
      System.err.println("или скачайте файл вручную и сохраните в " + output.toAbsolutePath());
      return 1;
    }

    if (response.statusCode() != 200) {
      System.err.println("Ошибка: HTTP " + response.statusCode() + " от " + URL);
      return 1;
    }

    Path tmpZip = Files.createTempFile("validum-bik-", ".zip");
    try (InputStream in = response.body()) {
      Files.copy(in, tmpZip, StandardCopyOption.REPLACE_EXISTING);
    }
    System.err.println("Скачано: " + Files.size(tmpZip) + " байт");

    try {
      Path parent = output.toAbsolutePath().getParent();
      if (parent != null) Files.createDirectories(parent);

      try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(tmpZip))) {
        ZipEntry entry;
        while ((entry = zip.getNextEntry()) != null) {
          if (entry.getName().toLowerCase().endsWith(".xml")) {
            try (var out = Files.newOutputStream(output)) {
              zip.transferTo(out);
            }
            System.err.println("Распаковано в: " + output.toAbsolutePath());
            break;
          }
        }
      }
    } finally {
      Files.deleteIfExists(tmpZip);
    }

    if (!Files.exists(output)) {
      System.err.println("Ошибка: в архиве не найден XML-файл");
      return 1;
    }

    System.err.println("Готово. Установите переменную окружения:");
    System.err.println("  VALIDUM_BIK_FILE=" + output.toAbsolutePath());
    return 0;
  }
}
