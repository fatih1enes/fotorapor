package com.fatihenes.photoreport.feature.camera.viewmodel

import android.net.Uri
import androidx.camera.view.PreviewView
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fatihenes.photoreport.feature.camera.capture.PhotoCaptureController
import com.fatihenes.photoreport.feature.camera.capture.VideoRecordingController
import com.fatihenes.photoreport.feature.camera.engine.AspectRatioSelection
import com.fatihenes.photoreport.feature.camera.engine.CameraEngine
import com.fatihenes.photoreport.feature.camera.engine.CameraMode
import com.fatihenes.photoreport.feature.camera.engine.CameraSessionState
import com.fatihenes.photoreport.feature.camera.engine.FlashMode
import com.fatihenes.photoreport.feature.camera.model.CameraUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import com.fatihenes.photoreport.feature.camera.engine.CameraKeyEventDispatcher
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    val cameraEngine: CameraEngine,
    val photoCaptureController: PhotoCaptureController,
    val videoRecordingController: VideoRecordingController,
    val keyEventDispatcher: CameraKeyEventDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val _savedMediaEvents = MutableSharedFlow<Uri>(extraBufferCapacity = 16)
    val savedMediaEvents: SharedFlow<Uri> = _savedMediaEvents.asSharedFlow()

    init {
        observeEngineState()
        observeCapturePipelines()
    }

    private fun observeEngineState() {
        viewModelScope.launch {
            cameraEngine.sessionState.collect { state ->
                when (state) {
                    is CameraSessionState.Ready -> {
                        _uiState.update { current ->
                            current.copy(
                                minZoom = state.capabilities.minZoomRatio,
                                maxZoom = state.capabilities.maxZoomRatio,
                                exposureRange = state.capabilities.exposureRangeLower..state.capabilities.exposureRangeUpper,
                                isTransitioning = false,
                                errorMessage = null
                            )
                        }
                    }
                    is CameraSessionState.Error -> {
                        _uiState.update { it.copy(errorMessage = state.message, isTransitioning = false) }
                    }
                    is CameraSessionState.Initializing -> {
                        _uiState.update { it.copy(isTransitioning = true) }
                    }
                    else -> Unit
                }
            }
        }

        viewModelScope.launch {
            cameraEngine.zoomState.collect { zoom ->
                if (zoom != null) {
                    _uiState.update { it.copy(zoomRatio = zoom.zoomRatio) }
                }
            }
        }

        viewModelScope.launch {
            cameraEngine.exposureState.collect { exposure ->
                if (exposure != null) {
                    _uiState.update { it.copy(exposureIndex = exposure.exposureCompensationIndex) }
                }
            }
        }

        viewModelScope.launch {
            cameraEngine.isFocusLocked.collect { locked ->
                _uiState.update { it.copy(isFocusLocked = locked) }
            }
        }
    }

    private fun observeCapturePipelines() {
        viewModelScope.launch {
            photoCaptureController.activeCapturesCount.collect { count ->
                _uiState.update { it.copy(activeCapturesCount = count) }
            }
        }

        viewModelScope.launch {
            photoCaptureController.capturedPhotos.collect { captured ->
                _uiState.update { current ->
                    current.copy(
                        lastCapturedUri = captured.uri,
                        sessionUris = current.sessionUris + captured.uri
                    )
                }
                _savedMediaEvents.tryEmit(captured.uri)
            }
        }

        viewModelScope.launch {
            videoRecordingController.isRecording.collect { rec ->
                _uiState.update { it.copy(isRecording = rec) }
            }
        }

        viewModelScope.launch {
            videoRecordingController.isPaused.collect { paused ->
                _uiState.update { it.copy(isPaused = paused) }
            }
        }

        viewModelScope.launch {
            videoRecordingController.durationSeconds.collect { sec ->
                _uiState.update { it.copy(recordingDurationSeconds = sec) }
            }
        }

        viewModelScope.launch {
            videoRecordingController.recordedVideos.collect { recorded ->
                _uiState.update { current ->
                    current.copy(
                        lastCapturedUri = recorded.uri,
                        sessionUris = current.sessionUris + recorded.uri
                    )
                }
                _savedMediaEvents.tryEmit(recorded.uri)
            }
        }
    }

    fun bind(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        viewModelScope.launch {
            cameraEngine.bind(lifecycleOwner, previewView)
        }
    }

    fun triggerShutter(
        rotation: Int,
        projectName: String,
        includeGps: Boolean,
        enableAudio: Boolean = true
    ) {
        if (_uiState.value.cameraMode == CameraMode.PHOTO) {
            photoCaptureController.capturePhoto(
                rotation = rotation,
                projectName = projectName,
                includeGps = includeGps,
                onInstantFeedback = {
                    triggerInstantShutterBlink()
                }
            )
        } else {
            if (_uiState.value.isRecording) {
                videoRecordingController.stopRecording()
            } else {
                videoRecordingController.startRecording(
                    rotation = rotation,
                    projectName = projectName,
                    includeGps = includeGps,
                    enableAudio = enableAudio
                )
            }
        }
    }

    private fun triggerInstantShutterBlink() {
        viewModelScope.launch {
            _uiState.update { it.copy(isShutterBlinking = true) }
            delay(50)
            _uiState.update { it.copy(isShutterBlinking = false) }
        }
    }

    fun setCameraMode(mode: CameraMode) {
        if (_uiState.value.cameraMode == mode) return
        _uiState.update { it.copy(cameraMode = mode) }
        viewModelScope.launch {
            cameraEngine.setCameraMode(mode)
        }
    }

    fun cycleFlashMode() {
        val nextFlash = when (_uiState.value.flashMode) {
            FlashMode.OFF -> FlashMode.AUTO
            FlashMode.AUTO -> FlashMode.ON
            FlashMode.ON -> FlashMode.TORCH
            FlashMode.TORCH -> FlashMode.OFF
        }
        _uiState.update { it.copy(flashMode = nextFlash) }
        viewModelScope.launch {
            cameraEngine.setFlashMode(nextFlash)
        }
    }

    fun toggleAspectRatio() {
        val nextAspect = _uiState.value.aspectRatio.toggle()
        _uiState.update { it.copy(aspectRatio = nextAspect, isTransitioning = true) }
        viewModelScope.launch {
            cameraEngine.setAspectRatio(nextAspect)
        }
    }

    fun switchLens() {
        _uiState.update { it.copy(isTransitioning = true) }
        viewModelScope.launch {
            cameraEngine.switchLens()
        }
    }

    fun setZoomRatio(ratio: Float) {
        _uiState.update { it.copy(zoomRatio = ratio) }
        viewModelScope.launch {
            cameraEngine.setZoomRatio(ratio)
        }
    }

    fun setExposureIndex(index: Int) {
        _uiState.update { it.copy(exposureIndex = index) }
        viewModelScope.launch {
            cameraEngine.setExposureIndex(index)
        }
    }

    fun onFocusRequested(offset: Offset, previewView: PreviewView) {
        _uiState.update { it.copy(tapOffset = offset) }
        viewModelScope.launch {
            cameraEngine.focusAndMeter(offset, previewView)
        }
    }

    fun onLockFocusRequested(offset: Offset, previewView: PreviewView) {
        _uiState.update { it.copy(tapOffset = offset) }
        viewModelScope.launch {
            cameraEngine.lockFocusAndMetering(offset, previewView)
        }
    }

    fun toggleGrid() {
        _uiState.update { it.copy(isGridVisible = !it.isGridVisible) }
    }

    fun toggleLevel() {
        _uiState.update { it.copy(isLevelVisible = !it.isLevelVisible) }
    }

    fun setQuickSettingsOpen(open: Boolean) {
        _uiState.update { it.copy(isQuickSettingsOpen = open) }
    }

    fun setSessionReviewOpen(open: Boolean) {
        _uiState.update { it.copy(isSessionReviewOpen = open) }
    }

    fun togglePauseRecording() {
        videoRecordingController.togglePause()
    }

    fun updateTargetRotation(rotation: Int) {
        viewModelScope.launch {
            cameraEngine.updateTargetRotation(rotation)
        }
    }

    override fun onCleared() {
        super.onCleared()
        cameraEngine.release()
    }
}
