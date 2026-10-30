package ru.validum.core.bank;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Единственная точка выбора справочника участников БИК.
 *
 * <p>Цепочка одна для API и CLI: явный путь ({@code --file} или {@code validum.bik.file}) →
 * переменная окружения {@value #ENV_VAR} → системное свойство {@value #PROPERTY} →
 * {@code data/ED807.xml}. Если ничего не найдено — {@link BikSourceNotFoundException}: тихого
 * отката на демо-набор нет, потому что подмена данных в тестовом контуре дороже ошибки запуска.
 *
 * <p>Класс не пишет в консоль и не логирует сам: замечания о выборе источника передаются в
 * {@link Consumer}, чтобы CLI печатал их в stderr, а API — в лог.
 */
public final class BikSources {

  /** Переменная окружения с путём к файлу ED807. */
  public static final String ENV_VAR = "VALIDUM_BIK_FILE";

  /** Системное свойство JVM (и Spring-свойство API) с путём к файлу ED807. */
  public static final String PROPERTY = "validum.bik.file";

  /** Путь по умолчанию, если ничего не задано. */
  public static final String DEFAULT_FILE = "data/ED807.xml";

  /** Подсказка, с которой падает поиск справочника. */
  public static final String MISSING_HINT =
    "Справочник БИК не найден. Задайте файл ED807 одним из способов: параметр --file, "
      + "переменная окружения " + ENV_VAR + ", системное свойство " + PROPERTY
      + " или положите файл в data/ED807.xml. Скачать справочник ЦБ: "
      + "validum fetch-bik -o data/ED807.xml";

  private static final Consumer<String> SILENT = message -> {};

  private BikSources() {}

  /** Банки: явный путь → env → свойство → {@code data/ED807.xml}. */
  public static BikSource resolve(String explicitPath) {
    return resolve(explicitPath, SILENT);
  }

  /** Банки с замечаниями о выборе источника. */
  public static BikSource resolve(String explicitPath, Consumer<String> notices) {
    Path path = requireExisting(explicitPath, notices);
    try {
      BikSource source = BikRegistryLoader.load(path);
      notices.accept("Справочник БИК: загружено " + source.size() + " банков из " + path.toAbsolutePath());
      return source;
    } catch (IOException e) {
      throw new BikSourceNotFoundException(
        "Не удалось прочитать справочник БИК " + path.toAbsolutePath() + ": " + e.getMessage());
    }
  }

  /** Все участники ED807 (17 типов PtType). */
  public static ParticipantRegistry resolveParticipants(String explicitPath) {
    return resolveParticipants(explicitPath, SILENT);
  }

  /** Все участники с замечаниями о выборе источника. */
  public static ParticipantRegistry resolveParticipants(String explicitPath, Consumer<String> notices) {
    Path path = requireExisting(explicitPath, notices);
    try {
      ParticipantRegistry registry = BikRegistryLoader.loadRegistry(path);
      notices.accept(
        "Справочник БИК: загружено " + registry.size() + " участников из " + path.toAbsolutePath());
      return registry;
    } catch (IOException e) {
      throw new BikSourceNotFoundException(
        "Не удалось прочитать справочник БИК " + path.toAbsolutePath() + ": " + e.getMessage());
    }
  }

  /**
   * Ленивая обёртка: ищет справочник при первом обращении.
   *
   * <p>Нужна там, где создание объекта не должно падать (Spring-бин, конструкторы генераторов по
   * умолчанию): ошибка «нет справочника» приходит в момент реальной работы, а не при сборке
   * графа объектов.
   */
  public static BikSource lazy(String explicitPath, Consumer<String> notices) {
    return new BikSource() {
      private volatile BikSource delegate;

      private BikSource current() {
        BikSource local = delegate;
        if (local == null) {
          synchronized (this) {
            local = delegate;
            if (local == null) {
              local = resolve(explicitPath, notices);
              delegate = local;
            }
          }
        }
        return local;
      }

      @Override
      public List<Bank> banks() {
        return current().banks();
      }

      @Override
      public Optional<Bank> findByBik(String bik) {
        return current().findByBik(bik);
      }

      @Override
      public int size() {
        return current().size();
      }
    };
  }

  /** Ленивый источник по умолчанию (без замечаний). */
  public static BikSource defaultSource() {
    return lazy(null, SILENT);
  }

  /** Первый существующий файл цепочки; если ни одного — {@link BikSourceNotFoundException}. */
  private static Path requireExisting(String explicitPath, Consumer<String> notices) {
    Path explicit = explicitPath == null || explicitPath.isBlank()
      ? null
      : toPath(explicitPath, notices);
    if (explicit != null && Files.isRegularFile(explicit)) {
      return explicit;
    }
    if (explicit != null) {
      notices.accept("Внимание: файл справочника БИК не найден: " + explicit.toAbsolutePath());
    }

    String env = System.getenv(ENV_VAR);
    Path envPath = env == null || env.isBlank() ? null : toPath(env, notices);
    if (envPath != null && Files.isRegularFile(envPath)) {
      return envPath;
    }
    if (envPath != null) {
      notices.accept(
        "Внимание: " + ENV_VAR + " указывает на несуществующий файл: " + envPath.toAbsolutePath());
    }

    String property = System.getProperty(PROPERTY);
    Path propertyPath = property == null || property.isBlank() ? null : toPath(property, notices);
    if (propertyPath != null && Files.isRegularFile(propertyPath)) {
      return propertyPath;
    }
    if (propertyPath != null) {
      notices.accept(
        "Внимание: " + PROPERTY + " указывает на несуществующий файл: " + propertyPath.toAbsolutePath());
    }

    Path fallback = toPath(DEFAULT_FILE, notices);
    if (fallback != null && Files.isRegularFile(fallback)) {
      return fallback;
    }
    throw new BikSourceNotFoundException(MISSING_HINT);
  }

  private static Path toPath(String value, Consumer<String> notices) {
    try {
      return Path.of(value);
    } catch (InvalidPathException e) {
      notices.accept("Внимание: некорректный путь к справочнику БИК: " + value);
      return null;
    }
  }
}
