plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "com.fatihenes.photoreport.core.database"
    compileSdk = 37

    defaultConfig {
        minSdk = 30
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.systemProperty("user.language", "en")
                it.systemProperty("user.country", "US")
            }
        }
    }
}

dependencies {
    api(project(":core:common"))
    api(project(":core:model"))
    api(project(":core:domain"))
    implementation(libs.androidx.core.ktx)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Compose runtime for @Stable animation/performance annotations on Entities
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.runtime)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.core.ktx)
}

kotlin {
    jvmToolchain(17)
}
