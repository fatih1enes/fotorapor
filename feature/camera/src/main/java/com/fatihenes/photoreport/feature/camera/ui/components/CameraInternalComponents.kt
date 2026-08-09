@file:Suppress("MagicNumber")
package com.fatihenes.photoreport.feature.camera.ui.components

import android.net.Uri
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.fatihenes.photoreport.feature.camera.ui.CameraStateHolder

private const val MODE_TEXT_ALPHA = 0.4f
private const val SHUTTER_ANIM_DURATION = 250
private const val COUNT_BADGE_OFFSET = 4

@Composable
fun toolbarBtn(
    size: Int = 44,
    rotation: Float = 0f,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .graphicsLayer(rotationZ = rotation)
            .clip(CircleShape)
            .background(ControlBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
fun zoomPill(
    text: String,
    isSelected: Boolean,
    rotation: Float = 0f,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (isSelected) Amber else ControlBg,
        contentColor = if (isSelected) Color.Black else Color.White,
        modifier = Modifier
            .size(40.dp)
            .graphicsLayer(rotationZ = rotation),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun modeText(
    text: String,
    isSelected: Boolean,
    rotation: Float = 0f,
    onClick: () -> Unit,
) {
    val alpha = if (isSelected) 1f else MODE_TEXT_ALPHA
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = if (isSelected) Amber else Color.White.copy(alpha),
        modifier = Modifier
            .graphicsLayer(rotationZ = rotation)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 4.dp, vertical = 4.dp),
    )
}

@Composable
fun shutterButton(
    isVideo: Boolean,
    isRecording: Boolean,
    onClick: () -> Unit,
) {
    val innerPadding by animateDpAsState(
        targetValue = if (isRecording) 20.dp else 4.dp,
        animationSpec = tween(SHUTTER_ANIM_DURATION),
        label = "shutterPadding",
    )
    val cornerRadius by animateDpAsState(
        targetValue = if (isRecording) 8.dp else 36.dp,
        animationSpec = tween(SHUTTER_ANIM_DURATION),
        label = "shutterCorner",
    )
    Surface(
        onClick = onClick,
        modifier = Modifier.size(72.dp),
        shape = CircleShape,
        color = Color.Transparent,
        border = BorderStroke(3.dp, Color.White),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    color = if (isVideo) Color.Red else Color.White,
                    shape = RoundedCornerShape(cornerRadius),
                ),
        )
    }
}

@Composable
fun lastCapturedPreview(
    uri: Uri?,
    count: Int,
    onClick: () -> Unit,
) {
    if (uri != null) {
        Box {
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color.White, CircleShape)
                    .clickable { onClick() },
            )
            if (count > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(COUNT_BADGE_OFFSET.dp, (-COUNT_BADGE_OFFSET).dp)
                        .background(Amber, CircleShape)
                        .padding(horizontal = 4.dp),
                ) {
                    Text(
                        text = count.toString(),
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    } else {
        Spacer(Modifier.size(44.dp))
    }
}

@Composable
fun zoomAndExposureControls(
    uiState: com.fatihenes.photoreport.feature.camera.model.CameraUiState,
    cameraState: CameraStateHolder,
    cameraViewModel: com.fatihenes.photoreport.feature.camera.viewmodel.CameraViewModel,
    rotation: Float,
) {
    exposureControls(uiState, cameraState, cameraViewModel)
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp),
    ) {
        zoomPill(
            text = "☀",
            isSelected = uiState.showExposure,
            rotation = rotation,
            onClick = { cameraViewModel.setShowExposure(show = !uiState.showExposure) },
        )
        Spacer(Modifier.width(12.dp))
        zoomControls(uiState, cameraState, cameraViewModel, rotation)
    }
}

@Composable
fun zoomControls(
    uiState: com.fatihenes.photoreport.feature.camera.model.CameraUiState,
    cameraState: CameraStateHolder,
    cameraViewModel: com.fatihenes.photoreport.feature.camera.viewmodel.CameraViewModel,
    rotation: Float,
) {
    if (cameraState.maxZoom <= cameraState.minZoom) return
    val levels = remember(cameraState.minZoom, cameraState.maxZoom) {
        calculateZoomLevels(cameraState.minZoom, cameraState.maxZoom)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        levels.forEach { level ->
            val isSelected = remember(uiState.zoomRatio, level) {
                isZoomLevelSelected(uiState.zoomRatio, level)
            }
            zoomPill(
                text = formatZoomText(level),
                isSelected = isSelected,
                rotation = rotation,
                onClick = {
                    cameraViewModel.setZoomRatio(level)
                    cameraState.setZoom(level)
                },
            )
        }
    }
}

private fun calculateZoomLevels(minZoom: Float, maxZoom: Float): List<Float> {
    val list = mutableListOf<Float>()
    if (minZoom < 1f) list.add(minZoom)
    list.add(1f)
    if (maxZoom >= 2f) list.add(2f)
    if (maxZoom >= 5f) list.add(5f)
    return list.toList()
}

private fun isZoomLevelSelected(zoomRatio: Float, level: Float): Boolean {
    return if (level < 1f) {
        zoomRatio < 0.9f
    } else {
        (zoomRatio >= (level - 0.1f)) && (zoomRatio <= (level + 0.1f))
    }
}

private fun formatZoomText(level: Float): String {
    return if (level < 1f) {
        String.format(java.util.Locale.US, "%.1fx", level)
    } else {
        "${level.toInt()}x"
    }
}
