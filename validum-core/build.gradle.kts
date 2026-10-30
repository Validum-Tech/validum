plugins {
    `java-library`
    jacoco
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-parameters"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    // Справочник БИК для тестов: встроенного демо-набора в продукте нет,
    // поэтому тесты явно указывают файл ED807 (см. BikSources).
    systemProperty(
        "validum.bik.file",
        file("src/test/resources/bik-sample.xml").absolutePath,
    )
}

dependencies {
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.assertj.core)
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
