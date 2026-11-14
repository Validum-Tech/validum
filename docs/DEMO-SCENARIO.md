# Демо-сценарий validum 0.4.0 — четыре видео + демо-репозиторий

Полный пошаговый сценарий: воспроизводишь каждый блок руками и показываешь на видео.
Материал разбит на **четыре отдельных видео** по способу подключения — каждый показывает
«что уже можно получить прямо сейчас», не пересекаясь с другими:

| № | Видео | Кому | Что доказывает |
|---|---|---|---|
| 1 | **CLI** — одна консоль | QA, аналитик, devops | весь продукт без единой строки кода |
| 2 | **Библиотека `validum-core`** | Java-разработчик | генерация внутри своих фикстур/тестов |
| 3 | **REST API** | интегратор, CI | self-host сервис в пайплайне |
| 4 | **Allure extension** | автоматизатор | данные приложены к каждому отчёту |

## Как читать шаг

Каждый шаг — три строки:
- **`команда`** или **`код`** — что ввести/написать;
- **Увидишь** — что появится на экране (по этому проверяешь, что всё снято с 0.3.0);
- **Скажи словами** — фраза для видео, объясняющая пользу.

---

## Предпосылки (для всех четырёх видео)

```powershell
# 1. JDK 17+ (REST-сервис и Allure — 21)
java -version

# 2. Jar для Видео 1
#    собран релизом: validum-cli\build\libs\validum.jar
#    или скачан: https://github.com/Validum-Tech/validum/releases/download/v0.4.0/validum-0.4.0.jar
$jar = ".\validum-cli\build\libs\validum.jar"

# 3. Справочник ЦБ — обязателен, демо-набора в продукте нет
java -jar $jar fetch-bik -o data\ED807.xml
```

В кадре на каждом прогоне CLI должна светиться строка `# validum 0.4.0-SNAPSHOT · seed=…` — это
доказательство, что снято с реального релиза.

---

# Видео 1. CLI — весь продукт одной консолью

## 1.0 Заставка — 20 секунд

**Без команд.** Кадр: `validum 0.4.0`, три строки на экране.

- Скажи: «Validum — генератор тестовых данных, валидных по требованиям Банка России:
  ИНН, СНИЛС, ОГРН, счета по 809-П, документы по возрасту, справочник БИК из ED807.
  Ноль зависимостей в ядре, ноль генеративных моделей — только алгоритмы.
  Четыре способа подключения: CLI, Java-библиотека, REST и Allure-расширение».
- Три цифры в кадре: **860 тестов**, **около 1,4 тыс. участников** в справочнике,
  **12 типов счетов**.
- Скажи: «Версия 0.4.0 лежит в Maven Central, код — на GitHub, лицензия Apache-2.0».

## 1.1 Окружение и справочник

**`java -jar $jar version`**
- Увидишь: `validum 0.4.0`.
- Скажи: «CLI читает версию из манифеста jar, а не из хардкода».

**`java -jar $jar participants -f data\ED807.xml`**
- Увидишь: таблица `БИК · PtType · Тип · Корсчёт · Наименование`, в конце — `Всего выведено: 1360`
  (число зависит от выгрузки ЦБ — на другой выгрузке будет другое).
- Скажи: «Справочник ED807 — это не "демо-данные", а настоящий справочник участников
  платёжной системы Банка России: около 1,4 тыс. участников, из них около 370 банков
  с корсчетами — точные числа зависят от выгрузки».

**`java -jar $jar scenarios`**
- Увидишь: 8 слагов — `adult-resident-with-account`, `adult-resident-without-account`,
  `pensioner-male`, `pensioner-female`, `salary-client`, `non-resident-with-residence-permit`,
  `serviceman`, `minor`.
- Скажи: «Готовые КУС-сценарии — не "случайные люди", а именно те кейсы, которые
  проверяют в онбординге».

## 1.2 Человек

**`java -jar $jar generate individual --seed 42 -n 3 --format json-array -o out\people.json`**
- Увидишь: 3 профиля с ИНН, СНИЛС, паспортом, адресом из КЛАДР, банком и счётом.
- Скажи: «Каждый реквизит валиден по алгоритму: ИНН — контрольные разряды, СНИЛС —
  контрольное число, счёт — ключ по письму ЦБ № 515. Это не случайные цифры».

**`java -jar $jar generate individual --seed 7 -n 1000 --age-range 18-70 --format csv -o out\cohort.csv`**
- Увидишь: возраст размазан равномерно от 18 до 70.
- Скажи: «Раньше без параметров генерировалась тысяча 19-летних — это было синтетическим
  шумом. Теперь можно задать диапазон».

