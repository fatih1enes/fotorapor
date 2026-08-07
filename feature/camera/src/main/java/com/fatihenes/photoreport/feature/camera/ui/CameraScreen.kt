@file:Suppress("LocalContextGetResourceValueCall")
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
import androidx.camera.core.*
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.fatihenes.photoreport.core.media.PhotoManager
import com.fatihenes.photoreport.core.ui.R
import com.fatihenes.photoreport.core.ui.navigation.LocalSnackbarHostState
import com.fatihenes.photoreport.feature.camera.model.CameraUiState
import com.fatihenes.photoreport.feature.camera.ui.components.*
import com.fatihenes.photoreport.feature.camera.viewmodel.CameraViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private val Amber = Color(0xFFFFD60A)

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
    DisposableEffect(Unit) { onDispose { activeRecording?.stop() } }

    val uiState by cameraViewModel.uiState.collectAsStateWithLifecycle()
    var showCaptureFeedback by remember { mutableStateOf(false) }
    var tapOffset by remember { mutableStateOf<Offset?>(null) }

    val deviceAngle = rememberDeviceAngle(context)
    val currentRotation = rememberCameraRotation(context, cameraState)

    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    CameraEffects(context, uiState, cameraState, cameraViewModel, enableOptimization, currentRotation, previewView, audioLauncher)

    val iconRotateAngle = rememberIconRotation(currentRotation)
    val triggerShutter = {
        performShutterAction(context, scope, snackbarHost, cameraState, uiState, cameraViewModel, haptic, { showCaptureFeedback = it }, onPhotoCaptured, audioLauncher, { activeRecording }, { activeRecording = it })
    }

    val isLandscape = (currentRotation == Surface.ROTATION_90) || (currentRotation == Surface.ROTATION_270)
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black).focusRequester(focusRequester).focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && (event.key == Key.VolumeUp || event.key == Key.VolumeDown)) {
                    triggerShutter()
                    true
                } else false
            }
    ) {
        CameraPreviewArea(cameraState, uiState, previewView, deviceAngle, tapOffset, { cameraViewModel.setZoomRatio(it) }, {
            tapOffset = it
            cameraViewModel.setExposureValue(0f)
            cameraState.focusAndMeter(it, previewView)
        }, {
            scope.launch { cameraState.bindCamera(previewView, uiState.lensFacing, uiState.aspectRatio, uiState.cameraMode, uiState.videoQuality, currentRotation, uiState.flashMode) }
        }, Modifier.align(Alignment.Center))

        CaptureFeedbackOverlay(showCaptureFeedback)
        CameraTopBar(uiState, cameraState, cameraViewModel, isLandscape, iconRotateAngle, enableOptimization, enableAvif, onToggleOptimization, onToggleAvif, onClose, Modifier.fillMaxSize())
        if (uiState.isRecording) RecordingIndicator(uiState.recordingDuration, Modifier.align(Alignment.TopCenter))
        CameraBottomControls(uiState, cameraState, cameraViewModel, isLandscape, iconRotateAngle, activeRecording, triggerShutter, onClose, Modifier.align(if (isLandscape) Alignment.CenterEnd else Alignment.BottomCenter))
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
                    val ax = event.values[0]
                    val ay = event.values[1]
                    val angle = Math.toDegrees(kotlin.math.atan2(ax.toDouble(), ay.toDouble())).toFloat()
                    deviceAngle = -angle
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (accelerometer != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }
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
                val rot = when (orientation) {
                    in 45..134 -> Surface.ROTATION_270
                    in 135..224 -> Surface.ROTATION_180
                    in 225..314 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
                if (currentRotation != rot) {
                    currentRotation = rot
                    cameraState.updateTargetRotation(rot)
                }
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
        targetValue = when (currentRotation) {
            Surface.ROTATION_90 -> -90f
            Surface.ROTATION_180 -> -180f
            Surface.ROTATION_270 -> 90f
            else -> 0f
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "iconRotation"
    ).value
}

@Composable
private fun CameraEffects(
    context: Context,
    uiState: CameraUiState,
    cameraState: CameraStateHolder,
    cameraViewModel: CameraViewModel,
    enableOptimization: Boolean,
    currentRotation: Int,
    previewView: PreviewView,
    audioLauncher: ManagedActivityResultLauncher<String, Boolean>
) {
    LaunchedEffect(uiState.showExposure) {
        if (uiState.showExposure) {
            delay(4000.milliseconds)
            cameraViewModel.setShowExposure(false)
        }
    }

    LaunchedEffect(uiState.cameraMode) {
        if (uiState.cameraMode == "VIDEO") {
            val p = android.Manifest.permission.RECORD_AUDIO
            if (ContextCompat.checkSelfPermission(context, p) != android.content.pm.PackageManager.PERMISSION_GRANTED)
                audioLauncher.launch(p)
        }
    }

    LaunchedEffect(uiState.lensFacing, uiState.aspectRatio, uiState.cameraMode, uiState.videoQuality, enableOptimization) {
        cameraState.bindCamera(previewView, uiState.lensFacing, uiState.aspectRatio, uiState.cameraMode, uiState.videoQuality, currentRotation, uiState.flashMode, enableOptimization)
        cameraViewModel.setZoomRatio(1f)
        cameraViewModel.setExposureValue(0f)
    }

    LaunchedEffect(uiState.isRecording) {
        if (uiState.isRecording) {
            while (isActive) {
                delay(1000.milliseconds)
                cameraViewModel.incrementRecordingDuration()
            }
        }
    }
}

private fun performShutterAction(
    context: Context,
    scope: CoroutineScope,
    snackbarHost: SnackbarHostState,
    cameraState: CameraStateHolder,
    uiState: CameraUiState,
    cameraViewModel: CameraViewModel,
    haptic: HapticFeedback,
    onCaptureFeedback: (Boolean) -> Unit,
    onPhotoCaptured: (Uri) -> Unit,
    audioLauncher: ManagedActivityResultLauncher<String, Boolean>,
    getActiveRecording: () -> Recording?,
    setActiveRecording: (Recording?) -> Unit
) {
    if (!cameraState.isBound) {
        scope.launch { snackbarHost.showSnackbar(context.getString(R.string.camera_preparing)) }
        return
    }

    haptic.performHapticFeedback(HapticFeedbackType.LongPress)

    if (uiState.cameraMode == "PHOTO") {
        if (!cameraState.isCapturing) {
            cameraState.isCapturing = true
            onCaptureFeedback(true)
            scope.launch { delay(100.milliseconds); onCaptureFeedback(false) }

            takePhoto(context, cameraState.imageCapture, cameraState.executor, onShowError = { err -> scope.launch { snackbarHost.showSnackbar(err) } }) { uri ->
                cameraViewModel.onPhotoCaptured(uri)
                onPhotoCaptured(uri)
                cameraState.isCapturing = false
            }
        }
    } else {
        val activeRecording = getActiveRecording()
        if (uiState.isRecording) {
            try { activeRecording?.stop() } catch (e: Exception) { Log.e("CameraScreen", "Stop failed", e) }
            setActiveRecording(null)
        } else {
            cameraState.videoCapture?.let { vc ->
                val opts = PhotoManager.getVideoOutputOptions(context)
                val hasAudio = ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
                val pending = vc.output.prepareRecording(context, opts)
                if (hasAudio) { try { pending.withAudioEnabled() } catch (_: SecurityException) {} }
                else audioLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                cameraViewModel.setIsRecording(true)
                setActiveRecording(pending.start(ContextCompat.getMainExecutor(context)) { ev ->
                    if (ev is VideoRecordEvent.Finalize) {
                        cameraViewModel.setIsRecording(false)
                        if (!ev.hasError()) {
                            cameraViewModel.onPhotoCaptured(ev.outputResults.outputUri)
                            onPhotoCaptured(ev.outputResults.outputUri)
                        } else Log.e("CameraScreen", "Video err: ${ev.error}")
                    }
                })
            }
        }
    }
}

@Composable
private fun CameraPreviewArea(
    cameraState: CameraStateHolder,
    uiState: CameraUiState,
    previewView: PreviewView,
    deviceAngle: Float,
    tapOffset: Offset?,
    onZoomChanged: (Float) -> Unit,
    onFocusRequested: (Offset) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val previewAspect = if (uiState.aspectRatio == AspectRatio.RATIO_4_3) 3f / 4f else 9f / 16f

    Box(modifier = modifier.fillMaxWidth().aspectRatio(previewAspect).clip(RoundedCornerShape(12.dp))) {
        if (cameraState.initializationError != null) {
            CameraErrorIndicator(cameraState.initializationError!!, onRetry)
        } else {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
                    .pointerInput(cameraState.camera) {
                        detectTransformGestures { _, _, zoom, _ ->
                            val nz = (uiState.zoomRatio * zoom).coerceIn(cameraState.minZoom, cameraState.maxZoom)
                            if (kotlin.math.floor(uiState.zoomRatio.toDouble()) != kotlin.math.floor(nz.toDouble())) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onZoomChanged(nz)
                            cameraState.setZoom(nz)
                        }
                    }
                    .pointerInput(cameraState.camera) { detectTapGestures { onFocusRequested(it) } }
            )
        }
        if (uiState.isGridVisible) GridOverlay()
        LevelerOverlay(deviceAngle)
        tapOffset?.let { FocusRing(it) }
    }
}

