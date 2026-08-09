@file:Suppress("LocalContextGetResourceValueCall", "TooManyFunctions", "MaxLineLength")
package com.fatihenes.photoreport.feature.camera.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.util.Log
import android.view.OrientationEventListener
import android.view.Surface
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fatihenes.photoreport.core.media.PhotoManager
import com.fatihenes.photoreport.core.ui.R
import com.fatihenes.photoreport.core.ui.navigation.LocalSnackbarHostState
import com.fatihenes.photoreport.feature.camera.ui.components.*
import com.fatihenes.photoreport.feature.camera.viewmodel.CameraViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun CameraScreen(
    onPhotoCaptured: (Uri) -> Unit,
    onClose: () -> Unit,
    enableOptimization: Boolean = true,
    enableAvif: Boolean = true,
    onToggleOptimization: (Boolean) -> Unit = {},
    onToggleAvif: (Boolean) -> Unit = {},
    cameraViewModel: CameraViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val snackbarHost = LocalSnackbarHostState.current
    val cameraState = rememberCameraStateHolder()
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    val uiState by cameraViewModel.uiState.collectAsStateWithLifecycle()
    var showCaptureFeedback by remember { mutableStateOf(false) }
    var tapOffset by remember { mutableStateOf<Offset?>(null) }
    val deviceAngle = rememberDeviceAngle(context)
    val currentRotation = rememberCameraRotation(context, cameraState)
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    CameraEffects(params = EffectsParams(context, uiState, cameraState, cameraViewModel,
        EffectsConfig(enableOptimization, currentRotation, previewView, audioLauncher)))

    val iconRotateAngle = rememberIconRotation(currentRotation)
    val triggerShutter = {
        performShutterAction(params = ShutterParams(context, scope, snackbarHost, cameraState,
            ShutterState(uiState, cameraViewModel, audioLauncher, ShutterActions(
                onCaptureFeedback = { showCaptureFeedback = it }, onPhotoCaptured = onPhotoCaptured,
                getActiveRecording = { activeRecording }, setActiveRecording = { activeRecording = it }))), haptic = haptic)
    }

    val isLandscape = (currentRotation == Surface.ROTATION_90) || (currentRotation == Surface.ROTATION_270)
    val focusRequester = remember { FocusRequester() }.also { LaunchedEffect(Unit) { it.requestFocus() } }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black).focusRequester(focusRequester).focusable()
        .onKeyEvent { if (it.type == KeyEventType.KeyDown && (it.key == Key.VolumeUp || it.key == Key.VolumeDown)) { triggerShutter(); true } else false }) {

        cameraPreviewArea(params = PreviewParams(cameraState, uiState, previewView, deviceAngle, tapOffset),
            actions = PreviewActions(onZoomChanged = { cameraViewModel.setZoomRatio(it) },
                onFocusRequested = { tapOffset = it; cameraViewModel.setExposureValue(0f); cameraState.focusAndMeter(it, previewView) },
                onRetry = { scope.launch { cameraState.bindCamera(previewView, CameraConfig(uiState.lensFacing, uiState.aspectRatio, uiState.cameraMode, uiState.videoQuality, currentRotation, uiState.flashMode, enableOptimization)) } }),
            modifier = Modifier.align(Alignment.Center))

        cameraTopBar(
            params = CameraTopBarParams(
                state = CameraToolbarState(
                    uiState.cameraMode, uiState.flashMode, uiState.videoQuality,
                    uiState.aspectRatio, uiState.isGridVisible, uiState.showSettingsPanel, iconRotateAngle
                ),
                actions = CameraToolbarActions(
                    onFlashChange = { cameraViewModel.setFlashMode(it) },
                    onAspectChange = { cameraViewModel.setAspectRatio(it) },
                    onGridChange = { cameraViewModel.setGridVisible(it) },
                    onSettingsChange = { cameraViewModel.setShowSettingsPanel(it) },
                    onQualityChange = { cameraViewModel.setVideoQuality(it) },
                    onClose = onClose
                ),
                cameraState = cameraState,
                settingsState = CameraSettingsState(enableOptimization, enableAvif, onToggleOptimization, onToggleAvif),
                isLandscape = isLandscape
            ),
            modifier = Modifier.fillMaxSize()
        )

        if (uiState.isRecording) recordingIndicator(uiState.recordingDuration, Modifier.align(Alignment.TopCenter))

        cameraBottomControls(params = ControlParams(uiState, cameraState, cameraViewModel, iconRotateAngle, isLandscape),
            actions = ControlActions(triggerShutter, onClose, activeRecording), modifier = Modifier.align(if (isLandscape) Alignment.CenterEnd else Alignment.BottomCenter))
    }
}

