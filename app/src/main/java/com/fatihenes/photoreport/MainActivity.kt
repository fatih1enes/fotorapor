package com.fatihenes.photoreport

import android.os.Bundle
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)


        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val language by viewModel.language.collectAsStateWithLifecycle()

            // The preference previously lived only in DataStore, so Compose kept
            // reading the device locale instead of the app language selection.
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
                    initialProjectDetailId = initialProjectDetailId,
                )
            }
        }
    }
}