@Composable
private fun CameraErrorIndicator(error: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${stringResource(R.string.camera_error_prefix)} $error", color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRetry) { Text(stringResource(R.string.camera_retry_btn)) }
        }
    }
}

@Composable
private fun CaptureFeedbackOverlay(visible: Boolean) {
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(50)), exit = fadeOut(tween(150)), modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.5f)))
    }
}

@Composable
private fun CameraTopBar(
    uiState: CameraUiState,
    cameraState: CameraStateHolder,
    cameraViewModel: CameraViewModel,
    isLandscape: Boolean,
    iconRotateAngle: Float,
    enableOptimization: Boolean,
    enableAvif: Boolean,
    onToggleOptimization: (Boolean) -> Unit,
    onToggleAvif: (Boolean) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        AnimatedVisibility(visible = !uiState.isRecording, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(if (isLandscape) Alignment.CenterStart else Alignment.TopCenter)) {
            val toolbarPadding = if (isLandscape) Modifier.padding(start = 16.dp) else Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)
            if (isLandscape) {
                Column(modifier = toolbarPadding.width(60.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    ToolbarItems(uiState.cameraMode, uiState.flashMode, uiState.videoQuality, uiState.aspectRatio, uiState.isGridVisible, uiState.showSettingsPanel, iconRotateAngle, cameraState, { cameraViewModel.setFlashMode(it) }, { cameraViewModel.setAspectRatio(it) }, { cameraViewModel.setGridVisible(it) }, { cameraViewModel.setShowSettingsPanel(it) }, { cameraViewModel.setVideoQuality(it) }, onClose)
                }
            } else {
                Row(modifier = toolbarPadding.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    ToolbarBtn(rotation = iconRotateAngle, onClick = onClose) { Icon(Icons.Default.Close, stringResource(R.string.close_label), tint = Color.White, modifier = Modifier.size(22.dp)) }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        ToolbarItems(uiState.cameraMode, uiState.flashMode, uiState.videoQuality, uiState.aspectRatio, uiState.isGridVisible, uiState.showSettingsPanel, iconRotateAngle, cameraState, { cameraViewModel.setFlashMode(it) }, { cameraViewModel.setAspectRatio(it) }, { cameraViewModel.setGridVisible(it) }, { cameraViewModel.setShowSettingsPanel(it) }, { cameraViewModel.setVideoQuality(it) }, null)
                    }
                }
            }
        }

        SettingsOverlay(uiState.showSettingsPanel && !uiState.isRecording, enableOptimization, enableAvif, onToggleOptimization, onToggleAvif, Modifier.align(Alignment.TopEnd))
    }
}

