package com.fatihenes.photoreport.core.model

import androidx.annotation.Keep
import java.util.UUID

@Keep
enum class MarkupTool {
    FREEHAND,
    NEON_PEN,
    HIGHLIGHTER,
    ARROW,
    RECTANGLE,
    CIRCLE,
    LINE,
    TEXT,
    CALLOUT,
    NUMBERED_PIN,
    ISSUE_MARKER,
    STATUS_MARKER,
    MEASUREMENT,
    BLUR,
    MOSAIC,
    REDACTION,
    CROP,
    ADJUST
}

@Keep
enum class MarkupTextStyle {
    BADGE,
    FROSTED,
    OUTLINE,
    TRANSPARENT
}

@Keep
enum class BrushType {
    PEN,
    MARKER,
    HIGHLIGHTER,
    NEON,
    SOFT_BRUSH,
    PENCIL,
    DASHED,
    DOTTED
}

@Keep
enum class ArrowHeadStyle {
    SINGLE,
    DOUBLE,
    BIDIRECTIONAL
}

@Keep
enum class PinShape {
    CIRCLE,
    SQUARE,
    HEXAGON,
    BADGE
}

@Keep
enum class IssueType {
    DEFECT,
    WARNING,
    ELECTRICAL,
    STRUCTURAL,
    WATER,
    CRACK,
    MISSING,
    SAFETY,
    APPROVED,
    FAILED,
    CHECKED
}

@Keep
enum class StatusType {
    BEFORE,
    AFTER,
    COMPLETED,
    PENDING,
    URGENT
}

@Keep
enum class MeasurementUnit(val symbol: String, val scaleToMeter: Float) {
    M("m", 1.0f),
    CM("cm", 0.01f),
    MM("mm", 0.001f),
    FT("ft", 0.3048f),
    IN("in", 0.0254f)
}

@Keep
enum class RedactionMode {
    GAUSSIAN_BLUR,
    MOSAIC,
    SOLID_BLACK,
    SOLID_COLOR
}

@Keep
sealed interface MarkupItem {
    val id: String
    val colorArgb: Int
    val strokeWidthNormalized: Float
    val rotation: Float
    val scale: Float
    val opacity: Float
    val isLocked: Boolean
    val isVisible: Boolean
    val zIndex: Int

    fun translateBy(dx: Float, dy: Float): MarkupItem
    fun rotateBy(degrees: Float): MarkupItem
    fun scaleBy(factor: Float): MarkupItem
    fun withZIndex(newZIndex: Int): MarkupItem
    fun withLocked(locked: Boolean): MarkupItem
    fun withVisibility(visible: Boolean): MarkupItem
    fun withOpacity(newOpacity: Float): MarkupItem

    @Keep
    data class Arrow(
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float,
        override val colorArgb: Int,
        override val strokeWidthNormalized: Float = 0.008f,
        val points: List<Pair<Float, Float>> = emptyList(),
        override val rotation: Float = 0f,
        override val scale: Float = 1f,
        override val opacity: Float = 1f,
        override val isLocked: Boolean = false,
        override val isVisible: Boolean = true,
        override val zIndex: Int = 0,
        val headStyle: ArrowHeadStyle = ArrowHeadStyle.SINGLE,
        override val id: String = UUID.randomUUID().toString()
    ) : MarkupItem {
        override fun translateBy(dx: Float, dy: Float): MarkupItem = copy(
            startX = startX + dx, startY = startY + dy,
            endX = endX + dx, endY = endY + dy,
            points = points.map { (px, py) -> (px + dx) to (py + dy) }
        )
        override fun rotateBy(degrees: Float): MarkupItem = copy(rotation = rotation + degrees)
        override fun scaleBy(factor: Float): MarkupItem = copy(scale = scale * factor)
        override fun withZIndex(newZIndex: Int): MarkupItem = copy(zIndex = newZIndex)
        override fun withLocked(locked: Boolean): MarkupItem = copy(isLocked = locked)
        override fun withVisibility(visible: Boolean): MarkupItem = copy(isVisible = visible)
        override fun withOpacity(newOpacity: Float): MarkupItem = copy(opacity = newOpacity)
    }

