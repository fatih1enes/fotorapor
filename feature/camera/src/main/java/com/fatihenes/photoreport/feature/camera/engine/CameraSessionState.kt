package com.fatihenes.photoreport.feature.camera.engine

import androidx.camera.core.AspectRatio
import androidx.camera.core.ImageCapture
import androidx.camera.video.Quality
import androidx.compose.runtime.Immutable

/**
 * Camera operational modes.
 */
enum class CameraMode {
    PHOTO,
    VIDEO
}

/**
 * Supported flash modes including Torch.
 */
enum class FlashMode {
    OFF,
    AUTO,
    ON,
    TORCH;

    fun toCameraXImageCaptureFlashMode(): Int = when (this) {
        AUTO -> ImageCapture.FLASH_MODE_AUTO
        ON -> ImageCapture.FLASH_MODE_ON
        OFF, TORCH -> ImageCapture.FLASH_MODE_OFF
    }
}

/**
 * Supported aspect ratio configurations.
 */
enum class AspectRatioSelection(val ratioValue: Int, val floatRatio: Float) {
    RATIO_4_3(AspectRatio.RATIO_4_3, 3f / 4f),
    RATIO_16_9(AspectRatio.RATIO_16_9, 9f / 16f);

    val displayLabel: String get() = when (this) {
        RATIO_4_3 -> "4:3"
        RATIO_16_9 -> "16:9"
    }

    fun toggle(): AspectRatioSelection = when (this) {
        RATIO_4_3 -> RATIO_16_9
        RATIO_16_9 -> RATIO_4_3
    }
}

/**
 * Standard camera errors mapped to user-understandable categories.
 */
enum class CameraErrorType {
    CAMERA_IN_USE,
    CAMERA_DISABLED,
    CAMERA_DISCONNECTED,
    INITIALIZATION_FAILED,
    SECURITY_VIOLATION,
    UNKNOWN
}

/**
 * Sealed hierarchy representing the state of the Camera engine session.
 */
@Immutable
sealed interface CameraSessionState {
    object Uninitialized : CameraSessionState
    object Initializing : CameraSessionState

    data class Ready(
        val isStreaming: Boolean = false,
        val capabilities: EnhancedCapabilities = EnhancedCapabilities()
    ) : CameraSessionState

    data class Capturing(
        val activeCapturesCount: Int = 1
    ) : CameraSessionState

    data class Recording(
        val durationNanos: Long = 0L,
        val isPaused: Boolean = false,
        val recordedBytes: Long = 0L
    ) : CameraSessionState

    data class Error(
        val errorType: CameraErrorType,
        val message: String? = null,
        val cause: Throwable? = null
    ) : CameraSessionState
}

/**
 * Snapshot of the current hardware capabilities for the bound lens.
 */
@Immutable
data class EnhancedCapabilities(
    val hardwareLevel: Int = 0,
    val isLegacy: Boolean = false,
    val isFullOrBetter: Boolean = false,
    val minZoomRatio: Float = 1f,
    val maxZoomRatio: Float = 8f,
    val exposureRangeLower: Int = 0,
    val exposureRangeUpper: Int = 0,
    val exposureStep: Float = 0f,
    val supportsPreviewStabilization: Boolean = false,
    val supportsVideoStabilization: Boolean = false,
    val supportsOis: Boolean = false,
    val hasFlashUnit: Boolean = false,
    val supportsTorch: Boolean = false,
    val supportsZeroShutterLag: Boolean = false,
    val supportsLowLightBoost: Boolean = false,
    val supportedVideoQualities: List<Quality> = emptyList(),
    val supportsHdrExtension: Boolean = false,
    val supportsNightExtension: Boolean = false
) {
    val isZoomSupported: Boolean get() = maxZoomRatio > minZoomRatio
    val isExposureSupported: Boolean get() = exposureRangeLower != exposureRangeUpper
}
