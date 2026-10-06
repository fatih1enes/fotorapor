@file:Suppress("TooManyFunctions", "TooGenericExceptionCaught")
package com.fatihenes.photoreport.feature.project.ui.markup

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fatihenes.photoreport.core.domain.repository.PhotoRepository
import com.fatihenes.photoreport.core.media.ExportOptions
import com.fatihenes.photoreport.core.media.ImageProcessor
import com.fatihenes.photoreport.core.media.ImageRotation
import com.fatihenes.photoreport.core.media.PhotoBlurEngine
import com.fatihenes.photoreport.core.media.PhotoExportEngine
import com.fatihenes.photoreport.core.model.CropAspectRatio
import com.fatihenes.photoreport.core.model.MarkupItem
import com.fatihenes.photoreport.core.model.MarkupTextStyle
import com.fatihenes.photoreport.core.model.MarkupTool
import com.fatihenes.photoreport.core.model.Photo
import com.fatihenes.photoreport.core.model.PhotoCropState
import com.fatihenes.photoreport.feature.project.ui.markup.history.EditorCommand
import com.fatihenes.photoreport.feature.project.ui.markup.history.EditorHistoryManager
import com.fatihenes.photoreport.feature.project.ui.markup.selection.MarkupSelectionController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MarkupViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
) : ViewModel() {

    private val historyManager = EditorHistoryManager()

    private val _canvasState = MutableStateFlow(MarkupCanvasState())
    val canvasState: StateFlow<MarkupCanvasState> = _canvasState.asStateFlow()

    private val _saveState = MutableStateFlow<MarkupSaveState>(MarkupSaveState.Idle)
    val saveState: StateFlow<MarkupSaveState> = _saveState.asStateFlow()

    // ── Command Operations (Undo/Redo Safe) ─────────────────────────────

    fun addItem(item: MarkupItem) {
        val newSession = historyManager.execute(EditorCommand.AddItem(item))
        _canvasState.update {
            it.copy(
                session = newSession,
                canUndo = historyManager.canUndo,
                canRedo = historyManager.canRedo,
                isDirty = historyManager.isDirty
            )
        }
    }

    fun updateItem(before: MarkupItem, after: MarkupItem) {
        val newSession = historyManager.execute(EditorCommand.ModifyItem(before, after))
        _canvasState.update {
            it.copy(
                session = newSession,
                canUndo = historyManager.canUndo,
                canRedo = historyManager.canRedo,
                isDirty = historyManager.isDirty
            )
        }
    }

    /**
     * Replaces an item directly without polluting history (e.g. during live drag/resize).
     */
    fun updateItemLive(after: MarkupItem) {
        val current = _canvasState.value.session
        val updated = current.copy(
            items = current.items.map { if (it.id == after.id) after else it }
        )
        _canvasState.update { it.copy(session = updated) }
    }

    fun deleteSelectedItem() {
        val selectedId = _canvasState.value.selectedItemId ?: return
        val item = _canvasState.value.items.find { it.id == selectedId } ?: return
        val newSession = historyManager.execute(EditorCommand.DeleteItem(item))
        _canvasState.update {
            it.copy(
                session = newSession,
                selectedItemId = null,
                canUndo = historyManager.canUndo,
                canRedo = historyManager.canRedo,
                isDirty = historyManager.isDirty
            )
        }
    }

    fun removeItem(id: String) {
        val item = _canvasState.value.items.find { it.id == id } ?: return
        val newSession = historyManager.execute(EditorCommand.DeleteItem(item))
        _canvasState.update {
            it.copy(
                session = newSession,
                selectedItemId = if (it.selectedItemId == id) null else it.selectedItemId,
                canUndo = historyManager.canUndo,
                canRedo = historyManager.canRedo,
                isDirty = historyManager.isDirty
            )
        }
    }

    fun duplicateSelectedItem() {
        val selectedId = _canvasState.value.selectedItemId ?: return
        val item = _canvasState.value.items.find { it.id == selectedId } ?: return
        val duplicated = MarkupSelectionController.duplicate(item)
        val newSession = historyManager.execute(EditorCommand.AddItem(duplicated))
        _canvasState.update {
            it.copy(
                session = newSession,
                selectedItemId = duplicated.id,
                canUndo = historyManager.canUndo,
                canRedo = historyManager.canRedo,
                isDirty = historyManager.isDirty
            )
        }
    }

    fun updateCropState(cropState: PhotoCropState) {
        val before = _canvasState.value.cropState
        val newSession = historyManager.execute(EditorCommand.UpdateCrop(before, cropState))
        _canvasState.update {
            it.copy(
                session = newSession,
                canUndo = historyManager.canUndo,
                canRedo = historyManager.canRedo,
                isDirty = historyManager.isDirty
            )
        }
    }

    fun rotateCrop90() {
        val curr = _canvasState.value.cropState
        val nextRotation = (curr.rotationDegrees + 90) % 360
        updateCropState(curr.copy(rotationDegrees = nextRotation))
    }

    fun setCropAspectRatio(aspectRatio: CropAspectRatio, imageAspect: Float = 1f) {
        val curr = _canvasState.value.cropState
        val targetPhysicalRatio = when (aspectRatio) {
            CropAspectRatio.FREE, CropAspectRatio.ORIGINAL -> null
            CropAspectRatio.SQUARE_1_1 -> 1.0f
            CropAspectRatio.RATIO_4_3 -> 4f / 3f
            CropAspectRatio.RATIO_3_4 -> 3f / 4f
            CropAspectRatio.RATIO_16_9 -> 16f / 9f
            CropAspectRatio.RATIO_9_16 -> 9f / 16f
        }

        val (l, t, r, b) = if (targetPhysicalRatio != null) {
            val normRatio = targetPhysicalRatio / imageAspect.coerceAtLeast(0.01f)
            val (normW, normH) = if (normRatio <= 1.0f) {
                normRatio to 1.0f
            } else {
                1.0f to (1.0f / normRatio)
            }
            val left = ((1.0f - normW) / 2f).coerceIn(0f, 0.49f)
            val top = ((1.0f - normH) / 2f).coerceIn(0f, 0.49f)
            listOf(left, top, (left + normW).coerceAtMost(1f), (top + normH).coerceAtMost(1f))
        } else {
            listOf(0f, 0f, 1f, 1f)
        }

        updateCropState(curr.copy(aspectRatioPreset = aspectRatio, cropLeft = l, cropTop = t, cropRight = r, cropBottom = b))
    }

    fun updateCropBoundsLive(left: Float, top: Float, right: Float, bottom: Float) {
        val current = _canvasState.value.session
        val updatedCrop = current.cropState.withCropBounds(left, top, right, bottom)
        _canvasState.update { it.copy(session = current.copy(cropState = updatedCrop)) }
    }

    fun undo() {
        historyManager.undo()?.let { newSession ->
            _canvasState.update {
                it.copy(
                    session = newSession,
                    selectedItemId = if (it.items.any { item -> item.id == it.selectedItemId }) it.selectedItemId else null,
                    canUndo = historyManager.canUndo,
                    canRedo = historyManager.canRedo,
                    isDirty = historyManager.isDirty
                )
            }
        }
    }

    fun redo() {
        historyManager.redo()?.let { newSession ->
            _canvasState.update {
                it.copy(
                    session = newSession,
                    canUndo = historyManager.canUndo,
                    canRedo = historyManager.canRedo,
                    isDirty = historyManager.isDirty
                )
            }
        }
    }

    fun clearAll() {
        val beforeItems = _canvasState.value.items
        if (beforeItems.isNotEmpty()) {
            val newSession = historyManager.execute(EditorCommand.DeleteItems(beforeItems))
            _canvasState.update {
                it.copy(
                    session = newSession,
                    selectedItemId = null,
                    canUndo = historyManager.canUndo,
                    canRedo = historyManager.canRedo,
                    isDirty = historyManager.isDirty
                )
            }
        }
    }

    // ── Tool & Parameter Selection ──────────────────────────────────────

    fun selectTool(tool: MarkupTool) {
        _canvasState.update { it.copy(selectedTool = tool, selectedItemId = null) }
    }

    fun selectColor(color: Color) {
        _canvasState.update { it.copy(selectedColor = color) }
        val selectedId = _canvasState.value.selectedItemId ?: return
        val item = _canvasState.value.items.find { it.id == selectedId } ?: return
        val updated = item.updateColor(color.toArgb())
        updateItem(item, updated)
    }

    fun setStrokeThickness(thickness: Float) {
        _canvasState.update { it.copy(strokeThickness = thickness) }
        val selectedId = _canvasState.value.selectedItemId ?: return
        val item = _canvasState.value.items.find { it.id == selectedId } ?: return
        val updated = item.updateStrokeWidth(thickness)
        updateItem(item, updated)
    }

    fun setBlurMode(mode: BlurMode) {
        _canvasState.update { it.copy(blurMode = mode) }
    }

    fun setBlurStrokeWidth(width: Float) {
        _canvasState.update { it.copy(blurStrokeWidth = width) }
    }

    fun setTextStyle(style: MarkupTextStyle) {
        _canvasState.update { it.copy(textStyle = style) }
        val selectedId = _canvasState.value.selectedItemId ?: return
        val item = _canvasState.value.items.find { it.id == selectedId } ?: return
        val updated = when (item) {
            is MarkupItem.TextCallout -> item.copy(style = style)
            else -> item
        }
        updateItem(item, updated)
    }

    fun selectItem(id: String?) {
        _canvasState.update { it.copy(selectedItemId = id) }
    }

    fun openTextEditor(position: Offset = Offset(0.5f, 0.5f), item: MarkupItem.TextCallout? = null) {
        _canvasState.update {
            it.copy(
                showTextModal = true,
                textPromptPosition = position,
                editingTextItem = item
            )
        }
    }

    fun dismissModals() {
        _canvasState.update {
            it.copy(
                showTextModal = false,
                editingTextItem = null
            )
        }
    }

    fun loadBlurredImage(context: Context, filePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rawBitmap = ImageProcessor.openInputStreamSafe(context, filePath)?.use {
                    BitmapFactory.decodeStream(it)
                } ?: return@launch
                val exifRotation = ImageProcessor.getExifRotation(context, filePath)
                val orientedBitmap = ImageRotation.rotateIfNeeded(rawBitmap, exifRotation)
                val scaleFactor = 0.2f
                val sw = (orientedBitmap.width * scaleFactor).toInt().coerceAtLeast(1)
                val sh = (orientedBitmap.height * scaleFactor).toInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(orientedBitmap, sw, sh, true)
                val blurRect = MarkupItem.BlurRect(0f, 0f, 1f, 1f, isMosaic = false)
                PhotoBlurEngine.applyRedaction(scaled, blurRect, sw.toFloat(), sh.toFloat())
                val finalBlurred = Bitmap.createScaledBitmap(scaled, orientedBitmap.width, orientedBitmap.height, true)
                _canvasState.update { it.copy(blurredImageBitmap = finalBlurred.asImageBitmap()) }
                if (rawBitmap !== orientedBitmap) rawBitmap.recycle()
                orientedBitmap.recycle()
                scaled.recycle()
            } catch (e: Exception) {
                Log.e("MarkupViewModel", "Live blur preview failed", e)
            }
        }
    }

    fun hasUnsavedChanges(): Boolean = historyManager.isDirty

    private var customPinStartNumber: Int? = null

    fun resetPinCounter() {
        customPinStartNumber = 1
    }

    fun getNextPinNumber(): Int {
        val custom = customPinStartNumber
        if (custom != null) {
            customPinStartNumber = custom + 1
            return custom
        }
        return (_canvasState.value.items.filterIsInstance<MarkupItem.NumberedPin>().maxOfOrNull { it.number } ?: 0) + 1
    }

    fun resetSaveState() { _saveState.value = MarkupSaveState.Idle }

    fun saveMarkups(
        context: Context,
        photo: Photo,
        onContentRefresh: () -> Unit,
        options: ExportOptions = ExportOptions()
    ) {
        if (_saveState.value is MarkupSaveState.Saving) return
        _saveState.value = MarkupSaveState.Saving

        viewModelScope.launch {
            val result = PhotoExportEngine.exportPhoto(
                context = context,
                photoId = photo.id,
                sourceFilePath = photo.filePath,
                session = _canvasState.value.session,
                options = options
            )

            if (result.success) {
                historyManager.markAsSaved()
                _canvasState.update { it.copy(isDirty = false) }

                if (result.outputFilePath != photo.filePath) {
                    photoRepository.updatePhotoFilePath(photo.id, result.outputFilePath)
                    photoRepository.updatePhotoRotation(photo.id, 0f)
                }
                try { coil3.SingletonImageLoader.get(context).memoryCache?.clear() } catch (_: Exception) { }
                onContentRefresh()
                _saveState.value = MarkupSaveState.Success(result.outputFilePath)
            } else {
                _saveState.value = MarkupSaveState.Error(result.errorMessage)
            }
        }
    }

    // ── Extension helpers ───────────────────────────────────────────────

    private fun MarkupItem.updateColor(newColor: Int): MarkupItem = when (this) {
        is MarkupItem.Arrow -> copy(colorArgb = newColor)
        is MarkupItem.Rectangle -> copy(colorArgb = newColor)
        is MarkupItem.Circle -> copy(colorArgb = newColor)
        is MarkupItem.Line -> copy(colorArgb = newColor)
        is MarkupItem.Freehand -> copy(colorArgb = newColor)
        is MarkupItem.Highlighter -> copy(colorArgb = newColor)
        is MarkupItem.TextCallout -> copy(colorArgb = newColor)
        is MarkupItem.Callout -> copy(colorArgb = newColor)
        is MarkupItem.NumberedPin -> copy(colorArgb = newColor)
        is MarkupItem.IssueMarker -> copy(colorArgb = newColor)
        is MarkupItem.StatusMarker -> copy(colorArgb = newColor)
        is MarkupItem.Measurement -> copy(colorArgb = newColor)
        is MarkupItem.BlurRect, is MarkupItem.BlurPath -> this
    }

    private fun MarkupItem.updateStrokeWidth(newWidth: Float): MarkupItem = when (this) {
        is MarkupItem.Arrow -> copy(strokeWidthNormalized = newWidth)
        is MarkupItem.Rectangle -> copy(strokeWidthNormalized = newWidth)
        is MarkupItem.Circle -> copy(strokeWidthNormalized = newWidth)
        is MarkupItem.Line -> copy(strokeWidthNormalized = newWidth)
        is MarkupItem.Freehand -> copy(strokeWidthNormalized = newWidth)
        is MarkupItem.Highlighter -> copy(strokeWidthNormalized = newWidth * 3f)
        is MarkupItem.TextCallout -> copy(fontSizeNormalized = newWidth * 3f)
        is MarkupItem.Callout -> copy(strokeWidthNormalized = newWidth)
        is MarkupItem.NumberedPin -> copy(strokeWidthNormalized = newWidth * 5f)
        is MarkupItem.IssueMarker -> copy(strokeWidthNormalized = newWidth * 5f)
        is MarkupItem.StatusMarker -> copy(strokeWidthNormalized = newWidth * 5f)
        is MarkupItem.Measurement -> copy(strokeWidthNormalized = newWidth)
        is MarkupItem.BlurPath -> copy(strokeWidthNormalized = newWidth * 4f)
        is MarkupItem.BlurRect -> this
    }
}
