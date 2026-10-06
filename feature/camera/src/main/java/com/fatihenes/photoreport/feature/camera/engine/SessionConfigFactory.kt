package com.fatihenes.photoreport.feature.camera.engine

import android.util.Size
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
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SessionConfigFactory"
private const val DEFAULT_JPEG_QUALITY = 92

/**
 * ~12MP target (iPhone default). Plenty for reports, and keeps capture latency,
 * memory and file size low even on 50-200MP sensors.
 */
private val PHOTO_TARGET_4_3 = Size(4032, 3024)
private val PHOTO_TARGET_16_9 = Size(4032, 2268)

@Singleton
class SessionConfigFactory @Inject constructor() {

    fun createPreview(
        aspectRatio: AspectRatioSelection,
        enableStabilization: Boolean,
        caps: EnhancedCapabilities
    ): Preview {
        val resSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(
                AspectRatioStrategy(aspectRatio.ratioValue, AspectRatioStrategy.FALLBACK_RULE_AUTO)
            )
            .build()

        val builder = Preview.Builder().setResolutionSelector(resSelector)
        if (enableStabilization && caps.supportsPreviewStabilization) {
            builder.setPreviewStabilizationEnabled(true)
        }
        return builder.build()
    }

    fun createImageCapture(
        aspectRatio: AspectRatioSelection,
        flashMode: FlashMode,
        targetRotation: Int,
        enableOptimization: Boolean,
        caps: EnhancedCapabilities
    ): ImageCapture {
        val isPortrait = targetRotation == android.view.Surface.ROTATION_0 || targetRotation == android.view.Surface.ROTATION_180
        val target = when (aspectRatio) {
            AspectRatioSelection.RATIO_16_9 -> if (isPortrait) Size(2268, 4032) else Size(4032, 2268)
            AspectRatioSelection.RATIO_4_3 -> if (isPortrait) Size(3024, 4032) else Size(4032, 3024)
        }
        val captureRes = ResolutionSelector.Builder()
            .setAspectRatioStrategy(
                AspectRatioStrategy(aspectRatio.ratioValue, AspectRatioStrategy.FALLBACK_RULE_AUTO)
            )
            .setResolutionStrategy(
                ResolutionStrategy(target, ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER)
            )
            .build()

        val captureMode = if (enableOptimization && caps.supportsZeroShutterLag) {
            ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG
        } else {
            ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
        }

        return ImageCapture.Builder()
            .setResolutionSelector(captureRes)
            .setFlashMode(flashMode.toCameraXImageCaptureFlashMode())
            .setTargetRotation(targetRotation)
            .setJpegQuality(DEFAULT_JPEG_QUALITY)
            .setCaptureMode(captureMode)
            .build()
    }

    fun createVideoCapture(
        videoQuality: Quality,
        targetRotation: Int
    ): VideoCapture<Recorder> {
        val recorder = Recorder.Builder()
            .setQualitySelector(
                QualitySelector.from(
                    videoQuality,
                    FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
                )
            )
            .build()

        return VideoCapture.withOutput(recorder).apply {
            this.targetRotation = targetRotation
        }
    }
}
