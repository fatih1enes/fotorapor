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
import android.view.KeyEvent
import com.fatihenes.photoreport.feature.camera.engine.CameraKeyEventDispatcher
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var cameraKeyEventDispatcher: CameraKeyEventDispatcher

    private val viewModel: MainViewModel by viewModels()

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (::cameraKeyEventDispatcher.isInitialized && cameraKeyEventDispatcher.onKeyEvent(event)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    // Widget deep-link activity yaşarken gelirse onNewIntent ile güncellenir.
    // Davranış korunur: yoksa (null, -1) -> eski -1 fallback.
    private val widgetIntentFlow = MutableStateFlow<Pair<String?, Long>>(null to -1L)

    private fun updateWidgetIntent(intent: Intent?) {
        val action = intent?.action
        if (action == "com.fatihenes.photoreport.ACTION_WIDGET_CAMERA" ||
            action == "com.fatihenes.photoreport.ACTION_WIDGET_PROJECT" ||
            action == "com.fatihenes.photoreport.ACTION_WIDGET_HOME"
        ) {
            widgetIntentFlow.value = action to intent.getLongExtra("PROJECT_ID", -1L)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        updateWidgetIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {
            viewModel.uiState.value.isLoading
        }

        // System splash → custom launch geçişinde siyah frame olmaması için
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            splashScreenViewProvider.remove()
        }

        updateWidgetIntent(intent)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val widgetIntent by widgetIntentFlow.collectAsStateWithLifecycle()

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
                "dark", "high_contrast" -> true
                else -> isSystemInDarkTheme()
            }

            val widgetAction = widgetIntent.first
            val initialCameraProjectId = if (widgetAction == "com.fatihenes.photoreport.ACTION_WIDGET_CAMERA") {
                widgetIntent.second
            } else {
                -1L
            }

            val initialProjectDetailId = if (widgetAction == "com.fatihenes.photoreport.ACTION_WIDGET_PROJECT") {
                widgetIntent.second
            } else {
                -1L
            }

            PhotoReportTheme(
                darkTheme = isDarkTheme,
                themeMode = uiState.themeMode,
            ) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNavGraph(
                        viewModel = viewModel,
                        widgetAction = widgetAction,
                        initialCameraProjectId = initialCameraProjectId,
                        initialProjectDetailId = initialProjectDetailId,
                    )
                }
            }
        }
    }
}

