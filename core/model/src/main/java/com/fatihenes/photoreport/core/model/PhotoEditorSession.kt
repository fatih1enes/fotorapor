package com.fatihenes.photoreport.core.model

import androidx.annotation.Keep

/**
 * Complete immutable snapshot of an editor session.
 * Used for session serialization, dirty checking, and full history undo/redo.
 */
@Keep
data class PhotoEditorSession(
    val items: List<MarkupItem> = emptyList(),
    val adjustments: PhotoAdjustments = PhotoAdjustments.DEFAULT,
    val cropState: PhotoCropState = PhotoCropState.DEFAULT
) {
    val isClean: Boolean
        get() = items.isEmpty() && adjustments.isDefault() && cropState.isDefault()

    fun isDirtyComparedTo(saved: PhotoEditorSession): Boolean = this != saved

    companion object {
        val EMPTY = PhotoEditorSession()
    }
}
