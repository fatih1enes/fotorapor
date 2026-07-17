package com.sarikaya.santiye.gunlugu

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sarikaya.santiye.gunlugu.ui.navigation.AppNavGraph
import com.sarikaya.santiye.gunlugu.ui.theme.SantiyeGunluguTheme
import com.sarikaya.santiye.gunlugu.ui.viewmodel.AppViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (intent.getBooleanExtra("CRASH_RECOVERY", false)) {
            Toast.makeText(this, getString(R.string.crash_recovery_message), Toast.LENGTH_LONG).show()
        }

        setContent {
            val viewModel: AppViewModel = hiltViewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

            val isDarkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            val initialCameraProjectId = if (intent.action == "com.sarikaya.santiye.gunlugu.ACTION_WIDGET_CAMERA") {
                intent.getLongExtra("PROJECT_ID", -1L)
            } else {
                -1L
            }

            val initialProjectDetailId = if (intent.action == "com.sarikaya.santiye.gunlugu.ACTION_WIDGET_PROJECT") {
                intent.getLongExtra("PROJECT_ID", -1L)
            } else {
                -1L
            }

            SantiyeGunluguTheme(darkTheme = isDarkTheme) {
                AppNavGraph(
                    viewModel = viewModel,
                    initialCameraProjectId = initialCameraProjectId,
                    initialProjectDetailId = initialProjectDetailId
                )
            }
        }
    }
}
