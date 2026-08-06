package com.fatihenes.photoreport.core.model

import androidx.annotation.Keep

/**
 * Core domain representation of AppSettings.
 */
@Keep
data class AppSettings(
    val themeMode: String,
    val language: String,
    val cameraOptimization: Boolean,
    val avifEnabled: Boolean,
    val gpsWatermarkEnabled: Boolean,
    val disclosureShown: Boolean
)