    @Keep
    data class Rectangle(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
        override val colorArgb: Int,
        override val strokeWidthNormalized: Float = 0.008f,
        override val rotation: Float = 0f,
        override val scale: Float = 1f,
        override val opacity: Float = 1f,
        override val isLocked: Boolean = false,
        override val isVisible: Boolean = true,
        override val zIndex: Int = 0,
        val isFilled: Boolean = false,
        val fillColorArgb: Int = 0,
        val cornerRadius: Float = 0.018f,
        override val id: String = UUID.randomUUID().toString()
    ) : MarkupItem {
        override fun translateBy(dx: Float, dy: Float): MarkupItem = copy(
            left = left + dx, top = top + dy,
            right = right + dx, bottom = bottom + dy
        )
        override fun rotateBy(degrees: Float): MarkupItem = copy(rotation = rotation + degrees)
        override fun scaleBy(factor: Float): MarkupItem = copy(scale = scale * factor)
        override fun withZIndex(newZIndex: Int): MarkupItem = copy(zIndex = newZIndex)
        override fun withLocked(locked: Boolean): MarkupItem = copy(isLocked = locked)
        override fun withVisibility(visible: Boolean): MarkupItem = copy(isVisible = visible)
        override fun withOpacity(newOpacity: Float): MarkupItem = copy(opacity = newOpacity)
    }

    @Keep
    data class Circle(
        val centerX: Float,
        val centerY: Float,
        val radius: Float,
        override val colorArgb: Int,
        override val strokeWidthNormalized: Float = 0.008f,
        override val rotation: Float = 0f,
        override val scale: Float = 1f,
        override val opacity: Float = 1f,
        override val isLocked: Boolean = false,
        override val isVisible: Boolean = true,
        override val zIndex: Int = 0,
        val isFilled: Boolean = false,
        val fillColorArgb: Int = 0,
        override val id: String = UUID.randomUUID().toString()
    ) : MarkupItem {
        override fun translateBy(dx: Float, dy: Float): MarkupItem = copy(
            centerX = centerX + dx, centerY = centerY + dy
        )
        override fun rotateBy(degrees: Float): MarkupItem = copy(rotation = rotation + degrees)
        override fun scaleBy(factor: Float): MarkupItem = copy(scale = scale * factor)
        override fun withZIndex(newZIndex: Int): MarkupItem = copy(zIndex = newZIndex)
        override fun withLocked(locked: Boolean): MarkupItem = copy(isLocked = locked)
        override fun withVisibility(visible: Boolean): MarkupItem = copy(isVisible = visible)
        override fun withOpacity(newOpacity: Float): MarkupItem = copy(opacity = newOpacity)
    }

    @Keep
    data class Line(
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float,
        override val colorArgb: Int,
        override val strokeWidthNormalized: Float = 0.008f,
        override val rotation: Float = 0f,
        override val scale: Float = 1f,
        override val opacity: Float = 1f,
        override val isLocked: Boolean = false,
        override val isVisible: Boolean = true,
        override val zIndex: Int = 0,
        val isDashed: Boolean = false,
        val isDotted: Boolean = false,
        override val id: String = UUID.randomUUID().toString()
    ) : MarkupItem {
        override fun translateBy(dx: Float, dy: Float): MarkupItem = copy(
            startX = startX + dx, startY = startY + dy,
            endX = endX + dx, endY = endY + dy
        )
        override fun rotateBy(degrees: Float): MarkupItem = copy(rotation = rotation + degrees)
        override fun scaleBy(factor: Float): MarkupItem = copy(scale = scale * factor)
        override fun withZIndex(newZIndex: Int): MarkupItem = copy(zIndex = newZIndex)
        override fun withLocked(locked: Boolean): MarkupItem = copy(isLocked = locked)
        override fun withVisibility(visible: Boolean): MarkupItem = copy(isVisible = visible)
        override fun withOpacity(newOpacity: Float): MarkupItem = copy(opacity = newOpacity)
    }

