# Validum — карта сценариев

> Дата: 22 октября 2026
> Версия: 4.1 (Итерации 3–7.5 + P0.1–P0.3 + P1.2–P1.4 + P2.2 + P2.3 + P3.1–P3.3 закрыты)
> Статус: MVP + каскадные дефолты + ЮЛ/ИП + вынос данных в ресурсы + Batch + Validation API (8 эндпоинтов) + Экспорт (CSV в core) + 7 ОПФ ЮЛ + CLI + Allure Extension + `.bik` / `.bank`.

---

## 🎯 Принципы проектирования

1. **Доменное — динамика.** ФИО, ДР, паспорт, ИНН, СНИЛС, ОГРН, КПП, адреса, email — всё генерируется.
2. **АБС-специфичное — константы в одном месте.** Все «магические» значения живут в `AbsDefaults`.
3. **Валидность — дефолт.** Портится только явным параметром.
4. **Каскадные дефолты вместо хардкоженных сценариев.**
5. **Параметры ортогональны.**
6. **Мягкие каскады.** Явное значение всегда побеждает auto.
7. **Дефолт 3-в-1, но с override.** `AddressStrategy.SAME_FOR_ALL`.
8. **Гибрид по FIAS.** Полный по умолчанию, `ADDRESS_ONLY` — по параметру.
9. **Воспроизводимость.** Один seed → идентичный профиль.
10. **Фасады — только для 3+ связанных полей.**
11. **Не делаем stub'ы.** QA парсит поля и собирает сам.
12. **Не делаем хардкоженные негативные сценарии.**
13. **Композиция вместо дублирования.** ИП = ФЛ + ОГРНИП.
14. **Детерминизм с seed.**
15. **Данные отделены от кода.**
16. **Батчи — инкремент seed (P1.2).**
17. **Валидация — в ядре (P1.3 + P2.3).**
18. **Экспорт — плоский CSV (P1.4 + P3.1a).** `CsvExporter` + флаттенеры **в core**.
19. **ОПФ — 7 типов (P2.2).**
20. **ИП не в `OrganizationType` (P2.2).**
21. **Валидаторы — отдельные фасады (P2.3).**
22. **CLI — тонкая обёртка над core (P3.1b).** Плоский JSON (те же поля, что CSV).
23. **Allure Extension — тонкая обёртка над core (P3.2).** Плоский JSON.
24. **Явный выбор банка (P3.3).** `.bik(String)` / `.bank(Bank)`. Приоритет: `bank` > `bik` > случайный. Невалидный БИК → явное исключение (400 / exit 2), не откат.

---

## 1. Классификация клиентов

### 1.1 По типу субъекта

| Тип | Приоритет | Статус | Банк (P3.3) |
|---|---|---|---|
| **Физлицо (ФЛ)** | 🔴 высокий | ✅ MVP | `.bik` / `.bank` |
| **ИП** | 🟡 средний | ✅ Итерация 7 | `.bik` / `.bank` (через base) |
| **ЮЛ** | 🟡 средний | ✅ Итерация 7 (7 ОПФ) | ✅ P2.4: банк + счёт 40702 |
| Нерезидент | 🟡 средний | ✅ Итерация 5 | `.bik` / `.bank` |

### 1.2 По резидентству

| Тип | Как получить |
|---|---|
| Резидент РФ | Дефолт (`Residency.residentRU()`) |
| Нерезидент | `.residency(Residency.nonResident(country))` + `.passportType(FOREIGN)` |

### 1.3 По отношению к банку

| Статус | Как получить |
|---|---|
| Со счётом | Дефолт (`.withAccount(true)`) / без `--no-account` |
| Без счёта | `.withAccount(false)` / `--no-account` |
| Зарплатник | `.categoryClient(CATEGORY_SALARY)` |
| Пенсионер | `.categoryClient(CATEGORY_PENSIONER).age(65/60)` |
| **Конкретный банк (P3.3)** | **`.bik("044525225")` / `.bank(Bank)` / `--bik 044525225` / `{"bik":"044525225"}`** |

### 1.4 По возрасту

| Возраст | Что происходит |
|---|---|
| 14–17 | `familyStatus=SINGLE`, паспорт в 14, действ. |
| 18–19 | `familyStatus` случайный, паспорт в 14, действ. |
| 20–44 | Паспорт в 20, действ. |
| 45–59 | Паспорт бессрочный, выдан в 45. |
| 60 / 65 | Возможность `ПЕНСИОНЕР`. |

### 1.5 По флагам

