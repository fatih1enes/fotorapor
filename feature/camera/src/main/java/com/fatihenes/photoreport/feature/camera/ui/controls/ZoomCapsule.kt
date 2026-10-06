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
        buildList {
            // Ultra-wide: show only if the camera actually goes below 1x
            if (minZoom < CameraTokens.UltraWideActiveThreshold) add(minZoom)
            // 1x is always shown
            add(CameraTokens.ZoomTierWide)
            // 2x telephoto: show if reachable with some headroom
            if (maxZoom >= CameraTokens.Telephoto2xThreshold) add(CameraTokens.ZoomTierTelephoto2x)
            // 3x telephoto: show if this tier is significantly below max
            if (maxZoom >= CameraTokens.Telephoto3xThreshold) add(CameraTokens.ZoomTierTelephoto3x)
            // 5x telephoto: show if reachable
            if (maxZoom >= CameraTokens.Telephoto5xThreshold) add(CameraTokens.ZoomTierTelephoto5x)
        }
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
    return if (tier < CameraTokens.UltraWideActiveThreshold) {
        // Ultra-wide: active when zoom is near the min tier value
        currentZoom < (tier + CameraTokens.UltraWideActiveOffset).coerceAtMost(CameraTokens.UltraWideActiveThreshold)
    } else {
        // Standard tiers: ±15% tolerance, preventing two tiers lighting up simultaneously
        val halfGap = (tier * CameraTokens.ZoomTierTolerancePercent).coerceIn(
            CameraTokens.ZoomTierMinTolerance,
            CameraTokens.ZoomTierMaxTolerance
        )
        currentZoom in (tier - halfGap)..(tier + halfGap)
    }
}

private fun formatZoomTier(tier: Float): String {
    return when {
        tier < CameraTokens.UltraWideActiveThreshold -> String.format(java.util.Locale.US, "%.1f×", tier)
        tier == CameraTokens.ZoomTierWide -> "1×"
        tier % 1f == 0f -> "${tier.toInt()}×"
        else -> String.format(java.util.Locale.US, "%.1f×", tier)
    }
}
