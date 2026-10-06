package com.fatihenes.photoreport.feature.project.ui.markup

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import com.fatihenes.photoreport.core.media.geometry.SnapGuideLine
import com.fatihenes.photoreport.core.media.geometry.TransformHandle
import com.fatihenes.photoreport.core.model.BrushType
import com.fatihenes.photoreport.core.model.MarkupItem
import com.fatihenes.photoreport.core.model.MarkupTextStyle
import com.fatihenes.photoreport.core.model.MarkupTool
import com.fatihenes.photoreport.core.model.MeasurementUnit
import com.fatihenes.photoreport.core.model.PhotoAdjustments
import com.fatihenes.photoreport.core.model.PhotoCropState
import com.fatihenes.photoreport.core.model.PhotoEditorSession
import com.fatihenes.photoreport.core.model.RedactionMode

/**
 * Clean, high-contrast palette optimized for construction & site photo annotations.
 */
val MarkupStudioColors = listOf(
    Color(0xFFFF1744), // Red
    Color(0xFFFFD600), // Yellow
    Color(0xFF00E676), // Green
    Color(0xFF2979FF), // Blue
    Color(0xFFFFFFFF), // White
    Color(0xFF212121), // Black
)

val MarkupAccentGreen = Color(0xFF00E676)
val MarkupAccentOnGreen = Color(0xFF0A1810)

enum class BlurMode {
    RECTANGLE,
    FREEHAND_BRUSH
}

data class MarkupCanvasState(
    val session: PhotoEditorSession = PhotoEditorSession.EMPTY,
    val selectedTool: MarkupTool = MarkupTool.FREEHAND,
    val selectedColor: Color = MarkupStudioColors[0],
    val strokeThickness: Float = 0.008f,
    val blurMode: BlurMode = BlurMode.FREEHAND_BRUSH,
    val blurStrokeWidth: Float = 0.045f,
    val textStyle: MarkupTextStyle = MarkupTextStyle.BADGE,
    val selectedItemId: String? = null,
    val activeHandle: TransformHandle? = null,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isDirty: Boolean = false,
    val showTextModal: Boolean = false,
    val textPromptPosition: Offset = Offset(0.5f, 0.5f),
    val editingTextItem: MarkupItem.TextCallout? = null,
    val snapGuides: List<SnapGuideLine> = emptyList(),
    val blurredImageBitmap: ImageBitmap? = null
) {
    val items: List<MarkupItem> get() = session.items
    val adjustments: PhotoAdjustments get() = session.adjustments
    val cropState: PhotoCropState get() = session.cropState
    val selectedItem: MarkupItem? get() = items.find { it.id == selectedItemId }
}

sealed interface MarkupSaveState {
    data object Idle : MarkupSaveState
    data object Saving : MarkupSaveState
    data class Success(val filePath: String) : MarkupSaveState
    data class Error(val message: String? = null) : MarkupSaveState
}

data class MarkupToolInfo(
    val tool: MarkupTool,
    val labelRes: Int,
    val icon: ImageVector,
    val usesColor: Boolean = true,
    val usesThickness: Boolean = true
)
