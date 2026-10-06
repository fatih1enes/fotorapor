package com.fatihenes.photoreport.core.media.geometry

import android.graphics.PointF
import android.graphics.RectF
import com.fatihenes.photoreport.core.model.MarkupItem
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class NormalizedPoint(val x: Float, val y: Float)
data class NormalizedRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f

    fun inflate(amount: Float): NormalizedRect = NormalizedRect(
        left - amount, top - amount, right + amount, bottom + amount
    )
}

object MarkupGeometry {

    private const val BASE_TOUCH_TOLERANCE = 0.04f

    fun getItemBounds(item: MarkupItem): NormalizedRect {
        return when (item) {
            is MarkupItem.Arrow -> {
                if (item.points.isNotEmpty()) {
                    val minX = item.points.minOf { it.first }
                    val maxX = item.points.maxOf { it.first }
                    val minY = item.points.minOf { it.second }
                    val maxY = item.points.maxOf { it.second }
                    NormalizedRect(minX, minY, maxX, maxY).inflate(0.02f)
                } else {
                    NormalizedRect(
                        minOf(item.startX, item.endX), minOf(item.startY, item.endY),
                        maxOf(item.startX, item.endX), maxOf(item.startY, item.endY)
                    ).inflate(0.015f)
                }
            }

            is MarkupItem.Rectangle -> NormalizedRect(
                minOf(item.left, item.right), minOf(item.top, item.bottom),
                maxOf(item.left, item.right), maxOf(item.top, item.bottom)
            ).inflate(0.008f)

            is MarkupItem.Circle -> NormalizedRect(
                item.centerX - item.radius, item.centerY - item.radius,
                item.centerX + item.radius, item.centerY + item.radius
            )

            is MarkupItem.Line -> NormalizedRect(
                minOf(item.startX, item.endX), minOf(item.startY, item.endY),
                maxOf(item.startX, item.endX), maxOf(item.startY, item.endY)
            ).inflate(0.015f)

            is MarkupItem.Freehand -> {
                if (item.points.isEmpty()) return NormalizedRect(0f, 0f, 0f, 0f)
                val minX = item.points.minOf { it.first }
                val maxX = item.points.maxOf { it.first }
                val minY = item.points.minOf { it.second }
                val maxY = item.points.maxOf { it.second }
                NormalizedRect(minX, minY, maxX, maxY).inflate(0.015f)
            }

            is MarkupItem.Highlighter -> {
                if (item.points.isEmpty()) return NormalizedRect(0f, 0f, 0f, 0f)
                val minX = item.points.minOf { it.first }
                val maxX = item.points.maxOf { it.first }
                val minY = item.points.minOf { it.second }
                val maxY = item.points.maxOf { it.second }
                NormalizedRect(minX, minY, maxX, maxY).inflate(0.025f)
            }

            is MarkupItem.TextCallout -> {
                val estHalfW = (item.text.length * 0.018f).coerceIn(0.06f, 0.4f)
                val estHalfH = 0.035f
                NormalizedRect(item.x - estHalfW, item.y - estHalfH, item.x + estHalfW, item.y + estHalfH)
            }

            is MarkupItem.Callout -> {
                val estHalfW = (item.text.length * 0.018f).coerceIn(0.06f, 0.4f)
                val estHalfH = 0.035f
                val minX = minOf(item.boxX - estHalfW, item.anchorX)
                val maxX = maxOf(item.boxX + estHalfW, item.anchorX)
                val minY = minOf(item.boxY - estHalfH, item.anchorY)
                val maxY = maxOf(item.boxY + estHalfH, item.anchorY)
                NormalizedRect(minX, minY, maxX, maxY).inflate(0.015f)
            }

            is MarkupItem.NumberedPin -> {
                val r = (item.strokeWidthNormalized * 0.8f).coerceIn(0.025f, 0.06f)
                NormalizedRect(item.x - r, item.y - r, item.x + r, item.y + r)
            }

            is MarkupItem.IssueMarker -> {
                val r = (item.strokeWidthNormalized * 0.8f).coerceIn(0.025f, 0.06f)
                NormalizedRect(item.x - r, item.y - r, item.x + r, item.y + r)
            }

            is MarkupItem.StatusMarker -> {
                val hw = 0.065f
                val hh = 0.035f
                NormalizedRect(item.x - hw, item.y - hh, item.x + hw, item.y + hh)
            }

            is MarkupItem.Measurement -> {
                NormalizedRect(
                    minOf(item.startX, item.endX), minOf(item.startY, item.endY),
                    maxOf(item.startX, item.endX), maxOf(item.startY, item.endY)
                ).inflate(0.02f)
            }

            is MarkupItem.BlurRect -> NormalizedRect(
                minOf(item.left, item.right), minOf(item.top, item.bottom),
                maxOf(item.left, item.right), maxOf(item.top, item.bottom)
            )

            is MarkupItem.BlurPath -> {
                if (item.points.isEmpty()) return NormalizedRect(0f, 0f, 0f, 0f)
                val minX = item.points.minOf { it.first }
                val maxX = item.points.maxOf { it.first }
                val minY = item.points.minOf { it.second }
                val maxY = item.points.maxOf { it.second }
                NormalizedRect(minX, minY, maxX, maxY).inflate(item.strokeWidthNormalized / 2f)
            }
        }
    }