**`java -jar $jar generate individual --seed 7 -n 1000 --age-distribution realistic --format csv -o out\cohort-real.csv`**
- Увидишь: когорты 0–6, 7–13, 14–17, 18–24, 25–34 … 80+.
- Скажи: «Набор похож на реальную клиентскую базу — есть дети, взрослые, пенсионеры».

**`java -jar $jar generate individual --seed 3 -n 5 --age-range 14-60 --documents auto --index --format csv-semicolon -o out\bios.csv`**
- Увидишь: в stderr таблица `№ · Идентификатор · ФИО · Возраст · Регион · Банк · Счёт`.
- Скажи: «`--index` — быстрый оглавление набора: каждому человеку присвоен стабильный
  `personId`, по которому его потом можно найти в любом экспорте того же seed».

## 1.3 Карточка человека

**`java -jar $jar describe --file out\bios.csv --row 2`**
- Увидишь: блоки `[Кто] [Проверки] [Паспорт] [Дополнительные документы] [Адреса] [Банк и счёт] [Флаги]`;
  проверки ИНН/СНИЛС/счёта — `✓`; паспорт с «кем выдан» из региона серии и кодом
  подразделения `77X-XXX`; у 20-летнего — права и военный билет, у 15-летнего — только паспорт.
- Скажи: «Документы согласованы с датой рождения: паспорт не выдан в 3 года, права не в 16,
  военный билет не у 40-летней женщины. А `describe` — это "прочитай этого человека": поймал
  баг → приложил карточку клиента вместо гадания».

## 1.4 Реальность

### Боль, которую лечим (30 секунд — снять до команд)

Кадр: обычный CSV (`--format csv`) открыт в русском Excel. Видно:
`5,86E+11` вместо ИНН, `1,07E+10` вместо СНИЛС, `053796` превратился в `53796`,
20-значный счёт обрезан до `4,08178E+19`, дата — `#######`.

- Скажи: «Это не ошибка генератора — это Excel угадывает типы. Поэтому есть
  второй CSV-профиль: `csv-semicolon`. Смотрим, что он делает иначе».

**`java -jar $jar generate individual --seed 11 -n 2 --residency non-resident --country KZ --age 34 --format json-array -o out\kz.json`**
- Увидишь: `passportType: FOREIGN`, `passportCountry: KZ`, адрес «Казахстан, г. Алматы…»,
  `isResident: false`, `citizenship: KZ` — при этом банк и счёт российские.
- Скажи: «Трансграничный кейс: иностранец обслуживается в РФ. ДУЛ, адрес, резидентство —
  всё согласовано, без ручной правки».

**`java -jar $jar describe --file out\kz.json --row 1`**
- Увидишь: в `[Паспорт]` — `Тип: FOREIGN`, `Страна: KZ`; в `[Флаги]` — `Резидент: false`,
  `Гражданство: KZ`; адрес «Казахстан, г. Алматы…»; банк и счёт — российские.
- Скажи: «То же самое глазами: не нужно вчитываться в JSON, чтобы увидеть страну паспорта
  и адрес. Сравни с карточкой резидента из 1.3 — вот разница, которую обычно набивают руками».

**`java -jar $jar generate individual --seed 5 -n 3 --format csv-semicolon -o out\excel.csv`**
- Увидишь (открой в Excel): ИНН, СНИЛС, счёт, корсчёт, индекс `053796` — текстом,
  без `5,86E+11`, без потери разрядов; заголовки русские.
- Скажи: «Обычный CSV Excel ломает: ИНН превращает в экспоненту, у 20-значного счёта теряет
  разряды. Формат `csv-semicolon` пишет кодовые поля как `="…"` — Excel показывает как текст».

**`java -jar $jar coverage --file out\excel-saved.csv`** — тот же файл после «Сохранить» в Excel
```powershell
# Excel → открыть out\excel.csv → Сохранить как → out\excel-saved.csv (CSV, разделитель ;)
# Не сохранял — проверь исходный файл: coverage понимает и «;», и формулы ="…"
java -jar $jar coverage --file out\excel.csv
```
- Увидишь: `inn`, `snils`, `account`, `bankBik` — `0` невалидных, индекс `053796` на месте.
- Скажи: «Excel пересохранил файл — и мы это проверили, а не поверили. Если бы Excel съел
  разряды или ведущий ноль, `coverage` показал бы это в колонке "невалидных". Именно так
  ловят порчу данных при обмене наборами между командами».