| Флаг | Дефолт | Как получить |
|---|---|---|
| `terrorist` | `false` | `.terrorist(true)` / `--terrorist` |
| `blocked` | `false` | `.blocked(true)` / `--blocked` |

---

## 2. Сценарии: фасады

### 2.1 ✅ ФЛ — `IndividualScenarios`

| # | Метод | Slug | Возраст | Категория | Пакет |
|---|---|---|---|---|---|
| 1 | `createAdultResidentWithAccount()` | `adult-resident-with-account` | 19 | КЛИЕНТ | БАЗОВЫЙ |
| 2 | `createAdultResidentWithoutAccount()` | `adult-resident-without-account` | 19 | КЛИЕНТ | БАЗОВЫЙ |
| 3 | `createPensionerMale()` | `pensioner-male` | **65** | ПЕНСИОНЕР | ПЕНСИОННЫЙ |
| 4 | `createPensionerFemale()` | `pensioner-female` | **60** | ПЕНСИОНЕР | ПЕНСИОННЫЙ |
| 5 | `createSalaryClient()` | `salary-client` | 35 | ЗАРПЛАТНИК | ЗАРПЛАТНЫЙ |
| 6 | `createNonResident(Country)` | — (Java only) | 19 | КЛИЕНТ | БАЗОВЫЙ |

**Два варианта:** `createXxx()` — случайный seed; `createXxx(long seed)` — воспроизводимый.

### 2.2 ✅ ИП — `IpProfileGenerator`

| Метод | Что делает |
|---|---|
| `from(IndividualProfile)` | Из готового профиля ФЛ → ИП |
| `from(IndividualProfile, long seed)` | Детерминизм ОГРНИП |
| `from(IndividualProfile, OgrnGenerator)` | Полный контроль над ОГРНИП |
| `generate(IndividualProfileGenerator)` | Из настроенного генератора ФЛ |
| `generate(IndividualProfileGenerator, long seed)` | Детерминизм |

**Композиция:** ИП = `IndividualProfile` + ОГРНИП + отчество «ИП Автотеста».

### 2.3 ✅ ЮЛ — `OrganizationProfileGenerator`

**7 ОПФ (P2.2):**

| Значение | Префикс | OKOPF | OKFS | Бюджет |
|---|---|---|---|---|
| `LLC` | ООО | 65 | 16 | Небюджетная |
| `JSC` | АО | 47 | 16 | Небюджетная |
| `PJSC` | ПАО | 47 | 16 | Небюджетная |
| `NCO` | НКО | 85 | 16 | Некоммерческая |
| `FOUNDATION` | Фонд | 88 | 16 | Некоммерческая |
| `SE` | ГУП | 61 | 14 | Бюджетная |
| `ME` | МУП | 62 | 13 | Бюджетная |

**Каскады ЮЛ:** `region → registrationAddress + postAddress + kpp`; `organizationType → префикс + okopf + okfs + budget`; `email` не задан → pool-based.

### 2.4 Когда фасад, а когда — параметр

| Что | Как | Почему |
|---|---|---|
| Взрослый ±счёт | Фасад | 2 поля |
| Пенсионер М/Ж | Фасад | 3 поля |
| Зарплатник | Фасад | 2 поля |
| Нерезидент | Фасад | 4 поля |
| ИП | Фасад | Композиция |
| ЮЛ | Фасад | Специфичные классификаторы |
| Несовершеннолетний | `.age(16)` | 1 поле |
| Просроченный паспорт | `.passportWasExchanged(false)` | 1 поле |
| Битый ИНН | `.corrupt(INN)` | 1 поле |
| Email на реалистичном домене | `.emailDomainMode(REALISTIC)` | 1 поле |
| **Конкретный банк (P3.3)** | **`.bik("044525225")` / `.bank(Bank)`** | **1 поле** |

---

## 3. Каскадные дефолты

### 3.1 Принцип

Пользователь задаёт **только важные параметры**. Остальное **досчитывается автоматически**.

### 3.2 Полная каскадная матрица (ФЛ)

