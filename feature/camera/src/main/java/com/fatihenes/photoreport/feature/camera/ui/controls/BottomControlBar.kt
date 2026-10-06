package com.fatihenes.photoreport.feature.camera.ui.controls

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.fatihenes.photoreport.feature.camera.engine.CameraMode
import com.fatihenes.photoreport.feature.camera.theme.CameraTokens
import java.util.Locale

@Composable
fun BottomControlBar(
    cameraMode: CameraMode,
    isRecording: Boolean,
    isPaused: Boolean,
    recordingDurationSeconds: Int,
    lastCapturedUri: Uri?,
    sessionCount: Int,
    iconRotation: Float,
    onShutterClick: () -> Unit,
    onSwitchLensClick: () -> Unit,
    onTogglePauseClick: () -> Unit,
    onThumbnailClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Video recording HUD
        AnimatedVisibility(
            visible = isRecording,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val formattedTime = remember(recordingDurationSeconds) {
                String.format(
                    Locale.US,
                    "%02d:%02d",
                    recordingDurationSeconds / 60,
                    recordingDurationSeconds % 60
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(CameraTokens.RecordRed)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color.White, CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formattedTime,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        // Bottom three-element row: Thumbnail | Shutter | Lens switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Session thumbnail preview button
            if (lastCapturedUri != null) {
                Box(
                    modifier = Modifier
                        .size(CameraTokens.ThumbnailSize)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onThumbnailClick()
                            }
                        )
                ) {
                    AsyncImage(
                        model = lastCapturedUri,
                        contentDescription = "Son Çekilenler",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(CameraTokens.ThumbnailSize)
                            .clip(CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                    )

                    if (sessionCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                                .background(CameraTokens.Amber, CircleShape)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = sessionCount.toString(),
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                Spacer(Modifier.size(CameraTokens.ThumbnailSize))
            }

            // 2. Shutter button
            ShutterButton(
                cameraMode = cameraMode,
                isRecording = isRecording,
                onClick = onShutterClick
            )

            // 3. Right action button (Lens switch or pause/resume)
            if (isRecording) {
                Box(
                    modifier = Modifier
                        .size(CameraTokens.LensSwitchSize)
                        .clip(CircleShape)
                        .background(CameraTokens.ControlBg)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onTogglePauseClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Devam Et" else "Duraklat",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(CameraTokens.LensSwitchSize)
                        .clip(CircleShape)
                        .background(CameraTokens.ControlBg)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSwitchLensClick()
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Lens Değiştir",
                        tint = Color.White,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer { rotationZ = iconRotation }
                    )
                }
            }
        }
    }
}
