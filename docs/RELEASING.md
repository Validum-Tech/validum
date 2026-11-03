# Релиз Validum

Как выпустить версию: от правки `CHANGELOG.md` до артефакта в Maven Central.

## Что где публикуется

| Куда | Что | Как |
|---|---|---|
| GitHub Release | `validum-<версия>.jar` + `.sha256` | автоматически по тегу `v*` |
| GitHub Packages | `validum-core`, `validum-api`, `validum-cli`, `validum-allure` (jar + sources + javadoc) | автоматически по тегу `v*` |
| Maven Central | те же четыре артефакта | ручной шаг, см. ниже |
| Docker Hub | `validum/validum-api` | пока вручную, автоматизация — после 0.2 |

## Перед релизом

1. **Проверить цифры.** Тесты, покрытие, число эндпоинтов и команд должны совпадать
   в `README.md`, `docs/ROADMAP.md`, `CHANGELOG.md`.
   Источник правды — `*/build/test-results/test/*.xml` и `*/build/reports/jacoco/test/*.csv`, не память.
2. **`CHANGELOG.md`** — секция `## [X.Y.Z] — ГГГГ-ММ-ДД`. Из неё берётся тело GitHub Release.
3. **Версия** — `version=X.Y.Z` в `gradle.properties` (единственное место).
4. **Сборка локально:**

   ```bash
   ./gradlew build publishToMavenLocal
   java -jar validum-cli/build/libs/validum.jar version
   ```

5. **Дымовой прогон** на чистом окружении: `fetch-bik`, `generate individual`, `validate`,
   `export ed807`, `--format sql`.

## Тег и CI

```bash
git tag -a v0.1.0 -m "Validum 0.1.0"
git push origin v0.1.0
```

`release.yml` (Actions → Release) соберёт проект, прогонит тесты, опубликует
`validum-core` и `validum-allure` в **Maven Central** и создаст GitHub Release с jar и
контрольной суммой.

**Секреты репозитория:**

| Секрет | Обязателен | Зачем |
|---|---|---|
| `CENTRAL_USERNAME` | для Central | username из токена Central Portal |
| `CENTRAL_TOKEN` | для Central | пароль из токена Central Portal |
| `SIGNING_KEY` | для Central | armored PGP-ключ (`gpg --armor --export-secret-keys`) |
| `SIGNING_PASSWORD` | для Central | пароль ключа |

Если секреты не заданы, шаг публикации в Central пропускается: сборка, тесты, релиз и jar
проходят как обычно, просто пакета в реестре не появится. Порядок получения ключей и токена —
внутренняя инструкция ведётся в `validum-dev` (внутри публичного репозитория её нет).

## Maven Central

Публикация автоматическая: `./gradlew publishToMavenCentral` (плагин `com.vanniktech.maven.publish`,
`SonatypeHost.CENTRAL_PORTAL`, авторелиз). Добавлять `validum-core` в свой проект можно без
подключения репозитория — Central есть по умолчанию:

```kotlin
testImplementation("io.github.validum-tech:validum-core:0.2.0")
```

Что проверить после релиза: Central Portal → **Deployments** (статус `PUBLISHED`) и
<https://repo1.maven.org/maven2/io/github/validum-tech/validum-core/> (синхронизация — от
10 минут до пары часов). Локально без ключей: `./gradlew publishToMavenLocal`.

## Версионирование

- SemVer: `MAJOR` — несовместимый API, `MINOR` — новые возможности, `PATCH` — исправления.
- Пока не вышли из 0.x: ломающие изменения допустимы в `MINOR`, но обязаны быть в `CHANGELOG.md`.
- `-SNAPSHOT` в `main` после релиза не возвращаем: версия поднимается сразу, тег ставится на коммит.

## Откат

- Неверный тег: `git tag -d vX.Y.Z && git push --delete origin vX.Y.Z`, затем пересобрать.
- Ошибочный релиз в Central не удаляется. Выпускаем `PATCH` с исправлением и помечаем старую
  версию в `CHANGELOG.md` как «не использовать».
