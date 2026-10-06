package com.fatihenes.photoreport.feature.camera.ui.viewfinder

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.fatihenes.photoreport.feature.camera.sensors.LevelSensorState
import com.fatihenes.photoreport.feature.camera.theme.CameraTokens
import kotlin.math.abs

@Composable
fun LevelOverlay(
    levelSensorState: LevelSensorState,
    visible: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val isFlat = levelSensorState.isPointingFlat.value

        if (isFlat) {
            // Overhead / downward inspection mode (bullseye reticle)
            val roll = levelSensorState.rollAngle.value
            val pitch = levelSensorState.pitchAngle.value
            val targetLevel = abs(pitch) >= 88.5f

            val ringColor = if (targetLevel) CameraTokens.Amber else CameraTokens.LevelInactive
            val dotColor = if (targetLevel) CameraTokens.Amber else Color.White

            // Outer reference ring
            drawCircle(
                color = ringColor,
                radius = 24f,
                center = center,
                style = Stroke(width = 2f)
            )

            // Dynamic bubble dot
            val xOffset = (roll * 1.5f).coerceIn(-30f, 30f)
            val yOffset = ((abs(pitch) - 90f) * 1.5f).coerceIn(-30f, 30f)
            drawCircle(
                color = dotColor,
                radius = 4f,
                center = Offset(center.x + xOffset, center.y + yOffset)
            )
        } else {
            // Standard horizon line mode
            val angle = levelSensorState.rollAngle.value
            val isLevel = levelSensorState.isLevel.value
            val lineColor = if (isLevel) CameraTokens.Amber else CameraTokens.LevelInactive

            val lineHalfLength = 70f
            rotate(degrees = angle, pivot = center) {
                // Left marker
                drawLine(
                    color = lineColor,
                    start = Offset(center.x - lineHalfLength, center.y),
                    end = Offset(center.x - 15f, center.y),
                    strokeWidth = if (isLevel) 3f else 2f
                )
                // Center gap dot
                drawCircle(
                    color = lineColor,
                    radius = if (isLevel) 3f else 2f,
                    center = center
                )
                // Right marker
                drawLine(
                    color = lineColor,
                    start = Offset(center.x + 15f, center.y),
                    end = Offset(center.x + lineHalfLength, center.y),
                    strokeWidth = if (isLevel) 3f else 2f
                )
            }
        }
    }
}