@Composable
private fun SettingsOverlay(visible: Boolean, enableOpt: Boolean, enableAvif: Boolean, onToggleOpt: (Boolean) -> Unit, onToggleAvif: (Boolean) -> Unit, modifier: Modifier) {
    AnimatedVisibility(visible = visible, enter = fadeIn() + expandVertically(expandFrom = Alignment.Top), exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top), modifier = modifier.statusBarsPadding().padding(top = 60.dp, end = 16.dp)) {
        Column(modifier = Modifier.width(220.dp).clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(alpha = 0.6f)).padding(16.dp)) {
            Text(stringResource(R.string.camera_settings_title), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(bottom = 8.dp))
            SettingsRow(stringResource(R.string.camera_hdr_title), stringResource(R.string.camera_hdr_desc), enableOpt, onToggleOpt)
            SettingsRow(stringResource(R.string.camera_avif_title), stringResource(R.string.camera_avif_desc), enableAvif, onToggleAvif)
        }
    }
}

@Composable
private fun SettingsRow(title: String, desc: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(desc, color = Color.LightGray, fontSize = 10.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.scale(0.8f))
    }
}

@Composable
private fun RecordingIndicator(duration: Int, modifier: Modifier) {
    val durText = remember(duration) { String.format(java.util.Locale.US, "%02d:%02d", duration / 60, duration % 60) }
    Row(modifier = modifier.statusBarsPadding().padding(top = 12.dp).background(Color.Red.copy(alpha = 0.9f), RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(Color.White, CircleShape)); Spacer(Modifier.width(8.dp))
        Text(durText, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CameraBottomControls(
    uiState: CameraUiState,
    cameraState: CameraStateHolder,
    cameraViewModel: CameraViewModel,
    isLandscape: Boolean,
    iconRotateAngle: Float,
    activeRecording: Recording?,
    triggerShutter: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val controlsModifier = if (isLandscape) Modifier.padding(end = 12.dp).fillMaxHeight().width(100.dp) else Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp)
    Box(modifier = modifier.then(controlsModifier).clip(RoundedCornerShape(if (isLandscape) 24.dp else 0.dp)).background(Color.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
        if (isLandscape) {
            LandscapeControls(uiState, cameraViewModel, activeRecording, iconRotateAngle, triggerShutter, onClose)
        } else {
            PortraitControls(uiState, cameraState, cameraViewModel, activeRecording, iconRotateAngle, triggerShutter, onClose)
        }
    }
}

@Composable
private fun LandscapeControls(uiState: CameraUiState, cameraViewModel: CameraViewModel, activeRecording: Recording?, rotation: Float, onShutter: () -> Unit, onClose: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxHeight().padding(vertical = 20.dp)) {
        if (uiState.isRecording) {
            ToolbarBtn(rotation = rotation, onClick = { if (uiState.isPaused) { activeRecording?.resume(); cameraViewModel.setIsPaused(false) } else { activeRecording?.pause(); cameraViewModel.setIsPaused(true) } }) { Icon(if (uiState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, null, tint = Color.White) }
        } else {
            ToolbarBtn(rotation = rotation, onClick = { cameraViewModel.toggleLensFacing() }) { Icon(Icons.Default.Cameraswitch, null, tint = Color.White) }
        }
        ShutterButton(isVideo = uiState.cameraMode == "VIDEO", isRecording = uiState.isRecording, onClick = onShutter)
        LastCapturedPreview(uiState.lastCapturedUri, uiState.sessionPhotoCount, onClose)
    }
}

@Composable
private fun PortraitControls(uiState: CameraUiState, cameraState: CameraStateHolder, cameraViewModel: CameraViewModel, activeRecording: Recording?, rotation: Float, onShutter: () -> Unit, onClose: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ZoomAndExposureControls(uiState, cameraState, cameraViewModel, rotation)
        if (!uiState.isRecording) {
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp, top = 4.dp)) {
                ModeText(stringResource(R.string.camera_mode_photo), uiState.cameraMode == "PHOTO", rotation) { cameraViewModel.setCameraMode("PHOTO") }
                Spacer(Modifier.width(32.dp))
                ModeText(stringResource(R.string.camera_mode_video), uiState.cameraMode == "VIDEO", rotation) { cameraViewModel.setCameraMode("VIDEO") }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 44.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            LastCapturedPreview(uiState.lastCapturedUri, uiState.sessionPhotoCount, onClose)
            ShutterButton(isVideo = uiState.cameraMode == "VIDEO", isRecording = uiState.isRecording, onClick = onShutter)
            if (uiState.isRecording) {
                ToolbarBtn(rotation = rotation, onClick = { if (uiState.isPaused) { activeRecording?.resume(); cameraViewModel.setIsPaused(false) } else { activeRecording?.pause(); cameraViewModel.setIsPaused(true) } }) { Icon(if (uiState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, null, tint = Color.White) }
            } else {
                ToolbarBtn(rotation = rotation, onClick = { cameraViewModel.toggleLensFacing() }) { Icon(Icons.Default.Cameraswitch, null, tint = Color.White) }
            }
        }
    }
}

@Composable
private fun ZoomAndExposureControls(uiState: CameraUiState, cameraState: CameraStateHolder, cameraViewModel: CameraViewModel, rotation: Float) {
    AnimatedVisibility(visible = uiState.showExposure) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.WbSunny, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
            Slider(value = uiState.exposureValue, onValueChange = { cameraViewModel.setExposureValue(it); cameraState.setExposure(it.toInt()); cameraViewModel.setShowExposure(true) }, valueRange = cameraState.exposureRange, modifier = Modifier.weight(1f).padding(horizontal = 8.dp), colors = SliderDefaults.colors(thumbColor = Amber, activeTrackColor = Amber.copy(alpha = 0.6f), inactiveTrackColor = Color.White.copy(alpha = 0.2f)))
            Icon(Icons.Default.WbSunny, null, tint = Amber, modifier = Modifier.size(20.dp))
        }
    }
    if (cameraState.exposureIndex != 0) Text("EV ${if (cameraState.exposureIndex > 0) "+" else ""}${cameraState.exposureIndex}", color = Amber, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 2.dp))
    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
        ZoomPill("☀", uiState.showExposure, rotation) { cameraViewModel.setShowExposure(!uiState.showExposure) }
        Spacer(Modifier.width(12.dp))
        if (cameraState.maxZoom > cameraState.minZoom) {
            if (cameraState.minZoom < 1f) {
                ZoomPill(String.format(java.util.Locale.US, "%.1fx", cameraState.minZoom), uiState.zoomRatio < 0.9f, rotation) { cameraViewModel.setZoomRatio(cameraState.minZoom); cameraState.setZoom(cameraState.minZoom) }
                Spacer(Modifier.width(8.dp))
            }
            ZoomPill("1x", uiState.zoomRatio in 0.9f..1.1f, rotation) { cameraViewModel.setZoomRatio(1f); cameraState.setZoom(1f) }
            if (cameraState.maxZoom >= 2f) { Spacer(Modifier.width(8.dp)); ZoomPill("2x", uiState.zoomRatio in 1.9f..2.1f, rotation) { cameraViewModel.setZoomRatio(2f); cameraState.setZoom(2f) } }
            if (cameraState.maxZoom >= 5f) { Spacer(Modifier.width(8.dp)); ZoomPill("5x", uiState.zoomRatio in 4.9f..5.1f, rotation) { cameraViewModel.setZoomRatio(5f); cameraState.setZoom(5f) } }
        }
    }
}

