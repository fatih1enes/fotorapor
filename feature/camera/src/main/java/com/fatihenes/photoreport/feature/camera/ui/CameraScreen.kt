package com.fatihenes.photoreport.feature.camera.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.fatihenes.photoreport.feature.camera.engine.AspectRatioSelection
import com.fatihenes.photoreport.feature.camera.engine.CameraMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fatihenes.photoreport.feature.camera.sensors.rememberLevelSensor
import com.fatihenes.photoreport.feature.camera.sensors.rememberOrientationMonitor
import com.fatihenes.photoreport.feature.camera.ui.controls.BottomControlBar
import com.fatihenes.photoreport.feature.camera.ui.controls.CameraQuickSettingsSheet
import com.fatihenes.photoreport.feature.camera.ui.controls.CameraTopBar
import com.fatihenes.photoreport.feature.camera.ui.controls.ModeCarousel
import com.fatihenes.photoreport.feature.camera.ui.controls.ZoomCapsule
import com.fatihenes.photoreport.feature.camera.ui.review.SessionReviewSheet
import com.fatihenes.photoreport.feature.camera.ui.viewfinder.CameraViewfinder
import com.fatihenes.photoreport.feature.camera.viewmodel.CameraViewModel

@Composable
fun CameraScreen(
    onPhotoCaptured: (Uri) -> Unit,
    onClose: () -> Unit,
    projectName: String = "",
    enableOptimization: Boolean = true,
    enableAvif: Boolean = true,
    gpsWatermarkEnabled: Boolean = true,
    onToggleOptimization: (Boolean) -> Unit = {},
    onToggleAvif: (Boolean) -> Unit = {},
    onToggleGpsWatermark: (Boolean) -> Unit = {},
    cameraViewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by cameraViewModel.uiState.collectAsStateWithLifecycle()

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // Keep display awake while camera viewfinder is active
    val activity = context as? android.app.Activity
    DisposableEffect(activity) {
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val orientationMonitor = rememberOrientationMonitor(context) { rot ->
        cameraViewModel.updateTargetRotation(rot)
    }

    val levelSensor = rememberLevelSensor(
        context = context,
        enabled = uiState.isLevelVisible
    )

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Audio granted or denied */ }

    // Bind camera session
    LaunchedEffect(lifecycleOwner, previewView) {
        cameraViewModel.bind(lifecycleOwner, previewView)
    }

    // Connect saved media events to callback
    LaunchedEffect(Unit) {
        cameraViewModel.savedMediaEvents.collect { uri ->
            onPhotoCaptured(uri)
        }
    }

    // Hardware volume key shutter trigger (both Activity-level and Focus-level)
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    DisposableEffect(cameraViewModel.keyEventDispatcher) {
        cameraViewModel.keyEventDispatcher.isListening = true
        onDispose {
            cameraViewModel.keyEventDispatcher.isListening = false
        }
    }

    val triggerShutter = {
        val hasAudio = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasAudio && uiState.cameraMode == com.fatihenes.photoreport.feature.camera.engine.CameraMode.VIDEO) {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        cameraViewModel.triggerShutter(
            rotation = orientationMonitor.surfaceRotation.value,
            projectName = projectName,
            includeGps = gpsWatermarkEnabled,
            enableAudio = hasAudio
        )
    }

    LaunchedEffect(cameraViewModel.keyEventDispatcher) {
        cameraViewModel.keyEventDispatcher.events.collect { keyEvent ->
            if (keyEvent is com.fatihenes.photoreport.feature.camera.engine.CameraHardwareKeyEvent.Shutter) {
                triggerShutter()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.key == Key.VolumeUp || event.key == Key.VolumeDown) {
                    if (event.type == KeyEventType.KeyDown) {
                        triggerShutter()
                    }
                    true
                } else {
                    false
                }
            }
    ) {
        if (uiState.aspectRatio == AspectRatioSelection.RATIO_16_9) {
            // 16:9 Fullscreen Immersive Viewfinder
            CameraViewfinder(
                previewView = previewView,
                aspectRatio = uiState.aspectRatio,
                currentZoomRatio = uiState.zoomRatio,
                minZoom = uiState.minZoom,
                maxZoom = uiState.maxZoom,
                onZoomChanged = { cameraViewModel.setZoomRatio(it) },
                tapOffset = uiState.tapOffset,
                isFocusLocked = uiState.isFocusLocked,
                onFocusRequested = { cameraViewModel.onFocusRequested(it, previewView) },
                onLockFocusRequested = { cameraViewModel.onLockFocusRequested(it, previewView) },
                onSwipeMode = { isRight ->
                    if (isRight) {
                        cameraViewModel.setCameraMode(CameraMode.PHOTO)
                    } else {
                        cameraViewModel.setCameraMode(CameraMode.VIDEO)
                    }
                },
                isGridVisible = uiState.isGridVisible,
                levelSensorState = levelSensor,
                isLevelVisible = uiState.isLevelVisible,
                isShutterBlinking = uiState.isShutterBlinking,
                isTransitioning = uiState.isTransitioning,
                exposureIndex = uiState.exposureIndex,
                exposureRange = uiState.exposureRange,
                onExposureChanged = { cameraViewModel.setExposureIndex(it) },
                watermarkVisible = gpsWatermarkEnabled,
                projectName = projectName,
                gpsInfo = if (gpsWatermarkEnabled) "GPS Etkin" else null,
                dateTime = "FotoRapor",
                modifier = Modifier.fillMaxSize()
            )

            // Floating Top Bar
            CameraTopBar(
                flashMode = uiState.flashMode,
                aspectRatio = uiState.aspectRatio,
                isGridVisible = uiState.isGridVisible,
                gpsAccuracyMeters = if (gpsWatermarkEnabled) 4f else null,
                projectName = projectName,
                iconRotation = orientationMonitor.iconRotation.value,
                onCloseClick = onClose,
                onFlashCycle = { cameraViewModel.cycleFlashMode() },
                onAspectRatioToggle = { cameraViewModel.toggleAspectRatio() },
                onGridToggle = { cameraViewModel.toggleGrid() },
                onSettingsClick = { cameraViewModel.setQuickSettingsOpen(true) },
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // Floating Bottom Controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ZoomCapsule(
                    currentZoom = uiState.zoomRatio,
                    minZoom = uiState.minZoom,
                    maxZoom = uiState.maxZoom,
                    iconRotation = orientationMonitor.iconRotation.value,
                    onZoomSelected = { cameraViewModel.setZoomRatio(it) }
                )
                Spacer(Modifier.height(8.dp))
                ModeCarousel(
                    currentMode = uiState.cameraMode,
                    isRecording = uiState.isRecording,
                    iconRotation = orientationMonitor.iconRotation.value,
                    onModeSelected = { cameraViewModel.setCameraMode(it) }
                )
                BottomControlBar(
                    cameraMode = uiState.cameraMode,
                    isRecording = uiState.isRecording,
                    isPaused = uiState.isPaused,
                    recordingDurationSeconds = uiState.recordingDurationSeconds,
                    lastCapturedUri = uiState.lastCapturedUri,
                    sessionCount = uiState.sessionCount,
                    iconRotation = orientationMonitor.iconRotation.value,
                    onShutterClick = triggerShutter,
                    onSwitchLensClick = { cameraViewModel.switchLens() },
                    onTogglePauseClick = { cameraViewModel.togglePauseRecording() },
                    onThumbnailClick = { cameraViewModel.setSessionReviewOpen(true) }
                )
            }
        } else {
            // 4:3 iPhone Classic Framing
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CameraTopBar(
                    flashMode = uiState.flashMode,
                    aspectRatio = uiState.aspectRatio,
                    isGridVisible = uiState.isGridVisible,
                    gpsAccuracyMeters = if (gpsWatermarkEnabled) 4f else null,
                    projectName = projectName,
                    iconRotation = orientationMonitor.iconRotation.value,
                    onCloseClick = onClose,
                    onFlashCycle = { cameraViewModel.cycleFlashMode() },
                    onAspectRatioToggle = { cameraViewModel.toggleAspectRatio() },
                    onGridToggle = { cameraViewModel.toggleGrid() },
                    onSettingsClick = { cameraViewModel.setQuickSettingsOpen(true) }
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CameraViewfinder(
                        previewView = previewView,
                        aspectRatio = uiState.aspectRatio,
                        currentZoomRatio = uiState.zoomRatio,
                        minZoom = uiState.minZoom,
                        maxZoom = uiState.maxZoom,
                        onZoomChanged = { cameraViewModel.setZoomRatio(it) },
                        tapOffset = uiState.tapOffset,
                        isFocusLocked = uiState.isFocusLocked,
                        onFocusRequested = { cameraViewModel.onFocusRequested(it, previewView) },
                        onLockFocusRequested = { cameraViewModel.onLockFocusRequested(it, previewView) },
                        onSwipeMode = { isRight ->
                            if (isRight) {
                                cameraViewModel.setCameraMode(CameraMode.PHOTO)
                            } else {
                                cameraViewModel.setCameraMode(CameraMode.VIDEO)
                            }
                        },
                        isGridVisible = uiState.isGridVisible,
                        levelSensorState = levelSensor,
                        isLevelVisible = uiState.isLevelVisible,
                        isShutterBlinking = uiState.isShutterBlinking,
                        isTransitioning = uiState.isTransitioning,
                        exposureIndex = uiState.exposureIndex,
                        exposureRange = uiState.exposureRange,
                        onExposureChanged = { cameraViewModel.setExposureIndex(it) },
                        watermarkVisible = gpsWatermarkEnabled,
                        projectName = projectName,
                        gpsInfo = if (gpsWatermarkEnabled) "GPS Etkin" else null,
                        dateTime = "FotoRapor",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 4f)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ZoomCapsule(
                        currentZoom = uiState.zoomRatio,
                        minZoom = uiState.minZoom,
                        maxZoom = uiState.maxZoom,
                        iconRotation = orientationMonitor.iconRotation.value,
                        onZoomSelected = { cameraViewModel.setZoomRatio(it) }
                    )
                    Spacer(Modifier.height(8.dp))
                    ModeCarousel(
                        currentMode = uiState.cameraMode,
                        isRecording = uiState.isRecording,
                        iconRotation = orientationMonitor.iconRotation.value,
                        onModeSelected = { cameraViewModel.setCameraMode(it) }
                    )
                    BottomControlBar(
                        cameraMode = uiState.cameraMode,
                        isRecording = uiState.isRecording,
                        isPaused = uiState.isPaused,
                        recordingDurationSeconds = uiState.recordingDurationSeconds,
                        lastCapturedUri = uiState.lastCapturedUri,
                        sessionCount = uiState.sessionCount,
                        iconRotation = orientationMonitor.iconRotation.value,
                        onShutterClick = triggerShutter,
                        onSwitchLensClick = { cameraViewModel.switchLens() },
                        onTogglePauseClick = { cameraViewModel.togglePauseRecording() },
                        onThumbnailClick = { cameraViewModel.setSessionReviewOpen(true) }
                    )
                }
            }
        }

        // Quick Settings Sheet
        CameraQuickSettingsSheet(
            visible = uiState.isQuickSettingsOpen,
            onDismiss = { cameraViewModel.setQuickSettingsOpen(false) },
            enableOptimization = enableOptimization,
            onToggleOptimization = onToggleOptimization,
            enableAvif = enableAvif,
            onToggleAvif = onToggleAvif,
            isLevelVisible = uiState.isLevelVisible,
            onToggleLevel = { cameraViewModel.toggleLevel() },
            isGridVisible = uiState.isGridVisible,
            onToggleGrid = { cameraViewModel.toggleGrid() },
            gpsWatermarkEnabled = gpsWatermarkEnabled,
            onToggleGpsWatermark = onToggleGpsWatermark
        )

        // Session Review Sheet
        SessionReviewSheet(
            visible = uiState.isSessionReviewOpen,
            capturedUris = uiState.sessionUris,
            onDismiss = { cameraViewModel.setSessionReviewOpen(false) },
            onFinishSession = {
                cameraViewModel.setSessionReviewOpen(false)
                onClose()
            }
        )
    }
}