**`generate --spec` требует файл — сначала пишем спецификацию, потом генерируем**
```powershell
$spec = @'
{ "type": "individual", "count": 50, "seed": 20261113, "asOf": "2026-11-13",
  "format": "csv-semicolon", "ageRange": "25-54", "documents": "auto", "region": "77" }
'@
# Set-Content -Encoding utf8 в PowerShell 5.1 пишет BOM и портит кириллицу;
# надёжный способ — явный UTF-8 без BOM:
[IO.File]::WriteAllText("out\set.json", $spec, (New-Object System.Text.UTF8Encoding($false)))
Get-Content out\set.json -Encoding utf8 | Select-Object -First 1   # проверить, что файл читается
```
**`java -jar $jar generate --spec out\set.json -o out\clients.csv`**
```json
{
  "type": "individual", "count": 50, "seed": 20261113, "asOf": "2026-11-13",
  "format": "csv-semicolon", "ageRange": "25-54", "documents": "auto", "region": "77"
}
```
- Увидишь: тот же набор из одного файла.
- Скажи: «Данные — это код: набор воспроизводим в CI, ревьюится в PR, версионируется вместе
  с тестами. Стенд больше не ломается от "у тебя не тот CSV"».

**`$spec.Replace('"seed"', '"sed"') | Set-Content out\set-typo.json -Encoding utf8`**
**`java -jar $jar generate --spec out\set-typo.json`**
- Увидишь: `Ошибка: Неизвестные ключи: sed. Допустимые: …`, код возврата 2.
- Скажи: «Спецификация строгая: опечатка не молча игнорируется, как в YAML-конфигах,
  а называется по имени. Набор нельзя "починить не туда"».

## 1.5 Надёжность

**`java -jar $jar generate individual --seed 1 -n 50 --unique --format csv -o out\unique.csv`**
- Увидишь: 50 строк, ИНН без повторов.
- Скажи: «Уникальность гарантируется, а не "надеемся на вероятность"».

**`java -jar $jar generate individual --seed 1 -n 11 --unique --unique-pool 10`**
- Увидишь: `Ошибка: пул исчерпан: выдано 10 из 10 вариантов ИНН; увеличьте --unique-pool или уменьшите -n`,
  код возврата 2.
- Скажи: «Пул ИНН — 10 миллиардов, но если ты просишь 11 из 10, продукт честно откажет
  до генерации, а не после 40 минут работы. Так же он посчитает, что 700 000 уникальных
  ИНН из пула 676 000 невозможны».

**`java -jar $jar generate individual --seed 4 -n 10 --corrupt inn,snils --format csv-semicolon -o out\bad.csv`**
**`java -jar $jar coverage --file out\bad.csv`**

> Каждая строка с командой — **отдельный** вызов. Если склеить две команды в одну строку, picocli ответит `Unmatched arguments from index …` — это не сбой продукта.
- Увидишь: `# испорчено: inn,snils` в stderr; в отчёте `inn: 10/10 невалидных`,
  `snils: 10/10 невалидных`, остальное чисто.
- Скажи: «Управляемый негатив — ровно одна ошибка в нужном поле. Идеальный вход для теста
  "наша система обязана отклонить такой файл"».

**`java -jar $jar coverage --file out\cohort-real.csv`**
- Увидишь: таблица «поле / заполнено % (N/M) / различных / невалидных», `Согласованность:
  нарушений нет`, распределения (пол, резидентство, регион, банк, семья, возраст).
- Скажи: «Качество набора проверяется машинно: документ/возраст, регион паспорта и прописки,
  счёт/БИК. Если данные испорчены на входе — нарушение будет названо текстом».

## 1.6 ИП и организации

**`java -jar $jar generate ip --seed 8 -n 2 --region 77 --okved 62.01 --format json-array -o out\ip.json`**
- Увидишь: ОГРНИП, ИНН, ОКВЭД, счёт 40802.
- Скажи: «Тот же конвейер для ИП — валидный ОГРНИП и счёт, привязанный к БИК».

**`java -jar $jar generate organization --seed 9 -n 2 --type LLC --region 77 --format json-array -o out\org.json`**
- Увидишь: ОГРН, ИНН, КПП, ОКПО, ОКВЭД, банк и расчётный счёт 40702.
- Скажи: «ОПФ — ООО, АО, ПАО, НКО, фонд, ГУП, МУП; реквизиты юридического лица согласованы».

## 1.7 Валидация и дозаполнение

