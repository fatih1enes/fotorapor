package com.fatihenes.photoreport.feature.camera.engine

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.util.Log
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraInfo
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SessionConfigFactory"

/**
 * JPEG quality. 97 is visually lossless; going to 100 only inflates file size
 * (~2x) and encode time without any visible gain.
 */
private const val DEFAULT_JPEG_QUALITY = 97

/** Single-thread executor dedicated to encoding; prevents blocking the main capture pipeline. */
private val videoEncodeExecutor = Executors.newSingleThreadExecutor { r ->
    Thread(r, "camera-encode").also { it.priority = Thread.MAX_PRIORITY }
}

@Singleton
class SessionConfigFactory @Inject constructor() {

    fun createPreview(
        aspectRatio: AspectRatioSelection,
        caps: EnhancedCapabilities
    ): Preview {
        val resSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(
                AspectRatioStrategy(aspectRatio.ratioValue, AspectRatioStrategy.FALLBACK_RULE_AUTO)
            )
            .setResolutionStrategy(
                ResolutionStrategy(
                    android.util.Size(1920, 1080),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                )
            )
            .build()

        return Preview.Builder()
            .setResolutionSelector(resSelector)
            .build()
    }

    /**
     * @param cameraInfo info of the camera that will be bound (without extension). Used to read
     * which Camera2 processing modes the hardware supports.
     * @param extensionActive true when a CameraX vendor extension (HDR/Auto) is used. The
     * extension runs its own tuned pipeline, so manual Camera2 overrides are skipped.
     */
    @Suppress("UnusedParameter")
    fun createImageCapture(
        aspectRatio: AspectRatioSelection,
        flashMode: FlashMode,
        targetRotation: Int,
        enableOptimization: Boolean,
        caps: EnhancedCapabilities,
        enableAvif: Boolean = false,
        cameraInfo: CameraInfo? = null,
        extensionActive: Boolean = false
    ): ImageCapture {
        val captureRes = ResolutionSelector.Builder()
            .setAspectRatioStrategy(
                AspectRatioStrategy(aspectRatio.ratioValue, AspectRatioStrategy.FALLBACK_RULE_AUTO)
            )
            .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
            .build()

        val captureMode = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY

        val builder = ImageCapture.Builder()
            .setResolutionSelector(captureRes)
            .setFlashMode(flashMode.toCameraXImageCaptureFlashMode())
            .setTargetRotation(targetRotation)
            .setJpegQuality(DEFAULT_JPEG_QUALITY)
            .setCaptureMode(captureMode)

        if (!extensionActive && cameraInfo != null && !caps.isLegacy) {
            applyHighQualityProcessing(builder, cameraInfo)
        }

        return builder.build()
    }

    /**
     * Asks the camera ISP for its slowest/best processing path: HIGH_QUALITY noise
     * reduction, edge enhancement (sharpening), hot-pixel correction, lens shading,
     * chromatic aberration correction and tonemapping, plus optical stabilization.
     * Every mode is checked against the hardware's advertised list first, so unsupported
     * values are never sent (which could make the capture session fail).
     */
    @androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
    private fun applyHighQualityProcessing(builder: ImageCapture.Builder, cameraInfo: CameraInfo) {
        try {
            val info = Camera2CameraInfo.from(cameraInfo)
            val extender = Camera2Interop.Extender(builder)

            fun supports(key: CameraCharacteristics.Key<IntArray>, value: Int): Boolean =
                info.getCameraCharacteristic(key)?.contains(value) == true

            if (supports(
                    CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES,
                    CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY
                )
            ) {
                extender.setCaptureRequestOption(
                    CaptureRequest.NOISE_REDUCTION_MODE,
                    CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY
                )
            }
            if (supports(
                    CameraCharacteristics.EDGE_AVAILABLE_EDGE_MODES,
                    CaptureRequest.EDGE_MODE_HIGH_QUALITY
                )
            ) {
                extender.setCaptureRequestOption(
                    CaptureRequest.EDGE_MODE,
                    CaptureRequest.EDGE_MODE_HIGH_QUALITY
                )
            }
            if (supports(
                    CameraCharacteristics.HOT_PIXEL_AVAILABLE_HOT_PIXEL_MODES,
                    CaptureRequest.HOT_PIXEL_MODE_HIGH_QUALITY
                )
            ) {
                extender.setCaptureRequestOption(
                    CaptureRequest.HOT_PIXEL_MODE,
                    CaptureRequest.HOT_PIXEL_MODE_HIGH_QUALITY
                )
            }
            if (supports(
                    CameraCharacteristics.SHADING_AVAILABLE_MODES,
                    CaptureRequest.SHADING_MODE_HIGH_QUALITY
                )
            ) {
                extender.setCaptureRequestOption(
                    CaptureRequest.SHADING_MODE,
                    CaptureRequest.SHADING_MODE_HIGH_QUALITY
                )
            }
            if (supports(
                    CameraCharacteristics.COLOR_CORRECTION_AVAILABLE_ABERRATION_MODES,
                    CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY
                )
            ) {
                extender.setCaptureRequestOption(
                    CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE,
                    CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY
                )
            }
            if (supports(
                    CameraCharacteristics.TONEMAP_AVAILABLE_TONE_MAP_MODES,
                    CaptureRequest.TONEMAP_MODE_HIGH_QUALITY
                )
            ) {
                extender.setCaptureRequestOption(
                    CaptureRequest.TONEMAP_MODE,
                    CaptureRequest.TONEMAP_MODE_HIGH_QUALITY
                )
            }
            if (supports(
                    CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION,
                    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON
                )
            ) {
                extender.setCaptureRequestOption(
                    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "High-quality processing overrides unavailable, using defaults", e)
        }
    }

    fun createVideoCapture(
        videoQuality: Quality,
        targetRotation: Int,
        caps: EnhancedCapabilities
    ): VideoCapture<Recorder> {
        // Dedicated high-priority encode executor prevents I/O or capture work
        // from starving the encoder — keeps frame drops near zero at FHD/4K.
        val recorder = Recorder.Builder()
            .setExecutor(videoEncodeExecutor)
            .setQualitySelector(
                QualitySelector.from(
                    videoQuality,
                    FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
                )
            )
            .apply {
                // Enable HDR video recording on capable devices (iPhone-like)
                if (caps.supportsHdrVideo && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // HDR video configuration would go here
                    // Requires device-specific support
                }
            }
            .build()

        return VideoCapture.withOutput(recorder).apply {
            this.targetRotation = targetRotation
        }
    }
}
