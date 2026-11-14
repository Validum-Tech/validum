# Проект

Как устроен репозиторий: архитектура, структура, релизы, участие.

## 🏗️ Архитектура

```
┌──────────────────────┐  ┌──────────────────────┐  ┌──────────────────────┐
│  validum-api         │  │  validum-cli         │  │  validum-allure      │
│  (Spring Boot 3.5)   │  │  (Picocli, ~15 МБ)   │  │  (JUnit 5 Extension) │
│  19 REST-эндпоинтов  │  │  10 команд            │  │  @ValidumSeed        │
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
├── validum-core/                  ← 651 тест (zero-dep)
│   ├── src/main/java/ru/validum/core/
│   │   ├── random/ · common/ · abs/ · inn/ · snils/ · ogrn/
│   │   ├── bank/ · account/ · name/ · passport/ · address/ · contact/
│   │   ├── profile/       ← IndividualProfileGenerator + .bik / .bank
│   │   ├── organization/
│   │   ├── export/        ← CsvExporter + 3 флаттенера
│   │   └── validation/    ← 7 валидаторов
│   └── src/main/resources/  ← names/ · addresses/ · emails/ · secret_words.txt · managers.txt
│
├── validum-api/           ← 88 тестов (Spring Boot)
│   └── src/main/java/ru/validum/api/
│       ├── config/ · controller/ (+GlobalExceptionHandler — P3.3)
│       ├── service/ · mapper/ · dto/
│
├── validum-cli/           ← 100 тестов (Picocli, без Spring)
│   └── src/main/java/ru/validum/cli/
│       ├── ValidumCli · OutputFormat · OutputWriter · BikSourceResolver
│       ├── Generate{Individual,Ip,Organization}Command (+ --bik в individual/ip)
│       └── ValidateCommand · FetchBikCommand · ScenariosCommand · VersionCommand
│
├── validum-allure/        ← 21 тест (JUnit 5 Extension, без Spring)
│   └── src/main/java/ru/validum/allure/
│       ├── ValidumSeed · ValidumGenerate · ValidumExtension
│
├── data/ED807.xml         ← Справочник ЦБ, НЕ коммитится
├── docs/                  ← README · SCENARIOS · ROADMAP · COMPATIBILITY · recipes/ · reference/{cbr,dul}
├── docker-compose.yml
└── gradle/libs.versions.toml
```

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
| Java-библиотеки (`validum-core`, `validum-allure`, …) | Maven Central: `io.github.validum-tech:*:0.3.1` (с первого релиза в Central; подключать репозиторий не нужно) |
| Контейнер API | `docker compose up` локально; Docker Hub — после 0.2 |
| Исходники | `git clone` → `./gradlew build` |

Как выпускается версия (тег, секреты, Maven Central) — [`docs/RELEASING.md`](RELEASING.md).
Что мы гарантируем потребителю (JDK, зависимости, контракт вывода, ломающие изменения) — [`docs/COMPATIBILITY.md`](COMPATIBILITY.md).

---

## 🤝 Для контрибьюторов

- **JDK 21** для сборки (toolchain), байткод CLI/библиотек — 17
- Java 21 Records, immutable, каскадные дефолты
- Фасады только для 3+ связанных полей
- Тесты: `src/test/`, имя файла = имя класса, `@DisplayName` на русском
- Core — zero-dep, CLI и Allure — без Spring
- CSV и флаттенеры — в core
- **P3.3: невалидный БИК → явное исключение**

### Как контрибьютить

1. Форк → ветка `feature/my-feature`
2. Тесты **до** реализации
3. `./gradlew build` — все 860 зелёные, покрытие не падает
4. PR — CI прогонит сборку на Linux и Windows

Подробности и чек-лист — [`CONTRIBUTING.md`](CONTRIBUTING.md).
Уязвимости — не в Issues, а на `security@validum.tech` ([`SECURITY.md`](SECURITY.md)).

---
