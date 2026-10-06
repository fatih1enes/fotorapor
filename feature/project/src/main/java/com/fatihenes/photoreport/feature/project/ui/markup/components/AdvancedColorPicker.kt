@file:Suppress("MagicNumber", "LongMethod")
package com.fatihenes.photoreport.feature.project.ui.markup.components

import android.graphics.Color as AndroidColor
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporMotion
import com.fatihenes.photoreport.core.ui.R
import com.fatihenes.photoreport.feature.project.ui.markup.MarkupAccentGreen
import com.fatihenes.photoreport.feature.project.ui.markup.MarkupAccentOnGreen
import com.fatihenes.photoreport.feature.project.ui.markup.MarkupStudioColors
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun AdvancedColorPicker(
    selectedColor: Color,
    onSelectColor: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MarkupStudioColors.forEach { color ->
            val isSelected = selectedColor.toArgb() == color.toArgb()
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.2f else 1.0f,
                animationSpec = tween(FotoRaporMotion.DURATION_SHORT),
                label = "color_scale"
            )

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(color)
                    .then(if (isSelected) {
                        Modifier.border(2.5.dp, Color.White, CircleShape)
                    } else {
                        Modifier.border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                    })
                    .clickable { onSelectColor(color) }
                    .semantics { contentDescription = "Color selection" }
            )
        }

        // Custom Color Wheel (+) button
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    Brush.sweepGradient(
                        listOf(
                            Color.Red, Color.Yellow, Color.Green,
                            Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                        )
                    )
                )
                .border(1.5.dp, Color.White, CircleShape)
                .clickable { showCustomDialog = true },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(0x99000000)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.markup_custom_color),
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }

    if (showCustomDialog) {
        CircularColorWheelDialog(
            initialColor = selectedColor,
            onDismiss = { showCustomDialog = false },
            onConfirm = { color ->
                onSelectColor(color)
                showCustomDialog = false
            }
        )
    }
}

/**
 * Circular HSV Color Wheel Picker dialog (Yuvarlak Renk Paleti).
 */
@Composable
private fun CircularColorWheelDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit
) {
    val hsv = remember(initialColor) {
        val array = FloatArray(3)
        AndroidColor.colorToHSV(initialColor.toArgb(), array)
        array
    }

    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var saturation by remember { mutableFloatStateOf(if (hsv[1] <= 0.01f) 1f else hsv[1]) }
    var value by remember { mutableFloatStateOf(if (hsv[2] <= 0.01f) 1f else hsv[2]) }

    val currentColor = remember(hue, saturation, value) {
        val argb = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value))
        Color(argb)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.markup_custom_color),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                // Color Preview pill
                Box(
                    modifier = Modifier
                        .size(44.dp, 26.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(currentColor)
                        .border(1.5.dp, Color.White, RoundedCornerShape(13.dp))
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Circular HSV Color Wheel
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    val radius = size.width / 2f
                                    val dx = offset.x - radius
                                    val dy = offset.y - radius
                                    val dist = hypot(dx, dy)
                                    if (dist <= radius) {
                                        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                        if (angle < 0) angle += 360f
                                        hue = angle
                                        saturation = (dist / radius).coerceIn(0f, 1f)
                                    }
                                }
                            }
                            .pointerInput(Unit) {
                                detectDragGestures { change, _ ->
                                    val radius = size.width / 2f
                                    val dx = change.position.x - radius
                                    val dy = change.position.y - radius
                                    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                    if (angle < 0) angle += 360f
                                    hue = angle
                                    saturation = (hypot(dx, dy) / radius).coerceIn(0f, 1f)
                                    change.consume()
                                }
                            }
                    ) {
                        val radius = size.width / 2f
                        val center = Offset(radius, radius)

                        // Outer Hue Sweep gradient
                        val hueColors = listOf(
                            Color.Red, Color.Yellow, Color.Green,
                            Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                        )
                        drawCircle(
                            brush = Brush.sweepGradient(hueColors, center),
                            radius = radius,
                            center = center
                        )

                        // Saturation Radial gradient (White in center to transparent on edges)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White, Color.White.copy(alpha = 0f)),
                                center = center,
                                radius = radius
                            ),
                            radius = radius,
                            center = center
                        )

                        // Inner border
                        drawCircle(
                            color = Color.White.copy(alpha = 0.4f),
                            radius = radius,
                            center = center,
                            style = Stroke(width = 1.5.dp.toPx())
                        )

                        // Draggable thumb indicator
                        val thumbAngleRad = Math.toRadians(hue.toDouble())
                        val thumbDist = saturation * radius
                        val thumbX = radius + (thumbDist * cos(thumbAngleRad)).toFloat()
                        val thumbY = radius + (thumbDist * sin(thumbAngleRad)).toFloat()

                        drawCircle(
                            color = Color.Black,
                            radius = 11.dp.toPx(),
                            center = Offset(thumbX, thumbY),
                            style = Stroke(width = 2.5.dp.toPx())
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 9.dp.toPx(),
                            center = Offset(thumbX, thumbY),
                            style = Stroke(width = 2.dp.toPx())
                        )
                        drawCircle(
                            color = currentColor,
                            radius = 7.dp.toPx(),
                            center = Offset(thumbX, thumbY)
                        )
                    }
                }

                // 2. Brightness / Value Slider
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Parlaklık",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = value,
                        onValueChange = { value = it },
                        valueRange = 0.05f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = MarkupAccentGreen
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(currentColor) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MarkupAccentGreen,
                    contentColor = MarkupAccentOnGreen
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(stringResource(R.string.save_label), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_label), color = Color.White.copy(alpha = 0.8f))
            }
        },
        containerColor = Color(0xFF191E28)
    )
}