    fun getItemCenter(item: MarkupItem): NormalizedPoint {
        return when (item) {
            is MarkupItem.Arrow -> {
                if (item.points.isNotEmpty()) {
                    NormalizedPoint(item.points.map { it.first }.average().toFloat(), item.points.map { it.second }.average().toFloat())
                } else {
                    NormalizedPoint((item.startX + item.endX) / 2f, (item.startY + item.endY) / 2f)
                }
            }
            is MarkupItem.Rectangle -> NormalizedPoint((item.left + item.right) / 2f, (item.top + item.bottom) / 2f)
            is MarkupItem.Circle -> NormalizedPoint(item.centerX, item.centerY)
            is MarkupItem.Line -> NormalizedPoint((item.startX + item.endX) / 2f, (item.startY + item.endY) / 2f)
            is MarkupItem.Freehand -> {
                if (item.points.isEmpty()) NormalizedPoint(0.5f, 0.5f)
                else NormalizedPoint(item.points.map { it.first }.average().toFloat(), item.points.map { it.second }.average().toFloat())
            }
            is MarkupItem.Highlighter -> {
                if (item.points.isEmpty()) NormalizedPoint(0.5f, 0.5f)
                else NormalizedPoint(item.points.map { it.first }.average().toFloat(), item.points.map { it.second }.average().toFloat())
            }
            is MarkupItem.BlurPath -> {
                if (item.points.isEmpty()) NormalizedPoint(0.5f, 0.5f)
                else NormalizedPoint(item.points.map { it.first }.average().toFloat(), item.points.map { it.second }.average().toFloat())
            }
            is MarkupItem.TextCallout -> NormalizedPoint(item.x, item.y)
            is MarkupItem.Callout -> NormalizedPoint(item.boxX, item.boxY)
            is MarkupItem.NumberedPin -> NormalizedPoint(item.x, item.y)
            is MarkupItem.IssueMarker -> NormalizedPoint(item.x, item.y)
            is MarkupItem.StatusMarker -> NormalizedPoint(item.x, item.y)
            is MarkupItem.Measurement -> NormalizedPoint((item.startX + item.endX) / 2f, (item.startY + item.endY) / 2f)
            is MarkupItem.BlurRect -> NormalizedPoint((item.left + item.right) / 2f, (item.top + item.bottom) / 2f)
        }
    }

