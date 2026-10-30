import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.plugins.signing.SigningExtension

allprojects {
    // groupId: GitHub-организация Validum-Tech (подтверждается через GitHub при публикации
    // в Maven Central). Альтернатива при наличии домена — `tech.validum` + DNS TXT на validum.tech.
    group = "io.github.validum-tech"
    version = "0.1.0"
}

// ---------------------------------------------------------------------------------------------
// Публикация библиотек (Maven Central / Maven Local / GitHub Packages)
//
// В Maven идут ДВЕ библиотеки: validum-core (ядро) и validum-allure (JUnit-расширение).
// Не публикуются:
//   validum-cli — приложение: fat jar в GitHub Releases (shadow добавляет второй jar,
//                 публикация падала бы на дубликате артефакта);
//   validum-api — сервис, а не библиотека: Docker-образ + boot-jar в GitHub Releases.
//                 Публиковать Spring-приложение в Maven смысла нет — его запускают, а не встраивают.
//
// Для подписи нужны секреты (CI) или ~/.gradle/gradle.properties:
//   signingKey      — armored PGP-ключ (-----BEGIN PGP PRIVATE KEY BLOCK-----…)
//   signingPassword — пароль ключа
//
// Локальная проверка без секретов:  ./gradlew publishToMavenLocal --no-configuration-cache
// ---------------------------------------------------------------------------------------------
subprojects {
    pluginManager.apply("maven-publish")
    pluginManager.apply("signing")

    val signingKey = providers.gradleProperty("signingKey")
    val signingPassword = providers.gradleProperty("signingPassword")

    afterEvaluate {
        // Публикуем только библиотеки: см. комментарий выше.
        if (!plugins.hasPlugin("java") || name !in setOf("validum-core", "validum-allure")) {
            return@afterEvaluate
        }

        extensions.configure<JavaPluginExtension> {
            withSourcesJar()
            withJavadocJar()
        }

        // Javadoc пишем по-русски и не валим сборку на строгом doclint
        tasks.withType<Javadoc>().configureEach {
            (options as StandardJavadocDocletOptions).apply {
                encoding = "UTF-8"
                charSet = "UTF-8"
                addStringOption("Xdoclint:none", "-quiet")
            }
        }

        // Только строки: pom не должен захватывать Project/Configuration, иначе ломается
        // configuration cache (Gradle 8.14: «cannot serialize object of type DefaultProject»).
        val moduleName = name
        val pomDescription =
            "Validum — генератор валидных тестовых данных для российского финтеха"
        val projectUrl = "https://github.com/Validum-Tech/validum"
        val scmUrl = "scm:git:https://github.com/Validum-Tech/validum.git"
        val scmDevUrl = "scm:git:ssh://git@github.com/Validum-Tech/validum.git"

        val publishing = extensions.getByType<PublishingExtension>()

        publishing.repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/Validum-Tech/validum")
                val actor = System.getenv("GITHUB_ACTOR")
                val token = System.getenv("GITHUB_TOKEN")
                if (actor != null && token != null) {
                    credentials {
                        username = actor
                        password = token
                    }
                }
            }
        }

        val publication = publishing.publications.create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name.set(moduleName)
                description.set(pomDescription)
                url.set(projectUrl)
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
                    connection.set(scmUrl)
                    developerConnection.set(scmDevUrl)
                    url.set(projectUrl)
                }
            }
        }

        extensions.configure<SigningExtension> {
            if (signingKey.isPresent && signingPassword.isPresent) {
                useInMemoryPgpKeys(signingKey.get(), signingPassword.get())
                sign(publication)
            }
        }
    }
}
