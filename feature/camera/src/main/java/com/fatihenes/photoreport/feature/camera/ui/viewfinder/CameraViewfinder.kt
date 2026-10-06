package com.fatihenes.photoreport.feature.camera.ui.viewfinder

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.fatihenes.photoreport.feature.camera.engine.AspectRatioSelection
import com.fatihenes.photoreport.feature.camera.sensors.LevelSensorState

@Composable
fun CameraViewfinder(
    previewView: PreviewView,
    aspectRatio: AspectRatioSelection,
    currentZoomRatio: Float,
    minZoom: Float,
    maxZoom: Float,
    onZoomChanged: (Float) -> Unit,
    tapOffset: Offset?,
    isFocusLocked: Boolean,
    onFocusRequested: (Offset) -> Unit,
    onLockFocusRequested: (Offset) -> Unit,
    onSwipeMode: (Boolean) -> Unit, // true = right, false = left
    isGridVisible: Boolean,
    levelSensorState: LevelSensorState,
    isLevelVisible: Boolean,
    isShutterBlinking: Boolean,
    isTransitioning: Boolean,
    exposureIndex: Int,
    exposureRange: ClosedRange<Int>,
    onExposureChanged: (Int) -> Unit,
    watermarkVisible: Boolean,
    projectName: String,
    gpsInfo: String?,
    dateTime: String,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val latestZoom by rememberUpdatedState(currentZoomRatio)

    Box(
        modifier = modifier
            .background(Color.Black)
    ) {
        // 1. AndroidView for PreviewView with integrated touch gestures
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        // Pinch to zoom with live, fresh zoom ratio
                        if (zoom != 1f) {
                            val newZoom = (latestZoom * zoom).coerceIn(minZoom, maxZoom)
                            if (newZoom != latestZoom) {
                                onZoomChanged(newZoom)
                            }
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onFocusRequested(offset)
                        },
                        onLongPress = { offset ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLockFocusRequested(offset)
                        }
                    )
                }
        )

        // 2. Overlays
        GridOverlay(visible = isGridVisible)

        LevelOverlay(
            levelSensorState = levelSensorState,
            visible = isLevelVisible
        )

        FocusReticle(
            tapOffset = tapOffset,
            isLocked = isFocusLocked,
            exposureIndex = exposureIndex,
            exposureRange = exposureRange,
            onExposureChanged = onExposureChanged
        )

        WatermarkPreviewOverlay(
            visible = watermarkVisible,
            projectName = projectName,
            gpsInfo = gpsInfo,
            dateTime = dateTime,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        )

        FreezeFrameTransition(isTransitioning = isTransitioning)

        ShutterBlinkFeedback(visible = isShutterBlinking)
    }
}