    /**
     * Hit tests a point in normalized coordinates against a transformed MarkupItem.
     * Takes into account rotation, scaling, and canvas zoom.
     */
    fun hitTest(item: MarkupItem, px: Float, py: Float, canvasZoom: Float = 1f): Boolean {
        if (!item.isVisible) return false

        val center = getItemCenter(item)
        val tolerance = (BASE_TOUCH_TOLERANCE / canvasZoom).coerceAtLeast(0.015f)

        // Transform touch point to object local coordinates (inverse rotate and scale around center)
        val localPoint = toLocalCoordinates(px, py, center.x, center.y, item.rotation, item.scale)

        return when (item) {
            is MarkupItem.Arrow -> {
                if (item.points.isNotEmpty()) {
                    item.points.indices.any { i ->
                        if (i == 0) {
                            dist(localPoint.x, localPoint.y, item.points[0].first, item.points[0].second) < tolerance
                        } else {
                            distToSegment(localPoint.x, localPoint.y,
                                item.points[i - 1].first, item.points[i - 1].second,
                                item.points[i].first, item.points[i].second) < tolerance
                        }
                    }
                } else {
                    distToSegment(localPoint.x, localPoint.y, item.startX, item.startY, item.endX, item.endY) < tolerance
                }
            }
            is MarkupItem.Rectangle -> {
                val l = minOf(item.left, item.right) - tolerance
                val r = maxOf(item.left, item.right) + tolerance
                val t = minOf(item.top, item.bottom) - tolerance
                val b = maxOf(item.top, item.bottom) + tolerance
                localPoint.x in l..r && localPoint.y in t..b
            }
            is MarkupItem.Circle -> {
                val dist = sqrt((localPoint.x - item.centerX).pow(2) + (localPoint.y - item.centerY).pow(2))
                dist <= item.radius + tolerance
            }
            is MarkupItem.Line -> distToSegment(localPoint.x, localPoint.y, item.startX, item.startY, item.endX, item.endY) < tolerance
            is MarkupItem.Freehand -> {
                item.points.indices.any { i ->
                    if (i == 0) {
                        dist(localPoint.x, localPoint.y, item.points[0].first, item.points[0].second) < tolerance
                    } else {
                        distToSegment(localPoint.x, localPoint.y,
                            item.points[i - 1].first, item.points[i - 1].second,
                            item.points[i].first, item.points[i].second) < tolerance
                    }
                }
            }
            is MarkupItem.Highlighter -> {
                item.points.indices.any { i ->
                    if (i == 0) {
                        dist(localPoint.x, localPoint.y, item.points[0].first, item.points[0].second) < tolerance * 1.5f
                    } else {
                        distToSegment(localPoint.x, localPoint.y,
                            item.points[i - 1].first, item.points[i - 1].second,
                            item.points[i].first, item.points[i].second) < tolerance * 1.5f
                    }
                }
            }
            is MarkupItem.BlurPath -> {
                item.points.indices.any { i ->
                    if (i == 0) {
                        dist(localPoint.x, localPoint.y, item.points[0].first, item.points[0].second) < tolerance + item.strokeWidthNormalized / 2f
                    } else {
                        distToSegment(localPoint.x, localPoint.y,
                            item.points[i - 1].first, item.points[i - 1].second,
                            item.points[i].first, item.points[i].second) < tolerance + item.strokeWidthNormalized / 2f
                    }
                }
            }
            is MarkupItem.TextCallout -> {
                val estHalfW = (item.text.length * 0.018f).coerceIn(0.06f, 0.4f) + tolerance
                val estHalfH = 0.035f + tolerance
                abs(localPoint.x - item.x) <= estHalfW && abs(localPoint.y - item.y) <= estHalfH
            }
            is MarkupItem.Callout -> {
                val estHalfW = (item.text.length * 0.018f).coerceIn(0.06f, 0.4f) + tolerance
                val estHalfH = 0.035f + tolerance
                val insideBox = abs(localPoint.x - item.boxX) <= estHalfW && abs(localPoint.y - item.boxY) <= estHalfH
                val nearLeader = distToSegment(localPoint.x, localPoint.y, item.boxX, item.boxY, item.anchorX, item.anchorY) < tolerance
                insideBox || nearLeader
            }
            is MarkupItem.NumberedPin -> {
                val r = (item.strokeWidthNormalized * 0.8f).coerceIn(0.025f, 0.06f) + tolerance
                dist(localPoint.x, localPoint.y, item.x, item.y) <= r
            }
            is MarkupItem.IssueMarker -> {
                val r = (item.strokeWidthNormalized * 0.8f).coerceIn(0.025f, 0.06f) + tolerance
                dist(localPoint.x, localPoint.y, item.x, item.y) <= r
            }
            is MarkupItem.StatusMarker -> {
                val hw = 0.065f + tolerance
                val hh = 0.035f + tolerance
                abs(localPoint.x - item.x) <= hw && abs(localPoint.y - item.y) <= hh
            }
            is MarkupItem.Measurement -> {
                distToSegment(localPoint.x, localPoint.y, item.startX, item.startY, item.endX, item.endY) < tolerance
            }
            is MarkupItem.BlurRect -> {
                val l = minOf(item.left, item.right) - tolerance
                val r = maxOf(item.left, item.right) + tolerance
                val t = minOf(item.top, item.bottom) - tolerance
                val b = maxOf(item.top, item.bottom) + tolerance
                localPoint.x in l..r && localPoint.y in t..b
            }
        }
    }

    fun findTopmostHitItem(items: List<MarkupItem>, px: Float, py: Float, canvasZoom: Float = 1f): MarkupItem? {
        // Search in descending Z-index / reverse list order (topmost first)
        return items.sortedByDescending { it.zIndex }
            .find { hitTest(it, px, py, canvasZoom) }
    }

    private fun toLocalCoordinates(
        px: Float, py: Float,
        cx: Float, cy: Float,
        rotationDeg: Float, scale: Float
    ): NormalizedPoint {
        val dx = px - cx
        val dy = py - cy

        val rad = -Math.toRadians(rotationDeg.toDouble())
        val cosA = cos(rad).toFloat()
        val sinA = sin(rad).toFloat()

        val rotatedX = (dx * cosA - dy * sinA)
        val rotatedY = (dx * sinA + dy * cosA)

        val unscaledX = if (scale != 0f) rotatedX / scale else rotatedX
        val unscaledY = if (scale != 0f) rotatedY / scale else rotatedY

        return NormalizedPoint(cx + unscaledX, cy + unscaledY)
    }

    fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return sqrt((x2 - x1).pow(2) + (y2 - y1).pow(2))
    }

    fun distToSegment(px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val l2 = (x2 - x1).pow(2) + (y2 - y1).pow(2)
        if (l2 == 0f) return dist(px, py, x1, y1)
        var t = ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / l2
        t = t.coerceIn(0f, 1f)
        val projX = x1 + t * (x2 - x1)
        val projY = y1 + t * (y2 - y1)
        return dist(px, py, projX, projY)
    }
}
