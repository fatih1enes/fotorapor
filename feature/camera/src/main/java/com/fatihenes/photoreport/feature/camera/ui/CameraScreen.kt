package com.fatihenes.photoreport.feature.camera.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fatihenes.photoreport.feature.camera.engine.AspectRatioSelection
import com.fatihenes.photoreport.feature.camera.engine.CameraHardwareKeyEvent
import com.fatihenes.photoreport.feature.camera.engine.CameraMode
import com.fatihenes.photoreport.feature.camera.model.CameraUiState
import com.fatihenes.photoreport.feature.camera.sensors.GpsStatus
import com.fatihenes.photoreport.feature.camera.sensors.LevelSensorState
import com.fatihenes.photoreport.feature.camera.sensors.LocationMonitorState
import com.fatihenes.photoreport.feature.camera.sensors.OrientationMonitorState
import com.fatihenes.photoreport.feature.camera.sensors.rememberLevelSensor
import com.fatihenes.photoreport.feature.camera.sensors.rememberLocationMonitor
import com.fatihenes.photoreport.feature.camera.sensors.rememberOrientationMonitor
import com.fatihenes.photoreport.feature.camera.theme.CameraTokens
import com.fatihenes.photoreport.feature.camera.ui.controls.BottomControlBar
import com.fatihenes.photoreport.feature.camera.ui.controls.CameraQuickSettingsSheet
import com.fatihenes.photoreport.feature.camera.ui.controls.CameraTopBar
import com.fatihenes.photoreport.feature.camera.ui.controls.ModeCarousel
import com.fatihenes.photoreport.feature.camera.ui.controls.ZoomCapsule
import com.fatihenes.photoreport.feature.camera.ui.review.SessionReviewSheet
import com.fatihenes.photoreport.feature.camera.ui.viewfinder.CameraViewfinder
import com.fatihenes.photoreport.feature.camera.viewmodel.CameraViewModel
import java.util.Locale