**`java -jar $jar validate inn 7707083893`** → `валиден`; **`java -jar $jar validate inn 7707083892`** → `не валиден`.
- Скажи: «Та же валидация, что и при генерации, доступна как самостоятельная команда.
  Типы: inn, snils, ogrn, ogrnip, account, kpp, okpo, bik».

**`java -jar $jar fill individual -i out\people-partial.csv -o out\people-filled.csv`**
```powershell
# 1) готовим входной CSV и руками оставляем пробелы (Excel/блокнот: удалить СНИЛС и счёт у второй строки)
java -jar $jar generate individual --seed 5 -n 2 --format csv -o out\people-partial.csv
# 2) дозаполняем
java -jar $jar fill individual -i out\people-partial.csv -o out\people-filled.csv
```
- Увидишь: пустые ячейки заполнены, непустые не тронуты.
- Скажи: «Дозаполнение: приходит CSV с пробелами — продукт достраивает только пустое».

## 1.8 Обмен с ЦБ и засев БД

**`java -jar $jar export ed807 -f data\ED807.xml -t 20 -l 5 -o out\ed807.xml`**
**`java -jar $jar export ed808 -b 044525225 -f data\ED807.xml -o out\ed808.xml`**
- Увидишь: XML по схемам ЦБ (`urn:cbr-ru:ed:v2.0`, кодировка windows-1251).
- Скажи: «Продукт понимает не только клиентов, но и расчётную инфраструктуру ЦБ».

**`java -jar $jar generate individual --seed 9 -n 100 --format sql --dialect postgres --table clients -o out\seed.sql`**
- Увидишь: `CREATE TABLE` + `INSERT`, шапка `-- validum 0.4.0, seed=9`.
- Скажи: «Стенд "с нуля" одной командой: `psql -f seed.sql`».

## 1.9 Воспроизводимость

**`java -jar $jar generate individual --seed 1234 --as-of 2026-01-15 -n 3 --format csv -o out\a1.csv`**
**`java -jar $jar generate individual --seed 1234 --as-of 2026-01-15 -n 3 --format csv -o out\a2.csv`**
**`Get-FileHash out\a1.csv, out\a2.csv`**
- Увидишь: хеши совпадают.
- Скажи: «Один seed + фиксированная дата расчёта `asOf` = побитово один и тот же набор.
  В тестах это закреплено SHA-256 golden-фикстурами».

---

## 1.10 Цена ошибки — что было бы без validum

**`java -jar $jar generate individual --seed 3 -n 1 --bik 044525225 --format csv -o out\one.csv`**
```powershell
$row = Import-Csv out\one.csv | Select-Object -First 1
java -jar $jar validate inn $row.inn          # валиден
java -jar $jar validate inn 7707083892        # не валиден (тот же ИНН, последняя цифра на 1)
```
- Увидишь: первая команда — `{"valid":true,…}`, вторая — `{"valid":false,…}` и **код возврата 1**.
- Скажи: «Faker и его аналоги выдают правдоподобные номера. Валидатор на стенде падает не
  на бизнес-логике, а на данных: ИНН не проходит контрольную сумму. Validum выдаёт только
  то, что проходит. И эту проверку можно вставить в свой пайплайн — код возврата 1».

**`java -jar $jar validate snils $row.snils`** — валиден.
> `$row` живёт только в текущем окне PowerShell: после перезапуска консоли сначала выполните
> `Import-Csv out\one.csv | Select-Object -First 1`. Пустая переменная превращается в пустую
> строку, и picocli ответит «Missing required parameter: '<value>'» — это ваш PowerShell, а не продукт.
**`java -jar $jar validate account $row.account --bik $row.bankBik`** — валиден.
**`java -jar $jar validate account $row.account --bik 044030653`** — **не** валиден.
- Скажи: «Счёт валиден только вместе со своим банком: контрольный ключ по письму ЦБ № 515
  считается от БИК. Подменили БИК — счёт перестал проходить. Это ловит ошибку
  «скопировали счёт из другого стенда»».

**`java -jar $jar validate bik $row.bankBik`** — валиден. Для КПП нужен профиль юрлица:

**`java -jar $jar generate organization --type SE --seed 4 -n 1 --format csv -o out\one-org.csv`**
```powershell
$org = Import-Csv out\one-org.csv | Select-Object -First 1
java -jar $jar validate kpp $org.kpp
```
- Скажи: «Семь типов реквизитов одной командой: `inn`, `snils`, `ogrn`, `ogrnip`, `account`,
  `kpp`, `okpo`, `bik` — и у каждого свой набор правил: где формат, где контрольная сумма,
  а у счёта ещё и БИК».

