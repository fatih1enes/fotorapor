package com.fatihenes.photoreport.feature.camera.ui

data class CameraScreenSettings(
    val enableOptimization: Boolean = true,
    val enableAvif: Boolean = true,
    val gpsWatermarkEnabled: Boolean = true,
    val onToggleOptimization: (Boolean) -> Unit = {},
    val onToggleAvif: (Boolean) -> Unit = {},
    val onToggleGpsWatermark: (Boolean) -> Unit = {},
)