@Composable
private fun rememberDeviceAngle(context: Context): Float {
    var deviceAngle by remember { mutableFloatStateOf(0f) }
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val angle = Math.toDegrees(kotlin.math.atan2(event.values[0].toDouble(), event.values[1].toDouble())).toFloat()
                    deviceAngle = -angle
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                /* Not needed for leveler */
            }
        }
        if (accelerometer != null) sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager.unregisterListener(listener) }
    }
    return deviceAngle
}

@Composable
private fun rememberCameraRotation(context: Context, cameraState: CameraStateHolder): Int {
    var currentRotation by remember { mutableIntStateOf(Surface.ROTATION_0) }
    DisposableEffect(Unit) {
        val listener = object : OrientationEventListener(context.applicationContext) {
            override fun onOrientationChanged(orientation: Int) {
                val rot = when (orientation) { in 45..134 -> Surface.ROTATION_270; in 135..224 -> Surface.ROTATION_180; in 225..314 -> Surface.ROTATION_90; else -> Surface.ROTATION_0 }
                if (currentRotation != rot) { currentRotation = rot; cameraState.updateTargetRotation(rot) }
            }
        }
        listener.enable()
        onDispose { listener.disable() }
    }
    return currentRotation
}

@Composable
private fun rememberIconRotation(currentRotation: Int): Float {
    return animateFloatAsState(
        targetValue = when (currentRotation) { Surface.ROTATION_90 -> -90f; Surface.ROTATION_180 -> -180f; Surface.ROTATION_270 -> 90f; else -> 0f },
        animationSpec = spring(stiffness = Spring.StiffnessLow), label = "iconRotation"
    ).value
}

data class EffectsConfig(val enableOptimization: Boolean, val currentRotation: Int, val previewView: PreviewView, val audioLauncher: ManagedActivityResultLauncher<String, Boolean>)
data class EffectsParams(val context: Context, val uiState: com.fatihenes.photoreport.feature.camera.model.CameraUiState, val cameraState: CameraStateHolder, val cameraViewModel: CameraViewModel, val config: EffectsConfig)

@Composable
private fun CameraEffects(params: EffectsParams) {
    LaunchedEffect(params.uiState.showExposure) { if (params.uiState.showExposure) { delay(4000.milliseconds); params.cameraViewModel.setShowExposure(false) } }
    LaunchedEffect(params.uiState.cameraMode) {
        if (params.uiState.cameraMode == "VIDEO") {
            val p = android.Manifest.permission.RECORD_AUDIO
            if (ContextCompat.checkSelfPermission(params.context, p) != android.content.pm.PackageManager.PERMISSION_GRANTED) params.config.audioLauncher.launch(p)
        }
    }
    LaunchedEffect(params.uiState.lensFacing, params.uiState.aspectRatio, params.uiState.cameraMode, params.uiState.videoQuality, params.config.enableOptimization) {
        params.cameraState.bindCamera(params.config.previewView, CameraConfig(params.uiState.lensFacing, params.uiState.aspectRatio, params.uiState.cameraMode, params.uiState.videoQuality, params.config.currentRotation, params.uiState.flashMode, params.config.enableOptimization))
        params.cameraViewModel.setZoomRatio(1f); params.cameraViewModel.setExposureValue(0f)
    }
    LaunchedEffect(params.uiState.isRecording) { if (params.uiState.isRecording) { while (isActive) { delay(1000.milliseconds); params.cameraViewModel.incrementRecordingDuration() } } }
}

