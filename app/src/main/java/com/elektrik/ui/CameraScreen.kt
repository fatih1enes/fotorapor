package com.elektrik.ui

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
import com.elektrik.R
import com.elektrik.util.PhotoManager
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

private val Amber = Color(0xFFFFD60A)
private val ControlBg = Color(0x66000000)

@Composable
fun CameraScreen(
    projectName: String = "",
    onPhotoCaptured: (Uri) -> Unit,
    onClose: () -> Unit,
    enableOptimization: Boolean = true,
    enableWebp: Boolean = true,
    onToggleOptimization: (Boolean) -> Unit = {},
    onToggleWebp: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val cameraState = rememberCameraStateHolder()
    var activeRecording: Recording? by remember { mutableStateOf(null) }
    DisposableEffect(Unit) { onDispose { activeRecording?.stop() } }

    // State
    var cameraMode by remember { mutableStateOf("PHOTO") }
    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableIntStateOf(0) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var videoQuality by remember { mutableStateOf(Quality.FHD) }
    var aspectRatio by remember { mutableIntStateOf(AspectRatio.RATIO_4_3) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var showCaptureFeedback by remember { mutableStateOf(false) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var isGridVisible by remember { mutableStateOf(false) }
    var showExposure by remember { mutableStateOf(false) }
    var exposureValue by remember { mutableFloatStateOf(0f) }
    var tapOffset by remember { mutableStateOf<Offset?>(null) }
    var currentRotation by remember { mutableIntStateOf(Surface.ROTATION_0) }
    var lastCapturedUri by remember { mutableStateOf<Uri?>(null) }
    var isPaused by remember { mutableStateOf(false) }
    var showSettingsPanel by remember { mutableStateOf(false) }

    // Auto-hide exposure
    LaunchedEffect(showExposure, exposureValue) {
        if (showExposure) { delay(4000.milliseconds); showExposure = false }
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

    LaunchedEffect(cameraMode) {
        if (cameraMode == "VIDEO") {
            val p = android.Manifest.permission.RECORD_AUDIO
            if (ContextCompat.checkSelfPermission(context, p) != android.content.pm.PackageManager.PERMISSION_GRANTED)
                audioLauncher.launch(p)
        }
    }

    LaunchedEffect(lensFacing, aspectRatio, cameraMode, videoQuality, enableOptimization) {
        cameraState.bindCamera(
            previewView = previewView,
            lensFacing = lensFacing,
            aspectRatio = aspectRatio,
            cameraMode = cameraMode,
            videoQuality = videoQuality,
            currentRotation = currentRotation,
            flashMode = flashMode,
            enableOptimization = enableOptimization
        )
        zoomRatio = 1f; exposureValue = 0f
    }

    LaunchedEffect(isRecording) {
        if (isRecording) { recordingDuration = 0; while (isRecording && isActive) { delay(1000.milliseconds); recordingDuration++ } }
    }

    val durText = remember(recordingDuration) {
        String.format(java.util.Locale.US, "%02d:%02d", recordingDuration / 60, recordingDuration % 60)
    }

    val previewAspect = if (aspectRatio == AspectRatio.RATIO_4_3) 3f / 4f else 9f / 16f
    
    val triggerShutter = {
        if (cameraMode == "PHOTO") {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            showCaptureFeedback = true
            scope.launch { delay(100.milliseconds); showCaptureFeedback = false }
            takePhoto(context, cameraState.imageCapture, cameraState.executor, projectName, enableWebp) { uri ->
                lastCapturedUri = uri
                onPhotoCaptured(uri)
            }
        } else {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (isRecording) { activeRecording?.stop(); activeRecording = null }
            else {
                cameraState.videoCapture?.let { vc ->
                    val opts = PhotoManager.getVideoOutputOptions(context)
                    val hasAudio = ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    val pending = vc.output.prepareRecording(context, opts)
                    if (hasAudio) { try { pending.withAudioEnabled() } catch (_: SecurityException) {} }
                    else audioLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                    isRecording = true
                    isPaused = false
                    activeRecording = pending.start(ContextCompat.getMainExecutor(context)) { ev ->
                        if (ev is VideoRecordEvent.Finalize) {
                            isRecording = false
                            isPaused = false
                            if (!ev.hasError()) ev.outputResults.outputUri?.let { 
                                lastCapturedUri = it
                                onPhotoCaptured(it) 
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
                        Button(onClick = { scope.launch { cameraState.bindCamera(previewView, lensFacing, aspectRatio, cameraMode, videoQuality, currentRotation, flashMode) } }) {
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
                                val nz = (zoomRatio * zoom).coerceIn(cameraState.minZoom, cameraState.maxZoom)
                                if (kotlin.math.floor(zoomRatio) != kotlin.math.floor(nz)) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                zoomRatio = nz; cameraState.setZoom(nz)
                            }
                        }
                        .pointerInput(cameraState.camera) {
                            detectTapGestures(onTap = { 
                                tapOffset = it
                                exposureValue = 0f 
                                cameraState.focusAndMeter(it, previewView) 
                            })
                        }
                )
            }
            if (isGridVisible) GridOverlay()
            tapOffset?.let { FocusRing(it) }
        }

        // 2) CAPTURE FLASH
        AnimatedVisibility(
            visible = showCaptureFeedback, enter = fadeIn(tween(50)), exit = fadeOut(tween(150)),
            modifier = Modifier.fillMaxSize()
        ) { Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.5f))) }

        // 3) TOP TOOLBAR — always accessible, overlays on top
        AnimatedVisibility(
            visible = !isRecording, enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Close
                ToolbarBtn(onClick = onClose) {
                    Icon(Icons.Default.Close, stringResource(R.string.close_label), tint = Color.White, modifier = Modifier.size(22.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Flash (photo) — cycles Auto/On/Off
                    if (cameraMode == "PHOTO") {
                        ToolbarBtn(onClick = {
                            flashMode = when (flashMode) {
                                ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
                                ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                                else -> ImageCapture.FLASH_MODE_OFF
                            }
                            cameraState.setFlashMode(flashMode)
                        }) {
                            Icon(
                                when (flashMode) {
                                    ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                                    ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                                    else -> Icons.Default.FlashOff
                                },
                                stringResource(R.string.flash_label),
                                tint = if (flashMode != ImageCapture.FLASH_MODE_OFF) Amber else Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Video quality
                    if (cameraMode == "VIDEO") {
                        val qt = when (videoQuality) { Quality.SD -> "SD"; Quality.HD -> "HD"; Quality.FHD -> "FHD"; Quality.UHD -> "4K"; else -> "FHD" }
                        ToolbarBtn(onClick = {
                            if (cameraState.supportedQualities.isNotEmpty()) {
                                val i = cameraState.supportedQualities.indexOf(videoQuality)
                                videoQuality = cameraState.supportedQualities[if (i != -1) (i + 1) % cameraState.supportedQualities.size else 0]
                            }
                        }) { Text(qt, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    }

                    // Aspect ratio
                    ToolbarBtn(onClick = {
                        aspectRatio = if (aspectRatio == AspectRatio.RATIO_4_3) AspectRatio.RATIO_16_9 else AspectRatio.RATIO_4_3
                    }) {
                        Text(if (aspectRatio == AspectRatio.RATIO_4_3) "4:3" else "16:9",
                            color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    // Grid
                    ToolbarBtn(onClick = { isGridVisible = !isGridVisible }) {
                        Icon(Icons.Default.GridOn, stringResource(R.string.grid_label),
                            tint = if (isGridVisible) Amber else Color.White, modifier = Modifier.size(22.dp))
                    }
                    
                    // Settings
                    ToolbarBtn(onClick = { showSettingsPanel = !showSettingsPanel }) {
                        Icon(Icons.Default.Settings, "Ayarlar",
                            tint = if (showSettingsPanel) Amber else Color.White, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }

        // 3.5) SETTINGS PANEL (Glassmorphism overlay)
        AnimatedVisibility(
            visible = showSettingsPanel && !isRecording,
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
                Text("Kamera Ayarları", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(bottom = 8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Donanım HDR", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Daha net ve pürüzsüz fotoğraflar çeker", color = Color.LightGray, fontSize = 10.sp)
                    }
                    androidx.compose.material3.Switch(checked = enableOptimization, onCheckedChange = onToggleOptimization, modifier = Modifier.scale(0.8f))
                }
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("WebP Kayıt", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Kaliteyi bozmadan boyutu 4 kat küçültür", color = Color.LightGray, fontSize = 10.sp)
                    }
                    androidx.compose.material3.Switch(checked = enableWebp, onCheckedChange = onToggleWebp, modifier = Modifier.scale(0.8f))
                }
            }
        }

        // 4) RECORDING INDICATOR
        if (isRecording) {
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

        // 5) BOTTOM CONTROLS — always at bottom, overlays preview edge
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.7f))
                .navigationBarsPadding().padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Exposure slider (horizontal, shown when toggled)
            androidx.compose.animation.AnimatedVisibility(
                visible = showExposure, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.WbSunny, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                    Slider(
                        value = exposureValue,
                        onValueChange = { exposureValue = it; cameraState.setExposure(it.toInt()); showExposure = true },
                        valueRange = cameraState.exposureRange,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Amber, activeTrackColor = Amber.copy(alpha = 0.6f),
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                    Icon(Icons.Default.WbSunny, null, tint = Amber, modifier = Modifier.size(20.dp))
                }
            }

            // EV indicator
            if (cameraState.exposureIndex != 0) {
                Text("EV ${if (cameraState.exposureIndex > 0) "+" else ""}${cameraState.exposureIndex}",
                    color = Amber, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 2.dp))
            }

            // Zoom pills + EV toggle
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                // EV toggle
                ZoomPill("☀", showExposure) { showExposure = !showExposure }
                Spacer(Modifier.width(12.dp))

                if (cameraState.maxZoom > cameraState.minZoom) {
                    // Ultra-wide (0.5x-0.6x) — shown when device supports zoom below 1.0
                    if (cameraState.minZoom < 1f) {
                        val wideLabel = String.format(java.util.Locale.US, "%.1fx", cameraState.minZoom)
                        ZoomPill(wideLabel, zoomRatio < 0.9f) {
                            zoomRatio = cameraState.minZoom; cameraState.setZoom(cameraState.minZoom)
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    ZoomPill("1x", zoomRatio in 0.9f..1.1f) { zoomRatio = 1f; cameraState.setZoom(1f) }
                    if (cameraState.maxZoom >= 2f) {
                        Spacer(Modifier.width(8.dp))
                        ZoomPill("2x", zoomRatio in 1.9f..2.1f) { zoomRatio = 2f; cameraState.setZoom(2f) }
                    }
                    if (cameraState.maxZoom >= 5f) {
                        Spacer(Modifier.width(8.dp))
                        ZoomPill("5x", zoomRatio in 4.9f..5.1f) { zoomRatio = 5f; cameraState.setZoom(5f) }
                    }
                }
            }

            // Mode selector
            androidx.compose.animation.AnimatedVisibility(visible = !isRecording, enter = fadeIn(), exit = fadeOut()) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp, top = 4.dp)
                ) {
                    ModeText(stringResource(R.string.camera_mode_photo), cameraMode == "PHOTO") {
                        if (!isRecording) { cameraMode = "PHOTO"; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
                    }
                    Spacer(Modifier.width(32.dp))
                    ModeText(stringResource(R.string.camera_mode_video), cameraMode == "VIDEO") {
                        if (!isRecording) { cameraMode = "VIDEO"; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
                    }
                }
            }

            // Shutter row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 44.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // MINI GALLERY (Left)
                if (lastCapturedUri != null) {
                    AsyncImage(
                        model = lastCapturedUri,
                        contentDescription = "Gallery",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color.White, CircleShape)
                            .clickable {
                                onClose()
                            }
                    )
                } else {
                    Spacer(Modifier.size(44.dp))
                }

                // SHUTTER
                ShutterButton(isVideo = cameraMode == "VIDEO", isRecording = isRecording) {
                    triggerShutter()
                }

                // PAUSE / FLIP (Right)
                if (isRecording) {
                    ToolbarBtn(size = 44, onClick = {
                        if (isPaused) {
                            activeRecording?.resume()
                            isPaused = false
                        } else {
                            activeRecording?.pause()
                            isPaused = true
                        }
                    }) {
                        Icon(
                            if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            "Pause/Resume", tint = Color.White, modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    ToolbarBtn(size = 44, onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                    }) { Icon(Icons.Default.Cameraswitch, stringResource(R.string.flip_camera_label), tint = Color.White, modifier = Modifier.size(24.dp)) }
                }
            }
        }
    }
}

// ===================== COMPONENTS =====================

@Composable
private fun ToolbarBtn(size: Int = 44, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.size(size.dp).clip(CircleShape).background(ControlBg)
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun ZoomPill(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick, shape = CircleShape,
        color = if (isSelected) Amber else ControlBg,
        contentColor = if (isSelected) Color.Black else Color.White,
        modifier = Modifier.size(40.dp)
    ) { Box(contentAlignment = Alignment.Center) { Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold) } }
}

@Composable
private fun ModeText(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Text(
        text = text, fontSize = 14.sp, fontWeight = FontWeight.Bold,
        color = if (isSelected) Amber else Color.White.copy(alpha = 0.4f),
        modifier = Modifier.clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp)
    )
}

@Composable
private fun ShutterButton(isVideo: Boolean, isRecording: Boolean, onClick: () -> Unit) {
    val innerPad by animateDpAsState(if (isRecording) 20.dp else 4.dp, tween(250), label = "sp")
    val corner by animateDpAsState(if (isRecording) 8.dp else 36.dp, tween(250), label = "cr")
    Surface(
        onClick = onClick, modifier = Modifier.size(72.dp), shape = CircleShape,
        color = Color.Transparent, border = BorderStroke(3.dp, Color.White)
    ) {
        Box(Modifier.fillMaxSize().padding(innerPad).background(if (isVideo) Color.Red else Color.White, RoundedCornerShape(corner)))
    }
}

@Composable
private fun GridOverlay() {
    Box(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize()) {
            Spacer(Modifier.weight(1f))
            Box(Modifier.fillMaxHeight().width(0.5.dp).background(Color.White.copy(alpha = 0.25f)))
            Spacer(Modifier.weight(1f))
            Box(Modifier.fillMaxHeight().width(0.5.dp).background(Color.White.copy(alpha = 0.25f)))
            Spacer(Modifier.weight(1f))
        }
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.weight(1f))
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(Color.White.copy(alpha = 0.25f)))
            Spacer(Modifier.weight(1f))
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(Color.White.copy(alpha = 0.25f)))
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun FocusRing(offset: Offset) {
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(offset) { visible = true; delay(1200.milliseconds); visible = false }
    AnimatedVisibility(visible = visible, enter = fadeIn() + scaleIn(initialScale = 1.4f), exit = fadeOut()) {
        val d = LocalDensity.current.density
        Box(Modifier.offset(x = (offset.x / d).dp - 28.dp, y = (offset.y / d).dp - 28.dp).size(56.dp)) {
            Box(Modifier.fillMaxSize().border(1.5.dp, Amber, CircleShape))
            Box(Modifier.size(4.dp).background(Amber, CircleShape).align(Alignment.Center))
        }
    }
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture?,
    executor: java.util.concurrent.ExecutorService,
    projectName: String,
    enableWebp: Boolean,
    onPhotoCaptured: (Uri) -> Unit
) {
    val cap = imageCapture ?: return
    val opts = PhotoManager.getCaptureOutputOptions(context)
    val mainExec = ContextCompat.getMainExecutor(context)
    cap.takePicture(opts, executor, object : ImageCapture.OnImageSavedCallback {
        override fun onError(exc: ImageCaptureException) { 
            Log.e("CameraScreen", "Capture failed", exc) 
            mainExec.execute {
                android.widget.Toast.makeText(context, "Fotoğraf kaydedilemedi: ${exc.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
            var finalUri = output.savedUri
            
            Log.d("CameraScreen", "Photo saved: $finalUri")
            mainExec.execute { finalUri?.let { onPhotoCaptured(it) } }
        }
    })
}
