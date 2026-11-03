# 🗺️ Validum — Roadmap и бэклог

> **Дата:** 22 октября 2026
> **Текущий статус:** MVP готов. **769 тестов**. P0.1–P0.3, P1.2–P1.4, P2.2, P2.3, P3.1–P3.3, P4.1 закрыты, ДУЛ (Неделя 3) — 11 видов, КУС-сценарии (9), экспорт ED807/ED808, Docker. Дальше — витрина и монетизация.
> **Как пользоваться:** идём строго по приоритетам. P0 блокирует публикацию. P1 закрывает MVP для QA. P2–P4 — рост.
> **Правило:** после каждой итерации — прогон тестов, обновление доков, коммит, зелёный CI.

---

## 📊 Текущее состояние

| Слой | Что готово |
|---|---|
| **Ядро** | **567 тест**, zero-dep Java 21, 18 пакетов |
| **API** | **60 тестов**, 16 бизнес-эндпоинтов + `GlobalExceptionHandler` |
| **CLI** | **78 тестов**, 9 команд + 8 типов валидации + `--bik`, fat JAR ~15 МБ (справочники КЛАДР и ОКВЭД внутри) |
| **Allure** | **19 тестов**, JUnit 5 Extension, attach профиля в отчёт |
| **Субъекты** | ФЛ (21 поле), ИП (композиция), ЮЛ (19 полей, **7 ОПФ**) |
| **Каскады** | `age → passport`, `region → addresses+KPP`, `category → package`, `orgType → okfs/budget` |
| **Батчи** | 3 эндпоинта `/batch`, до 10 000 профилей за раз |
| **Валидация** | **8 эндпоинтов** `/validate/*` + 8 типов в CLI |
| **Экспорт** | **CSV в core** (API, CLI, Allure). `?format=json|json-array|csv` |
| **Allure** | `@ValidumSeed` + `@ValidumGenerate`, flat JSON attachment |
| **Банк (P3.3)** | **`.bik(String)` / `.bank(Bank)`** для ФЛ и ИП. Невалидный БИК → 400 / exit 2 |
| **Негативные** | `corrupt(INN/SNILS)`, `passportWasExchanged=false`, `fioScript=LATIN` |
| **Флаги** | `terrorist`, `blocked` |
| **Справочник** | Топ-30 + полный ED807 (370 банков) |
| **Инфра** | Docker, GitHub Actions, GitHub Projects |

---

## ⚠️ Узкие места (закрыты в P0)

| Компонент | Было уникальных | Стало | Проблема при 100k профилей |
|---|---|---|---|
| **ФИО (кириллица)** | 14 000 | **~3 800 000** ✅ | Решено в P0.1 |
| **Адреса (на регион)** | **1** | **20** ✅ | Решено в P0.2 (всего 320) |
| **Email** | **1** | **298 доменов + ФИО** ✅ | Решено в P0.3 |
| **Секретное слово** | **1** | **298** ✅ | Решено в P0.3 |
| **Менеджер** | **1** | **50** ✅ | Решено в P0.3 |
| Регионы | 5 | **16** ✅ | Решено в P0.2 |
| ИНН | 5.2 млрд | 5.2 млрд | ✅ ок |
| СНИЛС | 900 млн | 900 млн | ✅ ок |
| Счёт | 9 млрд | 9 млрд | ✅ ок |

**Вывод:** P0.1, P0.2 и P0.3 закрыли все острые проблемы массовой генерации. Продукт готов к использованию внешними QA-командами.

---

# 🔴 P0 — Предрелизные блокеры (ЗАКРЫТЫ)

## ✅ P0.1 — Расширение пула ФИО
- 5 списков в `src/main/resources/names/*.txt` (10 файлов). Ёмкость: **~3.8 млн** уникальных ФИО.

