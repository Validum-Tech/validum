plugins {
    `java-library`
}

// toolchain, release, UTF-8, -Xlint и validum.bik.file — в корневом build.gradle.kts (subprojects).

dependencies {
    implementation(project(":validum-core"))

    // C11: версию Allure НЕ навязываем.
    // Компилируемся против минимального API (2.29.1) и не публикуем зависимость в POM:
    // потребитель приносит свою версию (allure-junit5 2.29.x, allure-jupiter 2.35.x, любую 2.x).
    compileOnly("io.qameta.allure:allure-java-commons:2.29.1")

    // JUnit API нужен в main-коде: BeforeEachCallback, ExtensionContext
    implementation("org.junit.jupiter:junit-jupiter-api:5.11.3")

    // Jackson — для сериализации профиля в JSON-attachment
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2")

    // Прогоняем наши тесты на самом старом API, который обещаем поддерживать.
    testRuntimeOnly("io.qameta.allure:allure-java-commons:2.29.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.26.3")
}
