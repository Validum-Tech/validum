# 📚 Validum — документация

> Единая точка входа. Что читать — зависит от того, кто ты и зачем пришёл.

---

## 🚀 Быстрый старт (5 минут)

### Вариант 1: CLI (без Docker, без Spring)

```powershell
.\gradlew :validum-cli:shadowJar
java -jar .\validum-cli\build\libs\validum.jar fetch-bik -o data/ED807.xml
java -jar .\validum-cli\build\libs\validum.jar generate individual --seed 42 --format csv -o profile.csv
```

### Вариант 2: REST API

```bash
docker compose up -d
# → http://localhost:8080/swagger-ui.html
curl -X POST http://localhost:8080/api/v1/generate/individual -d '{"seed":42}' -H "Content-Type: application/json"
```

### Вариант 3: Allure Extension

```java
@ExtendWith(ValidumExtension.class)
class MyTest {
  @Test
  @ValidumSeed(42)
  void test() {
    // Профиль с seed=42 — в Allure-отчёте
  }
}
```

Полная инструкция — в [README.md](../README.md).

---

## 👤 Кто ты?

### 🧪 QA-инженер — генерирую данные для тестов

| Что нужно | Куда |
|---|---|
| Что умеет Validum | [README.md](../README.md) → «Что уже работает» |
| Готовые сценарии | [SCENARIOS.md](SCENARIOS.md) → раздел 2 |
| Комбинирование параметров | [SCENARIOS.md](SCENARIOS.md) → раздел 3 |
| Генерировать из скрипта | [README.md](../README.md) → «CLI» |
| Attach профиля в Allure | [README.md](../README.md) → «Allure Extension» |
| Явный банк (`.bik`) | [README.md](../README.md) → «Явный выбор банка» |
| REST API | [README.md](../README.md) → «REST API» |
| Проверка данных | [README.md](../README.md) → «Валидация» |
| Встроить в Java-тесты | [README.md](../README.md) → «Java-библиотека» |
| Готовые рецепты | [recipes/README.md](recipes/README.md) |

### 🛠 Разработчик — понимаю, как устроено

| Что нужно | Куда |
|---|---|
| Архитектура | [README.md](../README.md) → «Архитектура» |
| Принципы проектирования | [SCENARIOS.md](SCENARIOS.md) → «Принципы проектирования» |
| Соглашения по коду | [CONTRIBUTING.md](../CONTRIBUTING.md) |
| План | [ROADMAP.md](ROADMAP.md) |
| Данные (ФИО, адреса, email) | [SCENARIOS.md](SCENARIOS.md) → 5.5 |
| Экспортные колонки | [SCENARIOS.md](SCENARIOS.md) → 5.6 |
| Нормативные форматы (БИК, счёт, PtType) | [SCENARIOS.md](SCENARIOS.md) → 5.7 |

### 🏗 Интегратор — встраиваю в свой процесс

| Что нужно | Куда |
|---|---|
| REST API | [README.md](../README.md) → «REST API» |
| CLI для скриптов | [README.md](../README.md) → «CLI» |
| Allure Extension | [README.md](../README.md) → «Allure Extension» |
| Java-библиотека | [README.md](../README.md) → «Java-библиотека» |
| Docker | [README.md](../README.md) → «Docker» |
| Артефакты и релизы | [RELEASING.md](RELEASING.md) |

### 📊 Менеджер — статус и планы

| Что нужно | Куда |
|---|---|
| Статус (тесты, покрытие) | [README.md](../README.md) → «Тесты» |
| Что закрыто / что осталось | [ROADMAP.md](ROADMAP.md) |
| Метрики | [ROADMAP.md](ROADMAP.md) → «Метрики успеха» |

---

## 📂 Структура документации

```
docs/
├── README.md          ← ты здесь (точка входа)
├── SCENARIOS.md       ← карта сценариев, каскады, поля профиля, CLI/Allure/.bik
├── ROADMAP.md         ← бэклог, приоритеты, статус
├── RELEASING.md       ← как выпускать релиз и проверять артефакты
└── recipes/README.md  ← 14 рецептов «задача → команда → результат»
```

| Файл | Аудитория | О чём |
|---|---|---|
| [SCENARIOS.md](SCENARIOS.md) | QA, разработчики | Типы клиентов, фасады, каскады, поля, контракт REST/CLI/Allure/Java, валидация, экспорт, `.bik` |
| [ROADMAP.md](ROADMAP.md) | Менеджеры, разработчики | Приоритеты P0–P4, статус итераций, метрики |
| [RELEASING.md](RELEASING.md) | Мейнтейнеры | Версия, тег, публикация, проверка артефактов |
| [recipes/README.md](recipes/README.md) | QA, интеграторы | 14 готовых сценариев: фикстуры, CSV/SQL, нерезиденты, ДУЛ, ED807/ED808, детерминизм, Docker |
| [../CHANGELOG.md](../CHANGELOG.md) | Все | Что вошло в 0.1.0 |
| [../README.md](../README.md) | Все | Основная документация |

