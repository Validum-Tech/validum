plugins {
    java
    jacoco
    id("org.springframework.boot") version "4.1.2"
    id("io.spring.dependency-management") version "1.1.7"
}

// toolchain 21, UTF-8, -Xlint, junit platform и validum.bik.file — в корневом build.gradle.kts.

dependencies {
    implementation(project(":validum-core"))

    // Boot 4: spring-boot-starter-web — deprecated, замена spring-boot-starter-webmvc
    // (см. Spring Boot 4.0 Migration Guide, раздел «Deprecated Starters»).
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // springdoc 2.x собран против Boot 3; для Boot 4 нужна ветка 3.x.
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.2.0")

    // Тестовый стартер технологии: тянет spring-boot-starter-test транзитивно,
    // но добавляет MockMvc/автоконфигурацию webmvc из модульной поставки Boot 4.
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    workingDir = rootProject.projectDir
}

// --- Публикация в Maven Central --------------------------------------------
// По умолчанию Boot-плагин собирает только boot-jar и отключает обычный jar.
// Для публикации нужен библиотечный артефакт без classifier, поэтому:
//   validum-api-<версия>.jar        — библиотека (DTO, котроллеры, конфигурация) для потребителей
//   validum-api-<версия>-boot.jar   — исполняемый Spring Boot jar (Docker, rest-smoke, demo)
tasks.jar {
    enabled = true
    archiveClassifier.set("")
}

tasks.bootJar {
    archiveClassifier.set("boot")
}

// --- Покрытие (jacoco) -------------------------------------------------------
tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(true)
    }
}

tasks.named("check") {
    dependsOn(tasks.jacocoTestReport)
}
