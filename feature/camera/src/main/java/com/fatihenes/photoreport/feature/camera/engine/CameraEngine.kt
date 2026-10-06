package com.fatihenes.photoreport.feature.camera.engine

import android.net.Uri
import androidx.camera.core.ExposureState
import androidx.camera.core.ZoomState
import androidx.camera.video.Quality
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.StateFlow
import java.io.File

interface CameraEngine {
    val sessionState: StateFlow<CameraSessionState>
    val zoomState: StateFlow<ZoomState?>
    val exposureState: StateFlow<ExposureState?>
    val torchState: StateFlow<Int>
    val isStreaming: StateFlow<Boolean>
    val activeLensFacing: StateFlow<Int>
    val currentMode: StateFlow<CameraMode>
    val currentFlashMode: StateFlow<FlashMode>
    val currentAspectRatio: StateFlow<AspectRatioSelection>
    val currentVideoQuality: StateFlow<Quality>
    val isFocusLocked: StateFlow<Boolean>

    suspend fun bind(lifecycleOwner: LifecycleOwner, previewView: PreviewView)
    suspend fun setCameraMode(mode: CameraMode)
    suspend fun setFlashMode(flashMode: FlashMode)
    suspend fun setTorchEnabled(enabled: Boolean)
    suspend fun switchLens()
    suspend fun setAspectRatio(aspectRatio: AspectRatioSelection)
    suspend fun setVideoQuality(quality: Quality)
    suspend fun setZoomRatio(ratio: Float)
    suspend fun setLinearZoom(linear: Float)
    suspend fun setExposureIndex(index: Int)
    suspend fun focusAndMeter(offset: Offset, previewView: PreviewView)
    suspend fun lockFocusAndMetering(offset: Offset, previewView: PreviewView)
    suspend fun unlockFocusAndMetering()
    suspend fun updateTargetRotation(rotation: Int)
    suspend fun takePhoto(outputFile: File, rotation: Int): Result<Uri>
    suspend fun startRecording(
        outputFile: File,
        rotation: Int,
        enableAudio: Boolean,
        onEvent: (VideoRecordEvent) -> Unit
    ): Result<Recording>
    fun stopRecording()
    fun pauseRecording()
    fun resumeRecording()
    fun unbindAll()
    fun release()
}
