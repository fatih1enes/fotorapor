@file:Suppress("MagicNumber")
package com.fatihenes.photoreport.feature.project.ui.markup.toolbars

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatihenes.photoreport.core.model.CropAspectRatio
import com.fatihenes.photoreport.core.model.MarkupTool
import com.fatihenes.photoreport.core.ui.R
import com.fatihenes.photoreport.feature.project.ui.markup.BlurMode
import com.fatihenes.photoreport.feature.project.ui.markup.MarkupAccentGreen
import com.fatihenes.photoreport.feature.project.ui.markup.MarkupAccentOnGreen
import com.fatihenes.photoreport.feature.project.ui.markup.MarkupCanvasState
import com.fatihenes.photoreport.feature.project.ui.markup.MarkupThicknessSelector
import com.fatihenes.photoreport.feature.project.ui.markup.components.AdvancedColorPicker

@Composable
fun ContextualToolbars(
    canvasState: MarkupCanvasState,
    onSelectColor: (Color) -> Unit,
    onSelectThickness: (Float) -> Unit,
    onSelectBlurMode: (BlurMode) -> Unit,
    onSelectBlurStrokeWidth: (Float) -> Unit,
    onRotateCrop90: () -> Unit,
    onSetCropAspectRatio: (CropAspectRatio) -> Unit,
    onResetPinCounter: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        when (canvasState.selectedTool) {
            MarkupTool.FREEHAND, MarkupTool.ARROW, MarkupTool.LINE -> {
                // Color Palette & Thickness
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AdvancedColorPicker(
                        selectedColor = canvasState.selectedColor,
                        onSelectColor = onSelectColor,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    MarkupThicknessSelector(
                        selectedThickness = canvasState.strokeThickness,
                        onSelectThickness = onSelectThickness
                    )
                }
            }

            MarkupTool.NUMBERED_PIN -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AdvancedColorPicker(
                        selectedColor = canvasState.selectedColor,
                        onSelectColor = onSelectColor,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0x331E232F),
                        modifier = Modifier.clickable(onClick = onResetPinCounter)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "1",
                                color = MarkupAccentGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                stringResource(R.string.markup_reset_pin_counter),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            MarkupTool.TEXT -> {
                // Color Palette
                AdvancedColorPicker(
                    selectedColor = canvasState.selectedColor,
                    onSelectColor = onSelectColor,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            MarkupTool.BLUR -> {
                // Blur Mode (Box vs Freehand Brush) + Width
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SubPillChip(
                            label = stringResource(R.string.markup_blur_mode_brush),
                            isSelected = canvasState.blurMode == BlurMode.FREEHAND_BRUSH,
                            onClick = { onSelectBlurMode(BlurMode.FREEHAND_BRUSH) }
                        )
                        SubPillChip(
                            label = stringResource(R.string.markup_blur_mode_box),
                            isSelected = canvasState.blurMode == BlurMode.RECTANGLE,
                            onClick = { onSelectBlurMode(BlurMode.RECTANGLE) }
                        )
                    }

                    if (canvasState.blurMode == BlurMode.FREEHAND_BRUSH) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                stringResource(R.string.markup_blur_brush_size),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Slider(
                                value = canvasState.blurStrokeWidth,
                                onValueChange = onSelectBlurStrokeWidth,
                                valueRange = 0.02f..0.12f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = MarkupAccentGreen
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            MarkupTool.CROP -> {
                // Crop Aspect Ratios & Rotate 90
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        CropAspectRatio.FREE to R.string.markup_crop_free,
                        CropAspectRatio.ORIGINAL to R.string.markup_crop_original,
                        CropAspectRatio.SQUARE_1_1 to R.string.markup_crop_1_1,
                        CropAspectRatio.RATIO_4_3 to R.string.markup_crop_4_3,
                        CropAspectRatio.RATIO_16_9 to R.string.markup_crop_16_9
                    ).forEach { (ratio, labelRes) ->
                        SubPillChip(
                            label = stringResource(labelRes),
                            isSelected = canvasState.cropState.aspectRatioPreset == ratio,
                            onClick = { onSetCropAspectRatio(ratio) }
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x331E232F),
                        modifier = Modifier.clickable(onClick = onRotateCrop90)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.CropRotate, contentDescription = stringResource(R.string.markup_crop_rotate), tint = Color.White, modifier = Modifier.size(16.dp))
                            Text(stringResource(R.string.markup_crop_rotate), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            else -> Unit
        }
    }
}

@Composable
private fun SubPillChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MarkupAccentGreen else Color(0x331E232F))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            color = if (isSelected) MarkupAccentOnGreen else Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