data class ShutterState(val uiState: com.fatihenes.photoreport.feature.camera.model.CameraUiState, val cameraViewModel: CameraViewModel, val audioLauncher: ManagedActivityResultLauncher<String, Boolean>, val actions: ShutterActions)
data class ShutterParams(val context: Context, val scope: CoroutineScope, val snackbarHost: SnackbarHostState, val cameraState: CameraStateHolder, val state: ShutterState)

private fun performShutterAction(params: ShutterParams, haptic: HapticFeedback) {
    if (!params.cameraState.isBound) { params.scope.launch { params.snackbarHost.showSnackbar(params.context.getString(R.string.camera_preparing)) }; return }
    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    if (params.state.uiState.cameraMode == "PHOTO") handlePhotoShutter(params) else handleVideoShutter(params)
}

private fun handlePhotoShutter(params: ShutterParams) {
    if (params.cameraState.isCapturing) return
    params.cameraState.isCapturing = true; params.state.actions.onCaptureFeedback(true)
    params.scope.launch { delay(100.milliseconds); params.state.actions.onCaptureFeedback(false) }
    takePhoto(params.context, params.cameraState.imageCapture, params.cameraState.executor, onShowError = { err -> params.scope.launch { params.snackbarHost.showSnackbar(err) } }) { uri ->
        params.state.cameraViewModel.onPhotoCaptured(uri); params.state.actions.onPhotoCaptured(uri); params.cameraState.isCapturing = false
    }
}

private fun handleVideoShutter(params: ShutterParams) {
    val active = params.state.actions.getActiveRecording()
    if (params.state.uiState.isRecording) { try { active?.stop() } catch (e: Exception) { Log.e("CameraScreen", "Stop failed", e) }; params.state.actions.setActiveRecording(null)
    } else params.cameraState.videoCapture?.let { startVideoRecording(it, params) }
}

private fun startVideoRecording(vc: androidx.camera.video.VideoCapture<androidx.camera.video.Recorder>, params: ShutterParams) {
    val opts = PhotoManager.getVideoOutputOptions(params.context)
    val pending = vc.output.prepareRecording(params.context, opts)
    if (ContextCompat.checkSelfPermission(params.context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
        try { pending.withAudioEnabled() } catch (e: SecurityException) { Log.e("CameraScreen", "Audio error", e) }
    } else params.state.audioLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
    params.state.cameraViewModel.setIsRecording(true)
    val recording = pending.start(ContextCompat.getMainExecutor(params.context)) { ev ->
        if (ev is VideoRecordEvent.Finalize) {
            params.state.cameraViewModel.setIsRecording(false)
            if (!ev.hasError()) {
                PhotoManager.commitPendingMediaStoreUri(params.context, ev.outputResults.outputUri)
                params.state.cameraViewModel.onPhotoCaptured(ev.outputResults.outputUri)
                params.state.actions.onPhotoCaptured(ev.outputResults.outputUri)
            }
        }
    }
    params.state.actions.setActiveRecording(recording)
}

data class ShutterActions(val onCaptureFeedback: (Boolean) -> Unit, val onPhotoCaptured: (Uri) -> Unit, val getActiveRecording: () -> Recording?, val setActiveRecording: (Recording?) -> Unit)

private fun takePhoto(context: Context, imageCapture: ImageCapture?, executor: java.util.concurrent.ExecutorService, onShowError: (String) -> Unit, onPhotoCaptured: (Uri) -> Unit) {
    if (imageCapture == null) { onShowError(context.getString(R.string.camera_not_ready)); return }
    val opts = PhotoManager.getCaptureOutputOptions(context)
    val mainExec = ContextCompat.getMainExecutor(context)
    imageCapture.takePicture(opts, executor, object : ImageCapture.OnImageSavedCallback {
        override fun onError(exc: ImageCaptureException) { mainExec.execute { onShowError(context.getString(R.string.camera_save_failed, exc.message ?: "")) } }
        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
            mainExec.execute {
                output.savedUri?.let { uri ->
                    PhotoManager.commitPendingMediaStoreUri(context, uri)
                    onPhotoCaptured(uri)
                }
            }
        }
    })
}
