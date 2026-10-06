package com.fatihenes.photoreport.feature.project.ui.markup.selection

import com.fatihenes.photoreport.core.media.geometry.MarkupGeometry
import com.fatihenes.photoreport.core.media.geometry.NormalizedPoint
import com.fatihenes.photoreport.core.media.geometry.NormalizedRect
import com.fatihenes.photoreport.core.media.geometry.TransformHandle
import com.fatihenes.photoreport.core.model.MarkupItem
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class HandlePosition(
    val handle: TransformHandle,
    val point: NormalizedPoint
)

object MarkupSelectionController {

    private const val HANDLE_TOUCH_RADIUS = 0.045f

    /**
     * Finds which handle was touched on the selected item, or null if outside handles.
     */
    fun findTouchedHandle(
        item: MarkupItem,
        touchX: Float,
        touchY: Float,
        canvasZoom: Float = 1f
    ): TransformHandle? {
        val handles = getHandlePositions(item)
        val tolerance = (HANDLE_TOUCH_RADIUS / canvasZoom).coerceAtLeast(0.02f)

        for (h in handles) {
            val dist = sqrt((touchX - h.point.x) * (touchX - h.point.x) + (touchY - h.point.y) * (touchY - h.point.y))
            if (dist <= tolerance) {
                return h.handle
            }
        }
        return null
    }

    /**
     * Computes the 9 handle positions (8 bounding handles + 1 rotation stalk) in normalized coordinates.
     */
    fun getHandlePositions(item: MarkupItem): List<HandlePosition> {
        val bounds = MarkupGeometry.getItemBounds(item)
        val center = MarkupGeometry.getItemCenter(item)

        val unrotated = listOf(
            HandlePosition(TransformHandle.TOP_LEFT, NormalizedPoint(bounds.left, bounds.top)),
            HandlePosition(TransformHandle.TOP, NormalizedPoint(bounds.centerX, bounds.top)),
            HandlePosition(TransformHandle.TOP_RIGHT, NormalizedPoint(bounds.right, bounds.top)),
            HandlePosition(TransformHandle.RIGHT, NormalizedPoint(bounds.right, bounds.centerY)),
            HandlePosition(TransformHandle.BOTTOM_RIGHT, NormalizedPoint(bounds.right, bounds.bottom)),
            HandlePosition(TransformHandle.BOTTOM, NormalizedPoint(bounds.centerX, bounds.bottom)),
            HandlePosition(TransformHandle.BOTTOM_LEFT, NormalizedPoint(bounds.left, bounds.bottom)),
            HandlePosition(TransformHandle.LEFT, NormalizedPoint(bounds.left, bounds.centerY)),
            // Rotation stalk is placed 0.04f above top edge
            HandlePosition(TransformHandle.ROTATION, NormalizedPoint(bounds.centerX, bounds.top - 0.045f))
        )

        if (item.rotation == 0f && item.scale == 1f) {
            return unrotated
        }

        val rad = Math.toRadians(item.rotation.toDouble())
        val cosA = cos(rad).toFloat()
        val sinA = sin(rad).toFloat()

        return unrotated.map { hp ->
            val dx = (hp.point.x - center.x) * item.scale
            val dy = (hp.point.y - center.y) * item.scale
            val rx = center.x + (dx * cosA - dy * sinA)
            val ry = center.y + (dx * sinA + dy * cosA)
            HandlePosition(hp.handle, NormalizedPoint(rx, ry))
        }
    }

    // ── Layer Operations ────────────────────────────────────────────────

    fun bringForward(items: List<MarkupItem>, selectedId: String): List<MarkupItem> {
        val index = items.indexOfFirst { it.id == selectedId }
        if (index == -1 || index >= items.lastIndex) return items
        val mutable = items.toMutableList()
        val item = mutable.removeAt(index)
        mutable.add(index + 1, item)
        return reindexZ(mutable)
    }

    fun bringToFront(items: List<MarkupItem>, selectedId: String): List<MarkupItem> {
        val index = items.indexOfFirst { it.id == selectedId }
        if (index == -1 || index == items.lastIndex) return items
        val mutable = items.toMutableList()
        val item = mutable.removeAt(index)
        mutable.add(item)
        return reindexZ(mutable)
    }

    fun sendBackward(items: List<MarkupItem>, selectedId: String): List<MarkupItem> {
        val index = items.indexOfFirst { it.id == selectedId }
        if (index <= 0) return items
        val mutable = items.toMutableList()
        val item = mutable.removeAt(index)
        mutable.add(index - 1, item)
        return reindexZ(mutable)
    }

    fun sendToBack(items: List<MarkupItem>, selectedId: String): List<MarkupItem> {
        val index = items.indexOfFirst { it.id == selectedId }
        if (index <= 0) return items
        val mutable = items.toMutableList()
        val item = mutable.removeAt(index)
        mutable.add(0, item)
        return reindexZ(mutable)
    }

    private fun reindexZ(items: List<MarkupItem>): List<MarkupItem> {
        return items.mapIndexed { idx, item -> item.withZIndex(idx) }
    }

    // ── Duplicate & Modifications ───────────────────────────────────────

    fun duplicate(item: MarkupItem): MarkupItem {
        val offset = 0.03f
        val newId = UUID.randomUUID().toString()
        val moved = item.translateBy(offset, offset)
        return when (moved) {
            is MarkupItem.Arrow -> moved.copy(id = newId)
            is MarkupItem.Rectangle -> moved.copy(id = newId)
            is MarkupItem.Circle -> moved.copy(id = newId)
            is MarkupItem.Line -> moved.copy(id = newId)
            is MarkupItem.Freehand -> moved.copy(id = newId)
            is MarkupItem.Highlighter -> moved.copy(id = newId)
            is MarkupItem.TextCallout -> moved.copy(id = newId)
            is MarkupItem.Callout -> moved.copy(id = newId)
            is MarkupItem.NumberedPin -> moved.copy(id = newId, number = moved.number + 1)
            is MarkupItem.IssueMarker -> moved.copy(id = newId)
            is MarkupItem.StatusMarker -> moved.copy(id = newId)
            is MarkupItem.Measurement -> moved.copy(id = newId)
            is MarkupItem.BlurRect -> moved.copy(id = newId)
            is MarkupItem.BlurPath -> moved.copy(id = newId)
        }
    }
}
