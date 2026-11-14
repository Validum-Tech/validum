# Validum

> Self-hosted платформа генерации валидных данных для российского финтеха и госсектора.

[![CI](https://github.com/Validum-Tech/validum/actions/workflows/ci.yml/badge.svg)](https://github.com/Validum-Tech/validum/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21_LTS-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Gradle](https://img.shields.io/badge/Gradle-8.14-blue.svg)](https://gradle.org/)
[![Tests](https://img.shields.io/badge/tests-860_passing-success.svg)](#-тесты)
[![Coverage](https://img.shields.io/badge/coverage-core_91%25_%7C_api_92%25-yellowgreen.svg)](#-тесты)
[![Banks](https://img.shields.io/badge/CBR_banks-370-blueviolet.svg)](#-полный-справочник-цб-рф)
[![Zero-deps](https://img.shields.io/badge/core-zero--dependency-lightgrey.svg)](#-архитектура)
[![CLI](https://img.shields.io/badge/CLI-picocli-informational.svg)](#-cli-p31)
[![Allure](https://img.shields.io/badge/Allure-JUnit_5_Extension-yellow.svg)](#-allure-extension-p32)

---

Validum — самодостаточная платформа генерации **валидных** тестовых данных для российского финтеха:
ИНН, СНИЛС, ОГРН/ОГРНИП, КПП, ОКПО, БИК и корсчета из справочника ЦБ, счета по 809-П, паспорта,
адреса из КЛАДР, ОКВЭД-2 — всё с настоящими контрольными суммами, детерминированно по `seed` и с
шагом `asOf`.

16 REST-эндпоинтов и 10 команд CLI; всё, что генерится, проверяется теми же валидаторами,
что используют банки, а справочник участников берётся из выгрузки ЦБ.

## За 30 секунд

```bash
# 1. Справочник участников ЦБ (обязателен: около 1,4 тыс. участников, около 370 банков)
java -jar validum.jar fetch-bik -o data/ED807.xml

# 2. Профиль клиента с реквизитами, банком и счётом
java -jar validum.jar generate individual --seed 42

# 3. Тысяча клиентов в CSV для Excel
java -jar validum.jar generate individual --seed 42 -n 1000 --format csv-semicolon -o out/clients.csv
```

## Документация

| Раздел | Что внутри |
|---|---|
| [Быстрый старт](docs/QUICKSTART.md) | установка, первый прогон, сборка из исходников, тесты, Docker |
| [Возможности](docs/FEATURES.md) | зачем нужен, ключевые свойства, что уже работает |
| [CLI](docs/CLI.md) | десять команд, пакетная выдача, валидация, экспорт, уникальность, `--corrupt`, справочник ЦБ |
| [REST API](docs/REST.md) | batch-эндпоинты, `fill`, `validate`, `scenarios`, `export`, коды ошибок |
| [Интеграции](docs/INTEGRATIONS.md) | Java-библиотека и JUnit 5 + Allure |
| [Совместимость](docs/COMPATIBILITY.md) | контракт потребителя: Java 17+, аддитивность полей, детерминизм |
| [Рецепты](docs/recipes/README.md) | 14 готовых сценариев под конкретные задачи |
| [Проект](docs/PROJECT.md) | архитектура, структура репозитория, релизы, участие |
| [Демо-сценарий](docs/DEMO-SCENARIO.md) | пошаговый прогон для видео и презентаций (внутреннее) |
| [История версий](CHANGELOG.md) | что менялось и когда |
| [Лицензия](LICENSE) | Apache-2.0 |

## Цифры

| Метрика | Значение |
|---|---|
| Тесты | 872 исполнения: 660 core + 100 CLI + 91 API + 21 Allure, 0 падений |
| Модули | 4: `validum-core` (без зависимостей), `validum-cli`, `validum-api`, `validum-allure` |
| Команды CLI | 9 (`generate`, `fill`, `validate`, `fetch-bik`, `participants`, `scenarios`, `export`, `coverage`, `describe`) |
| REST | 16 эндпоинтов |
| Справочник ЦБ | 1 360 участников, 368 банков, 1 297 счетов |
| Адреса | 90 субъектов, 98 730 населённых пунктов, 1 993 702 улицы, индексы из `DOMA.DBF` |
| ОКВЭД-2 | 3 069 позиций |

## Лицензия и контакты

Apache License 2.0 — [LICENSE](LICENSE). Вопросы и предложения: issue в репозитории
или `dev@validum.tech`. Сайт: [validum.tech](https://validum.tech).
