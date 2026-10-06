package com.fatihenes.photoreport.feature.camera.ui.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fatihenes.photoreport.feature.camera.engine.CameraMode
import com.fatihenes.photoreport.feature.camera.theme.CameraTokens

@Composable
fun ModeCarousel(
    currentMode: CameraMode,
    isRecording: Boolean,
    iconRotation: Float,
    onModeSelected: (CameraMode) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isRecording) return

    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Video mode
        ModeItem(
            title = "VİDEO",
            isSelected = currentMode == CameraMode.VIDEO,
            iconRotation = iconRotation,
            onClick = {
                if (currentMode != CameraMode.VIDEO) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onModeSelected(CameraMode.VIDEO)
                }
            }
        )

        // Photo mode
        ModeItem(
            title = "FOTO",
            isSelected = currentMode == CameraMode.PHOTO,
            iconRotation = iconRotation,
            onClick = {
                if (currentMode != CameraMode.PHOTO) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onModeSelected(CameraMode.PHOTO)
                }
            }
        )
    }
}

@Composable
private fun ModeItem(
    title: String,
    isSelected: Boolean,
    iconRotation: Float,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .graphicsLayer { rotationZ = iconRotation }
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = title,
            color = if (isSelected) CameraTokens.Amber else Color.White.copy(alpha = 0.6f),
            fontSize = CameraTokens.ModeLabelFontSize,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
        Spacer(Modifier.height(3.dp))
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(CameraTokens.Amber, CircleShape)
            )
        } else {
            Spacer(Modifier.height(4.dp))
        }
    }
}
