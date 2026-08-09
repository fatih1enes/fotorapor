package com.fatihenes.photoreport.feature.camera.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.fatihenes.photoreport.core.ui.R
import com.fatihenes.photoreport.feature.camera.ui.CameraStateHolder

private const val ASPECT_4_3 = 0.75f
private const val ASPECT_16_9 = 0.5625f

@Composable
fun cameraPreviewArea(
    params: PreviewParams,
    actions: PreviewActions,
    modifier: Modifier = Modifier,
) {
    val aspectRatio = if (params.uiState.aspectRatio == androidx.camera.core.AspectRatio.RATIO_4_3) {
        ASPECT_4_3
    } else {
        ASPECT_16_9
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(12.dp)),
    ) {
        val error = params.cameraState.initializationError
        if (error != null) {
            cameraErrorIndicator(error, actions.onRetry)
        } else {
            previewSurface(params, actions)
        }
        cameraOverlaysAndFeedback(
            angle = params.deviceAngle,
            gridVisible = params.uiState.isGridVisible,
            offset = params.tapOffset,
            isCapturing = params.cameraState.isCapturing,
        )
    }
}

@Composable
private fun previewSurface(params: PreviewParams, actions: PreviewActions) {
    val haptic = LocalHapticFeedback.current
    AndroidView(
        factory = { params.previewView },
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(params.cameraState.camera) {
                detectTransformGestures { _, _, zoom, _ ->
                    val currentZoom = params.uiState.zoomRatio
                    val nextZoom = (currentZoom * zoom).coerceIn(
                        params.cameraState.minZoom,
                        params.cameraState.maxZoom,
                    )
                    if (kotlin.math.floor(currentZoom.toDouble()) != kotlin.math.floor(nextZoom.toDouble())) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    actions.onZoomChanged(nextZoom)
                    params.cameraState.setZoom(nextZoom)
                }
            }
            .pointerInput(params.cameraState.camera) {
                detectTapGestures { actions.onFocusRequested(it) }
            },
    )
}

@Composable
fun cameraTopBar(params: CameraTopBarParams, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        AnimatedVisibility(
            visible = !params.state.showSettingsPanel,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(
                if (params.isLandscape) Alignment.CenterStart else Alignment.TopCenter,
            ),
        ) {
            cameraToolbar(params.state, params.actions, params.cameraState, params.isLandscape)
        }
        settingsOverlay(
            visible = params.state.showSettingsPanel,
            settingsState = params.settingsState,
            modifier = Modifier.align(Alignment.TopEnd),
        )
    }
}

@Composable
private fun cameraToolbar(
    state: CameraToolbarState,
    actions: CameraToolbarActions,
    cameraState: CameraStateHolder,
    isLandscape: Boolean,
) {
    if (isLandscape) {
        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .width(60.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            cameraTopBarButtons(state, actions, cameraState)
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            cameraTopBarButtons(state, actions, cameraState)
        }
    }
}

@Composable
private fun settingsOverlay(
    visible: Boolean,
    settingsState: CameraSettingsState,
    modifier: Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
        modifier = modifier
            .statusBarsPadding()
            .padding(top = 60.dp, end = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .width(220.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.camera_settings_title),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            settingsRow(
                title = stringResource(R.string.camera_hdr_title),
                desc = stringResource(R.string.camera_hdr_desc),
                checked = settingsState.enableOptimization,
                onToggle = settingsState.onToggleOptimization,
            )
            settingsRow(
                title = stringResource(R.string.camera_avif_title),
                desc = stringResource(R.string.camera_avif_desc),
                checked = settingsState.enableAvif,
                onToggle = settingsState.onToggleAvif,
            )
        }
    }
}

@Composable
fun cameraBottomControls(
    params: ControlParams,
    actions: ControlActions,
    modifier: Modifier = Modifier,
) {
    val controlModifier = if (params.isLandscape) {
        Modifier
            .padding(end = 12.dp)
            .fillMaxHeight()
            .width(100.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 12.dp)
    }
    Box(
        modifier = modifier
            .then(controlModifier)
            .clip(RoundedCornerShape(if (params.isLandscape) 24.dp else 0.dp))
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center,
    ) {
        if (params.isLandscape) {
            landscapeControls(params, actions)
        } else {
            portraitControls(params, actions)
        }
    }
}

@Composable
private fun landscapeControls(params: ControlParams, actions: ControlActions) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxHeight()
            .padding(vertical = 20.dp),
    ) {
        recordingActionButton(params, actions)
        shutterButton(
            isVideo = params.uiState.cameraMode == "VIDEO",
            isRecording = params.uiState.isRecording,
            onClick = actions.triggerShutter,
        )
        lastCapturedPreview(
            uri = params.uiState.lastCapturedUri,
            count = params.uiState.sessionPhotoCount,
            onClick = actions.onClose,
        )
    }
}

@Composable
private fun portraitControls(params: ControlParams, actions: ControlActions) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        zoomAndExposureControls(
            uiState = params.uiState,
            cameraState = params.cameraState,
            cameraViewModel = params.cameraViewModel,
            rotation = params.iconRotateAngle,
        )
        if (!params.uiState.isRecording) {
            cameraModeSelector(
                currentMode = params.uiState.cameraMode,
                rotation = params.iconRotateAngle,
                onModeSelected = { cameraMode -> params.cameraViewModel.setCameraMode(cameraMode) },
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 44.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            lastCapturedPreview(
                uri = params.uiState.lastCapturedUri,
                count = params.uiState.sessionPhotoCount,
                onClick = actions.onClose,
            )
            shutterButton(
                isVideo = params.uiState.cameraMode == "VIDEO",
                isRecording = params.uiState.isRecording,
                onClick = actions.triggerShutter,
            )
            recordingActionButton(params, actions)
        }
    }
}

@Composable
private fun cameraModeSelector(
    currentMode: String,
    rotation: Float,
    onModeSelected: (String) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp, top = 4.dp),
    ) {
        modeText(
            text = stringResource(R.string.camera_mode_photo),
            isSelected = currentMode == "PHOTO",
            rotation = rotation,
            onClick = { onModeSelected("PHOTO") },
        )
        Spacer(Modifier.width(32.dp))
        modeText(
            text = stringResource(R.string.camera_mode_video),
            isSelected = currentMode == "VIDEO",
            rotation = rotation,
            onClick = { onModeSelected("VIDEO") },
        )
    }
}

@Composable
private fun recordingActionButton(params: ControlParams, actions: ControlActions) {
    if (params.uiState.isRecording) {
        toolbarBtn(
            rotation = params.iconRotateAngle,
            onClick = {
                if (params.uiState.isPaused) {
                    actions.activeRecording?.resume()
                    params.cameraViewModel.setIsPaused(paused = false)
                } else {
                    actions.activeRecording?.pause()
                    params.cameraViewModel.setIsPaused(paused = true)
                }
            },
        ) {
            Icon(
                imageVector = if (params.uiState.isPaused) {
                    Icons.Default.PlayArrow
                } else {
                    Icons.Default.Pause
                },
                contentDescription = null,
                tint = Color.White,
            )
        }
    } else {
        toolbarBtn(
            rotation = params.iconRotateAngle,
            onClick = { params.cameraViewModel.toggleLensFacing() },
        ) {
            Icon(
                imageVector = Icons.Default.Cameraswitch,
                contentDescription = null,
                tint = Color.White,
            )
        }
    }
}
