# Быстрый старт

Установка, первый прогон, сборка из исходников, тесты, Docker.

## ⚠️ Важно: данные синтетические

Validum создаёт **вымышленные** данные для тестовых стендов. Совпадения с реальными людьми
или организациями случайны: пространство ИНН, ФИО и адресов конечно, а справочники взяты
из открытых источников. Не загружайте результат в продуктивные системы и не используйте
как основание для юридически значимых решений.

Государственные справочники (ED807/ED808, КЛАДР, ОКВЭД-2, коды ЦБ) персональных данных
не содержат. Соответствие требованиям 152-ФЗ обеспечивается тем, что в контуре нет боевых
данных клиентов, — но ответственность за то, что вы генерируете и куда кладёте, остаётся
на операторе.

## 🚀 Быстрый старт

Три команды — от нуля до данных. Нужен только JDK 17+ (или Docker).

```powershell
# 1. Справочник участников ЦБ (обязателен: 1380 участников, 370 банков)
java -jar validum.jar fetch-bik -o data/ED807.xml

# 2. Профиль клиента с реквизитами, банком и счётом
java -jar validum.jar generate individual --seed 42

# 3. Тысяча клиентов в CSV / SQL / JSON
java -jar validum.jar generate individual --seed 42 -n 1000 --format csv -o clients.csv
java -jar validum.jar generate individual --seed 42 -n 1000 --format sql --dialect postgres -o seed.sql
```

Собрать jar: `./gradlew :validum-cli:shadowJar` → `validum-cli/build/libs/validum.jar`.
Пошаговые сценарии под конкретные задачи — в [`docs/recipes/`](recipes/README.md)
(фикстуры в JUnit, заливка в БД, нерезиденты, ДУЛ, ED807/ED808, детерминизм, Docker).

---

## 🛠 Сборка из исходников

### Требования

- **JDK 17+** для CLI/библиотек (Temurin, Corretto, OpenJDK); **JDK 21** для REST-сервиса
- **Gradle Wrapper** (в репо)
- **Docker Desktop** (опционально)

### Клонирование и сборка

```bash
git clone https://github.com/Validum-Tech/validum.git
cd validum
./gradlew build
```

**На Windows:**

```powershell
.\gradlew.bat build
```

### CLI (без Docker, без Spring)

```powershell
.\gradlew :validum-cli:shadowJar
java -jar .\validum-cli\build\libs\validum.jar generate individual --seed 42 --format csv -o profile.csv
```

### REST API локально

```bash
./gradlew :validum-api:bootRun
```

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **Health:** http://localhost:8080/actuator/health

---

## 🔨 Сборка и тесты

```bash
./gradlew build         # всё
./gradlew test          # только тесты
```

**На Windows:**

```powershell
.\gradlew.bat test --rerun-tasks
# HTML-отчёт: build/reports/tests/test/index.html
```

**CLI fat JAR:**

```powershell
.\gradlew :validum-cli:shadowJar
# → validum-cli/build/libs/validum.jar
```

### 📊 Тесты

| Модуль | Тестов |
|---|---|
| `validum-core` | **567** |
| `validum-api` | **80** |
| `validum-cli` | **55** |
| `validum-allure` | **19** |
| **Итого** | **860** |

Покрытие строк по jacoco (прогон 30.10.2026): `validum-core` — **89,2%** (2309/2588), `validum-api` — **92,2%** (404/438), `validum-cli` — считается тем же отчётом (HTML — `validum-cli/build/reports/jacoco/test/html/index.html`). Худшие пакеты: `validation` (75,9%), `common` (77,2%), `address` (77,8%); полностью покрыты `passport`, `snils`, `ogrn`, `random`. HTML-отчёт — `validum-core/build/reports/jacoco/test/html/index.html`.

---

## 🔄 CI

**GitHub Actions** на каждый push / PR в `main`:
- Ubuntu + JDK 21 (Corretto), кэш Gradle
- `./gradlew build --no-daemon` — тесты всех четырёх модулей
- Сводка в Summary: тесты по модулям + покрытие строк (jacoco) для `validum-core`, `validum-api` и `validum-cli`
- Артефакты: HTML-отчёты тестов (14 дней), покрытие jacoco, готовый `validum.jar` (30 дней)

---

## 🐳 Docker

```bash
docker compose up -d
# → Swagger UI: http://localhost:8080/swagger-ui.html
```

- Multi-stage build: `gradle:8.14-jdk21` → `eclipse-temurin:21-jre-alpine`
- Non-root user, container-aware JVM
- Справочник ЦБ монтируется как volume: `./data:/app/data:ro`
- Healthcheck: `/actuator/health`
- Размер: ~250–350 МБ

**DNS (Windows Docker Desktop):** если `UnknownHostException` — Settings → Docker Engine → `"dns": ["8.8.8.8", "1.1.1.1"]`.

---
