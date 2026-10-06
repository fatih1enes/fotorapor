package com.fatihenes.photoreport.feature.camera.capture

import android.content.Context
import android.media.MediaCodecList
import android.net.Uri
import android.os.Build
import android.util.Log
import com.fatihenes.photoreport.core.common.di.Dispatcher
import com.fatihenes.photoreport.core.common.di.FotoRaporDispatchers
import com.fatihenes.photoreport.core.model.WatermarkData
import com.fatihenes.photoreport.feature.camera.engine.CameraEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
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

private const val TAG = "PhotoCaptureController"

data class CapturedPhotoResult(
    val uri: Uri,
    val watermarkData: WatermarkData,
    val capturedTimestamp: Long,
    val format: CaptureFormat = CaptureFormat.JPEG
)

enum class CaptureFormat {
    JPEG,
    HEIC,
    AVIF
}

@Singleton
class PhotoCaptureController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cameraEngine: CameraEngine,
    private val metadataProvider: CaptureMetadataProvider,
    @Dispatcher(FotoRaporDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) {
    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _activeCapturesCount = MutableStateFlow(0)
    val activeCapturesCount: StateFlow<Int> = _activeCapturesCount.asStateFlow()

    private val _capturedPhotos = MutableSharedFlow<CapturedPhotoResult>(extraBufferCapacity = 16)
    val capturedPhotos: SharedFlow<CapturedPhotoResult> = _capturedPhotos.asSharedFlow()

    private val _captureErrors = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val captureErrors: SharedFlow<String> = _captureErrors.asSharedFlow()

    fun capturePhoto(
        rotation: Int,
        projectName: String,
        includeGps: Boolean,
        onInstantFeedback: () -> Unit,
        enableHeic: Boolean = true,
        enableAvif: Boolean = false
    ) {
        // 1. Instant feedback: Shutter animation, haptic, sound immediately fires (<50ms)
        onInstantFeedback()
        _activeCapturesCount.value = _activeCapturesCount.value + 1

        controllerScope.launch {
            val captureTime = System.currentTimeMillis()

            // 2. Prepare app-private destination file immediately with proper extension
            val outputFile = withContext(ioDispatcher) {
                val dir = File(context.filesDir, "captures").apply { if (!exists()) mkdirs() }
                val format = when {
                    enableAvif && hasAvifSupport() -> CaptureFormat.AVIF
                    enableHeic && hasHeicSupport() -> CaptureFormat.HEIC
                    else -> CaptureFormat.JPEG
                }
                val extension = format.fileExtension()
                File(dir, "IMG_${captureTime}_${UUID.randomUUID().toString().take(6)}.$extension")
            }

            val selectedFormat = when {
                enableAvif && hasAvifSupport() -> CaptureFormat.AVIF
                enableHeic && hasHeicSupport() -> CaptureFormat.HEIC
                else -> CaptureFormat.JPEG
            }

            // 3. Trigger camera sensor capture IMMEDIATELY (zero lag)
            val captureDeferred = async {
                cameraEngine.takePhoto(outputFile, rotation)
            }

            // 4. Capture metadata snapshot concurrently while the sensor/JPEG pipeline runs
            val metadataDeferred = async {
                metadataProvider.createSnapshot(
                    projectName = projectName,
                    includeGps = includeGps
                )
            }

            val result = captureDeferred.await()
            val metadataSnapshot = metadataDeferred.await()

            _activeCapturesCount.value = (_activeCapturesCount.value - 1).coerceAtLeast(0)

            result.onSuccess { uri ->
                _capturedPhotos.tryEmit(
                    CapturedPhotoResult(
                        uri = uri,
                        watermarkData = metadataSnapshot,
                        capturedTimestamp = captureTime,
                        format = selectedFormat
                    )
                )
            }.onFailure { error ->
                Log.e(TAG, "Photo capture failed", error)
                _captureErrors.tryEmit(error.localizedMessage ?: "Çekim hatası")
            }
        }
    }

    private fun hasHeicSupport(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && hasHeicEncoder()
    }

    private fun hasAvifSupport(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && hasAvifEncoder()
    }

    private fun hasHeicEncoder(): Boolean {
        return try {
            val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
            codecList.codecInfos.any { info ->
                info.isEncoder && "image/heic".equals(info.getSupportedTypes().firstOrNull(), ignoreCase = true)
            }
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "HEIC encoder check failed: IllegalArgumentException", e)
            false
        } catch (e: SecurityException) {
            Log.w(TAG, "HEIC encoder check failed: SecurityException", e)
            false
        }
    }

    private fun hasAvifEncoder(): Boolean {
        return try {
            val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
            codecList.codecInfos.any { info ->
                info.isEncoder && "image/avif".equals(info.getSupportedTypes().firstOrNull(), ignoreCase = true)
            }
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "AVIF encoder check failed: IllegalArgumentException", e)
            false
        } catch (e: SecurityException) {
            Log.w(TAG, "AVIF encoder check failed: SecurityException", e)
            false
        }
    }
}

private fun CaptureFormat.fileExtension(): String = when (this) {
    CaptureFormat.JPEG -> "jpg"
    CaptureFormat.HEIC -> "heic"
    CaptureFormat.AVIF -> "avif"
}
