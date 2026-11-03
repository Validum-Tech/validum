# Validum

> Self-hosted платформа генерации валидных данных для российского финтеха и госсектора.

[![CI](https://github.com/Validum-Tech/validum/actions/workflows/ci.yml/badge.svg)](https://github.com/Validum-Tech/validum/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21_LTS-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Gradle](https://img.shields.io/badge/Gradle-8.14-blue.svg)](https://gradle.org/)
[![Tests](https://img.shields.io/badge/tests-769_passing-success.svg)](#-тесты)
[![Coverage](https://img.shields.io/badge/coverage-core_91%25_%7C_api_92%25-yellowgreen.svg)](#-тесты)
[![Banks](https://img.shields.io/badge/CBR_banks-370-blueviolet.svg)](#-полный-справочник-цб-рф)
[![Zero-deps](https://img.shields.io/badge/core-zero--dependency-lightgrey.svg)](#-архитектура)
[![CLI](https://img.shields.io/badge/CLI-picocli-informational.svg)](#-cli-p31)
[![Allure](https://img.shields.io/badge/Allure-JUnit_5_Extension-yellow.svg)](#-allure-extension-p32)

---

## ⚠️ Важно: данные синтетические

Validum создаёт **вымышленные** данные для тестовых стендов. Совпадения с реальными людьми
или организациями случайны: пространство ИНН, ФИО и адресов конечно, а справочники взяты
из открытых источников. Не загружайте результат в продуктивные системы и не используйте
как основание для юридически значимых решений.

Государственные справочники (ED807/ED808, КЛАДР, ОКВЭД-2, коды ЦБ) персональных данных
не содержат. Соответствие требованиям 152-ФЗ обеспечивается тем, что в контуре нет боевых
данных клиентов, — но ответственность за то, что вы генерируете и куда кладёте, остаётся
на операторе.

## 🚀 Быстрый старт

Три команды — от нуля до данных. Нужен только JDK 21 (или Docker).

```powershell
# 1. Справочник участников ЦБ (обязателен: 1380 участников, 370 банков)
java -jar validum.jar fetch-bik -o data/ED807.xml

# 2. Профиль клиента с реквизитами, банком и счётом
java -jar validum.jar generate individual --seed 42

# 3. Тысяча клиентов в CSV / SQL / JSON
java -jar validum.jar generate individual --seed 42 -n 1000 --format csv -o clients.csv
java -jar validum.jar generate individual --seed 42 -n 1000 --format sql --dialect postgres -o seed.sql
```

Собрать jar: `./gradlew :validum-cli:shadowJar` → `validum-cli/build/libs/validum.jar`.
Пошаговые сценарии под конкретные задачи — в [`docs/recipes/`](docs/recipes/README.md)
(фикстуры в JUnit, заливка в БД, нерезиденты, ДУЛ, ED807/ED808, детерминизм, Docker).

---

## 📖 Оглавление

- [Быстрый старт](#-быстрый-старт)
- [Зачем нужен Validum](#-зачем-нужен-validum)
- [Ключевые свойства](#-ключевые-свойства)
- [Что уже работает](#-что-уже-работает)
- [Четыре способа использовать](#-четыре-способа-использовать)
- [Готовые сценарии](#-готовые-сценарии)
- [Каскадные дефолты](#-каскадные-дефолты)
- [Пакетная генерация (Batch)](#-пакетная-генерация-batch)
- [Валидация (Validation API)](#-валидация-validation-api)
- [Экспорт (CSV / JSON Array)](#-экспорт-csv--json-array)
- [CLI (P3.1)](#-cli-p31)
- [Allure Extension (P3.2)](#-allure-extension-p32)
- [Явный выбор банка (P3.3)](#-явный-выбор-банка-p33)
- [Юрлица и ИП](#-юрлица-и-ип)
- [Полный справочник ЦБ РФ](#-полный-справочник-цб-рф)
- [Быстрый старт](#-быстрый-старт)
- [REST API](#-rest-api)
- [Java-библиотека](#-java-библиотека)
- [Архитектура](#-архитектура)
- [Структура репозитория](#-структура-репозитория)
- [Сборка и тесты](#-сборка-и-тесты)
- [CI](#-ci)
- [Docker](#-docker)
- [Roadmap](#-roadmap)
- [Для контрибьюторов](#-для-контрибьюторов)
- [Лицензия](#-лицензия)

---

## 🎯 Зачем нужен Validum

Тестирование банковских систем (**АБС**) требует данных, которые проходят валидацию:

- **ИНН** — контрольные разряды по алгоритму ФНС;
- **СНИЛС** — контрольное число по алгоритму ПФР;
- **ОГРН / ОГРНИП** — контрольная цифра по алгоритму ФНС;
- **КПП, ОКПО** — по алгоритмам ФНС/Росстата;
- **ОКВЭД-2** — из полного справочника ОК 029-2014 (3069 позиций) вместе с наименованием;
- **Расчётные счета** — контрольный ключ по Положению Банка России **№ 809-П**;
- **БИК и корсчета** — из справочника ЦБ РФ.

**Проблема:**

1. Обычные генераторы (`Faker`, `Mockaroo`) выдают **поверхностные** данные — контрольные суммы не сходятся.
2. **Боевые данные** нельзя — нарушение **152-ФЗ**.
3. Облачные SaaS **нельзя** в закрытом контуре банка.

**Validum решает:**

- Генерирует **валидные** данные — контрольные суммы сходятся.
- Работает **внутри периметра** — `docker compose up` и никаких внешних вызовов.
- Обеспечивает **воспроизводимость** — фиксированный `seed`.
- Поддерживает **каскадные дефолты**.
- **Пакетную генерацию** — до 10 000 профилей.
- **8 эндпоинтов валидации**.
- **Экспорт** — JSON, JSON-массив, CSV.
- **CLI без Spring** — fat JAR на Picocli (P3.1).
- **Allure Extension** для JUnit 5 (P3.2).
- **Явный выбор банка** — `.bik` / `.bank` (P3.3).

---

## ✨ Ключевые свойства

| Свойство | Что это значит |
|---|---|
| **Валидность** | Все данные проходят контрольные суммы ФНС, ПФР, Росстата, ЦБ РФ |
| **Связанность** | ФИО ↔ пол ↔ паспорт ↔ счёт ↔ БИК ↔ адреса ↔ КПП ↔ email |
| **Воспроизводимость** | Один `seed` → идентичный набор данных |
| **Каскадные дефолты** | Задаёшь `age=16` — система сама делает несовершеннолетнего |
| **Три типа субъектов** | ФЛ, ИП, ЮЛ (7 ОПФ) |
| **Четыре способа использовать** | REST API · CLI · Java-библиотека · Allure Extension |
| **Пакетная генерация** | 3 эндпоинта `/batch`, до 10 000 профилей |
| **Валидация** | **8 эндпоинтов** + 8 типов в CLI |
| **Экспорт** | JSON, JSON Array, CSV с UTF-8 BOM. `CsvExporter` — в core |
| **Явный банк (P3.3)** | `.bik` / `.bank` для ФЛ и ИП. Невалидный → 400 / exit 2 |
| **Self-hosted** | В закрытом контуре, без интернета |
| **Zero-dependency core** | Никаких Spring / Datafaker в ядре |
| **CI-ready** | GitHub Actions, зелёный билд |
| **Zero LLM** | Только детерминированные алгоритмы |

---

## ✅ Что уже работает

### Ядро `validum-core` — 585 тестов

| Генератор / Валидатор | Что делает |
|---|---|
| `InnGenerator` / `InnCorruptor` | ИНН ФЛ (12) / ЮЛ (10) + порча |
| `SnilsGenerator` / `SnilsCorruptor` | СНИЛС (11) + порча |
| `OgrnGenerator` | ОГРН (13), ОГРНИП (15) |
| `KppGenerator` / `OkpoGenerator` | КПП / ОКПО |
| `BikGenerator` | БИК, корсчёт, банк (из справочника ED807) |
| `AccountGenerator` | Счета по 809-П (40817, 40702, 40802, 40807) |
| `NameGenerator` | ФИО — **3.8 млн** (P0.1) |
| `PassportGenerator` | Паспорт РФ / FOREIGN (14/20/45) |
| `AddressGenerator` | Адреса из КЛАДР: 90 субъектов, ~125 тыс. населённых пунктов, ~2 млн улиц (P0.2, ГАР) |
| `ContactGenerator` | Мобильные + email из ФИО (P0.3) |
| `IndividualProfileGenerator` | **Профиль ФЛ** + **`.bik` / `.bank` (P3.3)** |
| `IpProfileGenerator` | Профиль ИП — композиция ФЛ + ОГРНИП |
| `OrganizationProfileGenerator` | Профиль ЮЛ (7 ОПФ, P2.2) + ОКВЭД-2 |
| `OkvedGenerator` / `OkvedRegistry` | ОКВЭД-2: 3069 позиций, код + наименование |
| `CsvExporter` + 3 флаттенера | Плоский CSV (P3.1a). Общий для API, CLI, Allure |
| 7 валидаторов | ИНН, СНИЛС, ОГРН, ОГРНИП, КПП, ОКПО, БИК |

### API `validum-api` — 87 тестов

- **Генерация:** `POST /api/v1/generate/{individual,ip,organization}`, `GET/POST /scenarios/*`.
- **Батчи (P1.2):** `POST /generate/{individual,ip,organization}/batch`.
- **Валидация (P1.3 + P2.3):** 8 эндпоинтов `/validate/*`.
- **Экспорт (P1.4):** `?format=json|json-array|csv`.
- **P3.3:** `bik` в DTO, `GlobalExceptionHandler → 400` на `IllegalArgumentException`.

### CLI `validum-cli` — 78 тестов (P3.1b + P3.3)

Fat JAR на Picocli, **без Spring** (~15 МБ вместе со справочниками КЛАДР и ОКВЭД, старт ~300 мс).

| Команда | Что |
|---|---|
| `validum generate individual [...]` | Профиль(и) ФЛ + **`--bik`** (P3.3) |
| `validum generate ip [...]` | Профиль(и) ИП + **`--bik`** (P3.3) |
| `validum generate organization [...]` | Профиль(и) ЮЛ (без `--bik`) |
| `validum validate <type> <value>` | Валидация (8 типов), exit 0/1 |
| `validum fetch-bik [--offline]` | Справочник ЦБ |
| `validum scenarios` / `version` | Утилиты |

### Allure `validum-allure` — 19 тестов (P3.2)

JUnit 5 Extension. `@ValidumSeed` / `@ValidumGenerate` → плоский JSON в Allure-отчёте. Только ФЛ.

**Проверено на реальном счёте из АБС:** `40817101000000000953 — валиден по 809-П ✅`

### 🐛 Баг, который прятался годами

В стандартном коде генерации СНИЛС цифры извлекаются через `% 10` (обратный порядок) — коэффициенты ПФР перевёрнуты, все СНИЛС бракуются. **Validum это исправляет.**

---

## 🎯 Четыре способа использовать

| Способ | Когда нужен | Как |
|---|---|---|
| **REST API** | Автотесты (JUnit + REST Assured), Swagger UI | `docker compose up` → `POST /api/v1/generate/...` |
| **CLI** | Скрипты сетапа, CI-пайплайны, «100 профилей в CSV» | `java -jar validum.jar generate individual --count 100 --format csv -o profiles.csv` |
| **Allure Extension** | JUnit 5: attach профиля в Allure-отчёт | `@ValidumSeed(42)` / `@ValidumGenerate(...)` |
| **Java-библиотека** | Свой генератор поверх core | `<dependency>io.github.validum-tech:validum-core:0.2.0</dependency>` (Maven Central) |

---

## 🎯 Готовые сценарии

### ФЛ через slug

| Slug | Возраст | Категория | Пакет | Счёт |
|---|---|---|---|---|
| `adult-resident-with-account` | 19 | КЛИЕНТ | БАЗОВЫЙ | ✅ |
| `adult-resident-without-account` | 19 | КЛИЕНТ | БАЗОВЫЙ | — |
| `pensioner-male` | **65** | ПЕНСИОНЕР | ПЕНСИОННЫЙ | ✅ |
| `pensioner-female` | **60** | ПЕНСИОНЕР | ПЕНСИОННЫЙ | ✅ |
| `salary-client` | 35 | ЗАРПЛАТНИК | ЗАРПЛАТНЫЙ | ✅ |
| `non-resident-with-residence-permit` | 32 | НЕРЕЗИДЕНТ | БАЗОВЫЙ | ✅ |
| `foreigner-with-visa` | 28 | НЕРЕЗИДЕНТ | БАЗОВЫЙ | ✅ |
| `serviceman` | 20 | КЛИЕНТ | БАЗОВЫЙ | ✅ |
| `minor` | **16** | КЛИЕНТ | БАЗОВЫЙ | ✅ |

Документы ДУЛ в этих сценариях: нерезидент с ВНЖ — паспорт иностранного гражданина (10) +
вид на жительство (12); иностранец с визой — паспорт (10) + виза; военнослужащий — паспорт РФ
(21) + военный билет (07); несовершеннолетний — паспорт РФ (21).

### Через REST / CLI / Java

```bash
# REST — список сценариев
curl http://localhost:8080/api/v1/scenarios

# REST — пакет из 100 профилей ФЛ в Москве
curl -X POST http://localhost:8080/api/v1/generate/individual/batch \
  -H "Content-Type: application/json" \
  -d '{"count":100,"seed":42,"region":"MOSCOW"}'
```

```powershell
# CLI — 100 профилей ФЛ в CSV
java -jar validum.jar generate individual --seed 42 --count 100 --format csv -o profiles.csv
java -jar validum.jar generate individual --seed 42 --as-of 2026-01-01 --format csv -o reproducible.csv
java -jar validum.jar generate individual --seed 42 --count 1000 --unique --format csv -o unique.csv

# CLI — 10 ЮЛ типа ГУП в Москве → JSON-массив
java -jar validum.jar generate organization --seed 42 --count 10 --type SE --region MOSCOW --format json-array -o gup.json
```

```java
// Java-библиотека
IndividualProfile adult = IndividualScenarios.createAdultResidentWithAccount(42L);
IpProfile ip = IpProfileGenerator.generate(
    IndividualProfileGenerator.builder().seed(42L).age(35).build(), 42L);
OrganizationProfile gup = OrganizationProfileGenerator.builder()
    .seed(42L).organizationType(OrganizationType.SE).region(Region.MOSCOW)
    .build().generate();
```

---

## 🌊 Каскадные дефолты

**Фишка Validum:** вместо хардкоженных негативных сценариев — **ортогональные параметры с умными дефолтами**.

### Каскадная матрица

| Ты задал | Что получаешь |
|---|---|
| (ничего) | 19 лет, паспорт в 14, действ. до 20, РФ, КЛИЕНТ, счёт, email из ФИО |
| `age=16` | Несовершеннолетний, паспорт в 14, действительный, SINGLE |
| `age=23` | Паспорт менялся в 20, действ. до 45 |
| `age=23` + `passportWasExchanged=false` | Паспорт в 14, **просрочен** в 20 |
| `age=50` | Паспорт бессрочный, выдан в 45 |
| `age=16` + `region=MOSCOW` | Несовершеннолетний + Москва + серия 77xx |
| `residencyCountry=TJ` | Все РФ-поля → TJ + иностранный паспорт |
| `categoryClient=ЗАРПЛАТНИК` | Пакет автоматически «ЗАРПЛАТНЫЙ» |
| `organizationType=LLC` | Префикс «ООО», OKOPF=65, OKFS=16 |
| `organizationType=SE` | Префикс «ГУП», OKOPF=61, OKFS=14, бюджетная |
| `emailDomainMode=REALISTIC` | Email на yandex.ru / mail.ru |
| **`bik="044525225"` (P3.3)** | **Банк Сбербанк, все его реквизиты** |
| **`bik="000000000"` (P3.3)** | **400 / exit 2 с понятным сообщением** |

### Правила каскадов

- **Валидность — дефолт.** Порча — только явным параметром.
- **Явное значение всегда побеждает auto.**
- **Параметры ортогональны.**
- **Мягкие каскады.** `age=16` + `familyStatus=MARRIED` → MARRIED.
- **Детерминизм с seed.** Один `seed` → один набор данных.
- **Детерминизм между днями — `asOf`.** Возраст и сроки документов считаются от даты расчёта:
  `--as-of 2026-01-01` (CLI) или `"asOf": "2026-01-01"` (REST). Один `seed` + одна дата дают
  одинаковый профиль в любой день; без `asOf` датой считается сегодняшняя.
- **P3.3:** `bank` > `bik` > случайный; невалидный БИК — явная ошибка, не откат.

---

## 📦 Пакетная генерация (Batch)

Один запрос — до 10 000 профилей. Первый использует `seed`, второй `seed+1` и т.д.

```json
POST /api/v1/generate/individual/batch
{
  "count": 100,
  "seed": 42,
  "region": "MOSCOW",
  "categoryClient": "ЗАРПЛАТНИК"
}
```

**P3.3:** в батч можно добавить `"bik": "044525225"` — все профили в пакете получат этот банк.

Ответ (по умолчанию `json`): `{ "count": 100, "seed": 42, "profiles": [...] }`.

Ограничение: `count` 1–10 000. Вне — `400 Bad Request`.

---

## ✅ Валидация (Validation API)

8 эндпоинтов (P1.3 + P2.3).

```bash
# ИНН
curl -X POST http://localhost:8080/api/v1/validate/inn \
  -H "Content-Type: application/json" -d '{"inn":"500100732259"}'
# → { "valid": true, "type": "INDIVIDUAL", "details": "Valid INN checksum" }

# Счёт по 809-П
curl -X POST http://localhost:8080/api/v1/validate/account \
  -H "Content-Type: application/json" -d '{"account":"40817101000000000953"}'

# БИК (с названием банка)
curl -X POST http://localhost:8080/api/v1/validate/bik \
  -H "Content-Type: application/json" -d '{"bik":"044525225"}'
# → { "valid": true, "type": "BANK", "details": "ПАО Сбербанк" }

# ОКПО (P2.3)
curl -X POST http://localhost:8080/api/v1/validate/okpo \
  -H "Content-Type: application/json" -d '{"okpo":"12345676"}'

# ОГРНИП (P2.3)
curl -X POST http://localhost:8080/api/v1/validate/ogrnip \
  -H "Content-Type: application/json" -d '{"ogrnip":"304500116000157"}'
```

**Никогда не возвращает `500`** — только `200` с `valid: false` (или 400 для `IllegalArgumentException`).

**Или из CLI** (exit 0/1):

```powershell
java -jar validum.jar validate inn 500100732259
# → {"valid":true,"type":"INDIVIDUAL","details":"Valid INN checksum"} → exit 0
```

---

## 📤 Экспорт (CSV / JSON Array)

| `format` | Content-Type | Кому |
|---|---|---|
| `json` (дефолт) | `application/json` | Автотесты, обёртка `{count, seed, profiles}` |
| `json-array` | `application/json` | Скрипты (Python, JS) |
| `csv` | `text/csv;charset=UTF-8` | Импорт в БД, скрипты: «,» и английские заголовки |
| `csv-semicolon` | `text/csv;charset=UTF-8` | **Русский Excel**: «;» и русские заголовки |

```bash
curl -OJ -X POST "http://localhost:8080/api/v1/generate/individual/batch?format=csv" \
  -H "Content-Type: application/json" -d '{"count":100,"seed":42}'
# → validum-individual-2026-10-22.csv

curl -OJ -X POST "http://localhost:8080/api/v1/generate/individual/batch?format=csv-semicolon" \
  -H "Content-Type: application/json" -d '{"count":100,"seed":42}'
# → тот же файл, но с колонками «ФИО;ИНН;СНИЛС…» — открывается двойным кликом
```

```bash
java -jar validum.jar generate individual --seed 42 -n 100 --format csv-semicolon -o clients.csv
java -jar validum.jar generate organization --seed 42 -n 10 --format csv-semicolon -o companies.csv
```

**Что внутри CSV:**
- **UTF-8 BOM** — Excel откроет кириллицу корректно.
- **Плоская структура** — `passportSeries`, `registrationCity`, `bankBik`, ...
- **Экранирование** — RFC 4180 (кавычки удваиваются, запятые и переносы строк берутся в кавычки).
- `csv-semicolon` — те же данные для человека: разделитель «;» (русский Excel по умолчанию так разбирает) и заголовки на русском. Машинные конвейеры продолжают читать `csv`.

**Архитектурно (P3.1a):** `CsvExporter` + 3 флаттенера живут **в core** (`ru.validum.core.export`) — используются REST API, CLI и Allure.

---

### SQL-доставка (`--format sql`)

Плоская таблица + `INSERT` для PostgreSQL / Oracle / MySQL — один файл сразу заливается в тестовую БД.

```bash
java -jar validum.jar generate individual --format sql --dialect postgres --table clients \
  -n 1000 -o seed.sql
# psql -h localhost -U qa -d testdb -f seed.sql

# Oracle:  --dialect oracle  → CREATE TABLE "clients" ("inn" VARCHAR2(400) ...)
# MySQL:   --dialect mysql   → CREATE TABLE `clients` (`inn` VARCHAR(400) ...)
# --no-ddl                   → только INSERT, если таблица уже есть
```

Тип колонки выводится из имени: `terrorist` и `blocked` — логический (`BOOLEAN` / `TINYINT(1)` / `NUMBER(1)`), `age` — целое, остальное — текст, чтобы ИНН, СНИЛС и счёт сохраняли ведущие нули. Апострофы экранируются (`'` → `''`), `null` пишется как `NULL`. Архитектурно — `ru.validum.core.sql` (`SqlDialect`, `SqlExporter`), без внешних зависимостей.

---

## 🖥 CLI (P3.1)

Fat JAR на Picocli. **Без Spring.** ~15 МБ со справочниками, старт ~300 мс.

```powershell
.\gradlew :validum-cli:shadowJar
# → validum-cli/build/libs/validum.jar
```

### Быстрый старт

```powershell
java -jar validum.jar --help
java -jar validum.jar version
java -jar validum.jar scenarios
```

### Генерация

```powershell
# 1 профиль ФЛ в stdout (JSON)
java -jar validum.jar generate individual --seed 42

# 100 профилей ФЛ в CSV
java -jar validum.jar generate individual --seed 42 --count 100 --format csv -o profiles.csv

# 10 ИП в Москве → JSON-массив
java -jar validum.jar generate ip --seed 1 --count 10 --region MOSCOW --format json-array -o ip.json

# 5 ЮЛ типа ГУП
java -jar validum.jar generate organization --seed 42 --count 5 --type SE --region MOSCOW --format csv -o gup.csv
```

### Валидация (exit 0/1)

```powershell
java -jar validum.jar validate inn 500100732259       # → exit 0
java -jar validum.jar validate inn 500100732258       # → exit 1
java -jar validum.jar validate ogrnip 304500116000157
```

Паттерн в скрипте:

```powershell
if ((& java -jar validum.jar validate inn $inn) -and ($LASTEXITCODE -eq 0)) {
    Write-Host "OK: $inn"
}
```

### `fetch-bik` — справочник ЦБ

```powershell
# Online: скачивает ZIP с cbr.ru, распаковывает
java -jar validum.jar fetch-bik -o data/ED807.xml

# Offline (банковский контур, нет доступа к cbr.ru)
java -jar validum.jar fetch-bik --offline
# → инструкция + exit 0, ничего не скачивает
```

### Ключевые решения CLI

- **`--format` явный.** Не угадываем по расширению.
- **Плоский JSON.** Те же поля, что CSV. Нужен 1:1 REST — используй REST.
- **`fetch-bik --offline`.** Для банковских контуров.
- **UTF-8 stdout.** Windows-консоль по умолчанию cp866 — форсим UTF-8 в `main()`.
- **`--output` работает всегда.** Даже без фикса консоли — файл пишется UTF-8.

---

## 🎭 Allure Extension (P3.2)

```kotlin
testImplementation("io.github.validum-tech:validum-allure:0.2.0")
```

```java
@ExtendWith(ValidumExtension.class)
class ClientCreationTest {

    @Test
    @ValidumSeed(42)
    void testCreateClient() {
        // Attachment "Validum Profile" (application/json) в отчёте
    }

    @Test
    @ValidumGenerate(seed = 42, age = 16, region = "MOSCOW")
    void testMinorClient() {
        // Профиль age=16, Москва — в отчёте
    }
}
```

**Что в attachment'е:** плоский JSON, те же поля, что CSV (`fullName`, `passportSeries`, `registrationCity`, `bankBik`, ...). Переиспользуется `IndividualProfileFlattener`.

**Ограничения:**
- **Только ФЛ** (ИП/ЮЛ — P3.2b, если попросят).
- **Без Spring.** Зависимости: `validum-core` + `allure-jupiter:2.35.3` + `junit-jupiter-api` + Jackson.
- **Best-effort attach.** Без Allure runtime — warning в stderr, тест не падает.

---

## 💳 Явный выбор банка (P3.3)

По умолчанию банк выбирается случайно из справочника. QA хотят фиксировать «тест со Сбербанком» — теперь это возможно.

### Java

```java
// По БИК
IndividualProfile sber = IndividualProfileGenerator.builder()
    .seed(42L)
    .bik("044525225")
    .build().generate();

// Готовый Bank
Bank vtb = BikSources.resolve("data/ED807.xml").findByBik("044525187").orElseThrow();
IndividualProfile vtbProfile = IndividualProfileGenerator.builder()
    .seed(42L)
    .bank(vtb)
    .build().generate();

// ИП наследует через IndividualProfile
IpProfile ip = IpProfileGenerator.generate(
    IndividualProfileGenerator.builder()
        .seed(42L).age(35).bik("044525225").build(), 42L);
```

### REST

```bash
# OK → банк Сбербанк
curl -X POST http://localhost:8080/api/v1/generate/individual \
  -H "Content-Type: application/json" \
  -d '{"seed":42,"bik":"044525225"}'
# → { ..., "bank": { "bik": "044525225", "name": "ПАО Сбербанк", ... } }

# 400 → БИК не найден
curl -X POST http://localhost:8080/api/v1/generate/individual \
  -H "Content-Type: application/json" \
  -d '{"seed":42,"bik":"000000000"}'
# → 400 { "error": "invalid_argument", "message": "БИК не найден в справочнике: 000000000..." }
```

### CLI

```powershell
# OK → все профили в CSV с банком Сбербанк
java -jar validum.jar generate individual --seed 42 --count 10 --bik 044525225 --format csv -o sber.csv

# exit 2 → невалидный БИК
java -jar validum.jar generate individual --bik 000000000 --format csv
# → stderr: "Ошибка: БИК не найден в справочнике: 000000000..."
```

### Правила

- **Приоритет:** `bank` > `bik` > случайный.
- **Невалидный БИК — явная ошибка.** Не откатываемся на случайный: REST 400, CLI exit 2.
- **ЮЛ с банком.** `OrganizationProfile` содержит `bank` (БИК из ED807) и `account` (40702, ключ по 515) — `--bank-bik` / `bik` / `.bank(...)`.
- **Справочник обязателен.** Источник — файл ED807 через `BikSources`; встроенного демо-набора нет.

---

## 🏢 Юрлица и ИП

### ЮЛ — `POST /api/v1/generate/organization`

**ОПФ (7 типов, P2.2):**

| Значение | Префикс | OKOPF | OKFS | Бюджет |
|---|---|---|---|---|
| `LLC` | ООО | 65 | 16 | Небюджетная |
| `JSC` | АО | 47 | 16 | Небюджетная |
| `PJSC` | ПАО | 47 | 16 | Небюджетная |
| `NCO` | НКО | 85 | 16 | Некоммерческая |
| `FOUNDATION` | Фонд | 88 | 16 | Некоммерческая |
| `SE` | ГУП | 61 | 14 | Бюджетная |
| `ME` | МУП | 62 | 13 | Бюджетная |

### ИП — `POST /api/v1/generate/ip`

Композиция над `IndividualProfile`:
- Всё как у ФЛ (ФИО, паспорт, ИНН 12, СНИЛС, адреса, счёт)
- Отчество — фиксированное «ИП Автотеста»
- ОГРНИП — 15 цифр, валидный по ФНС
- **Детерминизм:** `seed=42` → тот же ОГРНИП
- **P3.3:** `bik` работает через base

---

## 🏦 Справочник участников ЦБ РФ

Справочник **обязателен**: встроенного демо-набора в продукте нет. Если файла ED807 нет,
генерация банковских реквизитов возвращает понятную ошибку (REST `503 bik_source_missing`,
CLI exit 2), а не подставляет чужие реквизиты.

### Шаг установки: скачать справочник

```powershell
# CLI (P3.1)
java -jar validum.jar fetch-bik -o data/ED807.xml

# Вручную
curl.exe -o data\newbik.zip "http://www.cbr.ru/s/newbik"
Expand-Archive -Path data\newbik.zip -DestinationPath data\
Rename-Item data\*.xml data\ED807.xml
```

**Парсер:** WINDOWS-1251, namespace `urn:cbr-ru:ed:v2.0`, фильтры `PtType="20"`, `PSAC`, `CRSA`, `ACAC`. Из ~1380 участников — **370 банков** (все участники — `participants --file`).

### Где ищется файл

Одна цепочка на API и CLI (`ru.validum.core.bank.BikSources`):

1. явный путь — `--file`, свойство `validum.bik.file`;
2. переменная окружения `VALIDUM_BIK_FILE`;
3. системное свойство `validum.bik.file`;
4. `data/ED807.xml`.

**P3.3:** `.bik("044525225")` ищет банк в загруженном справочнике; не найдено — явная ошибка.

---

## 🛠 Сборка из исходников

### Требования

- **JDK 21** (Temurin, Corretto, OpenJDK)
- **Gradle Wrapper** (в репо)
- **Docker Desktop** (опционально)

### Клонирование и сборка

```bash
git clone https://github.com/Validum-Tech/validum.git
cd validum
./gradlew build
```

**На Windows:**

```powershell
.\gradlew.bat build
```

### CLI (без Docker, без Spring)

```powershell
.\gradlew :validum-cli:shadowJar
java -jar .\validum-cli\build\libs\validum.jar generate individual --seed 42 --format csv -o profile.csv
```

### REST API локально

```bash
./gradlew :validum-api:bootRun
```

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **Health:** http://localhost:8080/actuator/health

---

## 🌐 REST API

### `POST /api/v1/generate/individual`

**Запрос (полный):**

```json
{
  "seed": 42,
  "gender": "MALE",
  "age": 23,
  "passportWasExchanged": false,
  "residencyCountry": "TJ",
  "passportType": "FOREIGN",
  "fioScript": "LATIN",
  "corrupt": ["INN"],
  "region": "MOSCOW",
  "categoryClient": "ЗАРПЛАТНИК",
  "accountPackage": "VIP",
  "familyStatus": "MARRIED",
  "email": "custom@corp.ru",
  "emailDomainMode": "REALISTIC",
  "secretWord": "СЛОН",
  "personalManager": "СЗА02",
  "terrorist": false,
  "blocked": false,
  "bik": "044525225"
}
```

**Ответ (фрагмент):**

```json
{
  "fullName": "Кузнецов Александр Владимирович",
  "gender": "MALE",
  "birthDate": "1988-03-12",
  "age": 38,
  "inn": "500100732259",
  "snils": "12345678900",
  "bank": { "bik": "044525225", "name": "ПАО Сбербанк", "corrAccount": "30101810400000000225" },
  "account": "40817810123456789012"
}
```

### Батчи, валидация, экспорт, сценарии

- **Батчи:** `POST /generate/{individual,ip,organization}/batch`, `count` 1–10 000, **P3.3: `bik`**.
- **Валидация:** 8 эндпоинтов `/validate/*`.
- **Экспорт:** `?format=json-array` / `?format=csv`.
- **Сценарии:** `GET /scenarios`, `POST /scenarios/{name}?seed=42`.

### 🔁 Уникальность набора

```bash
# 1000 профилей без единого повтора ИНН (нужны --unique и --seed)
java -jar validum.jar generate individual --seed 42 --count 1000 --unique --format csv -o unique.csv
java -jar validum.jar generate ip --seed 42 --count 500 --unique -o ip.csv
java -jar validum.jar generate organization --seed 42 --count 200 --unique -o org.csv
```

`--unique` гарантирует отсутствие повторов ключевого поля: у ФЛ — ИНН, у ИП — ОГРНИП, у ЮЛ — ОГРН.
Повтор невозможен, потому что при совпадении генератор перезапускается с новым seed, а набор
проверяется по мере генерации. Seed в этом режиме обязателен: иначе набор нельзя воспроизвести.
Если попросить больше, чем пул вариантов может дать, будет честная ошибка
«не удалось получить уникальный ИНН за N попыток из M вариантов», а не бесконечный цикл.

### 🎯 Воспроизводимость

```bash
curl -X POST http://localhost:8080/api/v1/generate/individual \
  -H "Content-Type: application/json" -d '{"seed":42}'
```

Один и тот же `seed` даёт **идентичный ответ** при каждом запросе.

Воспроизводимость видно в самом ответе: REST отдаёт заголовки `X-Validum-Version` (какая сборка
сгенерировала данные) и `X-Validum-Seed` (фактический seed; `auto`, если вы его не задали), CLI
пишет ту же мету в stderr. Версия — из `ru.validum.core.Version` (Gradle → манифест → `dev`).

### 📄 Набор из спецификации (`.validum.json`)

Вместо простыни флагов — один файл, который можно положить в репозиторий тестов:

```json
{
  "type": "individual",
  "count": 100,
  "seed": 42,
  "asOf": "2026-01-01",
  "format": "csv-semicolon",
  "output": "clients.csv",
  "unique": true,
  "options": {"region": "MOSCOW", "age": 35}
}
```

```bash
java -jar validum.jar generate --spec clients.validum.json
java -jar validum.jar generate --spec clients.validum.json -o /tmp/other.csv   # -o важнее файла
```

`type` — `individual` | `ip` | `organization` | `documents` (синонимы: `фл`, `ип`, `юл`). Поле `okved`
действует для ИП и ЮЛ (у ФЛ его нет в профиле). Поля
`options` поднимаются на верхний уровень, `count` по умолчанию 1, `format` — `json`. Неизвестный
ключ (опечатка вроде `"formt"`) отклоняется с кодом 2 и списком допустимых — тихо проигнорированная
опция в генераторе тестовых данных опаснее ошибки. При `unique: true` обязателен `seed`.

### 🔍 Отчёт о покрытии набора

Перед заливкой в стенд видно, что реально попало в файл — и не по «на глаз»:

```bash
java -jar validum.jar coverage --file clients.csv
```

```
# validum 0.2.0 coverage · file=clients.csv · rows=100

поле               заполнено  различных  невалидных
--------------------------------------------------
fullName                100% (100/100)        100          —
inn                     100% (100/100)        100          0
snils                   100% (100/100)         99          1
account                 100% (100/100)        100          0

итог: 22 поля, 1 с невалидными значениями, 0 с пустыми
```

Проверяются контрольные суммы ИНН, СНИЛС, ОГРН/ОГРНИП, КПП, ОКПО и ключ счёта (для счёта берётся
БИК из той же строки); остальные поля — по полноте и различию. Понимает CSV (машинный и
`csv-semicolon` — BOM и `;` распознаются), JSON и JSON-массив; формат можно задать `--format`.

---

## 📚 Java-библиотека

`validum-core` — **чистая Java-библиотека без Spring и внешних зависимостей**.

```java
// ФЛ — фасады
IndividualProfile adult = IndividualScenarios.createAdultResidentWithAccount(42L);
IndividualProfile pensioner = IndividualScenarios.createPensionerMale(42L);

// ФЛ — каскады
IndividualProfile minor = IndividualProfileGenerator.builder()
    .age(16).seed(42L).build().generate();

// ФЛ — с конкретным банком (P3.3)
IndividualProfile sber = IndividualProfileGenerator.builder()
    .seed(42L).bik("044525225").build().generate();

// ИП — с детерминизмом
IpProfile ip = IpProfileGenerator.generate(
    IndividualProfileGenerator.builder().seed(42L).age(35).build(), 42L);

// ЮЛ (7 ОПФ)
OrganizationProfile gup = OrganizationProfileGenerator.builder()
    .seed(42L).organizationType(OrganizationType.SE).region(Region.MOSCOW)
    .build().generate();

// Экспорт в CSV (P3.1a)
String csv = CsvExporter.toCsv(IndividualProfileFlattener.flattenAll(List.of(sber)));

// Валидация (P1.3 + P2.3)
boolean innOk = InnValidator.isValidIndividual("500100732259");
boolean ogrnipOk = OgrnipValidator.isValid("304500116000157");
boolean okpoOk = OkpoValidator.isValid("12345676");
```

---

## 🏗️ Архитектура

```
┌──────────────────────┐  ┌──────────────────────┐  ┌──────────────────────┐
│  validum-api         │  │  validum-cli         │  │  validum-allure      │
│  (Spring Boot 3.5)   │  │  (Picocli, ~15 МБ)   │  │  (JUnit 5 Extension) │
│  19 REST-эндпоинтов  │  │  9 команд            │  │  @ValidumSeed        │
└──────────┬───────────┘  └──────────┬───────────┘  └──────────┬───────────┘
           │                         │                         │
           └─────────────────────────┼─────────────────────────┘
                                     │ зависит от
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  validum-core (чистая Java, ZERO-DEP)                                   │
│                                                                         │
│  ФЛ (.bik / .bank) · ЮЛ (7 ОПФ) · ИП (композиция)                       │
│                                                                         │
│  Экспорт: CsvExporter + 3 флаттенера → API, CLI, Allure                 │
│  Валидация: Inn, Snils, Ogrn, Ogrnip, Kpp, Okpo, Bik                    │
│  Стратегии: BikSource · AddressStrategy · FiasMode                      │
│  Каскады: age → passport, region → addresses, category → package        │
│  АБС-специфика: AbsDefaults                                             │
└─────────────────────────────────────────────────────────────────────────┘
```

**Принцип:** `validum-core` не зависит от Spring, БД, сети и **внешних библиотек**.

### Паттерны гибкости

| Паттерн | Что решает |
|---|---|
| `BikSource` | Топ-30 vs ED807 — один интерфейс |
| `AddressStrategy` / `FiasMode` | Три адреса в одно / полный FIAS |
| **Каскадные дефолты** | Задаёшь одно — согласуется всё остальное |
| **Композиция ИП** | ИП = ФЛ + ОГРНИП |
| **`IndividualScenarios`** | Именованные сценарии → стабильный контракт |
| **`CsvExporter` + флаттенеры в core** | Один CSV-код для REST, CLI, Allure |
| **`.bik` / `.bank` (P3.3)** | Явный выбор банка, приоритет bank > bik > случайный |

---

## 📁 Структура репозитория

```
validum/
├── validum-core/                  ← 585 тестов (zero-dep)
│   ├── src/main/java/ru/validum/core/
│   │   ├── random/ · common/ · abs/ · inn/ · snils/ · ogrn/
│   │   ├── bank/ · account/ · name/ · passport/ · address/ · contact/
│   │   ├── profile/       ← IndividualProfileGenerator + .bik / .bank
│   │   ├── organization/
│   │   ├── export/        ← CsvExporter + 3 флаттенера
│   │   └── validation/    ← 7 валидаторов
│   └── src/main/resources/  ← names/ · addresses/ · emails/ · secret_words.txt · managers.txt
│
├── validum-api/           ← 87 тестов (Spring Boot)
│   └── src/main/java/ru/validum/api/
│       ├── config/ · controller/ (+GlobalExceptionHandler — P3.3)
│       ├── service/ · mapper/ · dto/
│
├── validum-cli/           ← 78 тестов (Picocli, без Spring)
│   └── src/main/java/ru/validum/cli/
│       ├── ValidumCli · OutputFormat · OutputWriter · BikSourceResolver
│       ├── Generate{Individual,Ip,Organization}Command (+ --bik в individual/ip)
│       └── ValidateCommand · FetchBikCommand · ScenariosCommand · VersionCommand
│
├── validum-allure/        ← 19 тестов (JUnit 5 Extension, без Spring)
│   └── src/main/java/ru/validum/allure/
│       ├── ValidumSeed · ValidumGenerate · ValidumExtension
│
├── data/ED807.xml         ← Справочник ЦБ, НЕ коммитится
├── docs/                  ← README · SCENARIOS · ROADMAP · recipes/ · reference/{cbr,dul}
├── docker-compose.yml
└── gradle/libs.versions.toml
```

---

## 🔨 Сборка и тесты

```bash
./gradlew build         # всё
./gradlew test          # только тесты
```

**На Windows:**

```powershell
.\gradlew.bat test --rerun-tasks
# HTML-отчёт: build/reports/tests/test/index.html
```

**CLI fat JAR:**

```powershell
.\gradlew :validum-cli:shadowJar
# → validum-cli/build/libs/validum.jar
```

### 📊 Тесты

| Модуль | Тестов |
|---|---|
| `validum-core` | **567** |
| `validum-api` | **80** |
| `validum-cli` | **55** |
| `validum-allure` | **19** |
| **Итого** | **769** |

Покрытие строк по jacoco (прогон 30.10.2026): `validum-core` — **89,2%** (2309/2588), `validum-api` — **92,2%** (404/438), `validum-cli` — считается тем же отчётом (HTML — `validum-cli/build/reports/jacoco/test/html/index.html`). Худшие пакеты: `validation` (75,9%), `common` (77,2%), `address` (77,8%); полностью покрыты `passport`, `snils`, `ogrn`, `random`. HTML-отчёт — `validum-core/build/reports/jacoco/test/html/index.html`.

---

## 🔄 CI

**GitHub Actions** на каждый push / PR в `main`:
- Ubuntu + JDK 21 (Corretto), кэш Gradle
- `./gradlew build --no-daemon` — тесты всех четырёх модулей
- Сводка в Summary: тесты по модулям + покрытие строк (jacoco) для `validum-core`, `validum-api` и `validum-cli`
- Артефакты: HTML-отчёты тестов (14 дней), покрытие jacoco, готовый `validum.jar` (30 дней)

---

## 🐳 Docker

```bash
docker compose up -d
# → Swagger UI: http://localhost:8080/swagger-ui.html
```

- Multi-stage build: `gradle:8.14-jdk21` → `eclipse-temurin:21-jre-alpine`
- Non-root user, container-aware JVM
- Справочник ЦБ монтируется как volume: `./data:/app/data:ro`
- Healthcheck: `/actuator/health`
- Размер: ~250–350 МБ

**DNS (Windows Docker Desktop):** если `UnknownHostException` — Settings → Docker Engine → `"dns": ["8.8.8.8", "1.1.1.1"]`.

---

## 🗺️ Roadmap

### ✅ Закрыты

- Фундамент, ИНН/СНИЛС/ОГРН/ФИО/паспорта/адреса/счета/БИК
- REST API + Swagger UI + Actuator + Docker
- Каскадные дефолты, флаги, ЮЛ/ИП
- **P0.1–P0.3** — ФИО (3.8 млн), адреса (320 / 16), email/слова/менеджеры
- **P1.2–P1.4** — batch, validation, экспорт
- **P2.2–P2.3** — 7 ОПФ, ОКПО/ОГРНИП, расширенный КПП
- **P3.1** — CLI + CSV в core
- **P3.2** — Allure Extension
- **P3.3** — `.bik` / `.bank`

### ⏭️ Следующая

- **P4.1** — Импорт CSV (fill gaps) (2–3 дня)

### 📅 Отложено / Backlog

- **P1.1** — первая публикация в Maven Central (код готов, нужны секреты — см. `docs/RELEASING.md`)
- **P2.1** — Транзакции
- **P3.4** — Mock API по OpenAPI (3–5 дней)
- **P4.2–P4.4** — Иностранные паспорта, Web UI, ML-датасеты
- ~~**P2.4** — Банк для ЮЛ~~ — сделано (30.10.2026)
- **P3.2b** — Allure для ИП/ЮЛ (если попросят)

---

## 📦 Релизы и артефакты

| Что | Где взять |
|---|---|
| Fat jar `validum.jar` (+ `.sha256`) | [GitHub Releases](https://github.com/Validum-Tech/validum/releases) |
| Java-библиотеки (`validum-core`, `validum-allure`, …) | Maven Central: `io.github.validum-tech:*:0.2.0` (с первого релиза в Central; подключать репозиторий не нужно) |
| Контейнер API | `docker compose up` локально; Docker Hub — после 0.2 |
| Исходники | `git clone` → `./gradlew build` |

Как выпускается версия (тег, секреты, Maven Central) — [`docs/RELEASING.md`](docs/RELEASING.md).

---

## 🤝 Для контрибьюторов

- **JDK 21**, Gradle Wrapper, IntelliJ IDEA / VS Code
- Java 21 Records, immutable, каскадные дефолты
- Фасады только для 3+ связанных полей
- Тесты: `src/test/`, имя файла = имя класса, `@DisplayName` на русском
- Core — zero-dep, CLI и Allure — без Spring
- CSV и флаттенеры — в core
- **P3.3: невалидный БИК → явное исключение**

### Как контрибьютить

1. Форк → ветка `feature/my-feature`
2. Тесты **до** реализации
3. `./gradlew build` — все 769 зелёные, покрытие не падает
4. PR — CI прогонит сборку на Linux и Windows

Подробности и чек-лист — [`CONTRIBUTING.md`](CONTRIBUTING.md).
Уязвимости — не в Issues, а на `security@validum.tech` ([`SECURITY.md`](SECURITY.md)).

---

## 📜 Лицензия

Apache License 2.0 — см. [LICENSE](LICENSE).

---

## 📬 Контакты

- **GitHub Issues:** [github.com/Validum-Tech/validum/issues](https://github.com/Validum-Tech/validum/issues)
- **Домен:** validum.tech
- **Организация:** Validum-Tech

---

**Validum** — данные, которые проходят валидацию. **769 тестов. 370 банков. REST API + CLI + Java-библиотека + Allure. P0.1–P0.3, P1.2–P1.4, P2.2, P2.3, P3.1–P3.3, P4.1 закрыты, ДУЛ (11 видов) — в профиле, REST и CLI. 9 КУС-сценариев, экспорт ED807/ED808, Docker.**
