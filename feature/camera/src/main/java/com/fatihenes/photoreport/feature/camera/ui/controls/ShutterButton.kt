package com.fatihenes.photoreport.feature.camera.ui.controls

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.fatihenes.photoreport.feature.camera.engine.CameraMode
import com.fatihenes.photoreport.feature.camera.theme.CameraTokens

@Composable
fun ShutterButton(
    cameraMode: CameraMode,
    isRecording: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = CameraTokens.ShutterSpring,
        label = "shutterScale"
    )

    val innerCornerRadius by animateDpAsState(
        targetValue = if (isRecording) 8.dp else 36.dp,
        animationSpec = CameraTokens.SmoothSpringDp,
        label = "shutterCorner"
    )

    val innerPadding by animateDpAsState(
        targetValue = if (isRecording) 18.dp else 5.dp,
        animationSpec = CameraTokens.SmoothSpringDp,
        label = "shutterPadding"
    )

    val innerColor = if (cameraMode == CameraMode.VIDEO) {
        CameraTokens.RecordRed
    } else {
        Color.White
    }

    Box(
        modifier = modifier
            .size(CameraTokens.ShutterOuterSize)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Outer ring
        Surface(
            modifier = Modifier.size(CameraTokens.ShutterOuterSize),
            shape = CircleShape,
            color = Color.Transparent,
            border = BorderStroke(3.5.dp, Color.White)
        ) {}

        // Inner shutter core
        Box(
            modifier = Modifier
                .size(CameraTokens.ShutterInnerSize)
                .padding(innerPadding)
                .background(innerColor, RoundedCornerShape(innerCornerRadius))
        )
    }
}
