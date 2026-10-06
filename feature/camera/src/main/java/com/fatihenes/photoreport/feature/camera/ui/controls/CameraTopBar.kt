package com.fatihenes.photoreport.feature.camera.ui.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatihenes.photoreport.core.ui.R
import com.fatihenes.photoreport.feature.camera.engine.AspectRatioSelection
import com.fatihenes.photoreport.feature.camera.engine.FlashMode
import com.fatihenes.photoreport.feature.camera.theme.CameraTokens

@Composable
fun CameraTopBar(
    flashMode: FlashMode,
    aspectRatio: AspectRatioSelection,
    isGridVisible: Boolean,
    gpsAccuracyMeters: Float?,
    projectName: String,
    iconRotation: Float,
    onCloseClick: () -> Unit,
    onFlashCycle: () -> Unit,
    onAspectRatioToggle: () -> Unit,
    onGridToggle: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // Main action button row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Close
            TopBarIconButton(
                icon = Icons.Default.Close,
                contentDescription = stringResource(R.string.close_label),
                iconRotation = iconRotation,
                onClick = onCloseClick
            )

            // Flash mode
            val (flashIcon, flashTint) = when (flashMode) {
                FlashMode.AUTO -> Icons.Default.FlashAuto to CameraTokens.Amber
                FlashMode.ON -> Icons.Default.FlashOn to CameraTokens.Amber
                FlashMode.TORCH -> Icons.Default.Highlight to CameraTokens.Amber
                FlashMode.OFF -> Icons.Default.FlashOff to Color.White
            }
            TopBarIconButton(
                icon = flashIcon,
                contentDescription = "Flash",
                tint = flashTint,
                iconRotation = iconRotation,
                onClick = onFlashCycle
            )

            // Aspect ratio chip
            TopBarTextButton(
                text = aspectRatio.displayLabel,
                iconRotation = iconRotation,
                onClick = onAspectRatioToggle
            )

            // Grid toggle
            TopBarIconButton(
                icon = Icons.Default.GridOn,
                contentDescription = stringResource(R.string.grid_label),
                tint = if (isGridVisible) CameraTokens.Amber else Color.White,
                iconRotation = iconRotation,
                onClick = onGridToggle
            )

            // Settings
            TopBarIconButton(
                icon = Icons.Default.Settings,
                contentDescription = "Settings",
                iconRotation = iconRotation,
                onClick = onSettingsClick
            )
        }

        // Secondary status row (GPS pill + Project name chip)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // GPS pill
            val gpsColor = when {
                gpsAccuracyMeters == null -> Color.Gray
                gpsAccuracyMeters < 10f -> Color(0xFF34C759)
                gpsAccuracyMeters < 30f -> CameraTokens.Amber
                else -> Color(0xFFFF3B30)
            }
            val gpsText = if (gpsAccuracyMeters != null) {
                "● GPS ±${gpsAccuracyMeters.toInt()}m"
            } else {
                "● GPS Bekleniyor"
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(CameraTokens.SurfaceTranslucent)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = gpsText,
                    color = gpsColor,
                    fontSize = CameraTokens.StatusBadgeFontSize,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (projectName.isNotBlank()) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(CameraTokens.SurfaceTranslucent)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = projectName,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = CameraTokens.StatusBadgeFontSize,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBarIconButton(
    icon: ImageVector,
    contentDescription: String?,
    iconRotation: Float,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(CameraTokens.ControlBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer { rotationZ = iconRotation }
        )
    }
}

@Composable
private fun TopBarTextButton(
    text: String,
    iconRotation: Float,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(CameraTokens.ControlBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.graphicsLayer { rotationZ = iconRotation }
        )
    }
}