## ✅ P0.2 — AddressPool
- `Region` расширен с 5 до **16 регионов**. Адреса — из реального справочника КЛАДР (`AddressBook`), а не из пула.
- `AddressBook` на реальных данных КЛАДР. Каскады `region → адрес`, `region → паспорт`, `region → КПП` для всех 16.

## ✅ P0.3 — Email / SecretWord / Manager
- `Transliterator` по ГОСТ Р 52535.1-2006.
- `ContactGenerator.email(PersonName, EmailDomainMode)` — `petrov.si@test.ru`.
- 20 доменов (TEST/REALISTIC/MIXED). 298 слов, 50 менеджеров.
- Override для email, слова, менеджера.

---

# 🔴 P1 — MVP для QA-инженера

## 📅 P1.1 — Maven Central (отложено)

**Статус:** отложено до покупки домена `validum.tech` (нужен для верификации `groupId` в Sonatype).

**Что нужно будет сделать:** `maven-publish`, `signing`, GPG-ключ, Sonatype Central Portal, `./gradlew publish`, Sources JAR + Javadoc JAR.

**Оценка:** 1 день.

## ✅ P1.2 — Batch-генерация
- 3 эндпоинта: `POST /api/v1/generate/{individual,ip,organization}/batch`.
- Валидация `count` (1–10 000). Вне диапазона → 400.
- **Детерминизм:** первый профиль `seed`, второй `seed+1` и т.д.

## ✅ P1.3 — Validation API
- 6 эндпоинтов: `POST /validate/{inn,snils,ogrn,account,kpp,bik}`.
- Автоопределение типа (ИНН ФЛ/ЮЛ, ОГРН ЮЛ/ОГРНИП).
- `BikValidator` возвращает название банка.

## ✅ P1.4 — Экспорт (CSV / JSON Array)
- `CsvExporter` — плоский CSV, экранирование, UTF-8 BOM.
- `?format=json|json-array|csv` для батчей.
- **P3.1a:** `CsvExporter` переехал в core.

---

# 🟠 P2 — Расширение домена

## ✅ P2.2 — Расширение ОПФ ЮЛ
- `OrganizationType` — **7 типов**: `LLC`, `JSC`, `PJSC`, `NCO`, `FOUNDATION`, `SE`, `ME`.
- `IP` сознательно не добавлен (ИП = ФЛ + ОГРНИП).
- `okfs` / `budget` переехали в `OrganizationType`.

## ✅ P2.3 — Валидаторы ОКПО / ОГРНИП + расширенный КПП
- `OkpoValidator` — 8 (ЮЛ) / 10 (ИП), автоопределение по длине.
- `OgrnipValidator` — 15 цифр, mod 13. Отдельный фасад.
- `KppValidator` расширен — все 4 группы (регион, ИФНС, причина, порядковый) ≠ 0.
- 2 новых эндпоинта: `/validate/okpo`, `/validate/ogrnip`.
- Round-trip тесты (`ValidatorRoundTripTest`).
- **Итог:** +28 core, +10 api.

## 📅 P2.1 — Транзакции (отложено)

**Статус:** отложено. Приоритет отдан CLI и Allure. Вернёмся, когда появится реальный запрос от QA.

**Модель:** `Transaction { id, type, amount, currency, date, purpose, payerAccount, payeeAccount, payerBankBik, payeeBankBik, payerInn, payeeInn, status }`.

**API:** `POST /api/v1/generate/transaction[/batch]`.

**Оценка:** 2–3 дня.

---

# 🟡 P3 — Killer features

## ✅ P3.1 — CLI + CSV в core

### P3.1a — Вынос CSV в core
- `core/export/CsvExporter` (generic `List<Map<String,String>> → CSV`, RFC 4180 + BOM).
- 3 флаттенера: `IndividualProfileFlattener`, `IpProfileFlattener`, `OrganizationProfileFlattener`.
- `validum-api/export/CsvExporter` удалён. `GenerationService` raw-методы.
- +18 core-тестов. Было 478 → стало 525.