@Composable
@Suppress("LongMethod", "CyclomaticComplexMethod")
fun CameraScreen(
    onPhotoCaptured: (Uri) -> Unit,
    onClose: () -> Unit,
    projectName: String = "",
    settings: CameraScreenSettings = CameraScreenSettings(),
    cameraViewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by cameraViewModel.uiState.collectAsStateWithLifecycle()

    // 1. ImplementationMode.COMPATIBLE avoids Compose black screens and supports clipping
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // Keep screen on while camera is active
    val activity = context as? android.app.Activity
    DisposableEffect(activity) {
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(lifecycleOwner, previewView, hasCameraPermission) {
        if (hasCameraPermission) {
            cameraViewModel.bind(lifecycleOwner, previewView)
        }
    }

    val orientationMonitor = rememberOrientationMonitor(context) { rot ->
        cameraViewModel.updateTargetRotation(rot)
    }

    val levelSensor = rememberLevelSensor(
        context = context,
        enabled = uiState.isLevelVisible
    )

    val locationMonitor = rememberLocationMonitor(
        context = context,
        enabled = settings.gpsWatermarkEnabled
    )

    var showGpsDisabledDialog by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms.values.any { it }
        if (granted) {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
            if (lm != null && !LocationManagerCompat.isLocationEnabled(lm)) {
                showGpsDisabledDialog = true
            } else {
                settings.onToggleGpsWatermark(true)
            }
        }
    }

    val handleToggleGpsWatermark: (Boolean) -> Unit = { enable ->
        if (enable) {
            val hasFine = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasFine && !hasCoarse) {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            } else {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
                if (lm != null && !LocationManagerCompat.isLocationEnabled(lm)) {
                    showGpsDisabledDialog = true
                } else {
                    settings.onToggleGpsWatermark(true)
                }
            }
        } else {
            settings.onToggleGpsWatermark(false)
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Audio granted or denied */ }

    val triggerShutter = {
        val hasAudio = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasAudio && uiState.cameraMode == CameraMode.VIDEO) {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        cameraViewModel.triggerShutter(
            rotation = orientationMonitor.surfaceRotation.value,
            projectName = projectName,
            includeGps = settings.gpsWatermarkEnabled,
            enableAudio = hasAudio,
            enableHeic = settings.enableAvif,
            enableAvif = settings.enableAvif
        )
    }

    // Connect saved media events to callback
    LaunchedEffect(Unit) {
        cameraViewModel.savedMediaEvents.collect { uri ->
            onPhotoCaptured(uri)
        }
    }

    // Volume key hardware trigger
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

    LaunchedEffect(cameraViewModel.keyEventDispatcher) {
        cameraViewModel.keyEventDispatcher.events.collect { keyEvent ->
            if (keyEvent is CameraHardwareKeyEvent.Shutter) {
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
        if (!hasCameraPermission) {
            CameraPermissionRequiredContent(
                onRequestPermission = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                onClose = onClose,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            CameraMainContent(
                previewView = previewView,
                uiState = uiState,
                settings = settings,
                projectName = projectName,
                locationMonitor = locationMonitor,
                orientationMonitor = orientationMonitor,
                levelSensor = levelSensor,
                cameraViewModel = cameraViewModel,
                triggerShutter = triggerShutter,
                onClose = onClose
            )
        }

        // Quick Settings Sheet
        CameraQuickSettingsSheet(
            visible = uiState.isQuickSettingsOpen,
            onDismiss = { cameraViewModel.setQuickSettingsOpen(false) },
            enableOptimization = settings.enableOptimization,
            onToggleOptimization = settings.onToggleOptimization,
            enableHeic = settings.enableAvif,
            onToggleHeic = settings.onToggleAvif,
            enableAvif = settings.enableAvif,
            onToggleAvif = settings.onToggleAvif,
            isLevelVisible = uiState.isLevelVisible,
            onToggleLevel = { cameraViewModel.toggleLevel() },
            isGridVisible = uiState.isGridVisible,
            onToggleGrid = { cameraViewModel.toggleGrid() },
            gpsWatermarkEnabled = settings.gpsWatermarkEnabled,
            onToggleGpsWatermark = handleToggleGpsWatermark
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

        if (showGpsDisabledDialog) {
            GpsDisabledDialog(
                onDismiss = { showGpsDisabledDialog = false },
                onOpenSettings = {
                    showGpsDisabledDialog = false
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
            )
        }
    }
}

@Composable
@Suppress("LongMethod")
private fun CameraMainContent(
    previewView: PreviewView,
    uiState: CameraUiState,
    settings: CameraScreenSettings,
    projectName: String,
    locationMonitor: LocationMonitorState,
    orientationMonitor: OrientationMonitorState,
    levelSensor: LevelSensorState,
    cameraViewModel: CameraViewModel,
    triggerShutter: () -> Unit,
    onClose: () -> Unit
) {
    val is16x9 = uiState.aspectRatio == AspectRatioSelection.RATIO_16_9

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!is16x9) {
            CameraTopBar(
                flashMode = uiState.flashMode,
                aspectRatio = uiState.aspectRatio,
                isGridVisible = uiState.isGridVisible,
                gpsAccuracyMeters = locationMonitor.accuracyMeters.value,
                gpsStatusText = locationMonitor.displayText.value,
                projectName = projectName,
                iconRotation = orientationMonitor.iconRotation.value,
                onCloseClick = onClose,
                onFlashCycle = { cameraViewModel.cycleFlashMode() },
                onAspectRatioToggle = { cameraViewModel.toggleAspectRatio() },
                onGridToggle = { cameraViewModel.toggleGrid() },
                onSettingsClick = { cameraViewModel.setQuickSettingsOpen(true) }
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val hostModifier = if (is16x9) {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(CameraTokens.AspectRatio4_3)
                    .clip(RoundedCornerShape(12.dp))
            }

            CameraViewfinderHost(
                previewView = previewView,
                uiState = uiState,
                cameraViewModel = cameraViewModel,
                levelSensor = levelSensor,
                gpsWatermarkEnabled = settings.gpsWatermarkEnabled,
                projectName = projectName,
                locationMonitor = locationMonitor,
                modifier = hostModifier
            )

            if (is16x9) {
                CameraTopBar(
                    flashMode = uiState.flashMode,
                    aspectRatio = uiState.aspectRatio,
                    isGridVisible = uiState.isGridVisible,
                    gpsAccuracyMeters = locationMonitor.accuracyMeters.value,
                    gpsStatusText = locationMonitor.displayText.value,
                    projectName = projectName,
                    iconRotation = orientationMonitor.iconRotation.value,
                    onCloseClick = onClose,
                    onFlashCycle = { cameraViewModel.cycleFlashMode() },
                    onAspectRatioToggle = { cameraViewModel.toggleAspectRatio() },
                    onGridToggle = { cameraViewModel.toggleGrid() },
                    onSettingsClick = { cameraViewModel.setQuickSettingsOpen(true) },
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }

        CameraBottomControls(
            uiState = uiState,
            orientationMonitor = orientationMonitor,
            cameraViewModel = cameraViewModel,
            onShutterClick = triggerShutter
        )
    }
}

@Composable
private fun CameraViewfinderHost(
    previewView: PreviewView,
    uiState: CameraUiState,
    cameraViewModel: CameraViewModel,
    levelSensor: LevelSensorState,
    gpsWatermarkEnabled: Boolean,
    projectName: String,
    locationMonitor: LocationMonitorState,
    modifier: Modifier = Modifier
) {
    val gpsInfo = when (locationMonitor.status.value) {
        GpsStatus.ACQUIRED -> {
            val loc = locationMonitor.location.value
            if (loc != null) {
                String.format(Locale.US, "📍 %.4f, %.4f", loc.latitude, loc.longitude)
            } else {
                "GPS Etkin"
            }
        }
        GpsStatus.SEARCHING -> "GPS Aranıyor..."
        GpsStatus.PROVIDER_OFF -> "GPS Kapalı"
        GpsStatus.NO_PERMISSION -> "Konum İzni Yok"
        GpsStatus.DISABLED -> null
    }

    CameraViewfinder(
        previewView = previewView,
        currentZoomRatio = uiState.zoomRatio,
        minZoom = uiState.minZoom,
        maxZoom = uiState.maxZoom,
        onZoomChanged = { cameraViewModel.setZoomRatio(it) },
        tapOffset = uiState.tapOffset,
        isFocusLocked = uiState.isFocusLocked,
        onFocusRequested = { cameraViewModel.onFocusRequested(it, previewView) },
        onLockFocusRequested = { cameraViewModel.onLockFocusRequested(it, previewView) },
        onSwipeMode = { isRight ->
            cameraViewModel.setCameraMode(if (isRight) CameraMode.PHOTO else CameraMode.VIDEO)
        },
        isGridVisible = uiState.isGridVisible,
        levelSensorState = levelSensor,
        isLevelVisible = uiState.isLevelVisible,
        isShutterBlinking = uiState.isShutterBlinking,
        isTransitioning = uiState.isTransitioning,
        exposureIndex = uiState.exposureIndex,
        exposureRange = uiState.exposureRange,
        onExposureChanged = { cameraViewModel.setExposureIndex(it) },
        watermarkVisible = gpsWatermarkEnabled && (
            locationMonitor.status.value == GpsStatus.ACQUIRED ||
                locationMonitor.status.value == GpsStatus.SEARCHING
        ),
        projectName = projectName,
        gpsInfo = gpsInfo,
        dateTime = "FotoRapor",
        modifier = modifier
    )
}

@Composable
private fun CameraBottomControls(
    uiState: CameraUiState,
    orientationMonitor: OrientationMonitorState,
    cameraViewModel: CameraViewModel,
    onShutterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
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
            onShutterClick = onShutterClick,
            onSwitchLensClick = { cameraViewModel.switchLens() },
            onTogglePauseClick = { cameraViewModel.togglePauseRecording() },
            onThumbnailClick = { cameraViewModel.setSessionReviewOpen(true) }
        )
    }
}

@Composable
private fun CameraPermissionRequiredContent(
    onRequestPermission: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Kamera İzni Gerekli",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Rapor fotoğrafları çekebilmek için uygulamanın kameraya erişmesine izin vermelisiniz.",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = CameraTokens.Amber)
        ) {
            Text(text = "Kamera İzni Ver", color = Color.Black, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onClose) {
            Text(text = "Geri Dön", color = Color.White)
        }
    }
}

@Composable
private fun GpsDisabledDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Cihaz Konumu Kapalı", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                text = "Fotoğraflara GPS ve konum filigranı ekleyebilmek için lütfen " +
                    "cihazınızın konum servislerini (GPS) açın."
            )
        },
        confirmButton = {
            Button(onClick = onOpenSettings) {
                Text("Konum Ayarlarını Aç")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}
