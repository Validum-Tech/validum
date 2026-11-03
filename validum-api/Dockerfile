# ===== Stage 1: Builder =====
FROM gradle:8.14-jdk21 AS builder

WORKDIR /app

# Копируем только build-скрипты для кэширования зависимостей
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY validum-core/build.gradle.kts ./validum-core/
COPY validum-api/build.gradle.kts ./validum-api/
COPY validum-allure/build.gradle.kts ./validum-allure/
COPY validum-cli/build.gradle.kts ./validum-cli/

# Используем системный gradle из образа — wrapper не нужен
RUN gradle dependencies --no-daemon

# Копируем исходники
COPY validum-core/src ./validum-core/src
COPY validum-api/src ./validum-api/src

# Собираем JAR без тестов
RUN gradle :validum-api:bootJar -x test --no-daemon

# ===== Stage 2: Runtime =====
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -g 1000 appgroup && \
    adduser -D -u 1000 -G appgroup appuser && \
    chown -R appuser:appgroup /app

COPY --from=builder /app/validum-api/build/libs/*.jar app.jar
RUN chown appuser:appgroup app.jar

USER appuser

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
