package com.fatihenes.photoreport.feature.camera.ui.viewfinder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.fatihenes.photoreport.feature.camera.theme.CameraTokens

@Composable
fun WatermarkPreviewOverlay(
    visible: Boolean,
    projectName: String,
    gpsInfo: String?,
    dateTime: String,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    val label = buildString {
        if (!gpsInfo.isNullOrBlank()) {
            append(gpsInfo)
            append(" · ")
        }
        append(dateTime)
        if (projectName.isNotBlank()) {
            append(" · ")
            append(projectName)
        }
    }

    if (label.isBlank()) return

    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = CameraTokens.WatermarkMonospaceFontSize,
            fontFamily = FontFamily.Monospace,
            maxLines = 1
        )
    }
}
