package com.fatihenes.photoreport.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This test class generates a basic startup baseline profile for the target package.
 *
 * We recommend you start with this but add important user flows to the profile to improve their performance.
 * Refer to the [baseline profile documentation](https://d.android.com/topic/performance/baselineprofiles)
 * for more information.
 *
 * You can run the generator with the "Generate Baseline Profile" run configuration in Android Studio or
 * the equivalent `generateBaselineProfile` gradle task:
 * ```
 * ./gradlew :app:generateReleaseBaselineProfile
 * ```
 * The run configuration runs the Gradle task and applies filtering to run only the generators.
 *
 * Check [documentation](https://d.android.com/topic/performance/Benchmarking/macroBenchmark-instrumentation-args)
 * for more information about available instrumentation arguments.
 *
 * After you run the generator, you can verify the improvements running the [StartupBenchmarks] Benchmark.
 *
 * When using this class to generate a baseline profile, only API 33+ or rooted API 28+ are supported.
 *
 * The minimum required version of androidx.Benchmark to generate a baseline profile is 1.2.0.
 **/
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    private companion object {
        private const val TIMEOUT = 3_000L
    }

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        // The application id for the running build variant is read from the instrumentation arguments.
        rule.collect(
            packageName = InstrumentationRegistry.getArguments().getString("targetAppId")
                ?: throw Exception("targetAppId not passed as instrumentation runner arg"),

            // See: https://d.android.com/topic/performance/baselineprofiles/dex-layout-optimizations
            includeInStartupProfile = true
        ) {
            // This block defines the app's critical user journey. Here we are interested in
            // optimizing for app startup. But you can also navigate and scroll through your most important UI.

            // Start default activity for your app
            pressHome()
            startActivityAndWait()

            // 1. Dashboard -> Scroll
            device.wait(Until.hasObject(By.scrollable(true)), 5_000)
            val scrollableList = device.findObject(By.scrollable(true))
            if (scrollableList != null) {
                scrollableList.setGestureMargin(device.displayWidth / 5)
                scrollableList.scroll(Direction.DOWN, 1f)
                device.waitForIdle()
            }

            // 2. Dashboard -> Settings -> Theme Change Simulation
            val settingsBtn = device.findObject(By.descContains("Ayarlara git"))
            if (settingsBtn != null) {
                settingsBtn.click()
                device.waitForIdle()

                // Wait for settings screen and click Dark Theme option
                device.wait(Until.hasObject(By.textContains("Karanlık Tema")), TIMEOUT)
                val darkThemeOption = device.findObject(By.textContains("Karanlık Tema"))
                darkThemeOption?.click()
                device.waitForIdle()

                // Go back to Dashboard
                val backBtn = device.findObject(By.descContains("Geri"))
                backBtn?.click()
                device.waitForIdle()
            }

            // 3. Dashboard -> Project Detail -> Camera -> Capture Simulation
            val projectItem = device.findObject(By.scrollable(true))?.children?.firstOrNull { it.isClickable }
            if (projectItem != null) {
                projectItem.click()
                device.waitForIdle()

                // Wait for Project Detail and click Camera FAB
                // acc_shutter is used for both project detail FAB and camera capture button
                device.wait(Until.hasObject(By.descContains("Fotoğraf veya video çek")), TIMEOUT)
                val cameraFab = device.findObject(By.descContains("Fotoğraf veya video çek"))
                if (cameraFab != null) {
                    cameraFab.click()
                    device.waitForIdle()

                    // Now in Camera Screen, wait for capture button and click
                    device.wait(Until.hasObject(By.descContains("Fotoğraf veya video çek")), 5_000)
                    val captureBtn = device.findObject(By.descContains("Fotoğraf veya video çek"))
                    captureBtn?.click()
                    device.waitForIdle()

                    // Wait a bit for processing and go back
                    device.wait(Until.hasObject(By.descContains("Kapat")), 5_000)
                    val closeCameraBtn = device.findObject(By.descContains("Kapat"))
                    closeCameraBtn?.click()
                    device.waitForIdle()
                }

                // Go back to Dashboard
                val backToDashBtn = device.findObject(By.descContains("Geri"))
                backToDashBtn?.click()
                device.waitForIdle()
            }
        }
    }
}
