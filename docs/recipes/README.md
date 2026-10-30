# Рецепты Validum

Готовые сценарии «задача → команда → результат → проверка». Все команды — для fat jar
`validum-cli/build/libs/validum.jar` (собрать: `./gradlew :validum-cli:shadowJar` или
`./gradlew :validum-cli:shadowJar`).

**Один раз перед началом:** справочник участников ЦБ обязателен (демо-набора в продукте нет).

```powershell
java -jar validum.jar fetch-bik -o data/ED807.xml
# → Справочник: 1380 участников, из них 370 действующих банков
```

Если файла нет, банковские сценарии честно падают: CLI — `exit 2`, REST — `503 bik_source_missing`.

---

## 1. Фикстура в JUnit-тесте с Allure

**Задача.** Каждый тест получает свой профиль, а в Allure-отчёте видно, какие данные были в прогоне.

**Команда.** Ничего запускать не нужно — подключается зависимость `validum-allure` и аннотация:

```java
@ExtendWith(ValidumExtension.class)
class ClientOnboardingTest {

  @Test
  @ValidumSeed(42)
  void clientCanOpenAccount(IndividualProfile profile) {
    assertThat(AccountValidator.isValidAuto(profile.account(), profile.bank().bik())).isTrue();
  }

  @Test
  @ValidumGenerate(seed = 7, region = "SPB", gender = "FEMALE")
  void femaleClientFromSpbIsAccepted(IndividualProfile profile) {
    assertThat(profile.registrationAddress().region()).isEqualTo("Санкт-Петербург");
  }
}
```

**Что получилось.** Профиль приходит аргументом теста и автоматически прикрепляется к Allure-отчёту.
`@ValidumGenerate` принимает `seed`, `region`, `gender`, `categoryClient`.

**Как проверить.** `./gradlew test` → в `build/allure-results` лежит вложение с JSON профиля.

---

## 2. 1000 клиентов в CSV для Excel

**Задача.** Аналитик просит выгрузку «тысяча клиентов с реквизитами» — открыть в Excel и посмотреть глазами.

```powershell
java -jar validum.jar generate individual --seed 42 -n 1000 --format csv -o clients.csv
```

**Что получилось.** `clients.csv` — UTF-8 с BOM (Excel не ломает кириллицу), плоские колонки
`fullName, inn, snils, passportSeries, registrationCity, bankBik, account, …`, ДУЛ одной ячейкой
`documents` в режиме `inline`.

**Как проверить.** Открыть в Excel: 1000 строк, у каждого непустые ИНН/СНИЛС/счёт, номер счёта
не превратился в дату (текст с ведущими нулями).

---

## 3. Залить данные в тестовую БД

**Задача.** «Положи 10 000 клиентов в мою PostgreSQL» — без ручного конвейера CSV → INSERT.

```powershell
java -jar validum.jar generate individual --seed 42 -n 10000 --format sql `
  --dialect postgres --table clients -o seed.sql