    @Keep
    data class Freehand(
        val points: List<Pair<Float, Float>>,
        override val colorArgb: Int,
        override val strokeWidthNormalized: Float = 0.008f,
        override val rotation: Float = 0f,
        override val scale: Float = 1f,
        override val opacity: Float = 1f,
        override val isLocked: Boolean = false,
        override val isVisible: Boolean = true,
        override val zIndex: Int = 0,
        val isNeon: Boolean = false,
        val brushType: BrushType = if (isNeon) BrushType.NEON else BrushType.PEN,
        override val id: String = UUID.randomUUID().toString()
    ) : MarkupItem {
        override fun translateBy(dx: Float, dy: Float): MarkupItem = copy(
            points = points.map { it.first + dx to it.second + dy }
        )
        override fun rotateBy(degrees: Float): MarkupItem = copy(rotation = rotation + degrees)
        override fun scaleBy(factor: Float): MarkupItem = copy(scale = scale * factor)
        override fun withZIndex(newZIndex: Int): MarkupItem = copy(zIndex = newZIndex)
        override fun withLocked(locked: Boolean): MarkupItem = copy(isLocked = locked)
        override fun withVisibility(visible: Boolean): MarkupItem = copy(isVisible = visible)
        override fun withOpacity(newOpacity: Float): MarkupItem = copy(opacity = newOpacity)
    }

    @Keep
    data class Highlighter(
        val points: List<Pair<Float, Float>>,
        override val colorArgb: Int,
        override val strokeWidthNormalized: Float = 0.024f,
        override val rotation: Float = 0f,
        override val scale: Float = 1f,
        override val opacity: Float = 0.45f,
        override val isLocked: Boolean = false,
        override val isVisible: Boolean = true,
        override val zIndex: Int = 0,
        override val id: String = UUID.randomUUID().toString()
    ) : MarkupItem {
        override fun translateBy(dx: Float, dy: Float): MarkupItem = copy(
            points = points.map { it.first + dx to it.second + dy }
        )
        override fun rotateBy(degrees: Float): MarkupItem = copy(rotation = rotation + degrees)
        override fun scaleBy(factor: Float): MarkupItem = copy(scale = scale * factor)
        override fun withZIndex(newZIndex: Int): MarkupItem = copy(zIndex = newZIndex)
        override fun withLocked(locked: Boolean): MarkupItem = copy(isLocked = locked)
        override fun withVisibility(visible: Boolean): MarkupItem = copy(isVisible = visible)
        override fun withOpacity(newOpacity: Float): MarkupItem = copy(opacity = newOpacity)
    }

    @Keep
    data class TextCallout(
        val x: Float,
        val y: Float,
        val text: String,
        override val colorArgb: Int,
        override val strokeWidthNormalized: Float = 0.024f,
        override val rotation: Float = 0f,
        override val scale: Float = 1f,
        override val opacity: Float = 1f,
        override val isLocked: Boolean = false,
        override val isVisible: Boolean = true,
        override val zIndex: Int = 0,
        val style: MarkupTextStyle = MarkupTextStyle.BADGE,
        val isBold: Boolean = true,
        val isItalic: Boolean = false,
        val fontSizeNormalized: Float = 0.035f,
        val backgroundColorArgb: Int = 0,
        override val id: String = UUID.randomUUID().toString()
    ) : MarkupItem {
        override fun translateBy(dx: Float, dy: Float): MarkupItem = copy(
            x = x + dx, y = y + dy
        )
        override fun rotateBy(degrees: Float): MarkupItem = copy(rotation = rotation + degrees)
        override fun scaleBy(factor: Float): MarkupItem = copy(scale = scale * factor)
        override fun withZIndex(newZIndex: Int): MarkupItem = copy(zIndex = newZIndex)
        override fun withLocked(locked: Boolean): MarkupItem = copy(isLocked = locked)
        override fun withVisibility(visible: Boolean): MarkupItem = copy(isVisible = visible)
        override fun withOpacity(newOpacity: Float): MarkupItem = copy(opacity = newOpacity)
    }

    @Keep
    data class Callout(
        val boxX: Float,
        val boxY: Float,
        val text: String,
        val anchorX: Float,
        val anchorY: Float,
        override val colorArgb: Int,
        override val strokeWidthNormalized: Float = 0.008f,
        override val rotation: Float = 0f,
        override val scale: Float = 1f,
        override val opacity: Float = 1f,
        override val isLocked: Boolean = false,
        override val isVisible: Boolean = true,
        override val zIndex: Int = 0,
        val style: MarkupTextStyle = MarkupTextStyle.BADGE,
        val isBold: Boolean = true,
        override val id: String = UUID.randomUUID().toString()
    ) : MarkupItem {
        override fun translateBy(dx: Float, dy: Float): MarkupItem = copy(
            boxX = boxX + dx, boxY = boxY + dy,
            anchorX = anchorX + dx, anchorY = anchorY + dy
        )
        override fun rotateBy(degrees: Float): MarkupItem = copy(rotation = rotation + degrees)
        override fun scaleBy(factor: Float): MarkupItem = copy(scale = scale * factor)
        override fun withZIndex(newZIndex: Int): MarkupItem = copy(zIndex = newZIndex)
        override fun withLocked(locked: Boolean): MarkupItem = copy(isLocked = locked)
        override fun withVisibility(visible: Boolean): MarkupItem = copy(isVisible = visible)
        override fun withOpacity(newOpacity: Float): MarkupItem = copy(opacity = newOpacity)
    }

