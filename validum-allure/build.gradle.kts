plugins {
    `java-library`
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
    // Справочник БИК для тестов: встроенного демо-набора в продукте нет.
    systemProperty(
        "validum.bik.file",
        file("src/test/resources/bik-sample.xml").absolutePath,
    )
}

dependencies {
    implementation(project(":validum-core"))

    // Allure (2.35.x): allure-jupiter заменил allure-junit5
    implementation("io.qameta.allure:allure-jupiter:3.0.1")

    // JUnit API нужен в main-коде: BeforeEachCallback, ExtensionContext
    implementation("org.junit.jupiter:junit-jupiter-api:6.1.3")

    // Jackson — для сериализации профиля в JSON-attachment
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2")

    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.27.7")
}
