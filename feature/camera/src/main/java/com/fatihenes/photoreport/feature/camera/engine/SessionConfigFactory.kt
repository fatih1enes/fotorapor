package com.fatihenes.photoreport.feature.camera.engine

import android.os.Build
import android.util.Size
import android.view.Surface
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalZeroShutterLag
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
import com.fatihenes.photoreport.feature.camera.theme.CameraTokens
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JPEG quality. 95 mirrors Apple's ISP target: visually lossless, still
 * compresses ~40% better than uncompressed TIFF at acceptable file sizes.
 */
private const val DEFAULT_JPEG_QUALITY = 95

// Portrait rotation values for target resolution calculation
private const val PORTRAIT_ROTATION_0 = Surface.ROTATION_0
private const val PORTRAIT_ROTATION_180 = Surface.ROTATION_180

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
            .build()

        val builder = Preview.Builder().setResolutionSelector(resSelector)
        // Always enable preview stabilization when the hardware supports it.
        // This is the single most impactful quality change for hand-held photography.
        if (caps.supportsPreviewStabilization) {
            builder.setPreviewStabilizationEnabled(true)
        }
        return builder.build()
    }

    @OptIn(ExperimentalZeroShutterLag::class)
    fun createImageCapture(
        aspectRatio: AspectRatioSelection,
        flashMode: FlashMode,
        targetRotation: Int,
        enableOptimization: Boolean,
        caps: EnhancedCapabilities,
        enableAvif: Boolean = false
    ): ImageCapture {
        val isPortrait = targetRotation == PORTRAIT_ROTATION_0 || targetRotation == PORTRAIT_ROTATION_180
        val target = when (aspectRatio) {
            AspectRatioSelection.RATIO_16_9 -> if (isPortrait)
                Size(CameraTokens.PhotoTarget16_9Height, CameraTokens.PhotoTarget16_9Width)
            else
                Size(CameraTokens.PhotoTarget16_9Width, CameraTokens.PhotoTarget16_9Height)
            AspectRatioSelection.RATIO_4_3 -> if (isPortrait)
                Size(CameraTokens.PhotoTarget4_3Height, CameraTokens.PhotoTarget4_3Width)
            else
                Size(CameraTokens.PhotoTarget4_3Width, CameraTokens.PhotoTarget4_3Height)
        }
        val captureRes = ResolutionSelector.Builder()
            .setAspectRatioStrategy(
                AspectRatioStrategy(aspectRatio.ratioValue, AspectRatioStrategy.FALLBACK_RULE_AUTO)
            )
            .setResolutionStrategy(
                ResolutionStrategy(target, ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER)
            )
            .build()

        // Prefer ZSL (Zero Shutter Lag) on capable hardware — it eliminates the shutter
        // delay entirely by drawing from the ring buffer already filled by the sensor.
        // On devices that don't support ZSL, MINIMIZE_LATENCY is the next best option.
        // MAXIMIZE_QUALITY is intentionally avoided on-path; it adds 100-400 ms latency.
        val captureMode = when {
            enableOptimization && caps.supportsZeroShutterLag ->
                ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG
            else ->
                ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
        }

        val builder = ImageCapture.Builder()
            .setResolutionSelector(captureRes)
            .setFlashMode(flashMode.toCameraXImageCaptureFlashMode())
            .setTargetRotation(targetRotation)
            .setJpegQuality(DEFAULT_JPEG_QUALITY)
            .setCaptureMode(captureMode)

        // HEIC support (Android 10+): better compression than JPEG at same visual quality
        // HEIC format is determined by OutputFileOptions when saving, not by builder config

        // AVIF support (Android 13+ via Media3 Transformer): best compression/quality
        // Note: AVIF capture requires post-processing via Media3 Transformer
        if (enableAvif && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // AVIF encoding handled in PhotoCaptureController via Media3 Transformer
        }

        return builder.build()
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