@Composable
private fun LastCapturedPreview(uri: Uri?, count: Int, onClick: () -> Unit) {
    if (uri != null) {
        Box {
            AsyncImage(model = uri, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.size(44.dp).clip(CircleShape).border(1.dp, Color.White, CircleShape).clickable { onClick() })
            if (count > 0) {
                Box(Modifier.align(Alignment.TopEnd).offset(4.dp, (-4).dp).background(Amber, CircleShape).padding(horizontal = 4.dp)) {
                    Text(count.toString(), color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    } else Spacer(Modifier.size(44.dp))
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture?,
    executor: java.util.concurrent.ExecutorService,
    onShowError: (String) -> Unit,
    onPhotoCaptured: (Uri) -> Unit
) {
    if (imageCapture == null) {
        onShowError(context.getString(R.string.camera_not_ready))
        return
    }
    val opts = PhotoManager.getCaptureOutputOptions(context)
    val mainExec = ContextCompat.getMainExecutor(context)
    imageCapture.takePicture(opts, executor, object : ImageCapture.OnImageSavedCallback {
        override fun onError(exc: ImageCaptureException) {
            Log.e("CameraScreen", "Capture failed", exc)
            mainExec.execute { onShowError(context.getString(R.string.camera_save_failed, exc.message ?: "")) }
        }
        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
            val finalUri = output.savedUri
            mainExec.execute { finalUri?.let { onPhotoCaptured(it) } }
        }
    })
}