| Триггер | Что досчитывается |
|---|---|
| `age` | birthDate, passport.*, familyStatus (если <18) |
| `passportWasExchanged` не задан | `age >= 20` |
| `residency=nonResident(c)` | citizenship, residentCountry, taxResidentCountry |
| `passportType=FOREIGN` без `passportCountry` | из residency |
| `fioScript=LATIN` | ФИО из латинского набора |
| `region` | адреса, birthPlace, серия паспорта |
| `categoryClient` без `accountPackage` | БАЗОВЫЙ / ЗАРПЛАТНЫЙ / ПЕНСИОННЫЙ |
| `age < 18` | `familyStatus=SINGLE` |
| `email` не задан | из ФИО (P0.3) |
| `emailDomainMode` не задан | `TEST` |
| `secretWord` не задан | из 298 |
| `personalManager` не задан | из 50 |
| `corrupt=INN` | ИНН битый |
| `corrupt=SNILS` | СНИЛС битый |
| `terrorist` / `blocked` не заданы | `false` |
| **`bik` не задан (P3.3)** | **случайный банк** |
| **`bik` задан, найден (P3.3)** | **банк из справочника** |
| **`bik` задан, не найден (P3.3)** | **`IllegalArgumentException` → 400 / exit 2** |
| **`bank` задан (P3.3)** | **приоритет над `bik` и случайным** |

### 3.3 Каскады ЮЛ

| Триггер | Что |
|---|---|
| `region` | addresses + kpp |
| `organizationType` без `organizationName` | префикс + название |
| `organizationType` | okopf + okfs + budget |
| `email` не задан | pool-based |

### 3.4 Примеры

```java
// ФЛ — дефолт
IndividualProfile p = IndividualProfileGenerator.defaults().generate();

// ФЛ — нерезидент + латиница + битый ИНН
IndividualProfile broken = IndividualProfileGenerator.builder()
    .residency(Residency.nonResident(Country.TJ))
    .passportType(PassportType.FOREIGN)
    .fioScript(FioScript.LATIN)
    .corrupt(CorruptField.INN)
    .seed(42L).build().generate();

// ФЛ — конкретный банк (P3.3)
IndividualProfile sber = IndividualProfileGenerator.builder()
    .seed(42L)
    .bik("044525225")
    .build().generate();

// ФЛ — готовый Bank (P3.3)
Bank vtb = BikSources.resolve("data/ED807.xml").findByBik("044525187").orElseThrow();
IndividualProfile vtbProfile = IndividualProfileGenerator.builder()
    .seed(42L).bank(vtb).build().generate();

// ИП — воспроизводимо
IpProfile ip = IpProfileGenerator.generate(
    IndividualProfileGenerator.builder()
        .seed(42L).age(35).region(Region.MOSCOW).build(), 42L);

// ЮЛ — ГУП в Москве
OrganizationProfile gup = OrganizationProfileGenerator.builder()
    .seed(42L)
    .organizationType(OrganizationType.SE)
    .region(Region.MOSCOW)
    .build().generate();
```

### 3.5 Правила каскадов

| Правило | Пример |
|---|---|
| **Явное значение побеждает auto** | `.accountPackage("VIP")` перебьёт каскад |
| **Параметры ортогональны** | `age=16` + `fioScript=LATIN` + `corrupt=INN` — комбинируются |
| **Мягкие каскады** | `.age(16).familyStatus(MARRIED)` → MARRIED |
| **Валидность — дефолт** | Без `corrupt(...)` всё сходится |
| **Консистентность гарантируется** | `kpp` начинается с кода региона |
| **Детерминизм с seed** | `.seed(42)` + `IpProfileGenerator.generate(gen, 42)` |
| **P3.3: приоритет** | `bank` > `bik` > случайный |
| **P3.3: явная ошибка** | Невалидный БИК → 400 / exit 2, не откат |

### 3.6 Что порождает невалидный профиль

| Параметр | Что делает профиль невалидным |
|---|---|
| `passportWasExchanged=false` + `age >= 20` | Просроченный паспорт |
| `age < 18` + `withAccount=true` | Несовершеннолетний со счётом |
| `fioScript=LATIN` | ФИО содержит латиницу |
| `corrupt=INN` | ИНН битый |
| `corrupt=SNILS` | СНИЛС битый |
| `terrorist=true` / `blocked=true` | Флаги для негатива |

---

## 4. Публичный контракт

### 4.1 REST — Генерация

```
POST /api/v1/generate/individual
     { seed, gender, age, passportWasExchanged, residencyCountry,
       passportType, fioScript, corrupt, region, categoryClient,
       accountPackage, familyStatus, email, emailDomainMode,
       secretWord, personalManager, terrorist, blocked, bik }
     → IndividualProfileDto

POST /api/v1/generate/ip
     { seed, gender, age, region, categoryClient, accountPackage,
       terrorist, blocked, bik }
     → IpProfileDto

POST /api/v1/generate/organization
     { seed, organizationType, organizationName, region, terrorist, blocked }
     → OrganizationProfileDto
```

**P3.3:** `bik` (`@Pattern("\\d{9}")`) — опциональное поле. Если задан — банк берётся из справочника. Не найден → 400 `{"error":"invalid_argument","message":"..."}`.

