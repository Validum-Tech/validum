# REST API

Spring Boot-сервис: batch-эндпоинты, fill, validate, scenarios, export.

## 🎯 Четыре способа использовать

| Способ | Когда нужен | Как |
|---|---|---|
| **REST API** | Автотесты (JUnit + REST Assured), Swagger UI | `docker compose up` → `POST /api/v1/generate/...` |
| **CLI** | Скрипты сетапа, CI-пайплайны, «100 профилей в CSV» | `java -jar validum.jar generate individual --count 100 --format csv -o profiles.csv` |
| **Allure Extension** | JUnit 5: attach профиля в Allure-отчёт | `@ValidumSeed(42)` / `@ValidumGenerate(...)` |
| **Java-библиотека** | Свой генератор поверх core | `<dependency>io.github.validum-tech:validum-core:0.4.1</dependency>` (Maven Central) |

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

В каждом профиле есть `personId` вида `pd-9f2a6c1d0b7e4a53` — он выводится из seed
(в батче seed профиля = базовый + индекс), поэтому один и тот же человек всегда получает
один и тот же идентификатор в CLI, REST и CSV. Это основа адресного доступа к сущности.

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
# validum 0.4.1 coverage · file=clients.csv · rows=100

поле               заполнено  различных  невалидных
--------------------------------------------------
fullName                100% (100/100)        100          —
inn                     100% (100/100)        100          0
snils                   100% (100/100)         99          1
account                 100% (100/100)        100          0

итог: 22 поля, 1 с невалидными значениями, 0 с пустыми

Валидность значений: проверено 4 полей, невалидных 1 из 400 значений
Согласованность набора (правила: документ/возраст, серия/регион выдачи, счёт/банк): нарушений нет
```

Две строки внизу — про разное: «валидность» это контрольные суммы отдельных значений,
«согласованность набора» — смысловые правила между полями одной записи (права не могут быть
выданы до 18 лет, серия паспорта и «кем выдан» должны указывать на один регион, счёт — на свой банк).

Проверяются контрольные суммы ИНН, СНИЛС, ОГРН/ОГРНИП, КПП, ОКПО и ключ счёта (для счёта берётся
БИК из той же строки); остальные поля — по полноте и различию. Понимает CSV (машинный и
`csv-semicolon` — BOM и `;` распознаются), JSON и JSON-массив; формат можно задать `--format`.

### ✂️ Только нужные колонки

Полный профиль ФЛ — 64 колонки, и в большинстве сценариев столько не нужно. `--fields` оставляет
только запрошенные, не меняя остального вывода:

```bash
java -jar validum.jar generate individual --seed 42 -n 3 --format csv \
  --fields inn,fullName,age,registrationRegion -o short.csv
```

Пресет `short` — готовый «человеческий» набор (кто это, реквизиты, адрес, банк, счёт, документы):

```bash
java -jar validum.jar generate individual --seed 42 -n 3 --format csv --fields short -o people.csv
```

Название колонки проверяется: опечатка не уходит в пустой столбец, а падает с ошибкой
«Неизвестные колонки: passport. В наборе есть, например: personId, fullName, …». Порядок колонок
при явном списке — как у профиля, поэтому файл остаётся стабильным между прогонами. В спецификации
то же поле называется `"fields"`, а в REST — параметр запроса `?fields=short` — только для `format=csv`
и `csv-semicolon`: для `json`/`json-array` набор уже структурирован, и такой запрос честно отбивается
`400 {"error":"invalid_argument"}` вместо тихого игнорирования.

### 🧭 Найти человека в наборе

Сгенерировали тысячу профилей, тест упал на одной строке — как понять, что в ней? Сначала индекс:

```bash
java -jar validum.jar generate individual --seed 42 -n 1000 --format csv-semicolon -o clients.csv --index
```

`--index` печатает в stderr (данные не портит) таблицу «№ · идентификатор · ФИО · возраст · регион ·
банк · счёт». Дальше — карточка нужного человека:

```bash
java -jar validum.jar describe --file clients.csv --person pd-364c4379da9ab...   # или --row 3
```

`describe` показывает паспорт (серия, номер, кем выдан, срок, код подразделения), дополнительные
документы, адреса с индексами, банк и счёт, флаги — и проверки: ИНН, СНИЛС, контрольный ключ счёта и
согласованность набора (возраст против документов, регион против «кем выдан»). Читает и CSV с
русскими заголовками, и JSON.

### 🧪 Негативные данные (`--corrupt`)

Иногда нужен профиль, который валидатор АБС **обязан** отвергнуть — с ровно одной ошибкой, чтобы
тест падал на ней, а не на «всё сломалось»:

```bash
java -jar validum.jar generate individual --seed 7 --corrupt inn -o bad-inn.json
java -jar validum.jar generate individual --seed 7 -n 10 --corrupt inn,snils --format csv-semicolon -o bad-set.csv
java -jar validum.jar generate individual --seed 7 --corrupt address -o bad-address.json
```

| Поле | Что ломается |
|---|---|
| `inn` | контрольные разряды ИНН не сходятся |
| `snils` | контрольное число СНИЛС не сходится |
| `address` | индекс пустой, строка адреса — «ул. , д. , кв. » |

Остальные поля остаются согласованными. В stderr печатается `# испорчено: inn,address` — по этой
строке в отчёте теста видно, что набор испорчен намеренно. В спецификации: `"corrupt": ["inn"]`.

---
