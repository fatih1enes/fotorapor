package com.fatihenes.photoreport

import android.os.Bundle
import android.view.animation.AccelerateInterpolator
import android.view.animation.AnticipateInterpolator
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fatihenes.photoreport.ui.navigation.AppNavGraph
import com.fatihenes.photoreport.ui.theme.PhotoReportTheme
import com.fatihenes.photoreport.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Hold the splash screen until initial preferences are loaded
        // This prevents the "disclosure flicker" on cold starts.
        var isReady = false
        splashScreen.setKeepOnScreenCondition { !isReady }

        // Premium exit animation: Subtle scale and fade out
        splashScreen.setOnExitAnimationListener { splashProvider ->
            val iconView = splashProvider.iconView

            // Icon scale and fade
            iconView.animate()
                .scaleX(0.4f)
                .scaleY(0.4f)
                .alpha(0f)
                .setDuration(400L)
                .setInterpolator(AnticipateInterpolator())
                .withEndAction { splashProvider.remove() }
                .start()

            // Background fade
            splashProvider.view.animate()
                .alpha(0f)
                .setDuration(450L)
                .setInterpolator(AccelerateInterpolator())
                .start()
        }

        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val language by viewModel.language.collectAsStateWithLifecycle()
            val isInitialized by viewModel.isInitialized.collectAsStateWithLifecycle()

            // Update local state to dismiss splash screen
            LaunchedEffect(isInitialized) {
                if (isInitialized) {
                    isReady = true
                }
            }

            LaunchedEffect(language) {
                val requestedLocales = LocaleListCompat.forLanguageTags(language)
                if (AppCompatDelegate.getApplicationLocales() != requestedLocales) {
                    AppCompatDelegate.setApplicationLocales(requestedLocales)
                }
            }

            val isDarkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            val initialCameraProjectId = if (intent.action == "com.fatihenes.photoreport.ACTION_WIDGET_CAMERA") {
                intent.getLongExtra("PROJECT_ID", -1L)
            } else {
                -1L
            }

            val initialProjectDetailId = if (intent.action == "com.fatihenes.photoreport.ACTION_WIDGET_PROJECT") {
                intent.getLongExtra("PROJECT_ID", -1L)
            } else {
                -1L
            }

            PhotoReportTheme(darkTheme = isDarkTheme) {
                AppNavGraph(
                    viewModel = viewModel,
                    initialCameraProjectId = initialCameraProjectId,
                    initialProjectDetailId = initialProjectDetailId
                )
            }
        }
    }
}