### 4.2 REST — Батчи (P1.2 + P3.3)

```
POST /api/v1/generate/{individual,ip,organization}/batch
     { count, seed, ..., bik }
     → { count, seed, profiles: [ ... ] }
```

**P3.3:** `bik` в `IndividualBatchRequest` и `IpBatchRequest` — применяется ко всем профилям в пакете.

### 4.3 REST — Экспорт (P1.4 + P3.1a)

| `format` | Content-Type | Что возвращает |
|---|---|---|
| `json` (дефолт) | `application/json` | `{ count, seed, profiles }` |
| `json-array` | `application/json` | `[ {...}, {...} ]` |
| `csv` | `text/csv;charset=UTF-8` | Файл с BOM, плоские колонки (core-флаттенер) |

### 4.4 REST — Валидация (P1.3 + P2.3) — 8 эндпоинтов

```
POST /api/v1/validate/inn      { "inn": "500100732259" }
POST /api/v1/validate/snils    { "snils": "11223344595" }
POST /api/v1/validate/ogrn     { "ogrn": "1027700132195" }
POST /api/v1/validate/ogrnip   { "ogrnip": "304500116000157" }
POST /api/v1/validate/account  { "account": "..." }
POST /api/v1/validate/kpp      { "kpp": "773601001" }
POST /api/v1/validate/okpo     { "okpo": "12345676" }
POST /api/v1/validate/bik      { "bik": "044525225" }
```

### 4.5 CLI — Контракт (P3.1b + P3.3)

```
validum generate individual [--bik 044525225] [options]
validum generate ip         [--bik 044525225] [options]
validum generate organization [options]   # без --bik (ЮЛ)
validum validate <type> <value>
validum fetch-bik [--offline]
validum scenarios
validum version
```

**Опции `generate`:**

| Опция | Что | Значения |
|---|---|---|
| `--seed N` | Базовый seed | long |
| `-n N` / `--count N` | Количество | 1–10 000 |
| `--format` | Формат вывода | `json` \| `json-array` \| `csv` |
| `-o FILE` / `--output` | Путь | по умолчанию stdout |
| `--gender` | Пол | `MALE` / `FEMALE` |
| `--age N` | Возраст | int |
| `--region` | Регион | 16 регионов |
| **`--bik` (P3.3, individual/ip)** | **Явный БИК** | **9 цифр. Не найден → exit 2** |
| `--type` (organization) | ОПФ | 7 значений |
| `--name` (organization) | Наименование | строка |
| `--no-account` (individual) | Без счёта | флаг |
| `--terrorist` / `--blocked` | Флаги | флаг |

**Exit code:** `0` успех, `1` ошибка валидации, `2` ошибка аргументов / невалидный БИК.

### 4.6 Allure Extension (P3.2)

```java
@ExtendWith(ValidumExtension.class)
class MyTest {
    @Test
    @ValidumSeed(42)
    void testCreateClient() { /* профиль в Allure */ }

    @Test
    @ValidumGenerate(seed = 42, age = 16, region = "MOSCOW")
    void testMinorClient() { /* профиль в Allure */ }
}
```

**Attachment:** плоский JSON, те же поля, что CSV. Best-effort. Только ФЛ.

### 4.7 Примеры

**REST — ФЛ с конкретным банком (P3.3):**
```bash
curl -X POST http://localhost:8080/api/v1/generate/individual \
  -H "Content-Type: application/json" \
  -d '{"seed":42,"bik":"044525225"}'
# → { ..., "bank": { "bik": "044525225", "name": "ПАО Сбербанк", ... } }
```

**REST — невалидный БИК (P3.3):**
```bash
curl -X POST http://localhost:8080/api/v1/generate/individual \
  -H "Content-Type: application/json" \
  -d '{"seed":42,"bik":"000000000"}'
# → 400 { "error": "invalid_argument", "message": "БИК не найден в справочнике: 000000000..." }
```

**CLI — ФЛ с Сбером (P3.3):**
```powershell
java -jar validum.jar generate individual --seed 42 --count 10 --bik 044525225 --format csv -o sber.csv
```

**CLI — невалидный БИК (P3.3):**
```powershell
java -jar validum.jar generate individual --bik 000000000 --format csv
# → exit 2, stderr: "Ошибка: БИК не найден в справочнике: 000000000..."
```

**Батч ФЛ в CSV:**
```bash
curl -OJ -X POST "http://localhost:8080/api/v1/generate/individual/batch?format=csv" \
  -H "Content-Type: application/json" -d '{"count":100,"seed":42}'
```

### 4.8 Java-библиотека

