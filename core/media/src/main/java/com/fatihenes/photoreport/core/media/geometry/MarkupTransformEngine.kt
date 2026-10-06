package com.fatihenes.photoreport.core.media.geometry

import com.fatihenes.photoreport.core.model.MarkupItem
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class TransformHandle {
    TOP_LEFT,
    TOP,
    TOP_RIGHT,
    RIGHT,
    BOTTOM_RIGHT,
    BOTTOM,
    BOTTOM_LEFT,
    LEFT,
    ROTATION
}

object MarkupTransformEngine {

    private const val MIN_ITEM_DIMENSION = 0.02f
    private const val MIN_SCALE = 0.1f
    private const val MAX_SCALE = 10.0f

    /**
     * Resizes a MarkupItem from a dragged corner or edge handle.
     */
    fun resizeItem(
        item: MarkupItem,
        handle: TransformHandle,
        dx: Float,
        dy: Float,
        lockAspectRatio: Boolean = false
    ): MarkupItem {
        if (item.isLocked) return item

        return when (item) {
            is MarkupItem.Rectangle -> {
                var l = minOf(item.left, item.right)
                var r = maxOf(item.left, item.right)
                var t = minOf(item.top, item.bottom)
                var b = maxOf(item.top, item.bottom)

                when (handle) {
                    TransformHandle.TOP_LEFT -> { l += dx; t += dy }
                    TransformHandle.TOP -> { t += dy }
                    TransformHandle.TOP_RIGHT -> { r += dx; t += dy }
                    TransformHandle.RIGHT -> { r += dx }
                    TransformHandle.BOTTOM_RIGHT -> { r += dx; b += dy }
                    TransformHandle.BOTTOM -> { b += dy }
                    TransformHandle.BOTTOM_LEFT -> { l += dx; b += dy }
                    TransformHandle.LEFT -> { l += dx }
                    TransformHandle.ROTATION -> Unit
                }

                if (r - l < MIN_ITEM_DIMENSION) r = l + MIN_ITEM_DIMENSION
                if (b - t < MIN_ITEM_DIMENSION) b = t + MIN_ITEM_DIMENSION

                item.copy(left = l, top = t, right = r, bottom = b)
            }

            is MarkupItem.Circle -> {
                val scaleDelta = (dx + dy) / 2f
                val newRadius = (item.radius + scaleDelta).coerceIn(0.01f, 0.8f)
                item.copy(radius = newRadius)
            }

            is MarkupItem.Arrow -> {
                when (handle) {
                    TransformHandle.TOP_LEFT, TransformHandle.LEFT -> item.copy(startX = item.startX + dx, startY = item.startY + dy)
                    TransformHandle.BOTTOM_RIGHT, TransformHandle.RIGHT -> item.copy(endX = item.endX + dx, endY = item.endY + dy)
                    else -> {
                        val factor = 1f + (dx + dy)
                        item.scaleBy(factor.coerceIn(MIN_SCALE, MAX_SCALE))
                    }
                }
            }

            is MarkupItem.Line -> {
                when (handle) {
                    TransformHandle.TOP_LEFT, TransformHandle.LEFT -> item.copy(startX = item.startX + dx, startY = item.startY + dy)
                    TransformHandle.BOTTOM_RIGHT, TransformHandle.RIGHT -> item.copy(endX = item.endX + dx, endY = item.endY + dy)
                    else -> {
                        val factor = 1f + (dx + dy)
                        item.scaleBy(factor.coerceIn(MIN_SCALE, MAX_SCALE))
                    }
                }
            }

            is MarkupItem.Measurement -> {
                when (handle) {
                    TransformHandle.TOP_LEFT, TransformHandle.LEFT -> item.copy(startX = item.startX + dx, startY = item.startY + dy)
                    TransformHandle.BOTTOM_RIGHT, TransformHandle.RIGHT -> item.copy(endX = item.endX + dx, endY = item.endY + dy)
                    else -> {
                        val factor = 1f + (dx + dy)
                        item.scaleBy(factor.coerceIn(MIN_SCALE, MAX_SCALE))
                    }
                }
            }

            is MarkupItem.BlurRect -> {
                var l = minOf(item.left, item.right)
                var r = maxOf(item.left, item.right)
                var t = minOf(item.top, item.bottom)
                var b = maxOf(item.top, item.bottom)

                when (handle) {
                    TransformHandle.TOP_LEFT -> { l += dx; t += dy }
                    TransformHandle.TOP -> { t += dy }
                    TransformHandle.TOP_RIGHT -> { r += dx; t += dy }
                    TransformHandle.RIGHT -> { r += dx }
                    TransformHandle.BOTTOM_RIGHT -> { r += dx; b += dy }
                    TransformHandle.BOTTOM -> { b += dy }
                    TransformHandle.BOTTOM_LEFT -> { l += dx; b += dy }
                    TransformHandle.LEFT -> { l += dx }
                    TransformHandle.ROTATION -> Unit
                }

                if (r - l < MIN_ITEM_DIMENSION) r = l + MIN_ITEM_DIMENSION
                if (b - t < MIN_ITEM_DIMENSION) b = t + MIN_ITEM_DIMENSION

                item.copy(left = l, top = t, right = r, bottom = b)
            }

            else -> {
                // Freehand, Highlighter, Text, Callout, Pin, Markers, Arrow uniform scale
                val bounds = MarkupGeometry.getItemBounds(item)
                val baseDim = maxOf(bounds.width, bounds.height).coerceAtLeast(0.08f)
                val dragSign = when (handle) {
                    TransformHandle.BOTTOM_RIGHT, TransformHandle.RIGHT, TransformHandle.BOTTOM -> if (dx + dy > 0) 1f else -1f
                    TransformHandle.TOP_LEFT, TransformHandle.LEFT, TransformHandle.TOP -> if (dx + dy < 0) 1f else -1f
                    TransformHandle.TOP_RIGHT -> if (dx - dy > 0) 1f else -1f
                    TransformHandle.BOTTOM_LEFT -> if (-dx + dy > 0) 1f else -1f
                    TransformHandle.ROTATION -> 0f
                }
                val dragDist = sqrt(dx * dx + dy * dy)
                val scaleFactor = (1f + dragSign * (dragDist / baseDim)).coerceIn(MIN_SCALE, MAX_SCALE)
                item.scaleBy(scaleFactor)
            }
        }
    }

    /**
     * Computes new rotation in degrees based on drag touch point relative to center.
     */
    fun calculateRotation(
        center: NormalizedPoint,
        touchX: Float,
        touchY: Float,
        snapEnabled: Boolean = true
    ): Float {
        val dx = touchX - center.x
        val dy = touchY - center.y
        val angleRad = atan2(dy.toDouble(), dx.toDouble())
        val rawDegrees = Math.toDegrees(angleRad).toFloat() + 90f // 0 is top
        val snapResult = MarkupSnapEngine.snapRotation(rawDegrees, snapEnabled)
        return snapResult.snappedValue
    }
}
