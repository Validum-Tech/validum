# Релиз Validum

Как выпустить версию: от правки `CHANGELOG.md` до артефакта в Maven Central.

## Что где публикуется

| Куда | Что | Как |
|---|---|---|
| GitHub Release | `validum-<версия>.jar` + `.sha256` | автоматически по тегу `v*` |
| GitHub Packages | `validum-core`, `validum-allure` (jar + sources + javadoc) | автоматически по тегу `v*` |

В Maven идут только **две библиотеки**. `validum-cli` и `validum-api` — приложения: они
распространяются готовыми артефактами (fat jar / boot-jar / Docker-образ), а не как зависимости.
| Maven Central | те же четыре артефакта | ручной шаг, см. ниже |
| Docker Hub | `validum/validum-api` | пока вручную, автоматизация — после 0.2 |

## Перед релизом

1. **Проверить цифры.** Тесты, покрытие, число эндпоинтов и команд должны совпадать
   в `README.md`, `docs/SCENARIOS.md`, `docs/ROADMAP.md`, `CHANGELOG.md`.
   Источник правды — `*/build/test-results/test/*.xml` и `*/build/reports/jacoco/test/*.csv`, не память.
2. **`CHANGELOG.md`** — секция `## [X.Y.Z] — ГГГГ-ММ-ДД`. Из неё берётся тело GitHub Release.
3. **Версия** — `version = "X.Y.Z"` в корневом `build.gradle.kts`.
4. **Сборка локально:**

   ```bash
   ./gradlew build
   ./gradlew publishToMavenLocal --no-configuration-cache   # maven-publish и CC пока конфликтуют
   java -jar validum-cli/build/libs/validum.jar version
   ```

5. **Дымовой прогон** на чистом окружении: `fetch-bik`, `generate individual`, `validate`,
   `export ed807`, `--format sql`.

## Тег и CI

```bash
git tag -a v0.1.0 -m "Validum 0.1.0"
git push origin v0.1.0
```

`release.yml` (Actions → Release) соберёт проект, прогонит тесты, опубликует пакеты
в GitHub Packages и создаст GitHub Release с jar и контрольной суммой.

**Секреты репозитория:**

| Секрет | Обязателен | Зачем |
|---|---|---|
| `GITHUB_TOKEN` | да (выдаётся сам) | GitHub Packages + Release |
| `SIGNING_KEY` | для Central | armored PGP-ключ (`gpg --armor --export-secret-keys`) |
| `SIGNING_PASSWORD` | для Central | пароль ключа |
| `MAVEN_USER` / `MAVEN_PASSWORD` | для Central | токен Central Portal |

Без `SIGNING_KEY` сборка проходит, но артефакты не подписаны — Central такие не примет.

## Maven Central (ручной шаг для 0.1.0)

groupId `io.github.validum-tech` подтверждается через GitHub-организацию
`Validum-Tech` при первом релизе в [Central Portal](https://central.sonatype.com/).

1. `./gradlew publishToMavenLocal --no-configuration-cache` на машине с ключом — артефакты и `.asc` появятся
   в `~/.m2/repository/io/github/validum-tech/`.
2. Собрать bundle: каталог `io/github/validum-tech/**` с jar, sources, javadoc, pom и подписями
   упаковать в zip.
3. Central Portal → **Publish** → загрузить zip → дождаться проверки (обычно 15–60 минут).

Дальше имеет смысл подключить `com.gradleup.nmcp` (Central Portal Publisher) и перевести
этот шаг в CI — тогда публикация станет полностью автоматической.

## Версионирование

- SemVer: `MAJOR` — несовместимый API, `MINOR` — новые возможности, `PATCH` — исправления.
- Пока не вышли из 0.x: ломающие изменения допустимы в `MINOR`, но обязаны быть в `CHANGELOG.md`.
- `-SNAPSHOT` в `main` после релиза не возвращаем: версия поднимается сразу, тег ставится на коммит.

## Откат

- Неверный тег: `git tag -d vX.Y.Z && git push --delete origin vX.Y.Z`, затем пересобрать.
- Ошибочный релиз в Central не удаляется. Выпускаем `PATCH` с исправлением и помечаем старую
  версию в `CHANGELOG.md` как «не использовать».