```java
// ФЛ — фасады
IndividualProfile adult = IndividualScenarios.createAdultResidentWithAccount(42L);

// ФЛ — конкретный банк (P3.3)
IndividualProfile sber = IndividualProfileGenerator.builder()
    .seed(42L).bik("044525225").build().generate();

Bank vtb = BikSources.resolve("data/ED807.xml").findByBik("044525187").orElseThrow();
IndividualProfile vtbProfile = IndividualProfileGenerator.builder()
    .seed(42L).bank(vtb).build().generate();

// ИП — с детерминизмом + явный банк (P3.3)
IpProfile ip = IpProfileGenerator.generate(
    IndividualProfileGenerator.builder()
        .seed(42L).age(35).bik("044525225").build(), 42L);

// ЮЛ — ГУП
OrganizationProfile gup = OrganizationProfileGenerator.builder()
    .seed(42L).organizationType(OrganizationType.SE).region(Region.MOSCOW)
    .build().generate();

// Валидация
boolean isValidInn = InnValidator.isValidIndividual("500100732259");
boolean isValidOgrnip = OgrnipValidator.isValid("304500116000157");

// Экспорт в CSV
String csv = CsvExporter.toCsv(IndividualProfileFlattener.flattenAll(List.of(sber)));
```

---

## 5. Поля профиля

### 5.1 ФЛ — динамические

| Поле | Источник |
|---|---|
| ФИО | `NameGenerator` (P0.1) |
| Пол / Дата рождения | `Gender` / `LocalDate.now()` |
| Место рождения | `AddressGenerator.generateBirthPlace(region)` (КЛАДР) |
| Семейное положение | `FamilyStatus`, каскад от возраста |
| Резидентство | `Residency` |
| ИНН / СНИЛС / Паспорт | Генераторы (могут быть испорчены) |
| Адреса (3) | `AddressGenerator` + `AddressBook` — реальные улицы региона |
| Мобильный / Email | `ContactGenerator` (email — из ФИО, P0.3) |
| Секретное слово | 298 в `secret_words.txt` (P0.3) |
| Категория / Пакет | Параметр / каскад |
| Менеджер | 50 в `managers.txt` (P0.3) |
| **Банк (P3.3)** | **`BikGenerator` (случайный) или явный (`.bik` / `.bank`)** |
| Счёт | `AccountGenerator` |
| `terrorist` / `blocked` | Флаги (`false`) |

### 5.2 ЮЛ — поля (7 ОПФ)

| Поле | Как |
|---|---|
| `organizationName` | `OrganizationNameGenerator` |
| `organizationType` | Параметр (дефолт LLC) |
| `ogrn` / `inn` / `kpp` / `okpo` | Генераторы |
| `okopf` / `okfs` / `budget` | Из `OrganizationType` (P2.2) |
| `okogu` / `okved` / `swift` | Константы |
| Адреса / Контакты | `AddressGenerator` (КЛАДР) / `ContactGenerator` |
| **Банк** | ✅ `bank` + `account` (P2.4) |

### 5.3 ИП — поля

ИП = `IndividualProfile` + `OGRNIP` + отчество «ИП Автотеста».

### 5.4 Регионы (16)

| Константа | Код | Город |
|---|---|---|
| `SPB` | 78 | Санкт-Петербург |
| `MOSCOW` | 77 | Москва |
| `MO` | 50 | Мытищи |
| `KRASNODAR` | 23 | Сочи |
| `SVERDLOVSK` | 66 | Екатеринбург |
| `LENOBL` | 47 | Всеволожск |
| `NOVOSIBIRSK` | 54 | Новосибирск |
| `ROSTOV` | 61 | Ростов-на-Дону |
| `BASHKORTOSTAN` | 02 | Уфа |
| `TATARSTAN` | 16 | Казань |
| `CHELYABINSK` | 74 | Челябинск |
| `SAMARA` | 63 | Самара |
| `NIZHNY_NOVGOROD` | 52 | Нижний Новгород |
| `KRASNOYARSK` | 24 | Красноярск |
| `PRIMORSKY` | 25 | Владивосток |
| `SARATOV` | 64 | Саратов |

### 5.5 Данные в ресурсах

| Сущность | Источник | Размер | Override |
|---|---|---|---|
| ФИО | `names/*.txt` | 3.8 млн | — |
| Адреса | `addresses/XX.tsv.gz` (КЛАДР) | 1,99 млн улиц | `region` |
| Домены TEST/REALISTIC | `emails/*.txt` | 10 + 10 | — |
| Секретные слова | `secret_words.txt` | 298 | `.secretWord(...)` |
| Менеджеры | `managers.txt` | 50 | `.personalManager(...)` |
| **Банки** | **`BikRegistry` / ED807** | **30 / 370** | **`.bik` / `.bank` (P3.3)** |

