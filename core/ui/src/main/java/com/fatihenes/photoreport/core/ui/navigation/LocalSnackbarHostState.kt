package com.fatihenes.photoreport.core.ui.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.staticCompositionLocalOf

val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> {
    error("LocalSnackbarHostState provide edilmedi! AppNavGraph kontrol edilmeli.")
}
