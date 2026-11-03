plugins {
    application
    jacoco
    id("com.gradleup.shadow") version "8.3.7"
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
    // Версия из gradle.properties: в тестах манифеста нет, BuildInfo читает это свойство.
    systemProperty("validum.version", project.version.toString())
}

application {
    mainClass.set("ru.validum.cli.ValidumCli")
}

dependencies {
    implementation(project(":validum-core"))
    implementation("info.picocli:picocli:4.7.6")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.26.3")
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

tasks.shadowJar {
    archiveBaseName.set("validum")
    archiveClassifier.set("")
    archiveVersion.set("")
    manifest {
        attributes["Main-Class"] = "ru.validum.cli.ValidumCli"
        // Версия читается BuildInfo: единственный источник — gradle.properties.
        attributes["Implementation-Version"] = project.version.toString()
    }
    minimize()
}
