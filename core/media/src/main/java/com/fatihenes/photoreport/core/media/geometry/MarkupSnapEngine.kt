package com.fatihenes.photoreport.core.media.geometry

import com.fatihenes.photoreport.core.model.MarkupItem
import kotlin.math.abs
import kotlin.math.roundToInt

data class SnapGuideLine(
    val isVertical: Boolean,
    val position: Float, // x if vertical, y if horizontal
    val start: Float = 0f,
    val end: Float = 1f
)

data class SnapResult(
    val snappedValue: Float,
    val didSnap: Boolean,
    val guideLines: List<SnapGuideLine> = emptyList()
)

data class PositionSnapResult(
    val snappedX: Float,
    val snappedY: Float,
    val didSnapX: Boolean,
    val didSnapY: Boolean,
    val guideLines: List<SnapGuideLine> = emptyList()
)

object MarkupSnapEngine {

    private const val DEFAULT_SNAP_TOLERANCE = 0.015f
    private const val ROTATION_SNAP_TOLERANCE_DEG = 4.0f

    private val SNAP_ANGLES = listOf(0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f, 360f)

    /**
     * Snap a moving item's center to image center (0.5), margins, and other item centers/edges.
     */
    fun snapPosition(
        targetCenter: NormalizedPoint,
        targetBounds: NormalizedRect,
        otherItems: List<MarkupItem>,
        canvasZoom: Float = 1f,
        enabled: Boolean = true
    ): PositionSnapResult {
        if (!enabled) {
            return PositionSnapResult(targetCenter.x, targetCenter.y, false, false)
        }

        val tolerance = (DEFAULT_SNAP_TOLERANCE / canvasZoom).coerceAtLeast(0.005f)
        val guides = mutableListOf<SnapGuideLine>()

        var snappedX = targetCenter.x
        var didSnapX = false

        var snappedY = targetCenter.y
        var didSnapY = false

        // 1. Center photo snapping
        if (abs(targetCenter.x - 0.5f) < tolerance) {
            snappedX = 0.5f
            didSnapX = true
            guides.add(SnapGuideLine(isVertical = true, position = 0.5f))
        }

        if (abs(targetCenter.y - 0.5f) < tolerance) {
            snappedY = 0.5f
            didSnapY = true
            guides.add(SnapGuideLine(isVertical = false, position = 0.5f))
        }

        // 2. Snap to other items
        if (!didSnapX || !didSnapY) {
            for (item in otherItems) {
                val otherCenter = MarkupGeometry.getItemCenter(item)
                val otherBounds = MarkupGeometry.getItemBounds(item)

                // Center-to-center X
                if (!didSnapX && abs(targetCenter.x - otherCenter.x) < tolerance) {
                    snappedX = otherCenter.x
                    didSnapX = true
                    guides.add(SnapGuideLine(isVertical = true, position = otherCenter.x))
                }

                // Center-to-center Y
                if (!didSnapY && abs(targetCenter.y - otherCenter.y) < tolerance) {
                    snappedY = otherCenter.y
                    didSnapY = true
                    guides.add(SnapGuideLine(isVertical = false, position = otherCenter.y))
                }

                // Edge snapping X
                if (!didSnapX) {
                    if (abs(targetBounds.left - otherBounds.left) < tolerance) {
                        val shift = otherBounds.left - targetBounds.left
                        snappedX = targetCenter.x + shift
                        didSnapX = true
                        guides.add(SnapGuideLine(isVertical = true, position = otherBounds.left))
                    } else if (abs(targetBounds.right - otherBounds.right) < tolerance) {
                        val shift = otherBounds.right - targetBounds.right
                        snappedX = targetCenter.x + shift
                        didSnapX = true
                        guides.add(SnapGuideLine(isVertical = true, position = otherBounds.right))
                    }
                }

                // Edge snapping Y
                if (!didSnapY) {
                    if (abs(targetBounds.top - otherBounds.top) < tolerance) {
                        val shift = otherBounds.top - targetBounds.top
                        snappedY = targetCenter.y + shift
                        didSnapY = true
                        guides.add(SnapGuideLine(isVertical = false, position = otherBounds.top))
                    } else if (abs(targetBounds.bottom - otherBounds.bottom) < tolerance) {
                        val shift = otherBounds.bottom - targetBounds.bottom
                        snappedY = targetCenter.y + shift
                        didSnapY = true
                        guides.add(SnapGuideLine(isVertical = false, position = otherBounds.bottom))
                    }
                }

                if (didSnapX && didSnapY) break
            }
        }

        return PositionSnapResult(
            snappedX = snappedX,
            snappedY = snappedY,
            didSnapX = didSnapX,
            didSnapY = didSnapY,
            guideLines = guides
        )
    }

    /**
     * Snap rotation angle to key angles (0, 45, 90, 135, 180, etc.).
     */
    fun snapRotation(angleDeg: Float, enabled: Boolean = true): SnapResult {
        if (!enabled) return SnapResult(angleDeg, false)

        val normalized = ((angleDeg % 360f) + 360f) % 360f
        for (snapAngle in SNAP_ANGLES) {
            if (abs(normalized - snapAngle) < ROTATION_SNAP_TOLERANCE_DEG) {
                return SnapResult(snapAngle % 360f, true)
            }
        }

        return SnapResult(angleDeg, false)
    }
}
