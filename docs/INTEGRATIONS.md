# Интеграции

Java-библиотека и JUnit 5 + Allure: как подключить validum в код и в отчётность.

## 🎭 Allure Extension (P3.2)

```kotlin
testImplementation("io.github.validum-tech:validum-allure:0.4.1")
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
