// Top-level build file where you can add configuration options common to all subprojects/modules.
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
    id("io.gitlab.arturbosch.detekt") version "1.22.0" apply false
    id("org.jlleitschuh.gradle.ktlint") version "11.6.0" apply false
}

// Configure detekt and ktlint for all subprojects
subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    // Detekt configuration
    // Configure Detekt tasks directly to set JVM target and config
    tasks.withType(io.gitlab.arturbosch.detekt.Detekt::class.java).configureEach {
        jvmTarget = "17"
        config.setFrom(files(rootProject.file("detekt.yml")))
        buildUponDefaultConfig = true
    }

    // KtLint uses .editorconfig at repo root; no further Gradle config required here
}