## 1.11 Негатив по требованию

**`java -jar $jar generate individual --seed 4 -n 2 --terrorist --blocked --format json-array -o out\aml.json`**
- Увидишь: `"terrorist": true`, `"blocked": true` у обоих профилей, остальные реквизиты — валидные.
- Скажи: «AML-сценарии: клиент из перечня и клиент с блокировкой. Данные остаются валидными —
  ломается ровно тот флаг, который нужен тесту».

**`java -jar $jar generate documents --type 22 -n 2 --format json-array -o out\passports.json`**
- Увидишь: серия, номер, кем выдан, дата выдачи, код подразделения — без профиля человека.
- Скажи: «Бывает, что нужны только документы: тестируем валидатор ДУЛ, а человека не генерируем».

## 1.12 Документы отдельной таблицей

**`java -jar $jar generate individual --seed 12 -n 3 --age 30 --documents auto --documents-mode split --format csv -o out\people-docs.csv`**
- Увидишь: два файла — `out\people-docs.csv` и рядом `out\people-docs-documents.csv`
  (колонки `profileKey`, `spdulCode`, `title`, `series`, `number`, …).
- Скажи: «Документы — отдельная таблица, как в реальной модели данных. Связь — по идентификатору профиля».

**`java -jar $jar fill individual -i out\people-docs.csv -o out\people-back.csv --documents-file out\people-docs-documents.csv`**
- Увидишь: колонка `documents` заполнена теми же документами, что были во второй таблице.
- Скажи: «И обратно: две таблицы склеиваются по ключу — так данные едут из АБС в тест и назад».

## 1.13 Участники ЦБ по типам и офлайн-путь

**`java -jar $jar participants -f data\ED807.xml --type 20`**
- Увидишь: только банки — в конце `Всего выведено: N` (в выгрузке 2026 года N ≈ 368, точное число
  зависит от версии справочника — говори «около 370»).
- Скажи: «Справочник — это около 1,4 тыс. участников: банки, РКЦ, казначейство. Для генерации
  нужны банки — фильтр по типу PtType=20, и их около 370».

**`java -jar $jar fetch-bik --offline -o data\ED807.xml`**
- Увидишь: `Режим --offline: скачивание не выполняется` и ссылка на файл, код возврата 0.
- Скажи: «В банковском контуре cbr.ru закрыт: продукт не пытается лезть в сеть, а объясняет,
  что положить в `data`».

## 1.14 Адрес из КЛАДР и ОКВЭД

**`java -jar $jar generate ip --seed 8 -n 1 --region 50 --okved 62.01 --format csv -o out\ip-okved.csv`**
```powershell
Import-Csv out\ip-okved.csv | Select-Object okved, okvedName, registrationCity, registrationStreet, registrationPostalCode
```
- Увидишь: `62.01` и его расшифровка, город и улица из КЛАДР, индекс по региону.
- Скажи: «Адрес — не случайная строка: 1,99 млн улиц, 16 регионов, индекс и город согласованы.
  ОКВЭД проверяется по справочнику ОК 029-2014 (3069 позиций): ни одного кода вне классификатора».

---

# Видео 2. Библиотека `validum-core` (Java)

## 2.1 Подключение

```kotlin
// build.gradle.kts
implementation("io.github.validum-tech:validum-core:0.4.1")
```
- Увидишь: jar скачивается из Maven Central, `repositories { mavenCentral() }` уже есть.
- Скажи: «`validum-core` — обычная библиотека из Central: без Spring, без сети, ноль
  зависимостей. Единственный внешний артефакт — справочник ЦБ».

## 2.2 Генерация одного человека

```java
import ru.validum.core.bank.*;
import ru.validum.core.profile.*;
import java.nio.file.Path;

BikSource bik = BikSources.resolve(Path.of("data/ED807.xml"));

IndividualProfile p = IndividualProfileGenerator.builder()
    .seed(42)
    .ageRange(25, 54)
    .documentsAuto()
    .region(Region.MOSCOW)
    .bikSource(bik)
    .build()
    .generate();

p.fullName(); p.gender(); p.age(); p.inn(); p.snils();
p.passport().series(); p.passport().number();
p.registrationAddress(); p.account();
```
- Увидишь: объект профиля, все поля доступны типизированно.
- Скажи: «Builder — весь набор опций генератора: seed, asOf, age/ageRange/ageDistribution,
  gender, region, residency, country, bikSource/bik/bank, passportType, passportCountry,
  documents/documentsAuto, corrupt, terrorist, blocked, familyStatus, categoryClient,
  personalManager, email, secretWord».