---

## 🔍 Частые вопросы

**Это облачный сервис?**
Нет. Self-hosted, работает внутри периметра.

**Можно боевые данные?**
Нет, нарушение 152-ФЗ. Validum — синтетические.

**Данные детерминированы?**
Да, при фиксированной дате расчёта: `--seed 42 --as-of 2026-01-01` даёт один и тот же профиль
сегодня и через полгода. Без `--as-of` дата расчёта — сегодняшняя, поэтому возраст и даты
документов смещаются вместе с календарём.

**Чем REST отличается от CLI?**
REST — для автотестов (JUnit + REST Assured), Swagger UI. CLI — для скриптов сетапа, CI.
CLI не тянет Spring → ~15 МБ со справочниками, старт ~300 мс.

**CLI JSON 1:1 с REST?**
Нет. CLI выдаёт **плоский JSON** (те же поля, что CSV). Осознанно. Нужен 1:1 REST — используй REST.

**Что делает Allure Extension?**
Перехватывает тесты с `@ValidumSeed` / `@ValidumGenerate`, генерирует профиль, прикладывает плоский JSON в Allure. Best-effort: без Allure runtime — warning в stderr, тест не падает. Только ФЛ.

**Как задать конкретный банк?**
- Java: `.bik("044525225")` или `.bank(Bank)`
- REST: `{ "bik": "044525225" }`
- CLI: `--bik 044525225`
- Приоритет: `bank` > `bik` > случайный.

**Что если БИК не найден?**
`IllegalArgumentException` → REST 400 `{"error":"invalid_argument","message":"..."}`, CLI exit 2. **Не откатываемся** на случайный банк.

**ЮЛ и `.bik`?**
`OrganizationProfile` содержит `bank` и `account`: БИК из справочника + расчётный счёт 40702,
ключ по письму ЦБ № 515. CLI: `generate organization --bank-bik …`.

**Какие регионы?**
16: Москва, СПб, Московская, Краснодарский, Свердловская, Ленинградская, Новосибирская,
Ростовская, Башкортостан, Татарстан, Челябинская, Самарская, Нижегородская, Красноярский,
Приморский, Саратовская.

**Откуда адреса?**
Из КЛАДР: 90 субъектов, 125 413 населённых пунктов, 1 993 702 улицы. Дом и квартира —
процедурные, в пределах реального диапазона улицы.

**Как проверить, что данные валидны?**
REST: 8 эндпоинтов `/validate/*`. CLI: `validum validate <type> <value>` (exit 0/1).
Round-trip тесты гарантируют, что всё сгенерированное проходит валидацию.

**Чем `/validate/ogrn` от `/validate/ogrnip`?**
`/validate/ogrn` автоопределяет: 13 — ЮЛ, 15 — ОГРНИП. `/validate/ogrnip` — **только** 15-значный, вернёт `false` для 13-значного.

**Что нового в КПП?**
Все 4 группы (регион, ИФНС, причина, порядковый) ≠ 0.

**Где справочник банков?**
Полный ED807 обязателен: `validum fetch-bik -o data/ED807.xml`. Дальше он ищется в порядке:
явный `--file` → переменная `VALIDUM_BIK_FILE` → `data/ED807.xml`. Встроенного демо-набора нет —
без файла банковские сценарии отвечают `503 bik_source_missing` с подсказкой.

**`fetch-bik` за firewall?**
`--offline` — не скачивает, печатает инструкцию, exit 0. Без флага — понятное сообщение о причинах (сеть, прокси, `--offline`).

**JUnit 4 / pytest?**
Нет. JUnit 5 (через Allure Extension) и REST/CLI для любых языков.

**Где взять библиотеку?**
GitHub Packages: `io.github.validum-tech:validum-core:0.1.0` и `validum-allure:0.1.0`.
Сборка из исходников — `./gradlew publishToMavenLocal`. Maven Central — после настройки секретов,
см. [RELEASING.md](RELEASING.md).

---

## 🗺 Что дальше

Бэклог 0.2: транзакции (ED101, выписки ED210/211), справочники ГАР/ОКАТО, OpenAPI + SDK,
mock-сервер API. Полный список — в [ROADMAP.md](ROADMAP.md).

---

**Validum** — данные, которые проходят валидацию. **721 тест. REST API + CLI + Java-библиотека + Allure. 8 валидаторов. 7 ОПФ. ДУЛ, КЛАДР, ОКВЭД, ED807/ED808.**
