package com.fatihenes.photoreport.feature.camera.ui.components

import androidx.camera.core.ImageCapture
import androidx.camera.video.Quality
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatihenes.photoreport.core.ui.R
import com.fatihenes.photoreport.feature.camera.ui.CameraStateHolder

@Composable
fun cameraTopBarButtons(
    state: CameraToolbarState,
    actions: CameraToolbarActions,
    cameraState: CameraStateHolder,
) {
    actions.onClose?.let { onClose ->
        toolbarBtn(rotation = state.rotation, onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.close_label),
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
    }
    flashButton(state, actions, cameraState)
    qualityButton(state, actions, cameraState)
    aspectRatioButton(state, actions)
    gridToggleButton(state, actions)
    settingsButton(state, actions)
}

@Composable
private fun flashButton(
    state: CameraToolbarState,
    actions: CameraToolbarActions,
    cameraState: CameraStateHolder,
) {
    if (state.cameraMode == "PHOTO") {
        toolbarBtn(
            rotation = state.rotation,
            onClick = {
                val nextFlash = when (state.flashMode) {
                    ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
                    ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                    else -> ImageCapture.FLASH_MODE_OFF
                }
                actions.onFlashChange(nextFlash)
                cameraState.setFlashMode(nextFlash)
            },
        ) {
            Icon(
                imageVector = when (state.flashMode) {
                    ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                    ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                    else -> Icons.Default.FlashOff
                },
                contentDescription = stringResource(R.string.flash_label),
                tint = if (state.flashMode != ImageCapture.FLASH_MODE_OFF) Amber else Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun qualityButton(
    state: CameraToolbarState,
    actions: CameraToolbarActions,
    cameraState: CameraStateHolder,
) {
    if ((state.cameraMode == "VIDEO") && cameraState.supportedQualities.isNotEmpty()) {
        val qualityText = when (state.videoQuality) {
            Quality.SD -> "SD"
            Quality.HD -> "HD"
            Quality.FHD -> "FHD"
            Quality.UHD -> "4K"
            else -> "FHD"
        }
        toolbarBtn(
            rotation = state.rotation,
            onClick = {
                val currentIndex = cameraState.supportedQualities.indexOf(state.videoQuality)
                val nextQuality = cameraState.supportedQualities[
                    (currentIndex + 1) % cameraState.supportedQualities.size
                ]
                actions.onQualityChange(nextQuality)
            },
        ) {
            Text(
                text = qualityText,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun aspectRatioButton(state: CameraToolbarState, actions: CameraToolbarActions) {
    toolbarBtn(
        rotation = state.rotation,
        onClick = {
            val nextAspect = if (state.aspectRatio == androidx.camera.core.AspectRatio.RATIO_4_3) {
                androidx.camera.core.AspectRatio.RATIO_16_9
            } else {
                androidx.camera.core.AspectRatio.RATIO_4_3
            }
            actions.onAspectChange(nextAspect)
        },
    ) {
        Text(
            text = if (state.aspectRatio == androidx.camera.core.AspectRatio.RATIO_4_3) "4:3" else "16:9",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun gridToggleButton(state: CameraToolbarState, actions: CameraToolbarActions) {
    toolbarBtn(
        rotation = state.rotation,
        onClick = { actions.onGridChange(!state.isGridVisible) },
    ) {
        Icon(
            imageVector = Icons.Default.GridOn,
            contentDescription = stringResource(R.string.grid_label),
            tint = if (state.isGridVisible) Amber else Color.White,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun settingsButton(state: CameraToolbarState, actions: CameraToolbarActions) {
    toolbarBtn(
        rotation = state.rotation,
        onClick = { actions.onSettingsChange(!state.showSettingsPanel) },
    ) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Ayarlar",
            tint = if (state.showSettingsPanel) Amber else Color.White,
            modifier = Modifier.size(22.dp),
        )
    }
}