### 5.6 Экспортные поля (CSV, плоский JSON CLI, Allure attachment)

| Группа | Колонки |
|---|---|
| ФЛ идентификация | `fullName`, `gender`, `birthDate`, `age`, `familyStatus` |
| ФЛ документы | `inn`, `snils`, `passportSeries`, `passportNumber`, `passportIssuedBy`, `passportIssueDate`, `passportDepartmentCode`, `passportType`, `passportCountry` |
| Адреса | `registration{PostalCode,City,Street,Building,Apartment,Formatted}`, `post*`, `current*` |
| Контакты | `mobile*`, `email*` |
| Прочее ФЛ | `secretWord`, `categoryClient`, `accountPackage`, `personalManager`, `terrorist`, `blocked` |
| Банк и счёт | `bankBik`, `bankName`, `bankCorrAccount`, `bankCity`, `bankRegion`, `account` |
| ИП | `ogrnip` + все ФЛ-колонки |
| ЮЛ | `organizationName`, `organizationType`, `ogrn`, `inn`, `kpp`, `okpo`, `okopf`, `okfs`, `okogu`, `okved`, `budget`, `swift`, `registration*`, `post*`, `mobile*`, `email*`, `terrorist`, `blocked` |

**P3.3:** при `.bik("044525225")` в колонках `bankBik` / `bankName` / `bankCorrAccount` / `bankCity` / `bankRegion` — данные Сбера.

---

## 6. Валидация (P1.3 + P2.3)

### 6.1 Что проверяется

| Сущность | Алгоритм | Метод |
|---|---|---|
| ИНН ФЛ (12) | ФНС | `InnValidator.isValidIndividual()` |
| ИНН ЮЛ (10) | ФНС | `InnValidator.isValidLegalEntity()` |
| СНИЛС (11) | ПФР (mod 101) | `SnilsValidator.isValid()` |
| ОГРН (13) | ФНС (mod 11) | `OgrnValidator.isValidLegalEntity()` |
| ОГРНИП (15) | ФНС (mod 13) | `OgrnipValidator.isValid()` (P2.3) |
| КПП (9) | регион 01–99 + ИФНС ≠ 00 + причина ≠ 00 + порядковый ≠ 000 | `KppValidator.isValid()` |
| ОКПО ЮЛ (8) | Росстат | `OkpoValidator.isValidLegalEntity()` |
| ОКПО ИП (10) | Росстат | `OkpoValidator.isValidIndividualEntrepreneur()` |
| Счёт (20) | 809-П | `AccountValidator.isValid(account, type, bik)` |
| БИК (9) | справочник ЦБ | `BikValidator.isValid()` |

### 6.2 Контракт

```
POST /api/v1/validate/{inn,snils,ogrn,ogrnip,account,kpp,okpo,bik}
     → { "valid": bool, "type": "...", "details": "..." }
```

```
validum validate <type> <value>   → exit 0 / 1, JSON в stdout
```

### 6.3 Поведение

| Вход | Результат |
|---|---|
| `null` / пустая строка | `{"valid":false,"details":"Empty ..."}` |
| Буквы вместо цифр | `{"valid":false}` |
| Неверная длина / контрольная сумма | `{"valid":false}` |
| Неизвестный тип (CLI) | `{"valid":false,"details":"unknown type"}` + инструкция в stderr |

**Коды ответа:** `200` с `valid: false` на некорректное значение, `400` на битый запрос, `503`, если не настроен справочник БИК.

### 6.4 Round-trip проверка (P2.3)

`ValidatorRoundTripTest` проверяет, что значения, которые выдаёт генератор, проходят соответствующий валидатор. Тест покрывает все поддерживаемые виды реквизитов: сгенерированный профиль отправляется в validation API как есть.

---

## 7. Roadmap итераций