    @Keep
    data class NumberedPin(
        val x: Float,
        val y: Float,
        val number: Int,
        override val colorArgb: Int,
        override val strokeWidthNormalized: Float = 0.045f,
        override val rotation: Float = 0f,
        override val scale: Float = 1f,
        override val opacity: Float = 1f,
        override val isLocked: Boolean = false,
        override val isVisible: Boolean = true,
        override val zIndex: Int = 0,
        val shape: PinShape = PinShape.CIRCLE,
        override val id: String = UUID.randomUUID().toString()
    ) : MarkupItem {
        override fun translateBy(dx: Float, dy: Float): MarkupItem = copy(
            x = x + dx, y = y + dy
        )
        override fun rotateBy(degrees: Float): MarkupItem = copy(rotation = rotation + degrees)
        override fun scaleBy(factor: Float): MarkupItem = copy(scale = scale * factor)
        override fun withZIndex(newZIndex: Int): MarkupItem = copy(zIndex = newZIndex)
        override fun withLocked(locked: Boolean): MarkupItem = copy(isLocked = locked)
        override fun withVisibility(visible: Boolean): MarkupItem = copy(isVisible = visible)
        override fun withOpacity(newOpacity: Float): MarkupItem = copy(opacity = newOpacity)
    }

    @Keep
    data class IssueMarker(
        val x: Float,
        val y: Float,
        val issueType: IssueType,
        val customLabel: String? = null,
        override val colorArgb: Int,
        override val strokeWidthNormalized: Float = 0.045f,
        override val rotation: Float = 0f,
        override val scale: Float = 1f,
        override val opacity: Float = 1f,
        override val isLocked: Boolean = false,
        override val isVisible: Boolean = true,
        override val zIndex: Int = 0,
        override val id: String = UUID.randomUUID().toString()
    ) : MarkupItem {
        override fun translateBy(dx: Float, dy: Float): MarkupItem = copy(
            x = x + dx, y = y + dy
        )
        override fun rotateBy(degrees: Float): MarkupItem = copy(rotation = rotation + degrees)
        override fun scaleBy(factor: Float): MarkupItem = copy(scale = scale * factor)
        override fun withZIndex(newZIndex: Int): MarkupItem = copy(zIndex = newZIndex)
        override fun withLocked(locked: Boolean): MarkupItem = copy(isLocked = locked)
        override fun withVisibility(visible: Boolean): MarkupItem = copy(isVisible = visible)
        override fun withOpacity(newOpacity: Float): MarkupItem = copy(opacity = newOpacity)
    }

    @Keep
    data class StatusMarker(
        val x: Float,
        val y: Float,
        val statusType: StatusType,
        override val colorArgb: Int,
        override val strokeWidthNormalized: Float = 0.05f,
        override val rotation: Float = 0f,
        override val scale: Float = 1f,
        override val opacity: Float = 1f,
        override val isLocked: Boolean = false,
        override val isVisible: Boolean = true,
        override val zIndex: Int = 0,
        override val id: String = UUID.randomUUID().toString()
    ) : MarkupItem {
        override fun translateBy(dx: Float, dy: Float): MarkupItem = copy(
            x = x + dx, y = y + dy
        )
        override fun rotateBy(degrees: Float): MarkupItem = copy(rotation = rotation + degrees)
        override fun scaleBy(factor: Float): MarkupItem = copy(scale = scale * factor)
        override fun withZIndex(newZIndex: Int): MarkupItem = copy(zIndex = newZIndex)
        override fun withLocked(locked: Boolean): MarkupItem = copy(isLocked = locked)
        override fun withVisibility(visible: Boolean): MarkupItem = copy(isVisible = visible)
        override fun withOpacity(newOpacity: Float): MarkupItem = copy(opacity = newOpacity)
    }

