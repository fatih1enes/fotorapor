@file:Suppress("MagicNumber")
package com.fatihenes.photoreport.feature.camera.ui.components

import androidx.camera.video.Quality
import androidx.camera.video.Recording
import androidx.camera.view.PreviewView
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.fatihenes.photoreport.feature.camera.model.CameraUiState
import com.fatihenes.photoreport.feature.camera.ui.CameraStateHolder
import com.fatihenes.photoreport.feature.camera.viewmodel.CameraViewModel

val Amber = Color(0xFFFFD60A)
val ControlBg = Color(0x66000000)

data class CameraToolbarState(
    val cameraMode: String,
    val flashMode: Int,
    val videoQuality: Quality,
    val aspectRatio: Int,
    val isGridVisible: Boolean,
    val showSettingsPanel: Boolean,
    val rotation: Float
)

data class CameraToolbarActions(
    val onFlashChange: (Int) -> Unit,
    val onAspectChange: (Int) -> Unit,
    val onGridChange: (Boolean) -> Unit,
    val onSettingsChange: (Boolean) -> Unit,
    val onQualityChange: (Quality) -> Unit,
    val onClose: (() -> Unit)? = null
)

data class CameraSettingsState(
    val enableOptimization: Boolean,
    val enableAvif: Boolean,
    val onToggleOptimization: (Boolean) -> Unit,
    val onToggleAvif: (Boolean) -> Unit
)

data class ControlParams(
    val uiState: CameraUiState,
    val cameraState: CameraStateHolder,
    val cameraViewModel: CameraViewModel,
    val iconRotateAngle: Float,
    val isLandscape: Boolean
)

data class ControlActions(
    val triggerShutter: () -> Unit,
    val onClose: () -> Unit,
    val activeRecording: Recording? = null
)

data class PreviewParams(
    val cameraState: CameraStateHolder,
    val uiState: CameraUiState,
    val previewView: PreviewView,
    val deviceAngle: Float,
    val tapOffset: Offset?
)

data class PreviewActions(
    val onZoomChanged: (Float) -> Unit,
    val onFocusRequested: (Offset) -> Unit,
    val onRetry: () -> Unit
)

data class CameraTopBarParams(
    val state: CameraToolbarState,
    val actions: CameraToolbarActions,
    val cameraState: CameraStateHolder,
    val settingsState: CameraSettingsState,
    val isLandscape: Boolean
)
