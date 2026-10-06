package com.fatihenes.photoreport.feature.camera.engine

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.Surface
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ExposureState
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.core.ZoomState
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.ui.geometry.Offset
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.fatihenes.photoreport.core.common.di.Dispatcher
import com.fatihenes.photoreport.core.common.di.FotoRaporDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

private const val TAG = "CameraXEngine"
private const val FOCUS_AUTO_CANCEL_SECONDS = 4L

@Singleton
class CameraXEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val providerWarmup: ProviderWarmup,
    private val capabilityRepository: CapabilityRepository,
    private val sessionConfigFactory: SessionConfigFactory,
    @Dispatcher(FotoRaporDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : CameraEngine {

    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val commandChannel = Channel<CameraCommand>(Channel.UNLIMITED)
    private val captureExecutor = Executors.newSingleThreadExecutor()

    // Engine State Flows
    private val _sessionState = MutableStateFlow<CameraSessionState>(CameraSessionState.Uninitialized)
    override val sessionState: StateFlow<CameraSessionState> = _sessionState.asStateFlow()

    private val _zoomState = MutableStateFlow<ZoomState?>(null)
    override val zoomState: StateFlow<ZoomState?> = _zoomState.asStateFlow()

    private val _exposureState = MutableStateFlow<ExposureState?>(null)
    override val exposureState: StateFlow<ExposureState?> = _exposureState.asStateFlow()

    private val _torchState = MutableStateFlow(TorchState.OFF)
    override val torchState: StateFlow<Int> = _torchState.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    override val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _activeLensFacing = MutableStateFlow(CameraSelector.LENS_FACING_BACK)
    override val activeLensFacing: StateFlow<Int> = _activeLensFacing.asStateFlow()

    private val _currentMode = MutableStateFlow(CameraMode.PHOTO)
    override val currentMode: StateFlow<CameraMode> = _currentMode.asStateFlow()

    private val _currentFlashMode = MutableStateFlow(FlashMode.OFF)
    override val currentFlashMode: StateFlow<FlashMode> = _currentFlashMode.asStateFlow()

    private val _currentAspectRatio = MutableStateFlow(AspectRatioSelection.RATIO_4_3)
    override val currentAspectRatio: StateFlow<AspectRatioSelection> = _currentAspectRatio.asStateFlow()

    private val _currentVideoQuality = MutableStateFlow(Quality.FHD)
    override val currentVideoQuality: StateFlow<Quality> = _currentVideoQuality.asStateFlow()

    private val _isFocusLocked = MutableStateFlow(false)
    override val isFocusLocked: StateFlow<Boolean> = _isFocusLocked.asStateFlow()

    // Internal CameraX references
    private var cameraProvider: ProcessCameraProvider? = null
    private var extensionsManager: ExtensionsManager? = null
    private var currentCamera: Camera? = null
    private var currentPreview: Preview? = null
    private var currentImageCapture: ImageCapture? = null
    private var currentVideoCapture: VideoCapture<Recorder>? = null
    private var currentLifecycleOwner: LifecycleOwner? = null
    private var currentPreviewView: PreviewView? = null
    private var currentTargetRotation: Int = Surface.ROTATION_0
    private var currentCapabilities: EnhancedCapabilities = EnhancedCapabilities()
    private var isThreeUseCasesBound: Boolean = false
    private var activeRecording: Recording? = null

    init {
        startCommandActor()
    }

    private fun startCommandActor() {
        engineScope.launch {
            for (command in commandChannel) {
                try {
                    processCommand(command)
                } catch (e: Exception) {
                    Log.e(TAG, "Error executing camera command: $command", e)
                    _sessionState.value = CameraSessionState.Error(
                        errorType = CameraErrorType.UNKNOWN,
                        message = e.localizedMessage,
                        cause = e
                    )
                }
            }
        }
    }

    private suspend fun processCommand(command: CameraCommand) {
        when (command) {
            is CameraCommand.Bind -> executeBind(command.lifecycleOwner, command.previewView)
            is CameraCommand.SwitchLens -> executeSwitchLens()
            is CameraCommand.SetMode -> executeSetMode(command.mode)
            is CameraCommand.SetAspectRatio -> executeSetAspectRatio(command.ratio)
            is CameraCommand.SetFlash -> executeSetFlash(command.mode)
            is CameraCommand.SetTorch -> executeSetTorch(command.enabled)
            is CameraCommand.SetZoom -> executeSetZoom(command.ratio)
            is CameraCommand.SetLinearZoom -> executeSetLinearZoom(command.linear)
            is CameraCommand.SetExposure -> executeSetExposure(command.index)
            is CameraCommand.FocusAndMeter -> executeFocusAndMeter(command.offset, command.previewView, command.lock)
            is CameraCommand.UnlockFocus -> executeUnlockFocus()
            is CameraCommand.UpdateRotation -> executeUpdateRotation(command.rotation)
            is CameraCommand.SetQuality -> executeSetQuality(command.quality)
            is CameraCommand.UnbindAll -> executeUnbindAll()
        }
    }

    override suspend fun bind(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        commandChannel.send(CameraCommand.Bind(lifecycleOwner, previewView))
    }

    override suspend fun setCameraMode(mode: CameraMode) {
        commandChannel.send(CameraCommand.SetMode(mode))
    }

    override suspend fun setFlashMode(flashMode: FlashMode) {
        commandChannel.send(CameraCommand.SetFlash(flashMode))
    }

    override suspend fun setTorchEnabled(enabled: Boolean) {
        commandChannel.send(CameraCommand.SetTorch(enabled))
    }

    override suspend fun switchLens() {
        commandChannel.send(CameraCommand.SwitchLens)
    }

    override suspend fun setAspectRatio(aspectRatio: AspectRatioSelection) {
        commandChannel.send(CameraCommand.SetAspectRatio(aspectRatio))
    }

    override suspend fun setVideoQuality(quality: Quality) {
        commandChannel.send(CameraCommand.SetQuality(quality))
    }

    override suspend fun setZoomRatio(ratio: Float) {
        commandChannel.send(CameraCommand.SetZoom(ratio))
    }

    override suspend fun setLinearZoom(linear: Float) {
        commandChannel.send(CameraCommand.SetLinearZoom(linear))
    }

    override suspend fun setExposureIndex(index: Int) {
        commandChannel.send(CameraCommand.SetExposure(index))
    }

    override suspend fun focusAndMeter(offset: Offset, previewView: PreviewView) {
        commandChannel.send(CameraCommand.FocusAndMeter(offset, previewView, lock = false))
    }

    override suspend fun lockFocusAndMetering(offset: Offset, previewView: PreviewView) {
        commandChannel.send(CameraCommand.FocusAndMeter(offset, previewView, lock = true))
    }

    override suspend fun unlockFocusAndMetering() {
        commandChannel.send(CameraCommand.UnlockFocus)
    }

    override suspend fun updateTargetRotation(rotation: Int) {
        commandChannel.send(CameraCommand.UpdateRotation(rotation))
    }

    override fun stopRecording() {
        try {
            activeRecording?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping active recording", e)
        }
        activeRecording = null
    }

    override fun pauseRecording() {
        try {
            activeRecording?.pause()
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing recording", e)
        }
    }

    override fun resumeRecording() {
        try {
            activeRecording?.resume()
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming recording", e)
        }
    }

    override fun unbindAll() {
        commandChannel.trySend(CameraCommand.UnbindAll)
    }

    /**
     * Releases the current session. The engine is a process-wide singleton, so its
     * command actor and executor must stay alive for the next time the camera opens.
     * Cancelling them here would leave the viewfinder black on the second visit.
     */
    override fun release() {
        unbindAll()
    }

    // Command implementations
    private suspend fun executeBind(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        currentLifecycleOwner = lifecycleOwner
        currentPreviewView = previewView
        _sessionState.value = CameraSessionState.Initializing

        try {
            val provider = providerWarmup.awaitCameraProvider()
            cameraProvider = provider
            extensionsManager = providerWarmup.awaitExtensionsManager(provider)

            bindSessionInternal()
        } catch (e: Exception) {
            Log.e(TAG, "Camera initialization failed", e)
            _sessionState.value = CameraSessionState.Error(
                errorType = CameraErrorType.INITIALIZATION_FAILED,
                message = e.localizedMessage,
                cause = e
            )
        }
    }

    private suspend fun bindSessionInternal() {
        val provider = cameraProvider ?: return
        val lifecycle = currentLifecycleOwner ?: return
        val previewView = currentPreviewView ?: return

        removeCameraObservers()
        provider.unbindAll()
        _isStreaming.value = false
        // Reset the binding flag before each new session attempt so stale
        // three-use-case state never leaks across rebinds (e.g. after mode or
        // aspect-ratio changes).
        isThreeUseCasesBound = false

        val lensFacing = _activeLensFacing.value
        val selector = resolveCameraSelector(provider, lensFacing)

        // Query capabilities for the lens we are ABOUT to open (not the previous one),
        // so zoom range (e.g. 0.5x ultra-wide), ZSL and video qualities are correct.
        val targetInfo = try {
            provider.getCameraInfo(selector)
        } catch (e: Exception) {
            Log.w(TAG, "Could not resolve CameraInfo for selector", e)
            null
        }
        val caps = capabilityRepository.getCapabilities(lensFacing, targetInfo, extensionsManager)
        currentCapabilities = caps

        // Keep previewView implementation mode stable (COMPATIBLE mode set on creation for Compose safety)

        // Binding VideoCapture alongside ImageCapture caps photo resolution to the
        // RECORD size (~1080p) on LIMITED hardware and disables ZSL. Only FULL/LEVEL_3
        // devices guarantee a full-resolution JPEG stream next to a video stream.
        val bound3 = caps.isFullOrBetter &&
            tryBindConcurrentSession(provider, lifecycle, selector, previewView, caps)
        isThreeUseCasesBound = bound3

        if (!bound3) {
            tryBindFallbackSession(provider, lifecycle, selector, previewView, caps)
        }

        setupCameraObservers()
        restoreTorchIfNeeded()
        _sessionState.value = CameraSessionState.Ready(
            isStreaming = true,
            capabilities = currentCapabilities
        )
    }

    private fun restoreTorchIfNeeded() {
        if (_currentFlashMode.value == FlashMode.TORCH) {
            currentCamera?.cameraControl?.enableTorch(true)
        }
    }

    private fun tryBindConcurrentSession(
        provider: ProcessCameraProvider,
        lifecycle: LifecycleOwner,
        selector: CameraSelector,
        previewView: PreviewView,
        caps: EnhancedCapabilities
    ): Boolean {
        return try {
            val preview = sessionConfigFactory.createPreview(
                aspectRatio = _currentAspectRatio.value,
                caps = caps
            ).also { it.surfaceProvider = previewView.surfaceProvider }

            val capture = sessionConfigFactory.createImageCapture(
                aspectRatio = _currentAspectRatio.value,
                flashMode = _currentFlashMode.value,
                targetRotation = currentTargetRotation,
                enableOptimization = true,
                caps = caps,
                enableAvif = false // AVIF handled post-capture via Media3 Transformer
            )

            val video = sessionConfigFactory.createVideoCapture(
                videoQuality = _currentVideoQuality.value,
                targetRotation = currentTargetRotation,
                caps = caps
            )

            currentCamera = provider.bindToLifecycle(lifecycle, selector, preview, capture, video)
            currentPreview = preview
            currentImageCapture = capture
            currentVideoCapture = video
            true
        } catch (e: Exception) {
            Log.w(TAG, "3-use-case concurrent binding not supported, falling back", e)
            provider.unbindAll()
            false
        }
    }

    private fun tryBindFallbackSession(
        provider: ProcessCameraProvider,
        lifecycle: LifecycleOwner,
        selector: CameraSelector,
        previewView: PreviewView,
        caps: EnhancedCapabilities
    ) {
        try {
            bindStandardSession(provider, lifecycle, selector, previewView, caps)
        } catch (e: Exception) {
            Log.w(TAG, "Standard session bind failed, falling back to minimal safe session", e)
            provider.unbindAll()
            bindMinimalSafeSession(provider, lifecycle, selector, previewView)
        }
    }

    private fun bindStandardSession(
        provider: ProcessCameraProvider,
        lifecycle: LifecycleOwner,
        selector: CameraSelector,
        previewView: PreviewView,
        caps: EnhancedCapabilities
    ) {
        val preview = sessionConfigFactory.createPreview(
            aspectRatio = _currentAspectRatio.value,
            caps = caps
        ).also { it.surfaceProvider = previewView.surfaceProvider }

        if (_currentMode.value == CameraMode.PHOTO) {
            val capture = sessionConfigFactory.createImageCapture(
                aspectRatio = _currentAspectRatio.value,
                flashMode = _currentFlashMode.value,
                targetRotation = currentTargetRotation,
                enableOptimization = true,
                caps = caps,
                enableAvif = false
            )
            currentCamera = provider.bindToLifecycle(lifecycle, selector, preview, capture)
            currentPreview = preview
            currentImageCapture = capture
            currentVideoCapture = null
        } else {
            val video = sessionConfigFactory.createVideoCapture(
                videoQuality = _currentVideoQuality.value,
                targetRotation = currentTargetRotation,
                caps = caps
            )
            currentCamera = provider.bindToLifecycle(lifecycle, selector, preview, video)
            currentPreview = preview
            currentImageCapture = null
            currentVideoCapture = video
        }
    }

    private fun bindMinimalSafeSession(
        provider: ProcessCameraProvider,
        lifecycle: LifecycleOwner,
        selector: CameraSelector,
        previewView: PreviewView
    ) {
        try {
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            if (_currentMode.value == CameraMode.PHOTO) {
                val capture = ImageCapture.Builder()
                    .setTargetRotation(currentTargetRotation)
                    .build()
                currentCamera = provider.bindToLifecycle(lifecycle, selector, preview, capture)
                currentPreview = preview
                currentImageCapture = capture
                currentVideoCapture = null
            } else {
                val video = VideoCapture.withOutput(
                    Recorder.Builder().build()
                )
                currentCamera = provider.bindToLifecycle(lifecycle, selector, preview, video)
                currentPreview = preview
                currentImageCapture = null
                currentVideoCapture = video
            }
        } catch (e: Exception) {
            Log.e(TAG, "Minimal safe session bind also failed", e)
        }
    }

    private var observedCameraInfo: CameraInfo? = null

    private fun removeCameraObservers() {
        val owner = currentLifecycleOwner ?: return
        observedCameraInfo?.let { info ->
            info.cameraState.removeObservers(owner)
            info.zoomState.removeObservers(owner)
            info.torchState.removeObservers(owner)
        }
        observedCameraInfo = null
    }

    private fun setupCameraObservers() {
        val cam = currentCamera ?: return
        val owner = currentLifecycleOwner ?: return
        val info = cam.cameraInfo
        observedCameraInfo = info

        info.cameraState.observe(owner) { state ->
            _isStreaming.value = state.type == CameraState.Type.OPEN

            if (state.type == CameraState.Type.OPEN && _sessionState.value is CameraSessionState.Error) {
                // CameraX recovered on its own (e.g. another app released the camera).
                _sessionState.value = CameraSessionState.Ready(isStreaming = true, capabilities = currentCapabilities)
                return@observe
            }

            val err = state.error ?: return@observe
            // Recoverable errors (camera briefly busy, max cameras in use) are retried by
            // CameraX automatically. Surfacing them would just flash an error banner.
            if (err.type == CameraState.ErrorType.RECOVERABLE) {
                Log.w(TAG, "Recoverable camera error ${err.code}, waiting for CameraX to retry")
                return@observe
            }
            val errorType = when (err.code) {
                CameraState.ERROR_CAMERA_IN_USE -> CameraErrorType.CAMERA_IN_USE
                CameraState.ERROR_CAMERA_DISABLED -> CameraErrorType.CAMERA_DISABLED
                CameraState.ERROR_CAMERA_FATAL_ERROR -> CameraErrorType.CAMERA_DISCONNECTED
                CameraState.ERROR_DO_NOT_DISTURB_MODE_ENABLED -> CameraErrorType.SECURITY_VIOLATION
                else -> CameraErrorType.UNKNOWN
            }
            _sessionState.value = CameraSessionState.Error(
                errorType = errorType,
                message = err.cause?.message,
                cause = err.cause
            )
        }

        info.zoomState.observe(owner) { state ->
            _zoomState.value = state
        }

        info.torchState.observe(owner) { state ->
            _torchState.value = state
        }

        _exposureState.value = info.exposureState
    }

    private suspend fun executeSwitchLens() {
        val nextLens = if (_activeLensFacing.value == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        _activeLensFacing.value = nextLens
        // The front and back cameras have different hardware capability profiles.
        // Clear the cache so the next bind queries fresh characteristics for the
        // new lens instead of reusing stale data from the previous lens.
        capabilityRepository.clearCache()
        bindSessionInternal()
    }

    private suspend fun executeSetMode(mode: CameraMode) {
        if (_currentMode.value == mode) return
        _currentMode.value = mode

        // If 3 use cases are concurrently bound, NO rebind needed at all!
        if (!isThreeUseCasesBound) {
            bindSessionInternal()
        }
    }

    private suspend fun executeSetAspectRatio(ratio: AspectRatioSelection) {
        if (_currentAspectRatio.value == ratio) return
        _currentAspectRatio.value = ratio
        bindSessionInternal()
    }

    private fun executeSetFlash(mode: FlashMode) {
        _currentFlashMode.value = mode
        val cam = currentCamera ?: return

        if (mode == FlashMode.TORCH) {
            cam.cameraControl.enableTorch(true)
        } else {
            cam.cameraControl.enableTorch(false)
            currentImageCapture?.flashMode = mode.toCameraXImageCaptureFlashMode()
        }
    }

    private fun executeSetTorch(enabled: Boolean) {
        currentCamera?.cameraControl?.enableTorch(enabled)
    }

    private fun executeSetZoom(ratio: Float) {
        val cam = currentCamera ?: return
        // Clamp against the live ZoomState of the bound camera (authoritative range).
        val live = cam.cameraInfo.zoomState.value
        val min = live?.minZoomRatio ?: currentCapabilities.minZoomRatio
        val max = live?.maxZoomRatio ?: currentCapabilities.maxZoomRatio
        cam.cameraControl.setZoomRatio(ratio.coerceIn(min, max))
    }

    private fun executeSetLinearZoom(linear: Float) {
        currentCamera?.cameraControl?.setLinearZoom(linear.coerceIn(0f, 1f))
    }

    private fun executeSetExposure(index: Int) {
        val cam = currentCamera ?: return
        val clamped = index.coerceIn(
            currentCapabilities.exposureRangeLower,
            currentCapabilities.exposureRangeUpper
        )
        cam.cameraControl.setExposureCompensationIndex(clamped)
    }

    private fun executeFocusAndMeter(offset: Offset, previewView: PreviewView, lock: Boolean) {
        val cam = currentCamera ?: return
        val point = previewView.meteringPointFactory.createPoint(offset.x, offset.y, 0.15f)
        val builder = FocusMeteringAction.Builder(
            point,
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB
        )

        if (!lock) {
            builder.setAutoCancelDuration(FOCUS_AUTO_CANCEL_SECONDS, TimeUnit.SECONDS)
        }

        val action = builder.build()
        cam.cameraControl.startFocusAndMetering(action)
        _isFocusLocked.value = lock
    }

    private fun executeUnlockFocus() {
        currentCamera?.cameraControl?.cancelFocusAndMetering()
        _isFocusLocked.value = false
    }

    private fun executeUpdateRotation(rotation: Int) {
        currentTargetRotation = rotation
        currentImageCapture?.targetRotation = rotation
        currentVideoCapture?.targetRotation = rotation
    }

    private suspend fun executeSetQuality(quality: Quality) {
        if (_currentVideoQuality.value == quality) return
        _currentVideoQuality.value = quality
        bindSessionInternal()
    }

    private fun executeUnbindAll() {
        removeCameraObservers()
        cameraProvider?.unbindAll()
        currentLifecycleOwner = null
        currentPreviewView = null
        currentCamera = null
        currentPreview = null
        currentImageCapture = null
        currentVideoCapture = null
        _isStreaming.value = false
        _sessionState.value = CameraSessionState.Uninitialized
    }

    private fun resolveCameraSelector(provider: ProcessCameraProvider, lensFacing: Int): CameraSelector {
        val preferred = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        if (provider.hasCamera(preferred)) return preferred
        val fallbackLens = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        val fallback = CameraSelector.Builder().requireLensFacing(fallbackLens).build()
        return if (provider.hasCamera(fallback)) fallback else CameraSelector.DEFAULT_BACK_CAMERA
    }

    // Photo Capture
    override suspend fun takePhoto(outputFile: File, rotation: Int): Result<Uri> = suspendCancellableCoroutine { cont ->
        val capture = currentImageCapture
        if (capture == null) {
            cont.resume(Result.failure(IllegalStateException("ImageCapture is not initialized")))
            return@suspendCancellableCoroutine
        }

        capture.targetRotation = rotation
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        _sessionState.value = CameraSessionState.Capturing()

        try {
            capture.takePicture(
                outputOptions,
                captureExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        _sessionState.value = CameraSessionState.Ready(
                            isStreaming = true,
                            capabilities = currentCapabilities
                        )
                        val uri = outputFileResults.savedUri ?: Uri.fromFile(outputFile)
                        cont.resume(Result.success(uri))
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e(TAG, "Image capture failed", exception)
                        _sessionState.value = CameraSessionState.Ready(
                            isStreaming = true,
                            capabilities = currentCapabilities
                        )
                        cont.resume(Result.failure(exception))
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "takePicture exception", e)
            _sessionState.value = CameraSessionState.Ready(
                isStreaming = true,
                capabilities = currentCapabilities
            )
            cont.resume(Result.failure(e))
        }
    }

    // Video Recording
    @SuppressLint("MissingPermission")
    override suspend fun startRecording(
        outputFile: File,
        rotation: Int,
        enableAudio: Boolean,
        onEvent: (VideoRecordEvent) -> Unit
    ): Result<Recording> = withContext(ioDispatcher) {
        val vc = currentVideoCapture ?: return@withContext Result.failure(
            IllegalStateException("VideoCapture is not initialized")
        )

        vc.targetRotation = rotation
        val fileOptions = FileOutputOptions.Builder(outputFile).build()
        val pending = vc.output.prepareRecording(context, fileOptions)

        if (enableAudio) {
            try {
                pending.withAudioEnabled()
            } catch (e: SecurityException) {
                Log.w(TAG, "RECORD_AUDIO permission missing, continuing without audio", e)
            }
        }

        return@withContext try {
            val recording = pending.start(ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        _sessionState.value = CameraSessionState.Recording()
                    }
                    is VideoRecordEvent.Status -> {
                        _sessionState.value = CameraSessionState.Recording(
                            durationNanos = event.recordingStats.recordedDurationNanos,
                            recordedBytes = event.recordingStats.numBytesRecorded
                        )
                    }
                    is VideoRecordEvent.Pause -> {
                        val current = _sessionState.value as? CameraSessionState.Recording
                        _sessionState.value = current?.copy(isPaused = true)
                            ?: CameraSessionState.Recording(isPaused = true)
                    }
                    is VideoRecordEvent.Resume -> {
                        val current = _sessionState.value as? CameraSessionState.Recording
                        _sessionState.value = current?.copy(isPaused = false)
                            ?: CameraSessionState.Recording(isPaused = false)
                    }
                    is VideoRecordEvent.Finalize -> {
                        _sessionState.value = CameraSessionState.Ready(
                            isStreaming = true,
                            capabilities = currentCapabilities
                        )
                    }
                }
                onEvent(event)
            }
            activeRecording = recording
            Result.success(recording)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start video recording", e)
            Result.failure(e)
        }
    }
}

private sealed interface CameraCommand {
    data class Bind(val lifecycleOwner: LifecycleOwner, val previewView: PreviewView) : CameraCommand
    object SwitchLens : CameraCommand
    data class SetMode(val mode: CameraMode) : CameraCommand
    data class SetAspectRatio(val ratio: AspectRatioSelection) : CameraCommand
    data class SetFlash(val mode: FlashMode) : CameraCommand
    data class SetTorch(val enabled: Boolean) : CameraCommand
    data class SetZoom(val ratio: Float) : CameraCommand
    data class SetLinearZoom(val linear: Float) : CameraCommand
    data class SetExposure(val index: Int) : CameraCommand
    data class FocusAndMeter(val offset: Offset, val previewView: PreviewView, val lock: Boolean) : CameraCommand
    object UnlockFocus : CameraCommand
    data class UpdateRotation(val rotation: Int) : CameraCommand
    data class SetQuality(val quality: Quality) : CameraCommand
    object UnbindAll : CameraCommand
}