## 2.3 ИП и организации из того же seed

```java
IndividualProfileGenerator base = IndividualProfileGenerator.builder().seed(42).bikSource(bik).build();
IpProfile ip = IpProfileGenerator.generate(base, 42, "62.01");
OrganizationProfile org = OrganizationProfileGenerator.builder().seed(42).region(Region.MOSCOW).build().generate();
```
- Скажи: «ИП строится от профиля-человека, организация — своим билдером; реквизиты
  согласованы внутри каждой сущности».

## 2.4 Участники и счета

```java
ParticipantRegistry registry = BikRegistryLoader.loadRegistry(Path.of("data/ED807.xml"));
registry.size();                          // ≈1,4 тыс.
registry.banks().size();                  // ≈370
registry.findByBik("044525225");          // Сбербанк

AccountValidator.isValid("40817810600000000001", AccountType.CLAC, "044525225");
AccountGenerator.generate(AccountType.CLAC, "044525225", new java.util.Random(1));
```
- Скажи: «12 типов счетов (BANA, CBRA, CRSA, TRSA, CLRA, EPGA, EPSA, GARA, TRUA, UTRA,
  CLAC, CBDC), ключ по письму № 515 зависит от типа счёта и БИК».

## 2.5 Экспорт ED807 / ED808

```java
String xml = Ed807Exporter.toXml(registry.all(), header, Ed807Exporter.Defaults.moscow());
byte[] ed808 = Ed808Exporter.toBytes(registry, "044525225", header, defaults, seed);
```
- Скажи: «Экспортёры ЦБ вызываются из кода — можно встроить выгрузку в свой конвейер».

## 2.6 Готовые сценарии

```java
IndividualProfile pensioner = IndividualScenarios.createPensionerFemale(1);
IndividualProfile minor    = IndividualScenarios.createMinor(2);
// или по слагу:
IndividualProfile salary   = ScenarioCatalog.bySlug("salary-client").get().withSeed(3);
```
- Увидишь: 8 сценариев (взрослый с/без счёта, пенсионер м/ж, зарплатный, нерезидент с ВНЖ,
  военнослужащий, несовершеннолетний).
- Скажи: «КУС-кейсы как методы — не нужно собирать профиль руками под каждый тест».

## 2.7 Детерминизм и personId

```java
String id1 = PersonIds.of(42);
PersonIds.isValid(id1);                   // true
IndividualProfile a = IndividualProfileGenerator.builder().seed(42).asOf(LocalDate.of(2026,1,15)).bikSource(bik).build().generate();
IndividualProfile b = IndividualProfileGenerator.builder().seed(42).asOf(LocalDate.of(2026,1,15)).bikSource(bik).build().generate();
a.equals(b);                              // true
```
- Скажи: «`personId` — стабильный идентификатор от seed; два генератора с одним seed и
  `asOf` дают идентичный профиль».

---

## 2.8 Двенадцать типов счетов

```java
AccountGenerator gen = new AccountGenerator(new Random(7));
String bik = "044525225";                       // ПАО Сбербанк

String client = gen.generate("40817", AccountType.EPGA, bik);          // 20 цифр, ключ по 515
String corr   = gen.generate("30101", AccountType.BANA, bik);          // корсчёт банка
System.out.println(client + " " + AccountValidator.isValidAuto(client, bik));  // true
System.out.println(corr   + " " + AccountValidator.isValid(corr, AccountType.BANA, bik)); // true
System.out.println(AccountValidator.isValidAuto(client, "044030653"));  // false — не его банк
```
- Увидишь: три `true`/`false` ровно как в комментариях; счета 20-значные.
- Скажи: «`AccountType` — 12 типов из классификатора УФЭБС: клиентский, расчётный, корсчёт,
  транзитный, накопительный. Тип влияет на то, из какого БИК берётся условный номер подразделения,
  поэтому счёт нельзя проверять «сам по себе» — только вместе с банком».

---

# Видео 3. REST API

Запуск: `java -jar validum-api-0.4.0.jar` (Spring Boot, порт 8080). Справочник берётся из
`data/ED807.xml` рядом или через переменную `validum.bik.file`.

## 3.1 Генерация и заголовки воспроизводимости

```bash
curl -s -D - "http://localhost:8080/api/v1/generate/individual?seed=42"
```
- Увидишь: JSON профиля и заголовки **`X-Validum-Version: 0.4.0`** и **`X-Validum-Seed: 42`**.
- Скажи: «Пайплайн может записать в отчёт, каким seed и какой версией получены данные —
  воспроизводимость не "на словах"».

