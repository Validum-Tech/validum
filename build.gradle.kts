import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions

plugins {
    // Публикация библиотек в Maven Central (Central Portal).
    // Плагин объявлен здесь, применяется только к модулям-библиотекам (см. subprojects).
    id("com.vanniktech.maven.publish") version "0.30.0" apply false
}

allprojects {
    // groupId: GitHub-организация Validum-Tech (подтверждается через GitHub при публикации
    // в Maven Central). Альтернатива при наличии домена — `tech.validum` + DNS TXT на validum.tech.
    group = "io.github.validum-tech"
    // Версия — из gradle.properties (единственный источник правды), фолбэк — на случай
    // запуска без файла свойств.
    version = providers.gradleProperty("version").getOrElse("0.1.0")
}

// ---------------------------------------------------------------------------------------------
// Публикация артефактов
//
// Публикуются две библиотеки: validum-core и validum-allure. validum-cli и validum-api —
// приложения, они уходят в GitHub Release и в реестр не попадают.
//
// Куда: Maven Central через Central Portal (vanniktech/maven-publish-plugin, SonatypeHost.CENTRAL_PORTAL).
// Что нужно для боевой публикации:
//   CENTRAL_USERNAME / CENTRAL_TOKEN — токен Central Portal (CI: секреты);
//   signingInMemoryKey / signingInMemoryKeyPassword — PGP-ключ и его пароль
//   (CI: секреты SIGNING_KEY / SIGNING_PASSWORD через ORG_GRADLE_PROJECT_*).
//
// Без ключей всё работает локально: ./gradlew publishToMavenLocal (подпись не выполняется).
// Порядок получения ключей и токена — docs/RELEASING.md.
// ---------------------------------------------------------------------------------------------
subprojects {
    // ---------------------------------------------------------------------------------------
    // Общая конфигурация компиляции и тестов — одна копия вместо четырёх модульных.
    // Отличается только «пол» байткода: библиотеки (core/allure) и CLI обещают Java 17,
    // REST-сервис собирается под 21 (его поднимает Spring Boot 3.5).
    // ---------------------------------------------------------------------------------------
    val moduleRelease = if (name == "validum-api") 21 else 17

    plugins.withId("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                // CI-полоса JDK 17 передаёт -Pvalidum.javaToolchain=17.
                languageVersion.set(
                    JavaLanguageVersion.of(
                        providers.gradleProperty("validum.javaToolchain")
                            .getOrElse(moduleRelease.toString())
                    )
                )
            }
        }

        tasks.withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
            options.compilerArgs.addAll(listOf("-Xlint:all", "-parameters"))
            if (name != "validum-api") {
                // Контракт совместимости: байткод Java 17 (major 61) при сборке на 21.
                options.release.set(17)
            }
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
            // Справочник БИК для тестов: встроенного демо-набора в продукте нет.
            systemProperty(
                "validum.bik.file",
                file("src/test/resources/bik-sample.xml").absolutePath,
            )
        }
    }

    // Javadoc пишем по-русски и не валим сборку на строгом doclint
    tasks.withType<Javadoc>().configureEach {
        (options as StandardJavadocDocletOptions).apply {
            encoding = "UTF-8"
            charSet = "UTF-8"
            addStringOption("Xdoclint:none", "-quiet")
        }
    }

    if (name == "validum-core" || name == "validum-allure") {
        apply(plugin = "com.vanniktech.maven.publish")

        // Подпись включается только когда ключ реально задан: локальная сборка без секретов работает.
        val signingReady =
            listOf("signingInMemoryKey", "signingKey").any { providers.gradleProperty(it).isPresent } ||
                listOf("ORG_GRADLE_PROJECT_signingInMemoryKey", "ORG_GRADLE_PROJECT_signingKey")
                    .any { !System.getenv(it).isNullOrBlank() }

        extensions.configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
            publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL, true)
            if (signingReady) {
                signAllPublications()
            }
            coordinates("io.github.validum-tech", project.name, project.version.toString())
            pom {
                name.set(project.name)
                description.set(
                    "Validum — генератор валидных тестовых данных для российского финтеха",
                )
                url.set("https://github.com/Validum-Tech/validum")
                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }
                developers {
                    developer {
                        id.set("Validum-Tech")
                        name.set("Validum Tech")
                        url.set("https://validum.tech")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/Validum-Tech/validum.git")
                    developerConnection.set(
                        "scm:git:ssh://git@github.com/Validum-Tech/validum.git",
                    )
                    url.set("https://github.com/Validum-Tech/validum")
                }
            }
        }
    }
}