| # | Итерация | Что | Статус |
|---|---|---|---|
| 1–7.5 | Базовое ядро, фасады, каскады, ЮЛ/ИП | ✅ |
| P0.1 | ФИО (3.8 млн) | ✅ |
| P0.2 | Адреса (КЛАДР, 16 регионов) | ✅ |
| P0.3 | Email / SW / Manager | ✅ |
| P1.2 | Batch (3 эндпоинта) | ✅ |
| P1.3 | Validation API (6 эндпоинтов) | ✅ |
| P1.4 | Экспорт (CSV / JSON Array) | ✅ |
| P2.2 | 7 ОПФ ЮЛ | ✅ |
| P2.3 | ОКПО/ОГРНИП + расширенный КПП | ✅ |
| P3.1 | CLI + CSV в core | ✅ |
| P3.2 | Allure Extension | ✅ |
| **P3.3** | **`.bik(String)` / `.bank(Bank)`** | ✅ |
| **P4.1** | **Импорт CSV (fill gaps)** | ✅ |
| P2.1 | Транзакции | 📅 отложено |
| P3.4 | Mock API по OpenAPI | 📅 |
| P1.1 | Maven Central | 🟡 код готов, нужны секреты |

---

## 8. Что НЕ делаем

| Идея | Почему нет |
|---|---|
| Stub «только ФИО+ДР» | Тестер парсит поля сам |
| Hardcoded `createMinor()` | Каскад `.age(16)` |
| KYC-списки | QA парсит флаги сам |
| Каскад «регион → банк» | Искусственная связь |
| LLM-генерация | Не наша задача |
| Универсальный экспорт под любую БД | Плоский CSV, разложение — забота QA |
| Дублирование ИП в `OrganizationType` | ИП = ФЛ + ОГРНИП |
| 1:1 DTO в CLI и Allure | Плоский JSON = CSV |
| Interactive REPL в CLI | Скрипты |
| **Откат на случайный БИК при невалидном (P3.3)** | **Явный 400 / exit 2** |
| **`.bik` для ЮЛ (P3.3/P2.4)** | ✅ `.bik(...)` / `.bank(...)` в `OrganizationProfileGenerator` |

---

## 9. Вопросы, закрытые

| # | Вопрос | Ответ |
|---|---|---|
| 1 | Три адреса или один? | 3-в-1 по умолчанию, override |
| 2 | FIAS — полный или только строка? | Гибрид, дефолт — полный |
| 3 | Как делать негативные сценарии? | Каскадами |
| 4 | Пенсионный возраст? | 65/60 (ФЗ-350) |
| 5 | Где категории/пакеты? | `AbsDefaults`, каскад в builder |
| 6 | Как вызывать сценарии извне? | Три endpoint'а + slug'и |
| 7 | Что с латиницей? | `.fioScript(LATIN)` |
| 8 | Что с иностранным паспортом? | `.passportType(FOREIGN)` |
| 9 | Что с регионами? | `.region(N)` — адреса, серия, КПП |
| 10 | Что с битыми суммами? | `.corrupt(INN, SNILS)` |
| 11 | Что с age<18? | `familyStatus=SINGLE` |
| 12 | Что с категорией? | Каскад на пакет |
| 13 | Что с KYC/чёрными списками? | Не делаем — QA использует флаги |
| 14 | Как делаем ЮЛ и ИП? | ЮЛ — record, ИП — композиция |
| 15 | Что с банком? | **`.bik` / `.bank` для ФЛ/ИП (P3.3)**; ЮЛ — нет |
| 16 | Детерминизм ИП? | `IpProfileGenerator.from(base, seed)` |
| 17 | Как называть тесты? | Имя файла = класс, `@DisplayName` |
| 18 | Где хранить данные? | `names/`, `addresses/`, `emails/`, `secret_words.txt`, `managers.txt` |
| 19 | Как генерируется email? | Из ФИО по ГОСТ Р 52535.1-2006 |
| 20 | Домен по умолчанию? | `TEST` |
| 21 | Как работает батч? | `seed + i` |
| 22 | Что проверяет Validation API? | 8 валидаторов (ФНС/ПФР/Росстат/ЦБ) |
| 23 | Как работает экспорт? | `?format=...`. CSV — в core (P3.1a) |
| 24 | Сколько ОПФ ЮЛ? | 7 (P2.2) |
| 25 | Почему ИП не в `OrganizationType`? | ИП = ФЛ + ОГРНИП |
| 26 | Почему отдельный `/validate/ogrnip`? | Не смешивать с ОГРН ЮЛ (P2.3) |
| 27 | Почему ОКПО автоопределяет тип? | 8 → ЮЛ, 10 → ИП (P2.3) |
| 28 | Что нового в КПП? | Все 4 группы ≠ 0 (P2.3) |
| 29 | Что нового в CLI? | 5 команд, 8 валидаторов, `--format`, exit 0/1 (P3.1b) |
| 30 | CLI JSON — 1:1 REST? | Нет, плоский (P3.1b) |
| 31 | Как `fetch-bik` в банковском контуре? | `--offline` — инструкция, exit 0 (P3.1b) |
| 32 | Где `CsvExporter`? | В core (P3.1a). Используется API, CLI, Allure |
| 33 | CSV-путь в API после P3.1a? | `raw → flattener → CsvExporter` |
| 34 | Почему CLI не тянет Spring? | Fat JAR ~15 МБ со справочниками, старт ~300 мс (P3.1b) |
| 35 | Как QA видят профиль в Allure? | `@ValidumSeed` / `@ValidumGenerate` + Extension (P3.2) |
| 36 | Почему Allure плоский? | Те же поля, что CSV (P3.2) |
| 37 | Allure ломает тесты? | Нет, best-effort (P3.2) |
| 38 | Как задать конкретный банк? | `.bik("044525225")` / `.bank(Bank)` / `--bik` / `{"bik":...}` (P3.3) |
| 39 | Что если БИК не найден? | `IllegalArgumentException` → REST 400, CLI exit 2 (P3.3) |
| 40 | Приоритет `.bank` / `.bik`? | `bank` > `bik` > случайный (P3.3) |
| 41 | ЮЛ и `.bik`? | ЮЛ без `Bank` в модели; `.bik` только ФЛ/ИП (P3.3) |

