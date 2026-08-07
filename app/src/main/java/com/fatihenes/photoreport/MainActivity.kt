package com.fatihenes.photoreport

import androidx.activity.viewModels
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {
            viewModel.uiState.value.isLoading
        }

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(uiState.language) {
                if (uiState.language.isNotBlank()) {
                    val requestedLocales = LocaleListCompat.forLanguageTags(uiState.language)
                    if (AppCompatDelegate.getApplicationLocales() != requestedLocales) {
                        AppCompatDelegate.setApplicationLocales(requestedLocales)
                    }
                }
            }

            val isDarkTheme = when (uiState.themeMode) {
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
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNavGraph(
                        viewModel = viewModel,
                        initialCameraProjectId = initialCameraProjectId,
                        initialProjectDetailId = initialProjectDetailId,
                    )
                }
            }
        }
    }
}

