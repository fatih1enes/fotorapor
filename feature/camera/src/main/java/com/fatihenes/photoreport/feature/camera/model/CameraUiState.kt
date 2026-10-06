package com.fatihenes.photoreport.feature.camera.model

import android.net.Uri
import androidx.camera.video.Quality
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import com.fatihenes.photoreport.feature.camera.engine.AspectRatioSelection
import com.fatihenes.photoreport.feature.camera.engine.CameraMode
import com.fatihenes.photoreport.feature.camera.engine.FlashMode

/**
 * Immutable UI state container for CameraScreen.
 * Prevents unnecessary recompositions by grouping state changes efficiently.
 */
@Immutable
data class CameraUiState(
    val cameraMode: CameraMode = CameraMode.PHOTO,
    val flashMode: FlashMode = FlashMode.OFF,
    val aspectRatio: AspectRatioSelection = AspectRatioSelection.RATIO_4_3,
    val videoQuality: Quality = Quality.FHD,
    val zoomRatio: Float = 1f,
    val minZoom: Float = 1f,
    val maxZoom: Float = 8f,
    val exposureIndex: Int = 0,
    val exposureRange: ClosedRange<Int> = 0..0,
    val isGridVisible: Boolean = false,
    val isLevelVisible: Boolean = true,
    val isFocusLocked: Boolean = false,
    val tapOffset: Offset? = null,
    val isShutterBlinking: Boolean = false,
    val isTransitioning: Boolean = false,
    val isQuickSettingsOpen: Boolean = false,
    val isSessionReviewOpen: Boolean = false,
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val recordingDurationSeconds: Int = 0,
    val activeCapturesCount: Int = 0,
    val lastCapturedUri: Uri? = null,
    val sessionUris: List<Uri> = emptyList(),
    val errorMessage: String? = null
) {
    val sessionCount: Int get() = sessionUris.size
}
