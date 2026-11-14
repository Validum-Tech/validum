# CLI

Командная строка: генерация, пакетная выдача, валидация, экспорт, уникальность, негативные данные, справочник ЦБ.

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
- `csv-semicolon` — те же данные для человека: разделитель «;» (русский Excel по умолчанию так
  разбирает), заголовки на русском и кодовые значения текстом (`="586272979740"`). Без этой
  обёртки Excel угадывает тип и портит данные: ИНН показывает как `5,86E+11`, из 20-значного
  счёта выпадают цифры, индекс `053796` теряет ведущий ноль. Обёртка — только в «дружелюбном»
  формате; машинные конвейеры продолжают читать `csv` без формул.

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

## 🌍 Нерезиденты (`--residency`, `--country`)

Пока в банке есть трудовые мигранты, в тестовых данных должны быть и они. По умолчанию
генерируется резидент РФ — по всем полям; для нерезидента всё меняется согласованно:

```bash
java -jar validum.jar generate individual --seed 42 --residency non-resident --country KZ \
  --age 30 --documents auto --format json
```

- адрес регистрации, почтовый и фактический — в стране проживания, с городом и индексом в
  формате страны (Алматы `05xxxx`, Минск `22xxxx`, Берлин `10xxx`);
- набор ДУЛ собирается под нерезидента: паспорт иностранного гражданина, а с 18 лет — ВНЖ;
- в плоской выдаче появляются колонки `isResident` и `citizenship` (русские заголовки —
  «Резидент» и «Гражданство»), в CSV для Excel они уже есть.

Гражданство и место жительства независимы: нерезидент из Таджикистана может жить в Москве —
если задан `--region`, адрес берётся из КЛАДР, а паспорт остаётся иностранным.

Поддержаны страны: `KZ` (по умолчанию), `BY`, `UZ`, `KG`, `AM`, `GE`, `UA`, `DE`, `US`;
страна без своих данных получает нейтральный набор. Улицы — типовые для страны, а не выгрузка
из национального реестра: это синтетический адрес, а не предсказание реального.

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
