package com.fatihenes.photoreport.feature.camera.capture

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.video.VideoRecordEvent
import com.fatihenes.photoreport.core.common.di.Dispatcher
import com.fatihenes.photoreport.core.common.di.FotoRaporDispatchers
import com.fatihenes.photoreport.core.model.WatermarkData
import com.fatihenes.photoreport.feature.camera.engine.CameraEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "VideoRecordingCtrl"

data class CapturedVideoResult(
    val uri: Uri,
    val watermarkData: WatermarkData,
    val durationSeconds: Int
)

@Singleton
class VideoRecordingController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cameraEngine: CameraEngine,
    private val metadataProvider: CaptureMetadataProvider,
    @Dispatcher(FotoRaporDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) {
    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _durationSeconds = MutableStateFlow(0)
    val durationSeconds: StateFlow<Int> = _durationSeconds.asStateFlow()

    private val _recordedVideos = MutableSharedFlow<CapturedVideoResult>(extraBufferCapacity = 8)
    val recordedVideos: SharedFlow<CapturedVideoResult> = _recordedVideos.asSharedFlow()

    private val _recordingErrors = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val recordingErrors: SharedFlow<String> = _recordingErrors.asSharedFlow()

    private var activeMetadataSnapshot: WatermarkData? = null
    private var activeOutputFile: File? = null

    fun startRecording(
        rotation: Int,
        projectName: String,
        includeGps: Boolean,
        enableAudio: Boolean
    ) {
        if (_isRecording.value) return

        controllerScope.launch {
            val metadataSnapshot = metadataProvider.createSnapshot(
                projectName = projectName,
                includeGps = includeGps
            )
            activeMetadataSnapshot = metadataSnapshot

            val file = withContext(ioDispatcher) {
                val dir = File(context.filesDir, "captures").apply { if (!exists()) mkdirs() }
                File(dir, "VID_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.mp4")
            }
            activeOutputFile = file

            _durationSeconds.value = 0
            _isPaused.value = false

            val result = cameraEngine.startRecording(
                outputFile = file,
                rotation = rotation,
                enableAudio = enableAudio
            ) { event ->
                handleRecordEvent(event)
            }

            result.onSuccess {
                _isRecording.value = true
            }.onFailure { err ->
                Log.e(TAG, "Failed to start recording", err)
                _recordingErrors.tryEmit(err.localizedMessage ?: "Video başlatılamadı")
            }
        }
    }

    private fun handleRecordEvent(event: VideoRecordEvent) {
        when (event) {
            is VideoRecordEvent.Start -> {
                _isRecording.value = true
                _isPaused.value = false
            }
            is VideoRecordEvent.Status -> {
                val durationNanos = event.recordingStats.recordedDurationNanos
                _durationSeconds.value = (durationNanos / 1_000_000_000L).toInt()
            }
            is VideoRecordEvent.Pause -> {
                _isPaused.value = true
            }
            is VideoRecordEvent.Resume -> {
                _isPaused.value = false
            }
            is VideoRecordEvent.Finalize -> {
                _isRecording.value = false
                _isPaused.value = false
                val file = activeOutputFile
                val metadata = activeMetadataSnapshot
                if (!event.hasError() && file != null && metadata != null) {
                    val uri = Uri.fromFile(file)
                    _recordedVideos.tryEmit(
                        CapturedVideoResult(
                            uri = uri,
                            watermarkData = metadata,
                            durationSeconds = _durationSeconds.value
                        )
                    )
                } else if (event.hasError()) {
                    Log.e(TAG, "Video recording finalize error: ${event.error}")
                    _recordingErrors.tryEmit("Kayıt hatası: ${event.error}")
                }
                activeOutputFile = null
                activeMetadataSnapshot = null
            }
        }
    }

    fun stopRecording() {
        cameraEngine.stopRecording()
    }

    fun togglePause() {
        if (_isPaused.value) {
            cameraEngine.resumeRecording()
            _isPaused.value = false
        } else {
            cameraEngine.pauseRecording()
            _isPaused.value = true
        }
    }
}
