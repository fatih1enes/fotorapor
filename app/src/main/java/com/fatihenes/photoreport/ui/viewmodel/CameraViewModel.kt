package com.fatihenes.photoreport.ui.viewmodel

import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.video.Quality
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * ViewModel for managing camera UI state and user interactions.
 */
@HiltViewModel
class CameraViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    fun setCameraMode(mode: String) {
        _uiState.update { it.copy(cameraMode = mode) }
    }

    fun setFlashMode(mode: Int) {
        _uiState.update { it.copy(flashMode = mode) }
    }

    fun setAspectRatio(ratio: Int) {
        _uiState.update { it.copy(aspectRatio = ratio) }
    }

    fun setVideoQuality(quality: Quality) {
        _uiState.update { it.copy(videoQuality = quality) }
    }

    fun setZoomRatio(zoom: Float) {
        _uiState.update { it.copy(zoomRatio = zoom) }
    }

    fun toggleLensFacing() {
        _uiState.update {
            val nextLens = if (it.lensFacing == CameraSelector.LENS_FACING_BACK) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }
            it.copy(lensFacing = nextLens)
        }
    }

    fun setGridVisible(visible: Boolean) {
        _uiState.update { it.copy(isGridVisible = visible) }
    }

    fun setExposureValue(value: Float) {
        _uiState.update { it.copy(exposureValue = value) }
    }

    fun setShowExposure(show: Boolean) {
        _uiState.update { it.copy(showExposure = show) }
    }

    fun setShowSettingsPanel(show: Boolean) {
        _uiState.update { it.copy(showSettingsPanel = show) }
    }

    fun onPhotoCaptured(uri: Uri) {
        _uiState.update {
            it.copy(
                lastCapturedUri = uri,
                sessionPhotoCount = it.sessionPhotoCount + 1
            )
        }
    }

    fun setIsRecording(recording: Boolean) {
        _uiState.update { it.copy(isRecording = recording, isPaused = false, recordingDuration = 0) }
    }

    fun setIsPaused(paused: Boolean) {
        _uiState.update { it.copy(isPaused = paused) }
    }

    fun incrementRecordingDuration() {
        _uiState.update { it.copy(recordingDuration = it.recordingDuration + 1) }
    }
}
