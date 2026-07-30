package com.fatihenes.photoreport.ui.viewmodel

import android.net.Uri
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.video.Quality
import androidx.compose.runtime.Stable

/**
 * Immutable and @Stable UI state container for CameraScreen.
 * Prevents unnecessary recompositions by grouping state changes efficiently.
 */
@Stable
data class CameraUiState(
    val cameraMode: String = "PHOTO",
    val isRecording: Boolean = false,
    val recordingDuration: Int = 0,
    val flashMode: Int = ImageCapture.FLASH_MODE_OFF,
    val videoQuality: Quality = Quality.FHD,
    val aspectRatio: Int = AspectRatio.RATIO_4_3,
    val zoomRatio: Float = 1f,
    val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val isGridVisible: Boolean = false,
    val showExposure: Boolean = false,
    val exposureValue: Float = 0f,
    val lastCapturedUri: Uri? = null,
    val isPaused: Boolean = false,
    val showSettingsPanel: Boolean = false,
    val sessionPhotoCount: Int = 0
)