    @Keep
    data class Measurement(
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float,
        val realDistance: Float,
        val unit: MeasurementUnit = MeasurementUnit.M,
        val label: String? = null,
        override val colorArgb: Int,
        override val strokeWidthNormalized: Float = 0.008f,
        override val rotation: Float = 0f,
        override val scale: Float = 1f,
        override val opacity: Float = 1f,
        override val isLocked: Boolean = false,
        override val isVisible: Boolean = true,
        override val zIndex: Int = 0,
        override val id: String = UUID.randomUUID().toString()
    ) : MarkupItem {
        override fun translateBy(dx: Float, dy: Float): MarkupItem = copy(
            startX = startX + dx, startY = startY + dy,
            endX = endX + dx, endY = endY + dy
        )
        override fun rotateBy(degrees: Float): MarkupItem = copy(rotation = rotation + degrees)
        override fun scaleBy(factor: Float): MarkupItem = copy(scale = scale * factor)
        override fun withZIndex(newZIndex: Int): MarkupItem = copy(zIndex = newZIndex)
        override fun withLocked(locked: Boolean): MarkupItem = copy(isLocked = locked)
        override fun withVisibility(visible: Boolean): MarkupItem = copy(isVisible = visible)
        override fun withOpacity(newOpacity: Float): MarkupItem = copy(opacity = newOpacity)

        fun formattedDistance(): String {
            return if (!label.isNullOrBlank()) {
                label
            } else {
                val formattedNum = if (realDistance >= 10) {
                    String.format(java.util.Locale.US, "%.1f", realDistance)
                } else {
                    String.format(java.util.Locale.US, "%.2f", realDistance)
                }
                "$formattedNum ${unit.symbol}"
            }
        }
    }

    @Keep
    data class BlurRect(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
        val isMosaic: Boolean = false,
        val redactionType: RedactionMode = if (isMosaic) RedactionMode.MOSAIC else RedactionMode.GAUSSIAN_BLUR,
        val strength: Float = 1.0f,
        override val colorArgb: Int = 0,
        override val strokeWidthNormalized: Float = 0f,
        override val rotation: Float = 0f,
        override val scale: Float = 1f,
        override val opacity: Float = 1f,
        override val isLocked: Boolean = false,
        override val isVisible: Boolean = true,
        override val zIndex: Int = 0,
        override val id: String = UUID.randomUUID().toString()
    ) : MarkupItem {
        override fun translateBy(dx: Float, dy: Float): MarkupItem = copy(
            left = left + dx, top = top + dy,
            right = right + dx, bottom = bottom + dy
        )
        override fun rotateBy(degrees: Float): MarkupItem = copy(rotation = rotation + degrees)
        override fun scaleBy(factor: Float): MarkupItem = copy(scale = scale * factor)
        override fun withZIndex(newZIndex: Int): MarkupItem = copy(zIndex = newZIndex)
        override fun withLocked(locked: Boolean): MarkupItem = copy(isLocked = locked)
        override fun withVisibility(visible: Boolean): MarkupItem = copy(isVisible = visible)
        override fun withOpacity(newOpacity: Float): MarkupItem = copy(opacity = newOpacity)
    }

    @Keep
    data class BlurPath(
        val points: List<Pair<Float, Float>>,
        override val strokeWidthNormalized: Float = 0.045f,
        val strength: Float = 1.0f,
        override val colorArgb: Int = 0,
        override val rotation: Float = 0f,
        override val scale: Float = 1f,
        override val opacity: Float = 1f,
        override val isLocked: Boolean = false,
        override val isVisible: Boolean = true,
        override val zIndex: Int = 0,
        override val id: String = UUID.randomUUID().toString()
    ) : MarkupItem {
        override fun translateBy(dx: Float, dy: Float): MarkupItem = copy(
            points = points.map { (px, py) -> (px + dx) to (py + dy) }
        )
        override fun rotateBy(degrees: Float): MarkupItem = copy(rotation = rotation + degrees)
        override fun scaleBy(factor: Float): MarkupItem = copy(scale = scale * factor)
        override fun withZIndex(newZIndex: Int): MarkupItem = copy(zIndex = newZIndex)
        override fun withLocked(locked: Boolean): MarkupItem = copy(isLocked = locked)
        override fun withVisibility(visible: Boolean): MarkupItem = copy(isVisible = visible)
        override fun withOpacity(newOpacity: Float): MarkupItem = copy(opacity = newOpacity)
    }
}