### P3.1b — CLI на Picocli
- Новый модуль `validum-cli`, зависимости — только `validum-core` + `picocli` + `jackson`.
- 7 корневых команд (generate, fill, validate, participants, scenarios, fetch-bik, version).
  `--format json|json-array|csv`. `--output` (UTF-8) / stdout (UTF-8 форс).
- `fetch-bik --offline` — для банковских контуров. `validate` → exit 0/1.
- Fat JAR через GradleUp shadow 8.3.7 (~15 МБ со справочниками). +34 cli-теста.

## ✅ P3.2 — Allure Extension (JUnit 5)

**Зачем:** QA видят в Allure-отчёте, с какими данными упал/прошёл тест.

**Что сделано:**
- Новый модуль `validum-allure`. Зависимости: `validum-core` + `allure-jupiter:2.35.3` + `junit-jupiter-api` + `jackson`.
- `@ValidumSeed(long)` / `@ValidumGenerate(seed, age, region, gender, categoryClient)`.
- `ValidumExtension implements BeforeEachCallback`. Плоский JSON attachment (`IndividualProfileFlattener`).
- **Best-effort attach** — не ломает тесты, если Allure runtime отсутствует.
- **Только ФЛ** (ИП/ЮЛ — P3.2b, если попросят). +19 allure-тестов.

## ✅ P3.3 — `.bik(String)` / `.bank(Bank)` (ЗАКРЫТА)

**Зачем:** банк выбирался случайно из справочника. QA хотят фиксировать «тест со Сбербанком».

**Что сделано:**
1. **`.bik(String)`** в `IndividualProfileGenerator.Builder` — поиск в `BikSource`. Не найден → `IllegalArgumentException`.
2. **`.bank(Bank)`** — готовый объект (например, из `BikSources.resolve(...).findByBik(...)`). Приоритет > `.bik`.
3. **REST:** `bik` в `IndividualGenerationRequest`, `IpGenerationRequest`, `IndividualBatchRequest`, `IpBatchRequest` (`@Pattern("\\d{9}")`).
4. **`GenerationService`:** `bik` в raw-методах (одиночные и батчи).
5. **`GlobalExceptionHandler`:** `IllegalArgumentException → 400 {"error":"invalid_argument","message":"..."}`.
6. **CLI:** `--bik` в `generate individual` и `generate ip`. Невалидный БИК → exit 2.
7. **ЮЛ не трогаем** — у `OrganizationProfile` нет `Bank`.

**Итог:** +10 core, +11 api, +9 cli. Было 544 → стало **574**.

**Ключевые решения:**
- **Невалидный БИК — явное исключение, не откат на случайный.** REST 400, CLI exit 2.
- **Приоритет:** `bank` > `bik` > случайный.
- **ФЛ, ИП и ЮЛ с банком.** ЮЛ получил `bank` + `account` в P2.4.

---

## ⏭️ P4.1 — Импорт CSV (fill gaps) (следующая)

**Зачем:** QA хотят «взять существующий CSV, заполнить пустые ячейки валидными данными, сохранить». Это сценарий «у меня есть 50 профилей, но у 20 пустые ИНН — заполни».

**Что делать:**
1. **`CsvImporter`** в `validum-core/export` — читает CSV (RFC 4180, BOM, экранирование), возвращает `List<Map<String,String>>`.
2. **`ProfileFromCsv`** — собирает `IndividualProfile` из плоских полей (обратный флаттенеру).
3. **`CsvFiller`** — берёт CSV-строки, для пустых ячеек генерирует значения через существующие генераторы. Непустые не трогает.
4. **REST:** `POST /api/v1/fill/individual/csv` (multipart) → CSV обратно. Или `POST /api/v1/fill/individual` (JSON).
5. **CLI:** `validum fill individual --input profiles.csv --output filled.csv`.
6. **Тесты:** парсинг, round-trip, fill empty-only.