psql -h localhost -U qa -d testdb -f seed.sql
```

**Что получилось.** `seed.sql` = `CREATE TABLE "clients" (...)` + 10 000 `INSERT`. Диалекты:
`--dialect postgres | oracle | mysql`; `--no-ddl` — только `INSERT`, если таблица уже есть;
`--table my_schema.clients` — своя схема.

**Как проверить.** `select count(*) from clients;` → 10 000; `select inn from clients limit 3;` — 10 цифр,
ведущие нули не потеряны (колонка текстовая).

---

## 4. Проверить реквизиты, которые пришли снаружи

**Задача.** Из системы пришли реквизиты — надо понять, настоящие ли контрольные суммы.

```powershell
java -jar validum.jar validate inn 7707083893        # → valid, exit 0
java -jar validum.jar validate ogrn 1027700132195    # → valid
java -jar validum.jar validate account 4081781029991000431 --bik 044525225
```

**Что получилось.** Проверка ИНН, СНИЛС, ОГРН, ОГРНИП, КПП, ОКПО, счёта (ключ по письму ЦБ № 515)
и БИК (с названием банка из справочника). Код возврата: `0` — валидно, `1` — нет.

**Как проверить.** `$LASTEXITCODE` после команды. Счёт без `--bik` проверить нельзя: ключ зависит от БИК.

---

## 5. Нерезидент с видом на жительство

**Задача.** Нужен клиент-иностранец с ВНЖ — для КУС-сценариев и проверки «документ подтверждает право
находиться в РФ».

```powershell
curl -X POST "http://localhost:8080/api/v1/scenarios/non-resident-with-residence-permit?seed=42"
```

**Что получилось.** Профиль: паспорт иностранного гражданина (Таджикистан) + ВНЖ, `residency=NON_RESIDENT`.
Остальные slug'и: `adult-resident-with-account`, `adult-resident-without-account`, `pensioner-male`,
`pensioner-female`, `salary-client`, `foreigner-with-visa`, `serviceman`, `minor` (список — `scenarios`).

**Как проверить.** `java -jar validum.jar scenarios` — 9 slug'ов; в ответе `documents` содержит
`spdulCode: "10"` и `"12"`.

---

## 6. Иностранец с визой

```powershell
curl -X POST "http://localhost:8080/api/v1/scenarios/foreigner-with-visa?seed=42"
```

**Что получилось.** Паспорт США + виза (`spdulCode` у визы нет — в классификаторе её не существует,
поэтому в JSON `type: "VISA"`), `residency=NON_RESIDENT`.

**Как проверить.** `documents[1].type == "VISA"`, `expirationDate` больше `issueDate`.

---

## 7. Военнослужащий

```powershell
curl -X POST "http://localhost:8080/api/v1/scenarios/serviceman?seed=42"
```

**Что получилось.** Мужчина 20 лет, паспорт РФ + военный билет (код 07, серия — две заглавные буквы).
Правила совместимости проверяет `DocumentRules`: билет только мужчине, призывной возраст 18–27.

**Как проверить.** `gender=MALE`, `age=20`, `documents[1].spdulCode == "07"`.

---

## 8. Несовершеннолетний клиент

```powershell
curl -X POST "http://localhost:8080/api/v1/scenarios/minor?seed=42"
```

**Что получилось.** 16 лет, только паспорт РФ (загранпаспорт и военный билет несовместимы с возрастом),
счёт есть — проверка «может ли подросток открыть счёт».

**Как проверить.** `age=16`, в `documents` ровно один документ с `spdulCode: "21"`.

---

## 9. Дозаполнить свой CSV

**Задача.** Есть таблица с известными ИНН и счетами; не хватает ФИО, адресов, паспортов. Перегенерировать
всё нельзя — известные значения должны остаться.

```powershell
java -jar validum.jar fill individual -i my_clients.csv -o filled.csv --seed 42
```

**Что получилось.** Непустые ячейки не тронуты, пустые заполнены; недостающие колонки добавлены в конец.
`asOf` из входного файла уважается (дата расчёта возраста).

**Как проверить.** `filled.csv` содержит те же ИНН/счета, что и входной, а `fullName`/`registrationCity`
заполнены. Сравнить: `Compare-Object (Import-Csv my_clients.csv).inn (Import-Csv filled.csv).inn` — пусто.

---

## 10. Документы (ДУЛ) в трёх режимах

**Задача.** Документы — это список, а CSV плоский. Три способа упаковать, под разные инструменты.

```powershell
# inline — мини-таблица в одной ячейке (Excel, глазами)
java -jar validum.jar generate individual --documents 21,22 --documents-mode inline -o a.csv

# json — машинный формат (API, парсеры)
java -jar validum.jar generate individual --documents 21,22 --documents-mode json -o b.csv

# split — вторая таблица с ключом profileKey (загрузка в БД, 30+ видов)
java -jar validum.jar generate individual --documents 21,22 --documents-mode split -o c.csv
```

**Что получилось.** `a.csv` — ячейка `documents` с переносами строк `21|45 10|123456|…`; `b.csv` — JSON-массив;
`c.csv` + `c-documents.csv` — нормализованные две таблицы (ключ — `inn`). Обратно split собирается в `fill`:
`--documents-file c-documents.csv`.

**Как проверить.** `Import-Csv a.csv | Select documents` — видно документы; для split —
`Import-Csv c-documents.csv | Group-Object profileKey` — по строке на документ.

---

## 11. ED807 и ED808 для тестового контура ЦБ

**Задача.** Стенду нужен справочник участников и профиль банка — как у ЦБ, но без ЭП.

```powershell
# Справочник: 100 банков (PtType=20)
java -jar validum.jar export ed807 --type 20 --limit 100 -o ed807.xml

