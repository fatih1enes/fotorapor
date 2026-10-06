package com.fatihenes.photoreport.feature.camera.ui.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fatihenes.photoreport.feature.camera.theme.CameraTokens

@Composable
fun ZoomCapsule(
    currentZoom: Float,
    minZoom: Float,
    maxZoom: Float,
    iconRotation: Float,
    onZoomSelected: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val zoomTiers = remember(minZoom, maxZoom) {
        val list = mutableListOf<Float>()
        if (minZoom < 0.95f) list.add(minZoom)
        list.add(1f)
        if (maxZoom >= 2f) list.add(2f)
        if (maxZoom >= 5f) list.add(5f)
        list
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(CameraTokens.SurfaceFrosted)
            .padding(horizontal = 4.dp, vertical = 3.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            zoomTiers.forEach { tier ->
                val isSelected = isZoomTierActive(currentZoom, tier)
                val label = formatZoomTier(tier)

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) CameraTokens.Amber else Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onZoomSelected(tier)
                            }
                        )
                        .graphicsLayer { rotationZ = iconRotation },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = CameraTokens.ZoomLabelFontSize,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

private fun isZoomTierActive(currentZoom: Float, tier: Float): Boolean {
    return if (tier < 1f) {
        currentZoom < 0.95f
    } else {
        (currentZoom >= tier - 0.2f) && (currentZoom <= tier + 0.2f)
    }
}

private fun formatZoomTier(tier: Float): String {
    return if (tier < 1f) {
        String.format(java.util.Locale.US, "%.1f", tier)
    } else if (tier == 1f) {
        "1x"
    } else {
        "${tier.toInt()}"
    }
}
