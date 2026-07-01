package com.elektrik.ui

import android.content.Context
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.extensions.ExtensionMode
import android.hardware.camera2.CaptureRequest
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

@Stable
class CameraStateHolder(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    val executor: ExecutorService
) {
    var camera by mutableStateOf<Camera?>(null)
        private set
        
    var imageCapture by mutableStateOf<ImageCapture?>(null)
        private set
        
    var videoCapture by mutableStateOf<VideoCapture<Recorder>?>(null)
        private set

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

    @androidx.annotation.OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
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
        val cameraProvider = suspendCancellableCoroutine<ProcessCameraProvider> { continuation ->
            val providerFuture = ProcessCameraProvider.getInstance(context)
            providerFuture.addListener({
                if (continuation.isActive) {
                    continuation.resume(providerFuture.get())
                }
            }, ContextCompat.getMainExecutor(context))
        }

        // Initialize ExtensionsManager for HDR
        val extensionsManager = suspendCancellableCoroutine<ExtensionsManager> { continuation ->
            val extensionsManagerFuture = ExtensionsManager.getInstanceAsync(context, cameraProvider)
            extensionsManagerFuture.addListener({
                if (continuation.isActive) {
                    continuation.resume(extensionsManagerFuture.get())
                }
            }, ContextCompat.getMainExecutor(context))
        }

        // If coroutine was cancelled while waiting, bail out
        currentCoroutineContext().ensureActive()

        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy(aspectRatio, AspectRatioStrategy.FALLBACK_RULE_AUTO))
            .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
            .build()

        val previewBuilder = Preview.Builder()
            .setResolutionSelector(resolutionSelector)
            .setPreviewStabilizationEnabled(true) // Önizleme sarsıntı engelleme
        
        val captureBuilder = ImageCapture.Builder()
            .setResolutionSelector(resolutionSelector)
            .setFlashMode(flashMode)
            .setTargetRotation(currentRotation)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)

        var activeSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        // Enable HDR extension if available and requested
        if (enableOptimization && extensionsManager.isExtensionAvailable(activeSelector, ExtensionMode.HDR)) {
            activeSelector = extensionsManager.getExtensionEnabledCameraSelector(activeSelector, ExtensionMode.HDR)
            Log.d("CameraStateHolder", "HDR Extension enabled")
        } else if (enableOptimization) {
            // Fallback to Camera2Interop optimizations if HDR is not available
            val extender = Camera2Interop.Extender(captureBuilder)
            extender.setCaptureRequestOption(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_HIGH_QUALITY)
            extender.setCaptureRequestOption(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY)
            Log.d("CameraStateHolder", "Camera2Interop optimizations enabled")
        }

        val preview = previewBuilder.build().also { it.surfaceProvider = previewView.surfaceProvider }

        try {
            if (!cameraProvider.hasCamera(activeSelector)) {
                // If the selected camera with HDR is not available, fallback to basic selector
                activeSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                if (!cameraProvider.hasCamera(activeSelector)) {
                    val fallbackLens = if (lensFacing == CameraSelector.LENS_FACING_BACK) 
                        CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                    activeSelector = CameraSelector.Builder().requireLensFacing(fallbackLens).build()
                }
            }

            cameraProvider.unbindAll()

            if (cameraMode == "PHOTO") {
                val capture = captureBuilder.build()
                imageCapture = capture
                videoCapture = null
                
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    activeSelector,
                    preview,
                    capture
                )
            } else {
                val recorderBuilder = Recorder.Builder()
                    .setQualitySelector(
                        QualitySelector.from(
                            videoQuality,
                            FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
                        )
                    )
                // Video stabilization doesn't have a direct setter in Recorder.Builder in standard API yet, 
                // wait, actually we can just pass it directly if supported. Let's stick to standard Builder.
                val recorder = recorderBuilder.build()
                val capture = VideoCapture.withOutput(recorder)
                capture.targetRotation = currentRotation
                videoCapture = capture
                imageCapture = null
                
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    activeSelector,
                    preview,
                    capture
                )
            }

            camera?.cameraInfo?.let { info ->
                val qualities = QualitySelector.getSupportedQualities(info)
                if (qualities.isNotEmpty()) {
                    supportedQualities = qualities
                }
                
                info.zoomState.value?.let { zoomState ->
                    minZoom = zoomState.minZoomRatio
                    maxZoom = zoomState.maxZoomRatio
                }

                info.exposureState.let { state ->
                    exposureRange = state.exposureCompensationRange.run { lower.toFloat()..upper.toFloat() }
                    exposureIndex = state.exposureCompensationIndex
                }
            }
            
            initializationError = null

        } catch (exc: Exception) {
            Log.e("CameraStateHolder", "Use case binding failed", exc)
            initializationError = exc.message ?: "Kamera başlatılamadı"
        }
    }
    
    fun setZoom(ratio: Float) {
        val targetZoom = ratio.coerceIn(minZoom, maxZoom)
        camera?.cameraControl?.setZoomRatio(targetZoom)
    }

    fun setExposure(index: Int) {
        exposureIndex = index
        camera?.cameraControl?.setExposureCompensationIndex(index)
    }

    fun focusAndMeter(offset: androidx.compose.ui.geometry.Offset, previewView: PreviewView) {
        camera?.let { cam ->
            val meteringPointFactory = previewView.meteringPointFactory
            // Unified point for AF, AE, and AWB with a slightly larger area (20%) to ensure good exposure
            val point = meteringPointFactory.createPoint(offset.x, offset.y, 0.20f)
            
            // Reset manual exposure compensation when tap-to-focus happens so AE can take over completely
            setExposure(0)

            val action = FocusMeteringAction.Builder(
                point, 
                FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB
            )
                .setAutoCancelDuration(4, TimeUnit.SECONDS)
                .build()
                
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
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            try {
                val cameraProvider = providerFuture.get()
                cameraProvider.unbindAll()
            } catch (e: Exception) {
                Log.e("CameraStateHolder", "Unbind failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
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
