package com.fatihenes.photoreport.feature.camera.ui.viewfinder

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatihenes.photoreport.feature.camera.theme.CameraTokens
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun FocusReticle(
    tapOffset: Offset?,
    isLocked: Boolean,
    exposureIndex: Int,
    exposureRange: ClosedRange<Int>,
    onExposureChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (tapOffset == null) return

    var visible by remember(tapOffset, isLocked) { mutableStateOf(true) }

    LaunchedEffect(tapOffset, isLocked) {
        if (!isLocked) {
            visible = true
            delay(2800)
            visible = false
        } else {
            visible = true
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 1.35f, animationSpec = CameraTokens.SmoothSpring),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val density = LocalDensity.current
        val reticleSizeDp = 64.dp
        val reticleSizePx = with(density) { reticleSizeDp.toPx() }

        val posX = (tapOffset.x - reticleSizePx / 2f).coerceAtLeast(0f)
        val posY = (tapOffset.y - reticleSizePx / 2f).coerceAtLeast(0f)

        Box(
            modifier = Modifier.offset {
                IntOffset(posX.roundToInt(), posY.roundToInt())
            }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Lock badge
                if (isLocked) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(CameraTokens.Amber, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = "AE/AF KİLİTLİ",
                            color = Color.Black,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Reticle box
                    Canvas(modifier = Modifier.size(reticleSizeDp)) {
                        val stroke = Stroke(width = 2f)
                        val cornerLen = 14f
                        val color = if (isLocked) CameraTokens.Amber else Color.White

                        // 4 corners
                        // Top-left
                        drawLine(color, Offset(0f, 0f), Offset(cornerLen, 0f), strokeWidth = 2.5f)
                        drawLine(color, Offset(0f, 0f), Offset(0f, cornerLen), strokeWidth = 2.5f)

                        // Top-right
                        drawLine(color, Offset(size.width, 0f), Offset(size.width - cornerLen, 0f), strokeWidth = 2.5f)
                        drawLine(color, Offset(size.width, 0f), Offset(size.width, cornerLen), strokeWidth = 2.5f)

                        // Bottom-left
                        drawLine(color, Offset(0f, size.height), Offset(cornerLen, size.height), strokeWidth = 2.5f)
                        drawLine(color, Offset(0f, size.height), Offset(0f, size.height - cornerLen), strokeWidth = 2.5f)

                        // Bottom-right
                        drawLine(color, Offset(size.width, size.height), Offset(size.width - cornerLen, size.height), strokeWidth = 2.5f)
                        drawLine(color, Offset(size.width, size.height), Offset(size.width, size.height - cornerLen), strokeWidth = 2.5f)

                        // Center indicator dot
                        drawCircle(color = color, radius = 2.5f, center = Offset(size.width / 2f, size.height / 2f))
                    }

                    Spacer(Modifier.width(10.dp))

                    // Exposure vertical drag handle
                    if (exposureRange.endInclusive > exposureRange.start) {
                        var dragAccumulator by remember { mutableFloatStateOf(0f) }

                        Box(
                            modifier = Modifier
                                .height(reticleSizeDp)
                                .width(28.dp)
                                .draggable(
                                    orientation = Orientation.Vertical,
                                    state = rememberDraggableState { delta ->
                                        // Dragging up increases exposure, dragging down decreases
                                        dragAccumulator -= delta
                                        if (kotlin.math.abs(dragAccumulator) > 16f) {
                                            val step = if (dragAccumulator > 0) 1 else -1
                                            val newIndex = (exposureIndex + step).coerceIn(exposureRange)
                                            if (newIndex != exposureIndex) {
                                                onExposureChanged(newIndex)
                                            }
                                            dragAccumulator = 0f
                                        }
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.WbSunny,
                                    contentDescription = "Exposure",
                                    tint = if (exposureIndex != 0) CameraTokens.Amber else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                if (exposureIndex != 0) {
                                    val sign = if (exposureIndex > 0) "+" else ""
                                    Text(
                                        text = "$sign$exposureIndex",
                                        color = CameraTokens.Amber,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
