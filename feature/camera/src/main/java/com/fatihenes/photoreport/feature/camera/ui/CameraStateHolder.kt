@file:Suppress("WildcardImport", "MaxLineLength")
package com.fatihenes.photoreport.feature.camera.ui

import android.content.Context
import android.hardware.camera2.CaptureRequest
import android.util.Log
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.lifecycle.LifecycleOwner
import com.fatihenes.photoreport.feature.camera.model.CameraCapabilities
import com.fatihenes.photoreport.feature.camera.model.queryCameraCapabilities
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private const val TAG = "CameraStateHolder"

data class CameraConfig(
    val lensFacing: Int,
    val aspectRatio: Int,
    val cameraMode: String,
    val videoQuality: Quality,
    val currentRotation: Int,
    val flashMode: Int,
    val enableOptimization: Boolean
)

@Stable
class CameraStateHolder(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    val executor: ExecutorService,
    private val ioDispatcher: CoroutineDispatcher
) {
    private var cameraProvider: ProcessCameraProvider? = null

    var camera by mutableStateOf<Camera?>(null)
        private set

    var imageCapture by mutableStateOf<ImageCapture?>(null)
        private set

    var videoCapture by mutableStateOf<VideoCapture<Recorder>?>(null)
        private set

    var isBound by mutableStateOf(false)
        private set

    var isCapturing by mutableStateOf(false)

    var supportedQualities by mutableStateOf<List<Quality>>(emptyList())
        private set

    var minZoom by mutableFloatStateOf(1f)
        private set

    var maxZoom by mutableFloatStateOf(8f)
        private set

    var exposureRange by mutableStateOf(-1f..1f)
        private set

    var exposureIndex by mutableIntStateOf(0)
        private set

    var initializationError by mutableStateOf<String?>(null)
        private set

    @androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
    suspend fun bindCamera(previewView: PreviewView, config: CameraConfig) {
        isBound = false
        val caps = queryCameraCapabilities(context, config.lensFacing)

        previewView.implementationMode = if (caps.isLegacy)
            PreviewView.ImplementationMode.COMPATIBLE else PreviewView.ImplementationMode.PERFORMANCE

        val provider = awaitCameraProvider()
        val extensions = awaitExtensionsManager(provider)
        currentCoroutineContext().ensureActive()

        val safeFlash = if (config.lensFacing == CameraSelector.LENS_FACING_FRONT)
            ImageCapture.FLASH_MODE_OFF else config.flashMode
        val finalConfig = config.copy(flashMode = safeFlash)

        val success = tryBindLevel1(provider, extensions, previewView, finalConfig, caps) ||
                      tryBindLevel2(provider, previewView, finalConfig) ||
                      tryBindLevel3(provider, previewView, finalConfig)

        if (success) {
            readCameraMetadata()
            initializationError = null
            isBound = true
        }
    }

    @androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
    private fun tryBindLevel1(
        provider: ProcessCameraProvider,
        extensions: ExtensionsManager,
        previewView: PreviewView,
        config: CameraConfig,
        caps: CameraCapabilities
    ): Boolean {
        return try {
            provider.unbindAll()
            val previewRes = ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy(config.aspectRatio, AspectRatioStrategy.FALLBACK_RULE_AUTO))
                .build()
            val captureRes = ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy(config.aspectRatio, AspectRatioStrategy.FALLBACK_RULE_AUTO))
                .apply { if (!caps.isLegacy) setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY) }
                .build()

            val previewBuilder = Preview.Builder().setResolutionSelector(previewRes)
            if (config.enableOptimization && caps.supportsPreviewStabilization) previewBuilder.setPreviewStabilizationEnabled(true)

            val captureBuilder = ImageCapture.Builder()
                .setResolutionSelector(captureRes).setFlashMode(config.flashMode).setTargetRotation(config.currentRotation)
                .setJpegQuality(100).setCaptureMode(if (caps.isFullOrBetter) ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY else ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)

            var selector = CameraSelector.Builder().requireLensFacing(config.lensFacing).build()
            selector = resolveSelector(provider, selector, config.lensFacing)

            if (config.enableOptimization && extensions.isExtensionAvailable(selector, ExtensionMode.HDR)) {
                selector = extensions.getExtensionEnabledCameraSelector(selector, ExtensionMode.HDR)
            } else if (config.enableOptimization) applyCamera2Optimizations(captureBuilder, caps)

            val preview = previewBuilder.build().also { it.surfaceProvider = previewView.surfaceProvider }

            if (config.cameraMode == "PHOTO") {
                val capture = captureBuilder.build()
                imageCapture = capture
                videoCapture = null
                camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
            } else {
                val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(config.videoQuality, FallbackStrategy.lowerQualityOrHigherThan(Quality.SD))).build()
                val vc = VideoCapture.withOutput(recorder)
                vc.targetRotation = config.currentRotation
                videoCapture = vc
                imageCapture = null
                camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, vc)
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "Level 1 bind failed", e)
            false
        }
    }

    private fun tryBindLevel2(provider: ProcessCameraProvider, previewView: PreviewView, config: CameraConfig): Boolean {
        return try {
            provider.unbindAll()
            val resSelector = ResolutionSelector.Builder().setAspectRatioStrategy(AspectRatioStrategy(config.aspectRatio, AspectRatioStrategy.FALLBACK_RULE_AUTO)).build()
            val preview = Preview.Builder().setResolutionSelector(resSelector).build().also { it.surfaceProvider = previewView.surfaceProvider }
            val selector = resolveSelector(provider, CameraSelector.Builder().requireLensFacing(config.lensFacing).build(), config.lensFacing)

            if (config.cameraMode == "PHOTO") {
                val capture = ImageCapture.Builder().setResolutionSelector(resSelector).setFlashMode(config.flashMode).setTargetRotation(config.currentRotation).setJpegQuality(100).setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
                imageCapture = capture
                videoCapture = null
                camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
            } else {
                val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(config.videoQuality, FallbackStrategy.lowerQualityOrHigherThan(Quality.SD))).build()
                val vc = VideoCapture.withOutput(recorder).apply { targetRotation = config.currentRotation }
                videoCapture = vc
                imageCapture = null
                camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, vc)
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "Level 2 bind failed", e)
            false
        }
    }

    private fun tryBindLevel3(provider: ProcessCameraProvider, previewView: PreviewView, config: CameraConfig): Boolean {
        return try {
            provider.unbindAll()
            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            val selector = resolveSelector(provider, CameraSelector.Builder().requireLensFacing(config.lensFacing).build(), config.lensFacing)
            if (config.cameraMode == "PHOTO") {
                val capture = ImageCapture.Builder().setFlashMode(config.flashMode).setTargetRotation(config.currentRotation).setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
                imageCapture = capture
                videoCapture = null
                camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
            } else {
                val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.SD, FallbackStrategy.higherQualityOrLowerThan(Quality.SD))).build()
                val vc = VideoCapture.withOutput(recorder).apply { targetRotation = config.currentRotation }
                videoCapture = vc
                imageCapture = null
                camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, vc)
            }
            true
        } catch (e: Exception) {
            initializationError = e.message ?: "Kamera hatası"
            false
        }
    }

    @androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
    private fun applyCamera2Optimizations(builder: ImageCapture.Builder, caps: CameraCapabilities) {
        try {
            val extender = Camera2Interop.Extender(builder)
            if (caps.supportsEdgeHighQuality) extender.setCaptureRequestOption(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_HIGH_QUALITY)
            if (caps.supportsNoiseReductionHighQuality) extender.setCaptureRequestOption(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY)
        } catch (e: Exception) {
            Log.w(TAG, "Camera2Interop fail", e)
        }
    }

    private fun resolveSelector(provider: ProcessCameraProvider, preferred: CameraSelector, lensFacing: Int): CameraSelector {
        if (provider.hasCamera(preferred)) return preferred
        val fallbackLens = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
        val fallback = CameraSelector.Builder().requireLensFacing(fallbackLens).build()
        return if (provider.hasCamera(fallback)) fallback else CameraSelector.DEFAULT_BACK_CAMERA
    }

    @Suppress("kotlin:S6524")
    private fun readCameraMetadata() {
        camera?.cameraInfo?.let { info ->
            val caps = try {
                Recorder.getVideoCapabilities(info).getSupportedQualities(DynamicRange.SDR)
            } catch (_: Exception) {
                @Suppress("DEPRECATION")
                QualitySelector.getSupportedQualities(info)
            }
            if (caps.isNotEmpty()) {
                supportedQualities = caps.toList()
            }
            info.zoomState.value?.let { z -> minZoom = z.minZoomRatio; maxZoom = z.maxZoomRatio }
            info.exposureState.let { s ->
                exposureRange = s.exposureCompensationRange.run { lower.toFloat()..upper.toFloat() }
                exposureIndex = s.exposureCompensationIndex
            }
        }
    }

    private suspend fun awaitCameraProvider(): ProcessCameraProvider = withContext(ioDispatcher) {
        cameraProvider ?: ProcessCameraProvider.getInstance(context).get().also { cameraProvider = it }
    }

    private suspend fun awaitExtensionsManager(provider: ProcessCameraProvider): ExtensionsManager = withContext(ioDispatcher) {
        ExtensionsManager.getInstanceAsync(context, provider).get()
    }

    fun setZoom(ratio: Float) { camera?.cameraControl?.setZoomRatio(ratio.coerceIn(minZoom, maxZoom)) }
    fun setExposure(index: Int) { exposureIndex = index; camera?.cameraControl?.setExposureCompensationIndex(index) }

    fun focusAndMeter(offset: androidx.compose.ui.geometry.Offset, previewView: PreviewView) {
        camera?.let { cam ->
            val point = previewView.meteringPointFactory.createPoint(offset.x, offset.y, 0.20f)
            setExposure(0)
            val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB).setAutoCancelDuration(4, TimeUnit.SECONDS).build()
            cam.cameraControl.startFocusAndMetering(action)
        }
    }

    fun setFlashMode(mode: Int) { imageCapture?.flashMode = mode }
    fun updateTargetRotation(rot: Int) { imageCapture?.targetRotation = rot; videoCapture?.targetRotation = rot }

    fun unbindAll() { isBound = false; cameraProvider?.unbindAll(); camera = null; imageCapture = null; videoCapture = null }
}

@Composable
fun rememberCameraStateHolder(
    context: Context = androidx.compose.ui.platform.LocalContext.current,
    lifecycleOwner: LifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current,
    ioDispatcher: CoroutineDispatcher = kotlinx.coroutines.Dispatchers.IO
): CameraStateHolder {
    val executor = remember { Executors.newSingleThreadExecutor() }
    val holder = remember(context, lifecycleOwner, executor, ioDispatcher) { CameraStateHolder(context, lifecycleOwner, executor, ioDispatcher) }
    DisposableEffect(holder) { onDispose { holder.unbindAll(); holder.executor.shutdown() } }
    return holder
}
