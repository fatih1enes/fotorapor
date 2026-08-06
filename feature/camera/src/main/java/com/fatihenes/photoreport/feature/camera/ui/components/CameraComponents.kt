package com.fatihenes.photoreport.feature.camera.ui.components

import androidx.camera.core.ImageCapture
import androidx.camera.video.Quality
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatihenes.photoreport.core.ui.R
import com.fatihenes.photoreport.feature.camera.ui.CameraStateHolder
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

val Amber = Color(0xFFFFD60A)
val ControlBg = Color(0x66000000)

@Composable
fun ToolbarBtn(size: Int = 44, rotation: Float = 0f, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.size(size.dp).graphicsLayer(rotationZ = rotation).clip(CircleShape).background(ControlBg)
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
fun ZoomPill(text: String, isSelected: Boolean, rotation: Float = 0f, onClick: () -> Unit) {
    Surface(
        onClick = onClick, shape = CircleShape,
        color = if (isSelected) Amber else ControlBg,
        contentColor = if (isSelected) Color.Black else Color.White,
        modifier = Modifier.size(40.dp).graphicsLayer(rotationZ = rotation)
    ) { Box(contentAlignment = Alignment.Center) { Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold) } }
}

@Composable
fun ModeText(text: String, isSelected: Boolean, rotation: Float = 0f, onClick: () -> Unit) {
    Text(
        text = text, fontSize = 14.sp, fontWeight = FontWeight.Bold,
        color = if (isSelected) Amber else Color.White.copy(alpha = 0.4f),
        modifier = Modifier.graphicsLayer(rotationZ = rotation).clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp)
    )
}

@Composable
fun ShutterButton(isVideo: Boolean, isRecording: Boolean, onClick: () -> Unit) {
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
fun ToolbarItems(
    cameraMode: String,
    flashMode: Int,
    videoQuality: Quality,
    aspectRatio: Int,
    isGridVisible: Boolean,
    showSettingsPanel: Boolean,
    rotation: Float,
    cameraState: CameraStateHolder,
    onFlashChange: (Int) -> Unit,
    onAspectChange: (Int) -> Unit,
    onGridChange: (Boolean) -> Unit,
    onSettingsChange: (Boolean) -> Unit,
    onQualityChange: (Quality) -> Unit,
    onClose: (() -> Unit)? = null
) {
    if (onClose != null) {
        ToolbarBtn(rotation = rotation, onClick = onClose) {
            Icon(Icons.Default.Close, stringResource(R.string.close_label), tint = Color.White, modifier = Modifier.size(22.dp))
        }
    }

    if (cameraMode == "PHOTO") {
        ToolbarBtn(rotation = rotation, onClick = {
            val nextFlash = when (flashMode) {
                ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
                ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                else -> ImageCapture.FLASH_MODE_OFF
            }
            onFlashChange(nextFlash)
            cameraState.setFlashMode(nextFlash)
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

    if (cameraMode == "VIDEO") {
        val qt = when (videoQuality) { Quality.SD -> "SD"; Quality.HD -> "HD"; Quality.FHD -> "FHD"; Quality.UHD -> "4K"; else -> "FHD" }
        ToolbarBtn(rotation = rotation, onClick = {
            if (cameraState.supportedQualities.isNotEmpty()) {
                val i = cameraState.supportedQualities.indexOf(videoQuality)
                onQualityChange(cameraState.supportedQualities[if (i != -1) (i + 1) % cameraState.supportedQualities.size else 0])
            }
        }) { Text(qt, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
    }

    ToolbarBtn(rotation = rotation, onClick = {
        onAspectChange(if (aspectRatio == androidx.camera.core.AspectRatio.RATIO_4_3) androidx.camera.core.AspectRatio.RATIO_16_9 else androidx.camera.core.AspectRatio.RATIO_4_3)
    }) {
        Text(if (aspectRatio == androidx.camera.core.AspectRatio.RATIO_4_3) "4:3" else "16:9",
            color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }

    ToolbarBtn(rotation = rotation, onClick = { onGridChange(!isGridVisible) }) {
        Icon(Icons.Default.GridOn, stringResource(R.string.grid_label),
            tint = if (isGridVisible) Amber else Color.White, modifier = Modifier.size(22.dp))
    }

    ToolbarBtn(rotation = rotation, onClick = { onSettingsChange(!showSettingsPanel) }) {
        Icon(Icons.Default.Settings, "Ayarlar",
            tint = if (showSettingsPanel) Amber else Color.White, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun LevelerOverlay(angle: Float) {
    val isLevel = (kotlin.math.abs(angle) < 1.5f) || (kotlin.math.abs(angle - 90f) < 1.5f) ||
                  (kotlin.math.abs(angle + 90f) < 1.5f) || (kotlin.math.abs(kotlin.math.abs(angle) - 180f) < 1.5f)

    val color = if (isLevel) Amber else Color.White.copy(alpha = 0.3f)

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .width(100.dp)
                .height(1.5.dp)
                .graphicsLayer(rotationZ = angle)
                .background(color)
        )
        Box(
            Modifier
                .width(110.dp)
                .height(20.dp)
        ) {
            Box(Modifier.align(Alignment.CenterStart).size(2.dp, 10.dp).background(Color.White.copy(alpha = 0.5f)))
            Box(Modifier.align(Alignment.CenterEnd).size(2.dp, 10.dp).background(Color.White.copy(alpha = 0.5f)))
        }
    }
}

@Composable
fun GridOverlay() {
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
fun FocusRing(offset: Offset) {
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
