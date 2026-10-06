package com.fatihenes.photoreport.feature.camera.ui.viewfinder

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.fatihenes.photoreport.feature.camera.theme.CameraTokens

@Composable
fun GridOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val strokeWidth = 1f

        val col1 = width / 3f
        val col2 = width * 2f / 3f

        val row1 = height / 3f
        val row2 = height * 2f / 3f

        // Vertical lines
        drawLine(
            color = CameraTokens.GridLineColor,
            start = Offset(col1, 0f),
            end = Offset(col1, height),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = CameraTokens.GridLineColor,
            start = Offset(col2, 0f),
            end = Offset(col2, height),
            strokeWidth = strokeWidth
        )

        // Horizontal lines
        drawLine(
            color = CameraTokens.GridLineColor,
            start = Offset(0f, row1),
            end = Offset(width, row1),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = CameraTokens.GridLineColor,
            start = Offset(0f, row2),
            end = Offset(width, row2),
            strokeWidth = strokeWidth
        )
    }
}