**DoD:**
- [ ] `CsvImporter` + `CsvFiller` в core.
- [ ] REST-эндпоинт `/fill/*` (JSON и CSV).
- [ ] CLI `validum fill individual`.
- [ ] Round-trip: `flatten(parse(csv)) == csv`.
- [ ] Тесты: 15–20.

**Оценка:** 2–3 дня.

**Что НЕ делаем:** не «дополняем битые» — только пустые. Валидацию битых оставляем на `/validate/*`.

---

## 📅 P3.4 — Mock API по OpenAPI

Генерация JSON-ответов по схеме. Конкурент Mockaroo.

**Оценка:** 3–5 дней.

---

# 🟢 P4 — На будущее

| # | Фича | Оценка |
|---|---|---|
| P4.2 | Иностранные паспорта (5–7 стран) | 2 дня |
| P4.3 | Веб-UI для демо | 3–5 дней |
| P4.4 | ML-датасеты (связанные таблицы) | 5+ дней |
| P2.4 | ✅ Банк для ЮЛ (`.bik` / `.bank` в `OrganizationProfile`) | сделано 30.10 |
| P3.2b | Allure для ИП и ЮЛ | 0.5 дня (если попросят) |

---

# ❌ Что НЕ делаем

| Идея | Почему |
|---|---|
| LLM-генерация | Не гарантирует валидность |
| Собственный BIK-скрапер через Selenium | Хрупко |
| Публичный SaaS | Self-hosted модель |
| Плагин для JUnit 4 / pytest | JUnit 5 + REST/CLI |
| Интеграция с конкретной АБС | Пока только генерация |
| Универсальный экспорт под любую БД | Плоский CSV |
| Дублирование ИП в `OrganizationType` | ИП = ФЛ + ОГРНИП |
| 1:1 DTO в CLI и Allure | Плоский JSON = CSV |
| Interactive REPL в CLI | Скрипты |
| Config-файлы в CLI | Флагов достаточно |
| Fake `ExtensionContext` в тестах | Структурные тесты + реальный JUnit Platform |
| **Откат на случайный БИК при невалидном (P3.3)** | **Невалидный запрос должен явно провалиться** |

---

# 📊 Метрики успеха

| Метрика | Сейчас | После P4.1 | Цель через 3 мес |
|---|---|---|---|
| Тестов | **769** | 700+ | 750+ |
| Бизнес-эндпоинтов | **16** | 17–18 | 25+ |
| CLI-команд | **5** | 6 (fill) | 7+ |
| Allure аннотаций | **2** | 2 | 3+ |
| Сценариев | 6 | 6 | 15+ |
| Валидаторов | **8** | 8 | 10+ |
| ОПФ ЮЛ | **7** | 7 | 10+ |
| Скачиваний Maven Central | — | — | 500+/мес |
| Внешних потребителей | 0 | 0 | 5+ компаний |

---

# 🎯 Порядок работы

## Ближайшая последовательность

1. **P4.1** — Импорт CSV (fill gaps) (2–3 дня)
2. Пауза / покупка домена → **P1.1** (Maven Central)

**После P4.1** — полноценный MVP: REST + batch + validation (8 эндпоинтов) + экспорт + CLI + Allure + явный выбор банка + импорт CSV.

## Правило завершения итерации

1. Прогон `./gradlew test --rerun-tasks`
2. Все зелёные
3. Обновить счётчики в HANDOFF/SCENARIOS/README
4. Коммит + push
5. CI зелёный
6. Только потом — следующая

---

**Validum** — данные, которые проходят валидацию. **769 тестов. REST API + CLI + Java-библиотека + Allure. P0.1–P0.3, P1.2–P1.4, P2.2, P2.3, P3.1–P3.3, P4.1 закрыты, ДУЛ (11 видов), 9 КУС-сценариев, ED807/ED808.**