---

---

## 🎭 Сценарии в коде (ScenarioCatalog, 9 slug'ов)

Единый каталог — `validum-core/src/main/java/ru/validum/core/profile/ScenarioCatalog.java`.
REST (`GET/POST /api/v1/scenarios`), CLI (`validum scenarios`, `generate`) и Java читают один и тот же список;
`ScenarioService` (API) и `ScenarioRegistry` (CLI) — тонкие фасады.

| Slug | Кто | Документы ДУЛ | Счёт |
|---|---|---|---|
| `adult-resident-with-account` | взрослый резидент | паспорт РФ (21) | ✅ |
| `adult-resident-without-account` | взрослый резидент | паспорт РФ (21) | — |
| `pensioner-male` | мужчина 65 | паспорт РФ (21) | ✅ |
| `pensioner-female` | женщина 60 | паспорт РФ (21) | ✅ |
| `salary-client` | зарплатник 35 | паспорт РФ (21) | ✅ |
| `non-resident-with-residence-permit` | нерезидент (TJ) 32 | паспорт ин. (10) + ВНЖ (12) | ✅ |
| `foreigner-with-visa` | иностранец (US) 28 | паспорт ин. (10) + виза | ✅ |
| `serviceman` | военнослужащий 20 | паспорт РФ (21) + военный билет (07) | ✅ |
| `minor` | несовершеннолетний 16 | паспорт РФ (21) | ✅ |

Правила согласованности (`DocumentRules`): загранпаспорт требует паспорт РФ; паспорт иностранца, ВНЖ, РВП и
удостоверение беженца — только для нерезидента; военный билет — только для мужчин 18–27 (офицерский запас — с 18);
водительское удостоверение — с 18. Нарушение → `IllegalArgumentException` (REST 400, CLI exit 2).

## 📤 Экспорт в форматы ЦБ (ED807 / ED808)

- `Ed807Exporter` — справочник участников (SIRR) из `ParticipantRegistry`: `PtType`, статус, корсчёт `CRSA`,
  `AccountCBRBIC` = первые 6 цифр БИК + «000». windows-1251, `urn:cbr-ru:ed:v2.0`.
- `Ed808Exporter` — профиль участника (FIRR): `ParticipantProfile`, `ParticipantAttributes`, `AccountsInfo`
  (корсчёт `CRSA` + клиентский `BANA`).
- REST: `GET /api/v1/export/ed807?type=20&limit=50`, `GET /api/v1/export/ed808?bik=044525225&seed=42`.
- CLI: `validum export ed807 --file data/ED807.xml --type 20 --limit 50 -o ed807.xml`,
  `validum export ed808 --bik 044525225 --seed 42 -o ed808.xml`.

**Docker:** `docker compose up` поднимает API с `data/ED807.xml` внутри (`/app/data/ED807.xml`), healthcheck —
`/actuator/health`. Справочник обязателен: без смонтированного `data/ED807.xml` запросы к банковским
данным возвращают `503 bik_source_missing` (встроенного демо-набора нет).

---

**Validum** — данные, которые проходят валидацию. **721 тест. REST API + CLI + Java-библиотека + Allure.**
Каскады, батчи, валидация (8 типов), счета по 809-П (12 типов), ДУЛ (11 видов), 9 КУС-сценариев,
CSV в трёх режимах, экспорт ED807/ED808, Docker. **P0.1–P0.3, P1.2–P1.4, P2.2, P2.3, P3.1–P3.3, P4.1 закрыты.**