## 3.2 Пакеты и форматы

```bash
curl -s -X POST "http://localhost:8080/api/v1/generate/individual/batch?count=5&format=csv-semicolon" -o batch.csv
curl -s -X POST "http://localhost:8080/api/v1/generate/ip/batch?count=3&format=json-array"
curl -s -X POST "http://localhost:8080/api/v1/generate/organization/batch?count=3&format=json-array"
```
- Увидишь: `batch.csv` с русскими заголовками и `="…"`.
- Скажи: «Три сущности, батчи, форматы json / json-array / csv / csv-semicolon / sql».

## 3.3 Дозаполнение

```bash
curl -s -X POST "http://localhost:8080/api/v1/fill/individual" \
  -H "Content-Type: application/json" -d '{"fullName":"Иванов Иван","gender":"MALE"}'
```
- Увидишь: пустые поля достроены, заданные не тронуты.

## 3.4 Валидация

```bash
curl -s -X POST "http://localhost:8080/api/v1/validate/inn" -H "Content-Type: application/json" -d '"7707083893"'
```
- Увидишь: `{ "valid": true }`. Типы: inn, snils, ogrn, ogrnip, account, kpp, okpo, bik.

## 3.5 Сценарии и экспорт

```bash
curl -s "http://localhost:8080/api/v1/scenarios"
curl -s -X POST "http://localhost:8080/api/v1/scenarios/pensioner-female?seed=1"
curl -s "http://localhost:8080/api/v1/export/ed807"
curl -s "http://localhost:8080/api/v1/export/ed808?bik=044525225"
```
- Скажи: «Сценарии и ЦБ-экспорт доступны и по HTTP — для сервисных интеграций».

## 3.6 Ошибки честные: 400

```powershell
'{"seed":1,"gender":"WRONG","region":"MOSCOW"}' | Set-Content bad-body.json -Encoding utf8
curl -s -w "`nHTTP %{http_code}`n" -X POST http://localhost:8080/api/v1/generate/individual `
  -H "Content-Type: application/json" -d "@bad-body.json"
```
- Увидишь: **400** и тело с полем `message` — сервис отвечает по-человечески и не отдаёт 500.
- Скажи: «Ошибка входа — это 400 с объяснением. 500 в тестовом стенде означает, что сломались
  мы, а не тест».

---

## 3.7 Ошибка честная: 503 вместо пустого ответа

Запусти сервис **без** `data\ED807.xml` и без `VALIDUM_BIK_FILE` (например, из пустого каталога):

```powershell
java -jar validum-api\build\libs\validum-api-0.4.0.jar
# в соседнем окне, из каталога без data\ED807.xml
'{"seed":1,"gender":"MALE","region":"MOSCOW"}' | Set-Content body.json -Encoding utf8
curl -s -w "`nHTTP %{http_code}`n" -X POST http://localhost:8080/api/v1/generate/individual `
  -H "Content-Type: application/json" -d "@body.json"
```
- Увидишь: `HTTP 503` и тело `{"error":"bik_source_missing","message":"Не найден справочник БИК … скачать: validum fetch-bik"}`.
- Скажи: «Нет справочника — сервис не делает вид, что банк сгенерирован: он отвечает 503 и говорит,
  чем это лечится. Пустой массив или случайный БИК сломали бы тест позже и молча».

---

# Видео 4. Allure extension

## 4.1 Подключение

```kotlin
testImplementation("io.github.validum-tech:validum-allure:0.4.1")
testRuntimeOnly("io.qameta.allure:allure-junit5:2.35.3")   // ваша версия Allure
```
- Скажи: «Расширение **не навязывает** версию Allure — зависит от `allure-java-commons`
  как `compileOnly`, поэтому работает и с 2.29, и с 2.35».

## 4.2 Аннотации

```java
import ru.validum.allure.ValidumGenerate;
import ru.validum.allure.ValidumSeed;

class DepositTest {
  @Test
  @ValidumGenerate(seed = 42, age = 16, region = "MOSCOW", gender = "MALE")
  void minorCannotOpenDeposit() { /* тело теста */ }

