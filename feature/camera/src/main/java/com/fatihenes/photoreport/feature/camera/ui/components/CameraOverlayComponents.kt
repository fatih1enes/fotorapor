@file:Suppress("MagicNumber")
package com.fatihenes.photoreport.feature.camera.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatihenes.photoreport.core.ui.R
import com.fatihenes.photoreport.feature.camera.ui.CameraStateHolder
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private const val GRID_ALPHA = 0.25f
private const val LEVEL_THRESHOLD = 1.5f
private const val LEVEL_ANGLE_90 = 90f
private const val LEVEL_ANGLE_180 = 180f
private const val CAPTURE_FEEDBACK_ALPHA = 0.5f
private const val LEVEL_COLOR_ALPHA = 0.3f
private const val FEEDBACK_COLOR_ALPHA = 0.5f

@Composable
fun cameraOverlaysAndFeedback(
    angle: Float,
    gridVisible: Boolean,
    offset: Offset?,
    isCapturing: Boolean,
) {
    if (gridVisible) {
        cameraGrid()
    }
    levelIndicator(angle)
    captureFeedback(isCapturing)
    focusIndicator(offset)
}

@Composable
private fun cameraGrid() {
    Box(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize()) {
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(0.5.dp)
                    .background(Color.White.copy(GRID_ALPHA)),
            )
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(0.5.dp)
                    .background(Color.White.copy(GRID_ALPHA)),
            )
            Spacer(Modifier.weight(1f))
        }
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(Color.White.copy(GRID_ALPHA)),
            )
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(Color.White.copy(GRID_ALPHA)),
            )
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun levelIndicator(angle: Float) {
    val isLevel = (kotlin.math.abs(angle) < LEVEL_THRESHOLD) ||
        (kotlin.math.abs(angle - LEVEL_ANGLE_90) < LEVEL_THRESHOLD) ||
        (kotlin.math.abs(angle + LEVEL_ANGLE_90) < LEVEL_THRESHOLD) ||
        (kotlin.math.abs(kotlin.math.abs(angle) - LEVEL_ANGLE_180) < LEVEL_THRESHOLD)
    val color = if (isLevel) Amber else Color.White.copy(LEVEL_COLOR_ALPHA)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .width(100.dp)
                .height(1.5.dp)
                .graphicsLayer(rotationZ = angle)
                .background(color),
        )
        Box(Modifier.width(110.dp).height(20.dp)) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .size(2.dp, 10.dp)
                    .background(Color.White.copy(FEEDBACK_COLOR_ALPHA)),
            )
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .size(2.dp, 10.dp)
                    .background(Color.White.copy(FEEDBACK_COLOR_ALPHA)),
            )
        }
    }
}

@Composable
private fun captureFeedback(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(androidx.compose.animation.core.tween(50)),
        exit = fadeOut(androidx.compose.animation.core.tween(150)),
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(Modifier.fillMaxSize().background(Color.White.copy(CAPTURE_FEEDBACK_ALPHA)))
    }
}

@Composable
private fun focusIndicator(offset: Offset?) {
    offset?.let { o ->
        var visible by remember { mutableStateOf(value = true) }
        LaunchedEffect(o) {
            visible = true
            delay(1200.milliseconds)
            visible = false
        }
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + scaleIn(initialScale = 1.4f),
            exit = fadeOut(),
        ) {
            val density = LocalDensity.current.density
            Box(
                Modifier
                    .offset(x = (o.x / density).dp - 28.dp, y = (o.y / density).dp - 28.dp)
                    .size(56.dp),
            ) {
                Box(Modifier.fillMaxSize().border(1.5.dp, Amber, CircleShape))
                Box(
                    Modifier
                        .size(4.dp)
                        .background(Amber, CircleShape)
                        .align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
fun cameraErrorIndicator(err: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${stringResource(R.string.camera_error_prefix)} $err",
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp),
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRetry) {
                Text(stringResource(R.string.camera_retry_btn))
            }
        }
    }
}

@Composable
fun exposureControls(
    uiState: com.fatihenes.photoreport.feature.camera.model.CameraUiState,
    cameraState: CameraStateHolder,
    cameraViewModel: com.fatihenes.photoreport.feature.camera.viewmodel.CameraViewModel,
) {
    AnimatedVisibility(visible = uiState.showExposure) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.WbSunny,
                contentDescription = null,
                tint = Color.White.copy(0.5f),
                modifier = Modifier.size(16.dp),
            )
            Slider(
                value = uiState.exposureValue,
                onValueChange = {
                    cameraViewModel.setExposureValue(it)
                    cameraState.setExposure(it.toInt())
                    cameraViewModel.setShowExposure(show = true)
                },
                valueRange = cameraState.exposureRange,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Amber,
                    activeTrackColor = Amber.copy(0.6f),
                    inactiveTrackColor = Color.White.copy(0.2f),
                ),
            )
            Icon(
                imageVector = Icons.Default.WbSunny,
                contentDescription = null,
                tint = Amber,
                modifier = Modifier.size(20.dp),
            )
        }
    }
    if (cameraState.exposureIndex != 0) {
        val sign = if (cameraState.exposureIndex > 0) "+" else ""
        Text(
            text = "EV $sign${cameraState.exposureIndex}",
            color = Amber,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(bottom = 2.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun settingsRow(title: String, desc: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = desc,
                color = Color.LightGray,
                fontSize = 10.sp,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            modifier = Modifier.scale(0.8f),
        )
    }
}

@Composable
fun recordingIndicator(duration: Int, modifier: Modifier) {
    val timeText = remember(duration) {
        String.format(java.util.Locale.US, "%02d:%02d", duration / 60, duration % 60)
    }
    Row(
        modifier = modifier
            .statusBarsPadding()
            .padding(top = 12.dp)
            .background(Color.Red.copy(0.9f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).background(Color.White, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(
            text = timeText,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
