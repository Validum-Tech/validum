# Validum

> Self-hosted платформа генерации валидных данных для российского финтеха и госсектора.

[![CI](https://github.com/Validum-Tech/validum/actions/workflows/ci.yml/badge.svg)](https://github.com/Validum-Tech/validum/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://adoptium.net/)
[![Tests](https://img.shields.io/badge/tests-906_passing-success.svg)](docs/QUICKSTART.md)
[![Maven Central](https://img.shields.io/badge/Maven_Central-0.5.0-informational.svg)](https://central.sonatype.com/artifact/io.github.validum-tech/validum-core)
[![core](https://img.shields.io/badge/core-zero--dependency-lightgrey.svg)](docs/PROJECT.md)

ИНН, СНИЛС, ОГРН/ОГРНИП, КПП, ОКПО, БИК и корсчёт из справочника ЦБ, счета по 809-П, паспорта,
адреса из КЛАДР, ОКВЭД-2 — с настоящими контрольными суммами, детерминированно по `seed` и с
шагом `asOf`. Core без зависимостей; CLI и REST — тонкие обёртки над ним.

## За 30 секунд

```bash
# 1. Справочник участников ЦБ (обязателен для банковских сценариев)
java -jar validum.jar fetch-bik -o data/ED807.xml

# 2. Профиль клиента с реквизитами, банком и счётом
java -jar validum.jar generate individual --seed 42

# 3. Тысяча клиентов в CSV, который открывается в Excel
java -jar validum.jar generate individual --seed 42 -n 1000 --format csv-semicolon -o out/clients.csv
```

Jar ещё не собран? `./gradlew :validum-cli:shadowJar` — подробности в [QUICKSTART](docs/QUICKSTART.md).

## Документация

Начать стоит с **[туториала](docs/TUTORIAL.md)** — один проход «от установки до проверенного
набора» на 30–40 минут. Остальное — по мере необходимости.

| Документ | Когда открывать |
|---|---|
| [Туториал](docs/TUTORIAL.md) | первый раз в проекте |
| [QUICKSTART](docs/QUICKSTART.md) | установка, сборка, тесты, Docker |
| [CLI](docs/CLI.md) | все команды и флаги |
| [REST](docs/REST.md) | HTTP-эндпоинты, форматы, коды ошибок |
| [Интеграции](docs/INTEGRATIONS.md) | Java-библиотека, JUnit 5 + Allure |
| [Совместимость](docs/COMPATIBILITY.md) | Java 17+, аддитивность полей, детерминизм |
| [Рецепты](docs/recipes/README.md) | готовая команда под конкретную задачу |
| [Возможности](docs/FEATURES.md) | зачем нужен и что работает |
| [Сценарии](docs/SCENARIOS.md) | типы клиентов, каскады, поля профиля |
| [Проект](docs/PROJECT.md) | архитектура, модули, как участвовать |
| [История версий](CHANGELOG.md) | что менялось и когда |

## Цифры

| Метрика | Значение |
|---|---|
| Тесты | 906: 679 core + 114 CLI + 92 API + 21 Allure, 0 падений |
| Модули | `core` (без зависимостей), `cli`, `api`, `allure` |
| Поверхность | 10 команд CLI, 19 REST-эндпоинтов |
| Справочник ЦБ | около 1,4 тыс. участников, около 370 действующих банков (зависит от выгрузки) |
| Адреса | 90 субъектов, 98,7 тыс. населённых пунктов, ~2 млн улиц, индексы из `DOMA.DBF` |
| ОКВЭД-2 | 3 069 позиций |

## Лицензия и контакты

Apache License 2.0 — [LICENSE](LICENSE). Вопросы и предложения — issue в репозитории;
почта `dev@validum.tech`, сайт [validum.tech](https://validum.tech).
