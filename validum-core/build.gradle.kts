plugins {
    `java-library`
    jacoco
}

// toolchain, release 17, UTF-8, -Xlint и validum.bik.file — в корневом build.gradle.kts (subprojects).

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
