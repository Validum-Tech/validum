# 📚 Validum — документация

> Единая точка входа. Что читать — зависит от того, кто ты и зачем пришёл.

---

## 🚀 Быстрый старт (5 минут)

### Вариант 1: CLI (без Docker, без Spring)

```powershell
.\gradlew :validum-cli:shadowJar
java -jar .\validum-cli\build\libs\validum.jar generate individual --seed 42 --format csv -o profile.csv
```

### Вариант 2: REST API

```bash
docker compose up -d
# → http://localhost:8080/swagger-ui.html
curl -X POST http://localhost:8080/api/v1/generate/individual -d '{"seed":42}' -H "Content-Type: application/json"
```

### Вариант 3: Allure Extension (P3.2)

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
| Генерировать из скрипта | [README.md](../README.md) → «CLI (P3.1)» |
| Attach профиля в Allure | [README.md](../README.md) → «Allure Extension (P3.2)» |
| **Явный банк (`.bik`)** | **[README.md](../README.md) → «Явный выбор банка (P3.3)»** |
| REST API | [README.md](../README.md) → «REST API» |
| Проверка данных | [README.md](../README.md) → «Валидация» |
| Встроить в Java-тесты | [README.md](../README.md) → «Java-библиотека» |

### 🛠 Разработчик — понимаю, как устроено

| Что нужно | Куда |
|---|---|
| Архитектура | [README.md](../README.md) → «Архитектура» |
| Принципы проектирования | [SCENARIOS.md](SCENARIOS.md) → «Принципы проектирования» |
| Соглашения по коду | [README.md](../README.md) → «Для контрибьюторов» |
| План | [ROADMAP.md](ROADMAP.md) |
| Данные (ФИО, адреса, email) | [SCENARIOS.md](SCENARIOS.md) → 5.5 |
| Экспортные колонки | [SCENARIOS.md](SCENARIOS.md) → 5.6 |
| Нормативные форматы (БИК, счёт, PtType) | [reference/cbr/README.md](reference/cbr/README.md) |

### 🏗 Интегратор — встраиваю в свой процесс

| Что нужно | Куда |
|---|---|
| REST API | [README.md](../README.md) → «REST API» |
| CLI для скриптов | [README.md](../README.md) → «CLI (P3.1)» |
| Allure Extension | [README.md](../README.md) → «Allure Extension (P3.2)» |
| Java-библиотека | [README.md](../README.md) → «Java-библиотека» |
| Docker | [README.md](../README.md) → «Docker» |
| Maven Central | [ROADMAP.md](ROADMAP.md) → P1.1 (отложено) |

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
├── README.md          ← ты здесь (публичный)
├── SCENARIOS.md       ← карта сценариев, каскады, поля профиля, CLI/Allure/.bik
├── ROADMAP.md         ← бэклог, приоритеты, статус
├── recipes/README.md  ← 14 рецептов «задача → команда → результат»
│
├── reference/         ← нормативная база и форматы (публичная)
│   ├── cbr/           ← УФЭБС ЦБ РФ: форматы, коды, маски, счёт по 809-П
│   └── dul/           ← ДУЛ: форматы документов и источники (исследование)
│
└── internal/          ← только для автора (в .gitignore / локально)
    ├── HANDOFF.md     ← контекст сессии
    ├── PLAN-2026-11.md
    └── competitor-analysis.md
