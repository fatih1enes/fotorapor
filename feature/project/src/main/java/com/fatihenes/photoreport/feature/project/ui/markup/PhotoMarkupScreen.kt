@file:Suppress("MagicNumber")
package com.fatihenes.photoreport.feature.project.ui.markup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.fatihenes.photoreport.core.model.MarkupItem
import com.fatihenes.photoreport.core.model.Photo
import com.fatihenes.photoreport.feature.project.ui.markup.components.AdvancedTextEditorModal
import com.fatihenes.photoreport.feature.project.ui.markup.toolbars.MarkupStudioHeader

/**
 * PhotoReport Site Photo Markup Editor — Fast, reliable, professional construction annotation utility.
 */
@Composable
fun PhotoMarkupScreen(
    photo: Photo,
    onDismiss: () -> Unit,
    onSaveComplete: (String) -> Unit,
    onSaveError: () -> Unit,
    onContentRefresh: () -> Unit,
    viewModel: MarkupViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    val canvasState by viewModel.canvasState.collectAsState()
    val saveState by viewModel.saveState.collectAsState()

    var showDiscardConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(saveState) {
        when (val state = saveState) {
            is MarkupSaveState.Success -> {
                onSaveComplete(state.filePath)
                viewModel.resetSaveState()
            }
            is MarkupSaveState.Error -> {
                onSaveError()
                viewModel.resetSaveState()
            }
            else -> Unit
        }
    }

    LaunchedEffect(photo.filePath) {
        viewModel.loadBlurredImage(context, photo.filePath)
    }

    val handleClose = {
        if (viewModel.hasUnsavedChanges()) {
            showDiscardConfirm = true
        } else {
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = { handleClose() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        BackHandler { handleClose() }

        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            Box(modifier = Modifier.fillMaxSize()) {

                // 1. Dominant Photo Canvas with 7-Tool Annotations & Freehand Blur
                MarkupCanvas(
                    photo = photo,
                    canvasState = canvasState,
                    onItemCreated = { item ->
                        val finalItem = if (item is MarkupItem.NumberedPin && item.number == 0) {
                            item.copy(number = viewModel.getNextPinNumber())
                        } else {
                            item
                        }
                        viewModel.addItem(finalItem)
                    },
                    onItemUpdated = { before, after ->
                        viewModel.updateItem(before, after)
                    },
                    onItemUpdatedLive = { liveItem ->
                        viewModel.updateItemLive(liveItem)
                    },
                    onCropBoundsChanged = { l, t, r, b ->
                        val curr = canvasState.cropState
                        viewModel.updateCropState(curr.withCropBounds(l, t, r, b))
                    },
                    onCropBoundsChangedLive = { l, t, r, b ->
                        viewModel.updateCropBoundsLive(l, t, r, b)
                    },
                    onSelectItem = { id ->
                        viewModel.selectItem(id)
                    },
                    onItemDeleted = { id ->
                        viewModel.removeItem(id)
                    },
                    onTextTap = { position ->
                        viewModel.openTextEditor(position)
                    },
                    onDoubleTapItem = { item ->
                        if (item is MarkupItem.TextCallout) {
                            viewModel.openTextEditor(Offset(item.x, item.y), item)
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 48.dp, bottom = 90.dp)
                )

                // 2. Minimalist Header (Close, Undo, Redo, Save)
                MarkupStudioHeader(
                    canvasState = canvasState,
                    isSaving = saveState is MarkupSaveState.Saving,
                    onClose = handleClose,
                    onUndo = viewModel::undo,
                    onRedo = viewModel::redo,
                    onSave = {
                        viewModel.saveMarkups(context, photo, onContentRefresh)
                    },
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                // 3. Focused Bottom Toolbar (7 Tools, Contextual Options, Selected Item Bar)
                MarkupToolbar(
                    canvasState = canvasState,
                    onSelectTool = viewModel::selectTool,
                    onSelectColor = viewModel::selectColor,
                    onSelectThickness = viewModel::setStrokeThickness,
                    onSelectBlurMode = viewModel::setBlurMode,
                    onSelectBlurStrokeWidth = viewModel::setBlurStrokeWidth,
                    onRotateCrop90 = viewModel::rotateCrop90,
                    onSetCropAspectRatio = viewModel::setCropAspectRatio,
                    onResetPinCounter = viewModel::resetPinCounter,
                    onDuplicateItem = viewModel::duplicateSelectedItem,
                    onDeleteItem = viewModel::deleteSelectedItem,
                    onEditItem = {
                        val item = canvasState.selectedItem
                        if (item is MarkupItem.TextCallout) {
                            viewModel.openTextEditor(Offset(item.x, item.y), item)
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )

                // 4. Quick Text Annotation Modal
                if (canvasState.showTextModal) {
                    val editItem = canvasState.editingTextItem
                    AdvancedTextEditorModal(
                        initialText = editItem?.text ?: "",
                        initialColor = if (editItem != null) Color(editItem.colorArgb) else canvasState.selectedColor,
                        initialStyle = editItem?.style ?: canvasState.textStyle,
                        initialIsBold = editItem?.isBold ?: true,
                        initialIsItalic = editItem?.isItalic ?: false,
                        onDismiss = { viewModel.dismissModals() },
                        onConfirm = { text, color, style, isBold, isItalic ->
                            if (editItem != null) {
                                val updated = editItem.copy(
                                    text = text,
                                    colorArgb = color.toArgb(),
                                    style = style,
                                    isBold = isBold,
                                    isItalic = isItalic
                                )
                                viewModel.updateItem(editItem, updated)
                            } else {
                                val newItem = MarkupItem.TextCallout(
                                    x = canvasState.textPromptPosition.x,
                                    y = canvasState.textPromptPosition.y,
                                    text = text,
                                    colorArgb = color.toArgb(),
                                    style = style,
                                    isBold = isBold,
                                    isItalic = isItalic
                                )
                                viewModel.addItem(newItem)
                            }
                            viewModel.dismissModals()
                        }
                    )
                }

                // 5. Discard Changes Confirmation Dialog
                if (showDiscardConfirm) {
                    MarkupDiscardDialog(
                        onDismiss = { showDiscardConfirm = false },
                        onConfirmDiscard = {
                            showDiscardConfirm = false
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}