  @Test
  @ValidumSeed(7)
  void deterministicCase() { /* тело теста */ }
}
```
- Увидишь: расширение генерирует профиль **до** теста и кладёт его в контекст.

## 4.3 Что видно в Allure

- Увидишь: рядом с шагом — attachment **«Validum Profile»** с полным JSON профиля.
- Скажи: «Данные не "где-то сгенерированы", а приложены к каждому отчёту — разбор падения
  не требует догадок».

---

# Демо-репозиторий `validum-demo` (публичный)

**Зачем.** Сейчас у продукта нет живого примера «как подключить и что получается» — это
главный пробел для нового пользователя. Публичный репозиторий закрывает его и даёт:
`Used By` на Maven Central, площадку для статей, готовые команды для видео (линки в README),
один `git clone` вместо инструкции.

**Что внутри — и как это ложится на видео:**

```
validum-demo/
├── README.md                     ← 6 команд из Видео 1 + скриншоты + ссылки на видео
├── cli/                          ← Видео 1
│   ├── run.ps1                   ← сценарий из Видео 1 целиком
│   └── specs/*.json              ← наборы как код
├── core/                         ← Видео 2
│   ├── build.gradle.kts          ← validum-core:0.4.1
│   └── src/test/java/DemoCoreTest.java
├── api/                          ← Видео 3
│   ├── docker-compose.yml        ← REST-сервис одной командой
│   └── requests.http             ← curl'ы из Видео 3 (для IDEA HTTP Client)
├── allure/                       ← Видео 4
│   ├── build.gradle.kts          ← validum-allure + allure-junit5
│   └── src/test/java/DepositTest.java
├── out/                          ← эталонные выводы (обновляются релизом)
│   ├── clients.csv / excel.png / unique-fail.txt
└── .github/workflows/demo.yml    ← generate → coverage → публикация out/
```

**Что можно показывать в репо:**
1. `git clone` → `cd cli` → `./run.ps1` → за минуту весь Видео 1.
2. `docker compose up` в `api/` → REST-сервис без локальной сборки.
3. `core/` и `allure/` — примеры `build.gradle.kts` для копипаста в свой проект.
4. CI: пушишь в репо → workflow генерирует набор, прогоняет `coverage` и публикует `out/`
   как артефакт — наглядная демонстрация «данные как код».

**Что НЕ класть в репо:** `data/ED807.xml` (тяжёлый, скачивается в CI через `fetch-bik`),
внутренние скрипты сборки релиза, ключи/секреты.

---

## Проверить всё сразу (без съёмки)

Сценарий можно прогнать машинно: те же команды, но с проверками (код возврата, поля,
отсутствие регрессий). Пишет отчёт и складывает stdout/stderr каждой команды.
Скрипт живёт в приватном репозитории (`tools/demo-check.py` + обёртка `tools/demo-check.ps1`),
в публичную сборку не входит.

```powershell
.\gradlew :validum-cli:shadowJar
.\tools\demo-check.ps1                       # всё, включая Excel-формат
.\tools\demo-check.ps1 -SkipExcel            # без дружелюбного CSV
.\tools\demo-check.ps1 -SelfTest             # проверить сам скрипт, java не нужна
```

Результат — `out\demo-check\report.md`: таблица «шаг → статус → что не так», рядом
`env.txt` (хеш jar, версия Java, размер справочника) и по папке на каждый шаг
(`stdout.txt`, `stderr.txt`, `command.txt`). Скрипт ничего не меняет в репозитории.

## Чек-лист перед съёмкой

- [ ] каждая команда прогнана **на 0.4.0**, в кадре строка `# validum 0.4.0-SNAPSHOT · seed=…`;
- [ ] `data/ED807.xml` свежий (не старше месяца);
- [ ] Excel-кадр: ИНН текстовый (слева, без `E+11`), счёт 20 цифр, индекс `053796`;
- [ ] в `describe` рядом виден регион серии = регион прописки;
- [ ] отказ `--unique` показан целиком, включая код возврата 2;
- [ ] **все файлы в `out\` перегенерированы после свежей сборки** (`shadowJar`), иначе в кадре
  старый набор без новых колонок — например, без «Действителен до»;
- [ ] консоль в UTF-8 (`chcp 65001`), иначе кириллица в кадре будет кракозябрами.

## Порядок съёмки

1. **Видео 1 (CLI)** — самое длинное, оно «продаёт» продукт; остальные три — короткие,
   по 2–3 минуты, со ссылкой «детали — в Видео 1».
2. **Видео 2 (core)** — для разработчиков, врезка «добавил одну зависимость → получил
   валидный профиль».
3. **Видео 3 (API)** — для тех, кто хочет сервис, а не библиотеку.
4. **Видео 4 (Allure)** — для автоматизаторов: «данные в каждом отчёте».
5. После видео — **`validum-demo`**: README уже ссылается на все четыре ролика.
