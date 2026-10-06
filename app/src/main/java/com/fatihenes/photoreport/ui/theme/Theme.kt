package com.fatihenes.photoreport.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

@Composable
fun PhotoReportTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeMode: String = "system",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    com.fatihenes.photoreport.core.designsystem.theme.PhotoReportTheme(
        darkTheme = darkTheme,
        themeMode = themeMode,
        dynamicColor = dynamicColor,
        content = content
    )
}
