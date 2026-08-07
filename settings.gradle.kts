@file:Suppress("UnstableApiUsage")
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "PhotoReport"
include(":app")
include(":baselineProfile")
include(":core:common")
include(":core:database")
include(":core:model")
include(":core:domain")
include(":core:designsystem")
include(":core:ui")
include(":core:datastore")
include(":core:media")
include(":core:export")
include(":feature:dashboard")
include(":feature:settings")
include(":feature:trash")
include(":feature:project")
include(":feature:camera")
include(":feature:export")
include(":feature:backup")
