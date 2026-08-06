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
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.fatihenes.photoreport.feature.camera.model.CameraCapabilities
import com.fatihenes.photoreport.feature.camera.model.queryCameraCapabilities
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private const val TAG = "CameraStateHolder"

@Stable
class CameraStateHolder(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    val executor: ExecutorService
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

    var supportedQualities by mutableStateOf(listOf(Quality.FHD, Quality.HD, Quality.SD))
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
    suspend fun bindCamera(
        previewView: PreviewView,
        lensFacing: Int,
        aspectRatio: Int,
        cameraMode: String,
        videoQuality: Quality,
        currentRotation: Int,
        flashMode: Int,
        enableOptimization: Boolean = true
    ) {
        isBound = false
        val caps = queryCameraCapabilities(context, lensFacing)

        previewView.implementationMode = if (caps.isLegacy)
            PreviewView.ImplementationMode.COMPATIBLE
        else
            PreviewView.ImplementationMode.PERFORMANCE

        val cameraProvider = awaitCameraProvider()
        val extensionsManager = awaitExtensionsManager(cameraProvider)
        currentCoroutineContext().ensureActive()

        val safeFlashMode = if (lensFacing == CameraSelector.LENS_FACING_FRONT) ImageCapture.FLASH_MODE_OFF else flashMode

        val success = tryBindLevel1(
            cameraProvider, extensionsManager, previewView,
            lensFacing, aspectRatio, cameraMode, videoQuality,
            currentRotation, safeFlashMode, enableOptimization, caps
        ) || tryBindLevel2(
            cameraProvider, previewView,
            lensFacing, aspectRatio, cameraMode, videoQuality,
            currentRotation, flashMode
        ) || tryBindLevel3(
            cameraProvider, previewView,
            lensFacing, cameraMode, currentRotation, flashMode
        )

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
        lensFacing: Int,
        aspectRatio: Int,
        cameraMode: String,
        videoQuality: Quality,
        currentRotation: Int,
        flashMode: Int,
        enableOptimization: Boolean,
        caps: CameraCapabilities
    ): Boolean {
        return try {
            provider.unbindAll()

            val previewRes = ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy(aspectRatio, AspectRatioStrategy.FALLBACK_RULE_AUTO))
                .build()

            val captureRes = ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy(aspectRatio, AspectRatioStrategy.FALLBACK_RULE_AUTO))
                .apply {
                    if (!caps.isLegacy) setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                }
                .build()

            val previewBuilder = Preview.Builder().setResolutionSelector(previewRes)
            if (enableOptimization && caps.supportsPreviewStabilization) {
                previewBuilder.setPreviewStabilizationEnabled(true)
                Log.d(TAG, "L1: Preview stabilization ON")
            }

            val captureBuilder = ImageCapture.Builder()
                .setResolutionSelector(captureRes)
                .setFlashMode(flashMode)
                .setTargetRotation(currentRotation)
                .setJpegQuality(100)
                .setCaptureMode(
                    if (caps.isFullOrBetter) ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
                    else ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                )

            var selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
            selector = resolveSelector(provider, selector, lensFacing)

            if (enableOptimization && extensions.isExtensionAvailable(selector, ExtensionMode.HDR)) {
                selector = extensions.getExtensionEnabledCameraSelector(selector, ExtensionMode.HDR)
                Log.d(TAG, "L1: HDR extension ON")
            } else if (enableOptimization) {
                applyCamera2Optimizations(captureBuilder, caps)
            }

            val preview = previewBuilder.build().also { it.surfaceProvider = previewView.surfaceProvider }

            if (cameraMode == "PHOTO") {
                val capture = captureBuilder.build()
                imageCapture = capture
                videoCapture = null
                camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
            } else {
                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(videoQuality, FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)))
                    .build()
                val vc = VideoCapture.withOutput(recorder)
                vc.targetRotation = currentRotation
                videoCapture = vc
                imageCapture = null
                camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, vc)
            }

            Log.d(TAG, "L1: Bind succeeded (mode=$cameraMode)")
            true
        } catch (e: Exception) {
            Log.w(TAG, "L1: Bind failed, falling back to L2", e)
            false
        }
    }

    private fun tryBindLevel2(
        provider: ProcessCameraProvider,
        previewView: PreviewView,
        lensFacing: Int,
        aspectRatio: Int,
        cameraMode: String,
        videoQuality: Quality,
        currentRotation: Int,
        flashMode: Int
    ): Boolean {
        return try {
            provider.unbindAll()

            val resSelector = ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy(aspectRatio, AspectRatioStrategy.FALLBACK_RULE_AUTO))
                .build()

            val preview = Preview.Builder()
                .setResolutionSelector(resSelector)
                .build()
                .also { it.surfaceProvider = previewView.surfaceProvider }

            var selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
            selector = resolveSelector(provider, selector, lensFacing)

            if (cameraMode == "PHOTO") {
                val capture = ImageCapture.Builder()
                    .setResolutionSelector(resSelector)
                    .setFlashMode(flashMode)
                    .setTargetRotation(currentRotation)
                    .setJpegQuality(100)
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                imageCapture = capture
                videoCapture = null
                camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
            } else {
                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(videoQuality, FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)))
                    .build()
                val vc = VideoCapture.withOutput(recorder)
                vc.targetRotation = currentRotation
                videoCapture = vc
                imageCapture = null
                camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, vc)
            }

            Log.d(TAG, "L2: Safe bind succeeded")
            true
        } catch (e: Exception) {
            Log.w(TAG, "L2: Safe bind failed, falling back to L3", e)
            false
        }
    }

    private fun tryBindLevel3(
        provider: ProcessCameraProvider,
        previewView: PreviewView,
        lensFacing: Int,
        cameraMode: String,
        currentRotation: Int,
        flashMode: Int
    ): Boolean {
        return try {
            provider.unbindAll()

            val preview = Preview.Builder().build()
                .also { it.surfaceProvider = previewView.surfaceProvider }

            var selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
            selector = resolveSelector(provider, selector, lensFacing)

            if (cameraMode == "PHOTO") {
                val capture = ImageCapture.Builder()
                    .setFlashMode(flashMode)
                    .setTargetRotation(currentRotation)
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                imageCapture = capture
                videoCapture = null
                camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
            } else {
                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.SD, FallbackStrategy.higherQualityOrLowerThan(Quality.SD)))
                    .build()
                val vc = VideoCapture.withOutput(recorder)
                vc.targetRotation = currentRotation
                videoCapture = vc
                imageCapture = null
                camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, vc)
            }

            Log.d(TAG, "L3: Minimum bind succeeded")
            true
        } catch (e: Exception) {
            Log.e(TAG, "L3: All bind levels exhausted", e)
            initializationError = e.message ?: "Kamera başlatılamadı"
            camera = null
            imageCapture = null
            videoCapture = null
            false
        }
    }

    @androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
    private fun applyCamera2Optimizations(builder: ImageCapture.Builder, caps: CameraCapabilities) {
        try {
            val extender = Camera2Interop.Extender(builder)
            if (caps.supportsEdgeHighQuality) {
                extender.setCaptureRequestOption(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_HIGH_QUALITY)
                Log.d(TAG, "L1: Edge HQ ON")
            }
            if (caps.supportsNoiseReductionHighQuality) {
                extender.setCaptureRequestOption(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY)
                Log.d(TAG, "L1: Noise Reduction HQ ON")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Camera2Interop apply failed (non-fatal)", e)
        }
    }

    private fun resolveSelector(
        provider: ProcessCameraProvider,
        preferred: CameraSelector,
        lensFacing: Int
    ): CameraSelector {
        if (provider.hasCamera(preferred)) return preferred
        val fallbackLens = if (lensFacing == CameraSelector.LENS_FACING_BACK)
            CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
        val fallback = CameraSelector.Builder().requireLensFacing(fallbackLens).build()
        return if (provider.hasCamera(fallback)) fallback else CameraSelector.DEFAULT_BACK_CAMERA
    }

    private fun readCameraMetadata() {
        camera?.cameraInfo?.let { info ->
            val qualities = try {
                Recorder.getVideoCapabilities(info).getSupportedQualities(DynamicRange.SDR)
            } catch (_: Exception) {
                @Suppress("DEPRECATION")
                QualitySelector.getSupportedQualities(info)
            }
            if (qualities.isNotEmpty()) supportedQualities = qualities

            info.zoomState.value?.let { z ->
                minZoom = z.minZoomRatio
                maxZoom = z.maxZoomRatio
            }

            info.exposureState.let { s ->
                exposureRange = s.exposureCompensationRange.run { lower.toFloat()..upper.toFloat() }
                exposureIndex = s.exposureCompensationIndex
            }
        }
    }

    private suspend fun awaitCameraProvider(): ProcessCameraProvider = withContext(Dispatchers.IO) {
        cameraProvider?.let { return@withContext it }
        val provider = ProcessCameraProvider.getInstance(context).get()
        cameraProvider = provider
        provider
    }

    private suspend fun awaitExtensionsManager(provider: ProcessCameraProvider): ExtensionsManager = withContext(Dispatchers.IO) {
        ExtensionsManager.getInstanceAsync(context, provider).get()
    }

    fun setZoom(ratio: Float) {
        camera?.cameraControl?.setZoomRatio(ratio.coerceIn(minZoom, maxZoom))
    }

    fun setExposure(index: Int) {
        exposureIndex = index
        camera?.cameraControl?.setExposureCompensationIndex(index)
    }

    fun focusAndMeter(offset: androidx.compose.ui.geometry.Offset, previewView: PreviewView) {
        camera?.let { cam ->
            val point = previewView.meteringPointFactory.createPoint(offset.x, offset.y, 0.20f)
            setExposure(0)
            val action = FocusMeteringAction.Builder(
                point,
                FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB
            ).setAutoCancelDuration(4, TimeUnit.SECONDS).build()
            cam.cameraControl.startFocusAndMetering(action)
        }
    }

    fun setFlashMode(flashMode: Int) {
        imageCapture?.flashMode = flashMode
    }

    fun updateTargetRotation(rotation: Int) {
        imageCapture?.targetRotation = rotation
        videoCapture?.targetRotation = rotation
    }

    fun unbindAll() {
        isBound = false
        cameraProvider?.unbindAll()
        camera = null
        imageCapture = null
        videoCapture = null
    }
}

@Composable
fun rememberCameraStateHolder(
    context: Context = androidx.compose.ui.platform.LocalContext.current,
    lifecycleOwner: LifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
): CameraStateHolder {
    val executor = remember { Executors.newSingleThreadExecutor() }
    val holder = remember(context, lifecycleOwner, executor) {
        CameraStateHolder(context, lifecycleOwner, executor)
    }
    DisposableEffect(holder) {
        onDispose {
            holder.unbindAll()
            holder.executor.shutdown()
        }
    }
    return holder
}
