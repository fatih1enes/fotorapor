// Top-level build file where you can add configuration options common to all subprojects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.baselineprofile) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.sonarqube)
}

extensions.configure<org.sonarqube.gradle.SonarExtension> {
    properties {
        property("sonar.projectKey", "PhotoReport")
        property("sonar.projectName", "PhotoReport")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.kotlin.detekt.reportPaths", "**/build/reports/detekt/detekt.xml")
        property("sonar.androidLint.reportPaths", "**/build/reports/lint-results-*.xml")
        property("sonar.coverage.jacoco.xmlReportPaths", "**/build/reports/jacoco/test/jacocoTestReport.xml")
    }
}

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        buildUponDefaultConfig = true
        allRules = false
        config.setFrom(files("$rootDir/detekt.yml"))
        baseline = file("$projectDir/detekt-baseline.xml")
    }

    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        reports {
            html.required.set(true)
            xml.required.set(true)
            sarif.required.set(true)
        }
        mustRunAfter(tasks.matching { it.name == "detektBaseline" })
    }

    extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        android.set(true)
        ignoreFailures.set(false)
        reporters {
            reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.HTML)
            reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
        }
    }

    // Configure Lint baseline for all Android modules
    plugins.withId("com.android.application") {
        configure<com.android.build.api.dsl.ApplicationExtension> {
            lint.baseline = file("$projectDir/lint-baseline.xml")
        }
    }
    plugins.withId("com.android.library") {
        configure<com.android.build.api.dsl.LibraryExtension> {
            lint.baseline = file("$projectDir/lint-baseline.xml")
        }
    }
}

tasks.register("quality") {
    description = "Runs all code quality checks: detekt, ktlint, lint, and unit tests."
    group = "Verification"

    dependsOn(subprojects.map { it.tasks.matching { t -> t.name == "detekt" } })
    dependsOn(subprojects.map { it.tasks.matching { t -> t.name == "ktlintCheck" } })
    dependsOn(subprojects.map { it.tasks.matching { t -> t.name == "lint" } })
    dependsOn(subprojects.map { it.tasks.matching { t -> t.name == "test" } })
}