# Профиль одного банка (счета в расчётной сети + права на операции)
java -jar validum.jar export ed808 --bik 044525225 -o ed808.xml
```

**Что получилось.** XML в `urn:cbr-ru:ed:v2.0`, кодировка windows-1251, структура сверена со схемами
ЦБ v2026.09.0 (включая `CK`, `ParticipantAttributes`). В ED808 — клиентский счёт по письму 515.

**Как проверить.** Открыть в браузере/VS Code — должен разбираться как XML; количество
`<BICDirectoryEntry` = 100. Ограничение: подписи нет, в ЦБ такой файл не примут — это стенд/эмулятор.

---

## 12. Все участники платёжной системы, а не только банки

**Задача.** Нужны РКЦ, казначейство, конкурсные управляющие — не только банки (17 кодов `PtType`).

```powershell
java -jar validum.jar participants --file data/ED807.xml              # все 1380 участников
java -jar validum.jar participants --file data/ED807.xml --type 20    # 370 банков
java -jar validum.jar participants --file data/ED807.xml --type RKC   # 13 расчётно-кассовых центров
java -jar validum.jar participants --file data/ED807.xml --type 90    # 280 конкурсных управляющих
java -jar validum.jar participants --file data/ED807.xml --type 52    # 141 территориальный орган ФК
```

**Что получилось.** Таблица БИК / PtType / тип / корсчёт / наименование. Код принимается числом (`52`)
или именем (`TREASURY`).

**Как проверить.** Сумма по всем типам = 1380; `--type 20` = 370.

---

## 13. Одинаковый клиент вчера и сегодня

**Задача.** Тест упал вчера, нужно воспроизвести ровно те же данные сегодня (дата в расчёте возраста и
паспорта не должна «уплывать»).

```powershell
java -jar validum.jar generate individual --seed 42 --as-of 2026-01-01 --format csv -o day1.csv
java -jar validum.jar generate individual --seed 42 --as-of 2026-01-01 --format csv -o day2.csv
Compare-Object (Get-Content day1.csv) (Get-Content day2.csv)   # → пусто
```

**Что получилось.** `seed + asOf` полностью определяют профиль: возраст, даты паспорта и ДУЛ считаются от
`asOf`, а не от «сегодня». В REST — поле `"asOf"`, в builder — `.asOf(LocalDate)`.

**Как проверить.** Файлы побайтово равны. Без `--as-of` дата берётся `LocalDate.now()` — тогда совпадения
между днями не гарантируются.

---

## 14. Поднять API в Docker со справочником

**Задача.** Общий стенд для команды: REST на 8080, справочник ЦБ смонтирован снаружи.

```powershell
docker compose up --build
# API: http://localhost:8080/api/v1, Swagger: /swagger-ui.html

curl "http://localhost:8080/api/v1/validate/inn/7707083893"
curl -X POST "http://localhost:8080/api/v1/generate/individual" -H "Content-Type: application/json" `
  -d '{"seed": 42, "region": "MOSCOW", "asOf": "2026-01-01"}'
```

**Что получилось.** `docker-compose.yml` монтирует `./data:/app/data:ro` и ставит
`VALIDUM_BIK_FILE=/app/data/ED807.xml`. Без файла банковские запросы вернут `503 bik_source_missing`
(это ожидаемое поведение, а не баг).

**Как проверить.** `curl localhost:8080/actuator/health` → `UP`; без `data/ED807.xml` — `503` на
`/generate/individual`.

---

## Куда смотреть дальше

- Полный REST — `docs/README.md` и Swagger UI (`/swagger-ui.html`).
- Каскады (регион → ОПФ → КПП → адрес) — `docs/SCENARIOS.md`.
- Форматы ЦБ (счета, БИК, PtType) и правила ДУЛ — `docs/SCENARIOS.md`.