```

| Файл | Аудитория | О чём |
|---|---|---|
| [SCENARIOS.md](SCENARIOS.md) | QA, разработчики | Типы клиентов, фасады, каскады, поля, контракт REST/CLI/Allure/Java, валидация, экспорт, `.bik` |
| [ROADMAP.md](ROADMAP.md) | Менеджеры, разработчики | Приоритеты P0–P4, статус итераций, метрики |
| [recipes/README.md](recipes/README.md) | QA, интеграторы | 14 готовых сценариев: фикстуры, CSV/SQL, нерезиденты, ДУЛ, ED807/ED808, детерминизм, Docker |
| [../CHANGELOG.md](../CHANGELOG.md) | Все | Что вошло в 0.1.0 |
| `internal/HANDOFF.md` | Только автор | Локальный контекст сессии (в `.gitignore`) |
| [../README.md](../README.md) | Все | Основная документация |
| [reference/cbr/README.md](reference/cbr/README.md) | Все | Нормативная база УФЭБС ЦБ РФ v2026.09.0: PtType, форматы счёта/БИК/ИНН, ED-сообщения |
| [reference/dul/research-dul-formats.md](reference/dul/research-dul-formats.md) | Все | ДУЛ: 15 видов документов, подтверждённые форматы и пробелы |

---

## 🔍 Частые вопросы

**Это облачный сервис?**
Нет. Self-hosted, работает внутри периметра.

**Можно боевые данные?**
Нет, нарушение 152-ФЗ. Validum — синтетические.

**Данные детерминированы?**
Да. `seed=42` всегда даёт одинаковый профиль. `seed=42, count=100` — первый как одиночный с seed=42, второй seed=43 и т.д.

**Чем REST отличается от CLI?**
REST — для автотестов (JUnit + REST Assured), Swagger UI. CLI — для скриптов сетапа, CI. CLI не тянет Spring → ~15 МБ со справочниками, старт ~300 мс.

**CLI JSON 1:1 с REST?**
Нет. CLI выдаёт **плоский JSON** (те же поля, что CSV). Осознанно. Нужен 1:1 REST — используй REST.

**Что делает Allure Extension?**
Перехватывает тесты с `@ValidumSeed` / `@ValidumGenerate`, генерирует профиль, прикладывает плоский JSON в Allure. Best-effort: без Allure runtime — warning в stderr, тест не падает. Только ФЛ.

**Как задать конкретный банк (P3.3)?**
- Java: `.bik("044525225")` или `.bank(Bank)`
- REST: `{ "bik": "044525225" }`
- CLI: `--bik 044525225`
- Приоритет: `bank` > `bik` > случайный.

**Что если БИК не найден (P3.3)?**
`IllegalArgumentException` → REST 400 `{"error":"invalid_argument","message":"..."}`, CLI exit 2. **Не откатываемся** на случайный банк.

**ЮЛ и `.bik`?**
`OrganizationProfile` получил `bank` и `account` (P2.4): БИК из справочника + расчётный счёт 40702, ключ по письму 515. CLI `generate organization --bank-bik …`.

**Какие регионы?**
16: Москва, СПб, Московская, Краснодарский, Свердловская, Ленинградская, Новосибирская, Ростовская, Башкортостан, Татарстан, Челябинская, Самарская, Нижегородская, Красноярский, Приморский, Саратовская. По 20 адресов.

**Как проверить, что данные валидны?**
REST: 8 эндпоинтов `/validate/*`. CLI: `validum validate <type> <value>` (exit 0/1). Round-trip тесты (`ValidatorRoundTripTest`) гарантируют, что всё сгенерированное проходит валидацию.

**Чем `/validate/ogrn` от `/validate/ogrnip`?**
`/validate/ogrn` автоопределяет: 13 — ЮЛ, 15 — ОГРНИП. `/validate/ogrnip` — **только** 15-значный, вернёт `false` для 13-значного (P2.3).

**Что нового в КПП?**
Все 4 группы (регион, ИФНС, причина, порядковый) ≠ 0 (P2.3).

**Где справочник банков?**
Топ-30 встроен. Полный ED807 (370): `validum fetch-bik -o data/ED807.xml`, или через `VALIDUM_BIK_FILE`, или `fetch-bik --offline` в банковском контуре.

**`fetch-bik` за firewall?**
`--offline` — не скачивает, печатает инструкцию, exit 0. Без флага — понятное сообщение о причинах (сеть, прокси, `--offline`).

**JUnit 4 / pytest?**
Нет. JUnit 5 (через Allure Extension — P3.2) и REST/CLI для любых языков.

**Maven Central?**
Отложено до покупки `validum.tech` (верификация groupId). См. [ROADMAP.md](ROADMAP.md) → P1.1.

---

## 🗺 Что дальше

Следующая итерация — **P4.1 (Импорт CSV — fill gaps)**. Полный бэклог — в [ROADMAP.md](ROADMAP.md).

---

**Validum** — данные, которые проходят валидацию. **769 тестов. REST API + CLI + Java-библиотека + Allure. 8 валидаторов. 7 ОПФ. ДУЛ, КЛАДР, ОКВЭД, ED807/ED808.**
