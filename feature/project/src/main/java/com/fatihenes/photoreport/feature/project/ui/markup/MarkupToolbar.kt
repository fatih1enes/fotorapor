@file:Suppress("MagicNumber")
package com.fatihenes.photoreport.feature.project.ui.markup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporMotion
import com.fatihenes.photoreport.core.model.CropAspectRatio
import com.fatihenes.photoreport.core.model.MarkupTool
import com.fatihenes.photoreport.feature.project.ui.markup.toolbars.ContextualToolbars
import com.fatihenes.photoreport.feature.project.ui.markup.toolbars.MarkupPrimaryToolbar
import com.fatihenes.photoreport.feature.project.ui.markup.toolbars.SelectedItemToolbar

@Composable
fun MarkupToolbar(
    canvasState: MarkupCanvasState,
    onSelectTool: (MarkupTool) -> Unit,
    onSelectColor: (Color) -> Unit,
    onSelectThickness: (Float) -> Unit,
    onSelectBlurMode: (BlurMode) -> Unit,
    onSelectBlurStrokeWidth: (Float) -> Unit,
    onRotateCrop90: () -> Unit,
    onSetCropAspectRatio: (CropAspectRatio) -> Unit,
    onResetPinCounter: () -> Unit = {},
    onDuplicateItem: () -> Unit,
    onDeleteItem: () -> Unit,
    onEditItem: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color(0x99000000), Color(0xFA000000))))
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Contextual Action Bar for Selected Item (Edit, Duplicate, Delete)
            SelectedItemToolbar(
                selectedItem = canvasState.selectedItem,
                onDuplicate = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onDuplicateItem()
                },
                onDelete = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDeleteItem()
                },
                onEdit = onEditItem
            )

            // 2. Contextual Sub-Toolbar for Active Tool (Color, Thickness, Blur Options, Crop Presets)
            AnimatedVisibility(
                visible = canvasState.selectedItemId == null,
                enter = fadeIn(tween(FotoRaporMotion.DURATION_SHORT)) + slideInVertically { it / 2 },
                exit = fadeOut(tween(FotoRaporMotion.DURATION_SHORT)) + slideOutVertically { it / 2 }
            ) {
                ContextualToolbars(
                    canvasState = canvasState,
                    onSelectColor = onSelectColor,
                    onSelectThickness = onSelectThickness,
                    onSelectBlurMode = onSelectBlurMode,
                    onSelectBlurStrokeWidth = onSelectBlurStrokeWidth,
                    onRotateCrop90 = onRotateCrop90,
                    onSetCropAspectRatio = onSetCropAspectRatio,
                    onResetPinCounter = onResetPinCounter
                )
            }

            // 3. Primary 7-Tool Bar (Pen, Arrow, Line, Number, Text, Blur, Crop)
            MarkupPrimaryToolbar(
                selectedTool = canvasState.selectedTool,
                onSelectTool = { tool ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelectTool(tool)
                }
            )
        }
    }
}
