package com.fatihenes.photoreport.ui

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.isActive
import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.OrientationEventListener
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import coil3.compose.AsyncImage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.fatihenes.photoreport.R
import com.fatihenes.photoreport.util.PhotoManager
import com.fatihenes.photoreport.ui.camera.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.fatihenes.photoreport.ui.viewmodel.CameraViewModel

private val Amber = Color(0xFFFFD60A)
private val ControlBg = Color(0x66000000)

@Composable
fun CameraScreen(
    onPhotoCaptured: (Uri) -> Unit,
    onClose: () -> Unit,
    enableOptimization: Boolean = true,
    enableAvif: Boolean = true,
    onToggleOptimization: (Boolean) -> Unit = {},
    onToggleAvif: (Boolean) -> Unit = {},
    cameraViewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val cameraState = rememberCameraStateHolder()
    var activeRecording: Recording? by remember { mutableStateOf(null) }
    DisposableEffect(Unit) { onDispose { activeRecording?.stop() } }

    val uiState by cameraViewModel.uiState.collectAsStateWithLifecycle()

    // Local UI states (view-specific / gestures / sensors)
    var showCaptureFeedback by remember { mutableStateOf(false) }
    var tapOffset by remember { mutableStateOf<Offset?>(null) }
    var currentRotation by remember { mutableIntStateOf(Surface.ROTATION_0) }

    // Leveler State
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
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    // Auto-hide exposure
    LaunchedEffect(uiState.showExposure, uiState.exposureValue) {
        if (uiState.showExposure) { delay(4000.milliseconds); cameraViewModel.setShowExposure(false) }
    }

    // Orientation listener
    DisposableEffect(Unit) {
        val listener = object : OrientationEventListener(context.applicationContext) {
            override fun onOrientationChanged(orientation: Int) {
                val rot = when (orientation) {
                    in 45..134 -> Surface.ROTATION_270
                    in 135..224 -> Surface.ROTATION_180
                    in 225..314 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
                if (currentRotation != rot) { currentRotation = rot; cameraState.updateTargetRotation(rot) }
            }
        }
        listener.enable()
        onDispose { listener.disable() }
    }

    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }

    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    LaunchedEffect(uiState.cameraMode) {
        if (uiState.cameraMode == "VIDEO") {
            val p = android.Manifest.permission.RECORD_AUDIO
            if (ContextCompat.checkSelfPermission(context, p) != android.content.pm.PackageManager.PERMISSION_GRANTED)
                audioLauncher.launch(p)
        }
    }

    LaunchedEffect(uiState.lensFacing, uiState.aspectRatio, uiState.cameraMode, uiState.videoQuality, enableOptimization) {
        cameraState.bindCamera(
            previewView = previewView,
            lensFacing = uiState.lensFacing,
            aspectRatio = uiState.aspectRatio,
            cameraMode = uiState.cameraMode,
            videoQuality = uiState.videoQuality,
            currentRotation = currentRotation,
            flashMode = uiState.flashMode,
            enableOptimization = enableOptimization,
        )
        cameraViewModel.setZoomRatio(1f)
        cameraViewModel.setExposureValue(0f)
    }

    LaunchedEffect(uiState.isRecording) {
        if (uiState.isRecording) {
            while (uiState.isRecording && isActive) {
                delay(1000.milliseconds)
                cameraViewModel.incrementRecordingDuration()
            }
        }
    }

    val durText = remember(uiState.recordingDuration) {
        String.format(java.util.Locale.US, "%02d:%02d", uiState.recordingDuration / 60, uiState.recordingDuration % 60)
    }

    val previewAspect = if (uiState.aspectRatio == AspectRatio.RATIO_4_3) 3f / 4f else 9f / 16f

    // Orientation helpers
    val isLandscape = (currentRotation == Surface.ROTATION_90) || (currentRotation == Surface.ROTATION_270)
    val iconRotateAngle by animateFloatAsState(
        targetValue = when (currentRotation) {
            Surface.ROTATION_90 -> -90f
            Surface.ROTATION_180 -> -180f
            Surface.ROTATION_270 -> 90f
            else -> 0f
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "iconRotation"
    )

    val triggerShutter = {
        // Bind-guard: prevent capture when camera is not ready
        if (!cameraState.isBound) {
            android.widget.Toast.makeText(context, context.getString(R.string.camera_preparing), android.widget.Toast.LENGTH_SHORT).show()
        } else if (uiState.cameraMode == "PHOTO") {
            if (!cameraState.isCapturing) {
                cameraState.isCapturing = true
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                // Front camera flash overlay logic
                if (uiState.lensFacing == CameraSelector.LENS_FACING_FRONT && uiState.flashMode != ImageCapture.FLASH_MODE_OFF) {
                    showCaptureFeedback = true
                    scope.launch { delay(250.milliseconds); showCaptureFeedback = false }
                } else {
                    showCaptureFeedback = true
                    scope.launch { delay(100.milliseconds); showCaptureFeedback = false }
                }

                takePhoto(context, cameraState.imageCapture, cameraState.executor) { uri ->
                    cameraViewModel.onPhotoCaptured(uri)
                    onPhotoCaptured(uri)
                    cameraState.isCapturing = false
                }
            }
        } else {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (uiState.isRecording) { activeRecording?.stop(); activeRecording = null }
            else {
                cameraState.videoCapture?.let { vc ->
                    val opts = PhotoManager.getVideoOutputOptions(context)
                    val hasAudio = ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    val pending = vc.output.prepareRecording(context, opts)
                    if (hasAudio) { try { pending.withAudioEnabled() } catch (_: SecurityException) {} }
                    else audioLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                    cameraViewModel.setIsRecording(true)
                    activeRecording = pending.start(ContextCompat.getMainExecutor(context)) { ev ->
                        if (ev is VideoRecordEvent.Finalize) {
                            cameraViewModel.setIsRecording(false)
                            val uri = ev.outputResults.outputUri
                            if (!ev.hasError()) {
                                cameraViewModel.onPhotoCaptured(uri)
                                onPhotoCaptured(uri)
                            }
                            else Log.e("CameraScreen", "Video err: ${ev.error}")
                        }
                    }
                }
            }
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    // ========== LAYOUT (Box overlay approach like Samsung/iPhone) ==========
    Box(Modifier.fillMaxSize().background(Color.Black)
        .focusRequester(focusRequester)
        .focusable()
        .onKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown && (event.key == Key.VolumeUp || event.key == Key.VolumeDown)) {
                triggerShutter()
                true
            } else false
        }
    ) {

        // 1) CAMERA PREVIEW — centered, aspect-ratio constrained
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(previewAspect).align(Alignment.Center).clip(RoundedCornerShape(12.dp))
        ) {
            if (cameraState.initializationError != null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${stringResource(R.string.camera_error_prefix)} ${cameraState.initializationError}",
                            color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { scope.launch { cameraState.bindCamera(previewView, uiState.lensFacing, uiState.aspectRatio, uiState.cameraMode, uiState.videoQuality, currentRotation, uiState.flashMode) } }) {
                            Text(stringResource(R.string.camera_retry_btn))
                        }
                    }
                }
            } else {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                        .pointerInput(cameraState.camera) {
                            detectTransformGestures { _, _, zoom, _ ->
                                val nz = (uiState.zoomRatio * zoom).coerceIn(cameraState.minZoom, cameraState.maxZoom)
                                if (kotlin.math.floor(uiState.zoomRatio) != kotlin.math.floor(nz)) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                cameraViewModel.setZoomRatio(nz)
                                cameraState.setZoom(nz)
                            }
                        }
                        .pointerInput(cameraState.camera) {
                            detectTapGestures {
                                tapOffset = it
                                cameraViewModel.setExposureValue(0f)
                                cameraState.focusAndMeter(it, previewView)
                            }
                        }
                )
            }
            if (uiState.isGridVisible) GridOverlay()
            LevelerOverlay(deviceAngle)
            tapOffset?.let { FocusRing(it) }
        }

        // 2) CAPTURE FLASH
        AnimatedVisibility(
            visible = showCaptureFeedback, enter = fadeIn(tween(50)), exit = fadeOut(tween(150)),
            modifier = Modifier.fillMaxSize()
        ) { Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.5f))) }

        // 3) TOP TOOLBAR — always accessible, overlays on top
        AnimatedVisibility(
            visible = !uiState.isRecording, enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(if (isLandscape) Alignment.CenterStart else Alignment.TopCenter)
        ) {
            val toolbarPadding = if (isLandscape) Modifier.padding(start = 16.dp) else Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)
            val containerModifier = if (isLandscape) Modifier.width(60.dp) else Modifier.fillMaxWidth()

                    if (isLandscape) {
                        Column(
                            modifier = toolbarPadding.then(containerModifier),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ToolbarItems(
                                uiState.cameraMode, uiState.flashMode, uiState.videoQuality, uiState.aspectRatio, uiState.isGridVisible, uiState.showSettingsPanel, iconRotateAngle, cameraState,
                                { cameraViewModel.setFlashMode(it) }, { cameraViewModel.setAspectRatio(it) }, { cameraViewModel.setGridVisible(it) }, { cameraViewModel.setShowSettingsPanel(it) }, { cameraViewModel.setVideoQuality(it) }, onClose
                            )
                        }
                    } else {
                        Row(
                            modifier = toolbarPadding.then(containerModifier),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ToolbarBtn(rotation = iconRotateAngle, onClick = onClose) {
                                Icon(Icons.Default.Close, stringResource(R.string.close_label), tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                ToolbarItems(
                                    uiState.cameraMode, uiState.flashMode, uiState.videoQuality, uiState.aspectRatio, uiState.isGridVisible, uiState.showSettingsPanel, iconRotateAngle, cameraState,
                                    { cameraViewModel.setFlashMode(it) }, { cameraViewModel.setAspectRatio(it) }, { cameraViewModel.setGridVisible(it) }, { cameraViewModel.setShowSettingsPanel(it) }, { cameraViewModel.setVideoQuality(it) }, null
                                )
                            }
                        }
                    }
        }

        // 3.5) SETTINGS PANEL (Glassmorphism overlay)
        AnimatedVisibility(
            visible = uiState.showSettingsPanel && !uiState.isRecording,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 60.dp, end = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .width(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(16.dp)
            ) {
                Text(stringResource(R.string.camera_settings_title), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(bottom = 8.dp))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.camera_hdr_title), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.camera_hdr_desc), color = Color.LightGray, fontSize = 10.sp)
                    }
                    Switch(checked = enableOptimization, onCheckedChange = onToggleOptimization, modifier = Modifier.scale(0.8f))
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.camera_avif_title), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.camera_avif_desc), color = Color.LightGray, fontSize = 10.sp)
                    }
                    Switch(checked = enableAvif, onCheckedChange = onToggleAvif, modifier = Modifier.scale(0.8f))
                }
            }
        }

        // 4) RECORDING INDICATOR
        if (uiState.isRecording) {
            Row(
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding()
                    .padding(top = 12.dp)
                    .background(Color.Red.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(8.dp).background(Color.White, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(durText, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }

        // 5) BOTTOM/SIDE CONTROLS — always at bottom or side, overlays preview edge
        val controlsAlignment = if (isLandscape) Alignment.CenterEnd else Alignment.BottomCenter
        val controlsPadding = if (isLandscape) Modifier.padding(end = 12.dp) else Modifier.padding(bottom = 12.dp)

        Box(
            modifier = Modifier.align(controlsAlignment).then(controlsPadding)
                .clip(RoundedCornerShape(if (isLandscape) 24.dp else 0.dp))
                .background(Color.Black.copy(alpha = 0.7f))
                .then(if (isLandscape) Modifier.fillMaxHeight().width(100.dp) else Modifier.fillMaxWidth().navigationBarsPadding()),
            contentAlignment = Alignment.Center
        ) {
            if (isLandscape) {
                // LANDSCAPE UI: Shutter on right, modes vertical
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxHeight()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxHeight().padding(vertical = 20.dp)
                    ) {
                        // Flip / Pause
                        if (uiState.isRecording) {
                            ToolbarBtn(rotation = iconRotateAngle, size = 44, onClick = {
                                if (uiState.isPaused) { activeRecording?.resume(); cameraViewModel.setIsPaused(false) }
                                else { activeRecording?.pause(); cameraViewModel.setIsPaused(true) }
                            }) {
                                Icon(if (uiState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, "Pause", tint = Color.White)
                            }
                        } else {
                            ToolbarBtn(rotation = iconRotateAngle, size = 44, onClick = {
                                cameraViewModel.toggleLensFacing()
                            }) { Icon(Icons.Default.Cameraswitch, null, tint = Color.White) }
                        }

                        // Shutter
                        ShutterButton(isVideo = uiState.cameraMode == "VIDEO", isRecording = uiState.isRecording) { triggerShutter() }

                        // Gallery
                        if (uiState.lastCapturedUri != null) {
                            Box {
                                AsyncImage(
                                    model = uiState.lastCapturedUri, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.size(44.dp).clip(CircleShape).border(1.dp, Color.White, CircleShape).clickable { onClose() }
                                )
                                if (uiState.sessionPhotoCount > 0) {
                                    Box(Modifier.align(Alignment.TopEnd).offset(4.dp, (-4).dp).background(Amber, CircleShape).padding(horizontal = 4.dp)) {
                                        Text(uiState.sessionPhotoCount.toString(), color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else Spacer(Modifier.size(44.dp))
                    }
                }
            } else {
                // PORTRAIT UI: Existing column
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Exposure slider
                    androidx.compose.animation.AnimatedVisibility(visible = uiState.showExposure) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WbSunny, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                            Slider(value = uiState.exposureValue, onValueChange = { cameraViewModel.setExposureValue(it); cameraState.setExposure(it.toInt()); cameraViewModel.setShowExposure(true) }, valueRange = cameraState.exposureRange, modifier = Modifier.weight(1f).padding(horizontal = 8.dp), colors = SliderDefaults.colors(thumbColor = Amber, activeTrackColor = Amber.copy(alpha = 0.6f), inactiveTrackColor = Color.White.copy(alpha = 0.2f)))
                            Icon(Icons.Default.WbSunny, null, tint = Amber, modifier = Modifier.size(20.dp))
                        }
                    }
                    if (cameraState.exposureIndex != 0) {
                        Text("EV ${if (cameraState.exposureIndex > 0) "+" else ""}${cameraState.exposureIndex}", color = Amber, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 2.dp))
                    }
                    // Zoom pills
                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                        ZoomPill("☀", uiState.showExposure, iconRotateAngle) { cameraViewModel.setShowExposure(!uiState.showExposure) }
                        Spacer(Modifier.width(12.dp))
                        if (cameraState.maxZoom > cameraState.minZoom) {
                            if (cameraState.minZoom < 1f) {
                                ZoomPill(String.format(java.util.Locale.US, "%.1fx", cameraState.minZoom), uiState.zoomRatio < 0.9f, iconRotateAngle) { cameraViewModel.setZoomRatio(cameraState.minZoom); cameraState.setZoom(cameraState.minZoom) }
                                Spacer(Modifier.width(8.dp))
                            }
                            ZoomPill("1x", uiState.zoomRatio in 0.9f..1.1f, iconRotateAngle) { cameraViewModel.setZoomRatio(1f); cameraState.setZoom(1f) }
                            if (cameraState.maxZoom >= 2f) { Spacer(Modifier.width(8.dp)); ZoomPill("2x", uiState.zoomRatio in 1.9f..2.1f, iconRotateAngle) { cameraViewModel.setZoomRatio(2f); cameraState.setZoom(2f) } }
                            if (cameraState.maxZoom >= 5f) { Spacer(Modifier.width(8.dp)); ZoomPill("5x", uiState.zoomRatio in 4.9f..5.1f, iconRotateAngle) { cameraViewModel.setZoomRatio(5f); cameraState.setZoom(5f) } }
                        }
                    }
                    // Mode selector
                    androidx.compose.animation.AnimatedVisibility(visible = !uiState.isRecording) {
                        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp, top = 4.dp)) {
                            ModeText(stringResource(R.string.camera_mode_photo), uiState.cameraMode == "PHOTO", iconRotateAngle) { if (!uiState.isRecording) cameraViewModel.setCameraMode("PHOTO") }
                            Spacer(Modifier.width(32.dp))
                            ModeText(stringResource(R.string.camera_mode_video), uiState.cameraMode == "VIDEO", iconRotateAngle) { if (!uiState.isRecording) cameraViewModel.setCameraMode("VIDEO") }
                        }
                    }
                    // Shutter row
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 44.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        if (uiState.lastCapturedUri != null) {
                            AsyncImage(model = uiState.lastCapturedUri, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.size(44.dp).clip(CircleShape).border(1.dp, Color.White, CircleShape).clickable { onClose() })
                        } else Spacer(Modifier.size(44.dp))
                        ShutterButton(isVideo = uiState.cameraMode == "VIDEO", isRecording = uiState.isRecording) { triggerShutter() }
                        if (uiState.isRecording) {
                            ToolbarBtn(rotation = iconRotateAngle, size = 44, onClick = { if (uiState.isPaused) { activeRecording?.resume(); cameraViewModel.setIsPaused(false) } else { activeRecording?.pause(); cameraViewModel.setIsPaused(true) } }) { Icon(if (uiState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, null, tint = Color.White) }
                        } else {
                            ToolbarBtn(rotation = iconRotateAngle, size = 44, onClick = { cameraViewModel.toggleLensFacing() }) { Icon(Icons.Default.Cameraswitch, null, tint = Color.White) }
                        }
                    }
                }
            }
        }
    }
}



private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture?,
    executor: java.util.concurrent.ExecutorService,
    onPhotoCaptured: (Uri) -> Unit
) {
    val cap = imageCapture
    if (cap == null) {
        Log.w("CameraScreen", "takePhoto called but imageCapture is null")
        android.widget.Toast.makeText(context, context.getString(R.string.camera_not_ready), android.widget.Toast.LENGTH_SHORT).show()
        return
    }
    val opts = PhotoManager.getCaptureOutputOptions(context)
    val mainExec = ContextCompat.getMainExecutor(context)
    cap.takePicture(opts, executor, object : ImageCapture.OnImageSavedCallback {
        override fun onError(exc: ImageCaptureException) {
            Log.e("CameraScreen", "Capture failed", exc)
            mainExec.execute {
                android.widget.Toast.makeText(context, context.getString(R.string.camera_save_failed, exc.message ?: ""), android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
            val finalUri = output.savedUri

            Log.d("CameraScreen", "Photo saved: $finalUri")
            mainExec.execute { finalUri?.let { onPhotoCaptured(it) } }
        }
    })
}
