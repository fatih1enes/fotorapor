@file:Suppress("MagicNumber", "TooManyFunctions", "TooGenericExceptionCaught", "LongMethod", "CyclomaticComplexMethod")
package com.fatihenes.photoreport.core.media

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import com.fatihenes.photoreport.core.model.ArrowHeadStyle
import com.fatihenes.photoreport.core.model.BrushType
import com.fatihenes.photoreport.core.model.IssueType
import com.fatihenes.photoreport.core.model.MarkupItem
import com.fatihenes.photoreport.core.model.MarkupTextStyle
import com.fatihenes.photoreport.core.model.PinShape
import com.fatihenes.photoreport.core.model.StatusType
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object PhotoMarkupRenderer {

    fun applyMarkupsToBitmap(
        source: Bitmap,
        markups: List<MarkupItem>
    ): Bitmap {
        if (markups.isEmpty()) return source

        val mutableBitmap = if (source.isMutable) source else source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val width = mutableBitmap.width.toFloat()
        val height = mutableBitmap.height.toFloat()
        val minDim = minOf(width, height)

        // 1. Process Blurs / Freehand Blurs / Redactions first directly on pixels
        val visibleItems = markups.filter { it.isVisible }.sortedBy { it.zIndex }
        visibleItems.forEach { item ->
            when (item) {
                is MarkupItem.BlurRect -> PhotoBlurEngine.applyRedaction(mutableBitmap, item, width, height)
                is MarkupItem.BlurPath -> PhotoBlurEngine.applyFreehandBlur(mutableBitmap, item, width, height)
                else -> Unit
            }
        }

        // 2. Render all vector layers on top in ascending zIndex order
        visibleItems.forEach { item ->
            if (item is MarkupItem.BlurRect || item is MarkupItem.BlurPath) return@forEach // Processed above

            canvas.save()
            val center = getItemCenter(item, width, height)
            canvas.rotate(item.rotation, center.x, center.y)
            canvas.scale(item.scale, item.scale, center.x, center.y)

            when (item) {
                is MarkupItem.Arrow -> drawArrow(canvas, item, width, height, minDim)
                is MarkupItem.Rectangle -> drawRectangle(canvas, item, width, height, minDim)
                is MarkupItem.Circle -> drawCircle(canvas, item, width, height, minDim)
                is MarkupItem.Line -> drawLine(canvas, item, width, height, minDim)
                is MarkupItem.Freehand -> drawFreehand(canvas, item, width, height, minDim)
                is MarkupItem.Highlighter -> drawHighlighter(canvas, item, width, height, minDim)
                is MarkupItem.TextCallout -> drawTextCallout(canvas, item, width, height, minDim)
                is MarkupItem.Callout -> drawCallout(canvas, item, width, height, minDim)
                is MarkupItem.NumberedPin -> drawNumberedPin(canvas, item, width, height, minDim)
                is MarkupItem.IssueMarker -> drawIssueMarker(canvas, item, width, height, minDim)
                is MarkupItem.StatusMarker -> drawStatusMarker(canvas, item, width, height, minDim)
                is MarkupItem.Measurement -> drawMeasurement(canvas, item, width, height, minDim)
                is MarkupItem.BlurRect, is MarkupItem.BlurPath -> Unit
            }
            canvas.restore()
        }

        return mutableBitmap
    }

    private fun getItemCenter(item: MarkupItem, width: Float, height: Float): android.graphics.PointF {
        return when (item) {
            is MarkupItem.Arrow -> {
                if (item.points.isNotEmpty()) {
                    val avgX = item.points.map { it.first }.average().toFloat()
                    val avgY = item.points.map { it.second }.average().toFloat()
                    android.graphics.PointF(avgX * width, avgY * height)
                } else {
                    android.graphics.PointF((item.startX + item.endX) / 2f * width, (item.startY + item.endY) / 2f * height)
                }
            }
            is MarkupItem.Rectangle -> android.graphics.PointF((item.left + item.right) / 2f * width, (item.top + item.bottom) / 2f * height)
            is MarkupItem.Circle -> android.graphics.PointF(item.centerX * width, item.centerY * height)
            is MarkupItem.Line -> android.graphics.PointF((item.startX + item.endX) / 2f * width, (item.startY + item.endY) / 2f * height)
            is MarkupItem.Freehand -> {
                if (item.points.isEmpty()) return android.graphics.PointF(0f, 0f)
                val avgX = item.points.map { it.first }.average().toFloat()
                val avgY = item.points.map { it.second }.average().toFloat()
                android.graphics.PointF(avgX * width, avgY * height)
            }
            is MarkupItem.Highlighter -> {
                if (item.points.isEmpty()) return android.graphics.PointF(0f, 0f)
                val avgX = item.points.map { it.first }.average().toFloat()
                val avgY = item.points.map { it.second }.average().toFloat()
                android.graphics.PointF(avgX * width, avgY * height)
            }
            is MarkupItem.BlurPath -> {
                if (item.points.isEmpty()) return android.graphics.PointF(0f, 0f)
                val avgX = item.points.map { it.first }.average().toFloat()
                val avgY = item.points.map { it.second }.average().toFloat()
                android.graphics.PointF(avgX * width, avgY * height)
            }
            is MarkupItem.TextCallout -> android.graphics.PointF(item.x * width, item.y * height)
            is MarkupItem.Callout -> android.graphics.PointF(item.boxX * width, item.boxY * height)
            is MarkupItem.NumberedPin -> android.graphics.PointF(item.x * width, item.y * height)
            is MarkupItem.IssueMarker -> android.graphics.PointF(item.x * width, item.y * height)
            is MarkupItem.StatusMarker -> android.graphics.PointF(item.x * width, item.y * height)
            is MarkupItem.Measurement -> android.graphics.PointF((item.startX + item.endX) / 2f * width, (item.startY + item.endY) / 2f * height)
            is MarkupItem.BlurRect -> android.graphics.PointF((item.left + item.right) / 2f * width, (item.top + item.bottom) / 2f * height)
        }
    }

    private fun drawArrow(canvas: Canvas, item: MarkupItem.Arrow, width: Float, height: Float, minDim: Float) {
        val strokePx = (item.strokeWidthNormalized * minDim).coerceAtLeast(4f)
        val alphaInt = (item.opacity * 255).toInt().coerceIn(0, 255)
        val arrowHeadLength = maxOf(minDim * 0.045f, strokePx * 3.5f)

        if (item.points.size >= 2) {
            // Fluid natural curved arrow
            val p0 = item.points.first()
            val last = item.points.last()
            val prevPoint = if (item.points.size >= 3) item.points[item.points.size - 2] else p0
            val fromX = prevPoint.first * width
            val fromY = prevPoint.second * height
            val toX = last.first * width
            val toY = last.second * height

            val angle = atan2((toY - fromY).toDouble(), (toX - fromX).toDouble())
            val shaftTrim = arrowHeadLength * 0.65f
            val shaftEndX = (toX - shaftTrim * cos(angle)).toFloat()
            val shaftEndY = (toY - shaftTrim * sin(angle)).toFloat()

            val path = Path()
            path.moveTo(p0.first * width, p0.second * height)
            for (i in 1 until item.points.size - 1) {
                val prev = item.points[i - 1]
                val curr = item.points[i]
                val midX = (prev.first + curr.first) / 2f * width
                val midY = (prev.second + curr.second) / 2f * height
                path.quadTo(prev.first * width, prev.second * height, midX, midY)
            }
            if (item.points.size > 2) {
                val secondLast = item.points[item.points.size - 2]
                path.quadTo(secondLast.first * width, secondLast.second * height, shaftEndX, shaftEndY)
            } else {
                path.lineTo(shaftEndX, shaftEndY)
            }

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = item.colorArgb
                alpha = alphaInt
                strokeWidth = strokePx
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                setShadowLayer(strokePx * 0.8f + 4f, 0f, strokePx * 0.3f, Color.argb((alphaInt * 0.5f).toInt(), 0, 0, 0))
            }
            canvas.drawPath(path, paint)

            // Sharp arrowhead at (toX, toY)
            drawArrowHead(canvas, fromX, fromY, toX, toY, arrowHeadLength, item.colorArgb, alphaInt)
        } else {
            val startX = item.startX * width
            val startY = item.startY * height
            val endX = item.endX * width
            val endY = item.endY * height

            val angle = atan2((endY - startY).toDouble(), (endX - startX).toDouble())
            val shaftTrim = arrowHeadLength * 0.65f
            val shaftEndX = (endX - shaftTrim * cos(angle)).toFloat()
            val shaftEndY = (endY - shaftTrim * sin(angle)).toFloat()

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = item.colorArgb
                alpha = alphaInt
                strokeWidth = strokePx
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                setShadowLayer(strokePx * 0.8f + 4f, 0f, strokePx * 0.3f, Color.argb((alphaInt * 0.5f).toInt(), 0, 0, 0))
            }
            canvas.drawLine(startX, startY, shaftEndX, shaftEndY, paint)

            drawArrowHead(canvas, startX, startY, endX, endY, arrowHeadLength, item.colorArgb, alphaInt)

            if (item.headStyle == ArrowHeadStyle.DOUBLE || item.headStyle == ArrowHeadStyle.BIDIRECTIONAL) {
                drawArrowHead(canvas, endX, endY, startX, startY, arrowHeadLength, item.colorArgb, alphaInt)
            }
        }
    }

    private fun drawArrowHead(
        canvas: Canvas, fromX: Float, fromY: Float, toX: Float, toY: Float,
        arrowHeadLength: Float, colorArgb: Int, alphaInt: Int
    ) {
        val angle = atan2((toY - fromY).toDouble(), (toX - fromX).toDouble())
        val headAngle = 0.54

        val x1 = toX - arrowHeadLength * cos(angle - headAngle).toFloat()
        val y1 = toY - arrowHeadLength * sin(angle - headAngle).toFloat()
        val x2 = toX - arrowHeadLength * cos(angle + headAngle).toFloat()
        val y2 = toY - arrowHeadLength * sin(angle + headAngle).toFloat()

        val headPath = Path().apply {
            moveTo(toX, toY)
            lineTo(x1, y1)
            lineTo(x2, y2)
            close()
        }

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorArgb
            alpha = alphaInt
            style = Paint.Style.FILL
            setShadowLayer(arrowHeadLength * 0.3f + 4f, 0f, arrowHeadLength * 0.15f, Color.argb((alphaInt * 0.5f).toInt(), 0, 0, 0))
        }
        canvas.drawPath(headPath, fillPaint)
    }

    private fun drawRectangle(canvas: Canvas, item: MarkupItem.Rectangle, width: Float, height: Float, minDim: Float) {
        val left = minOf(item.left, item.right) * width
        val right = maxOf(item.left, item.right) * width
        val top = minOf(item.top, item.bottom) * height
        val bottom = maxOf(item.top, item.bottom) * height
        val strokePx = (item.strokeWidthNormalized * minDim).coerceAtLeast(4f)
        val cornerRadius = minDim * item.cornerRadius
        val rect = RectF(left, top, right, bottom)
        val alphaInt = (item.opacity * 255).toInt().coerceIn(0, 255)

        if (item.isFilled && item.fillColorArgb != 0) {
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = item.fillColorArgb
                alpha = (alphaInt * 0.4f).toInt()
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, fillPaint)
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = item.colorArgb
            alpha = alphaInt
            strokeWidth = strokePx
            style = Paint.Style.STROKE
            setShadowLayer(strokePx * 0.8f + 4f, 0f, strokePx * 0.3f, Color.argb((alphaInt * 0.5f).toInt(), 0, 0, 0))
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
    }

    private fun drawCircle(canvas: Canvas, item: MarkupItem.Circle, width: Float, height: Float, minDim: Float) {
        val cx = item.centerX * width
        val cy = item.centerY * height
        val radius = item.radius * minDim
        val strokePx = (item.strokeWidthNormalized * minDim).coerceAtLeast(4f)
        val alphaInt = (item.opacity * 255).toInt().coerceIn(0, 255)

        if (item.isFilled && item.fillColorArgb != 0) {
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = item.fillColorArgb
                alpha = (alphaInt * 0.4f).toInt()
                style = Paint.Style.FILL
            }
            canvas.drawCircle(cx, cy, radius, fillPaint)
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = item.colorArgb
            alpha = alphaInt
            strokeWidth = strokePx
            style = Paint.Style.STROKE
            setShadowLayer(strokePx * 0.8f + 4f, 0f, strokePx * 0.3f, Color.argb((alphaInt * 0.5f).toInt(), 0, 0, 0))
        }
        canvas.drawCircle(cx, cy, radius, paint)
    }

    private fun drawLine(canvas: Canvas, item: MarkupItem.Line, width: Float, height: Float, minDim: Float) {
        val startX = item.startX * width
        val startY = item.startY * height
        val endX = item.endX * width
        val endY = item.endY * height
        val strokePx = (item.strokeWidthNormalized * minDim).coerceAtLeast(4f)
        val alphaInt = (item.opacity * 255).toInt().coerceIn(0, 255)

        val effect = when {
            item.isDashed -> DashPathEffect(floatArrayOf(strokePx * 3, strokePx * 2), 0f)
            item.isDotted -> DashPathEffect(floatArrayOf(strokePx, strokePx * 2), 0f)
            else -> null
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = item.colorArgb
            alpha = alphaInt
            strokeWidth = strokePx
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            pathEffect = effect
            setShadowLayer(strokePx * 0.8f + 4f, 0f, strokePx * 0.3f, Color.argb((alphaInt * 0.5f).toInt(), 0, 0, 0))
        }
        canvas.drawLine(startX, startY, endX, endY, paint)
    }

    private fun drawFreehand(canvas: Canvas, item: MarkupItem.Freehand, width: Float, height: Float, minDim: Float) {
        if (item.points.size < 2) return
        val strokePx = (item.strokeWidthNormalized * minDim).coerceAtLeast(4f)
        val path = buildSmoothPath(item.points, width, height)
        val alphaInt = (item.opacity * 255).toInt().coerceIn(0, 255)

        when (item.brushType) {
            BrushType.NEON -> {
                val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = item.colorArgb
                    alpha = alphaInt
                    strokeWidth = strokePx * 3.5f
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    maskFilter = BlurMaskFilter(strokePx * 1.5f, BlurMaskFilter.Blur.NORMAL)
                }
                canvas.drawPath(path, glowPaint)

                val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    alpha = alphaInt
                    strokeWidth = strokePx * 0.8f
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
                canvas.drawPath(path, corePaint)
            }
            BrushType.DASHED -> {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = item.colorArgb
                    alpha = alphaInt
                    strokeWidth = strokePx
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    pathEffect = DashPathEffect(floatArrayOf(strokePx * 3, strokePx * 2), 0f)
                }
                canvas.drawPath(path, paint)
            }
            BrushType.DOTTED -> {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = item.colorArgb
                    alpha = alphaInt
                    strokeWidth = strokePx
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    pathEffect = DashPathEffect(floatArrayOf(strokePx, strokePx * 2), 0f)
                }
                canvas.drawPath(path, paint)
            }
            else -> {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = item.colorArgb
                    alpha = alphaInt
                    strokeWidth = strokePx
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
                canvas.drawPath(path, paint)
            }
        }
    }

    private fun drawHighlighter(canvas: Canvas, item: MarkupItem.Highlighter, width: Float, height: Float, minDim: Float) {
        if (item.points.size < 2) return
        val strokePx = (item.strokeWidthNormalized * minDim).coerceAtLeast(18f)
        val path = buildSmoothPath(item.points, width, height)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = item.colorArgb
            alpha = (item.opacity * 255).toInt().coerceIn(40, 160)
            strokeWidth = strokePx
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(path, paint)
    }

    private fun drawTextCallout(canvas: Canvas, item: MarkupItem.TextCallout, width: Float, height: Float, minDim: Float) {
        if (item.text.isBlank()) return
        val x = item.x * width
        val y = item.y * height
        val fontSize = (item.fontSizeNormalized * minDim).coerceIn(24f, 120f)
        val alphaInt = (item.opacity * 255).toInt().coerceIn(0, 255)

        val tfStyle = when {
            item.isBold && item.isItalic -> Typeface.BOLD_ITALIC
            item.isBold -> Typeface.BOLD
            item.isItalic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (item.style == MarkupTextStyle.OUTLINE) item.colorArgb else Color.WHITE
            alpha = alphaInt
            textSize = fontSize
            typeface = Typeface.create(Typeface.SANS_SERIF, tfStyle)
            if (item.style == MarkupTextStyle.TRANSPARENT) {
                setShadowLayer(fontSize * 0.15f + 4f, 0f, fontSize * 0.05f, Color.argb((alphaInt * 0.7f).toInt(), 0, 0, 0))
            }
        }

        val textWidth = textPaint.measureText(item.text)
        val padH = fontSize * 0.55f
        val padV = fontSize * 0.35f
        val pillRect = RectF(x - padH, y - fontSize - padV, x + textWidth + padH, y + padV)
        val radius = fontSize * 0.3f

        when (item.style) {
            MarkupTextStyle.BADGE -> {
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = item.colorArgb
                    alpha = alphaInt
                    style = Paint.Style.FILL
                    setShadowLayer(8f, 2f, 4f, Color.argb((alphaInt * 0.5f).toInt(), 0, 0, 0))
                }
                canvas.drawRoundRect(pillRect, radius, radius, bgPaint)
                canvas.drawText(item.text, x, y - padV * 0.2f, textPaint)
            }
            MarkupTextStyle.FROSTED -> {
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    alpha = (alphaInt * 0.8f).toInt()
                    style = Paint.Style.FILL
                }
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = item.colorArgb
                    alpha = alphaInt
                    strokeWidth = 4f
                    style = Paint.Style.STROKE
                }
                canvas.drawRoundRect(pillRect, radius, radius, bgPaint)
                canvas.drawRoundRect(pillRect, radius, radius, borderPaint)
                canvas.drawText(item.text, x, y - padV * 0.2f, textPaint)
            }
            MarkupTextStyle.OUTLINE -> {
                val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    alpha = alphaInt
                    textSize = fontSize
                    typeface = Typeface.create(Typeface.SANS_SERIF, tfStyle)
                    style = Paint.Style.STROKE
                    strokeWidth = 8f
                }
                canvas.drawText(item.text, x, y - padV * 0.2f, outlinePaint)
                canvas.drawText(item.text, x, y - padV * 0.2f, textPaint)
            }
            MarkupTextStyle.TRANSPARENT -> {
                canvas.drawText(item.text, x, y - padV * 0.2f, textPaint)
            }
        }
    }

    private fun drawCallout(canvas: Canvas, item: MarkupItem.Callout, width: Float, height: Float, minDim: Float) {
        if (item.text.isBlank()) return
        val boxX = item.boxX * width
        val boxY = item.boxY * height
        val anchorX = item.anchorX * width
        val anchorY = item.anchorY * height
        val fontSize = (0.032f * minDim).coerceIn(22f, 90f)
        val alphaInt = (item.opacity * 255).toInt().coerceIn(0, 255)

        // 1. Draw Leader Line & Arrowhead to Anchor
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = item.colorArgb
            alpha = alphaInt
            strokeWidth = 4f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        val shadowLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            alpha = (alphaInt * 0.6f).toInt()
            strokeWidth = 8f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(boxX, boxY, anchorX, anchorY, shadowLinePaint)
        canvas.drawLine(boxX, boxY, anchorX, anchorY, linePaint)
        val arrowHeadLength = (0.035f * minDim).coerceIn(16f, 40f)
        drawArrowHead(canvas, boxX, boxY, anchorX, anchorY, arrowHeadLength, item.colorArgb, alphaInt)

        // 2. Draw Callout Box & Text
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = alphaInt
            textSize = fontSize
            typeface = Typeface.create(Typeface.SANS_SERIF, if (item.isBold) Typeface.BOLD else Typeface.NORMAL)
        }
        val textWidth = textPaint.measureText(item.text)
        val padH = fontSize * 0.5f
        val padV = fontSize * 0.35f
        val pillRect = RectF(boxX - padH, boxY - fontSize - padV, boxX + textWidth + padH, boxY + padV)
        val radius = fontSize * 0.35f

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = item.colorArgb
            alpha = alphaInt
            style = Paint.Style.FILL
            setShadowLayer(8f, 2f, 4f, Color.argb((alphaInt * 0.6f).toInt(), 0, 0, 0))
        }
        canvas.drawRoundRect(pillRect, radius, radius, bgPaint)
        canvas.drawText(item.text, boxX, boxY - padV * 0.2f, textPaint)
    }

    private fun drawNumberedPin(canvas: Canvas, item: MarkupItem.NumberedPin, width: Float, height: Float, minDim: Float) {
        val cx = item.x * width
        val cy = item.y * height
        val radius = (item.strokeWidthNormalized * minDim).coerceIn(24f, 70f)
        val alphaInt = (item.opacity * 255).toInt().coerceIn(0, 255)

        val r = (item.colorArgb ushr 16) and 0xFF
        val g = (item.colorArgb ushr 8) and 0xFF
        val b = item.colorArgb and 0xFF
        val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
        val isLight = luminance > 0.65

        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            alpha = (alphaInt * 0.5f).toInt()
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, radius + 3f, shadowPaint)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = item.colorArgb
            alpha = alphaInt
            style = Paint.Style.FILL
        }
        when (item.shape) {
            PinShape.SQUARE -> {
                val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
                canvas.drawRoundRect(rect, radius * 0.3f, radius * 0.3f, fillPaint)
            }
            PinShape.BADGE -> {
                val rect = RectF(cx - radius * 1.2f, cy - radius * 0.9f, cx + radius * 1.2f, cy + radius * 0.9f)
                canvas.drawRoundRect(rect, radius * 0.4f, radius * 0.4f, fillPaint)
            }
            else -> canvas.drawCircle(cx, cy, radius, fillPaint)
        }

        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isLight) Color.argb((alphaInt * 0.4f).toInt(), 0, 0, 0) else Color.WHITE
            alpha = alphaInt
            strokeWidth = 3f
            style = Paint.Style.STROKE
        }
        canvas.drawCircle(cx, cy, radius - 2f, ringPaint)

        val numStr = item.number.toString()
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isLight) Color.BLACK else Color.WHITE
            alpha = alphaInt
            textSize = radius * 1.15f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(numStr, cx, textY, textPaint)
    }

    private fun drawIssueMarker(canvas: Canvas, item: MarkupItem.IssueMarker, width: Float, height: Float, minDim: Float) {
        val cx = item.x * width
        val cy = item.y * height
        val radius = (item.strokeWidthNormalized * minDim).coerceIn(24f, 65f)
        val alphaInt = (item.opacity * 255).toInt().coerceIn(0, 255)

        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            alpha = (alphaInt * 0.7f).toInt()
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, radius + 4f, shadowPaint)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = item.colorArgb
            alpha = alphaInt
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, radius, fillPaint)

        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = alphaInt
            strokeWidth = 3f
            style = Paint.Style.STROKE
        }
        canvas.drawCircle(cx, cy, radius - 2f, ringPaint)

        // Issue text/icon label
        val label = item.customLabel ?: item.issueType.name.take(3)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = alphaInt
            textSize = radius * 0.75f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(label, cx, textY, textPaint)
    }

    private fun drawStatusMarker(canvas: Canvas, item: MarkupItem.StatusMarker, width: Float, height: Float, minDim: Float) {
        val cx = item.x * width
        val cy = item.y * height
        val hw = minDim * 0.08f
        val hh = minDim * 0.035f
        val rect = RectF(cx - hw, cy - hh, cx + hw, cy + hh)
        val alphaInt = (item.opacity * 255).toInt().coerceIn(0, 255)

        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            alpha = (alphaInt * 0.6f).toInt()
            style = Paint.Style.FILL
            setShadowLayer(8f, 2f, 4f, Color.argb((alphaInt * 0.6f).toInt(), 0, 0, 0))
        }
        canvas.drawRoundRect(rect, hh * 0.5f, hh * 0.5f, shadowPaint)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = item.colorArgb
            alpha = alphaInt
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(rect, hh * 0.5f, hh * 0.5f, fillPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = alphaInt
            textSize = hh * 1.1f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(item.statusType.name, cx, textY, textPaint)
    }

    private fun drawMeasurement(canvas: Canvas, item: MarkupItem.Measurement, width: Float, height: Float, minDim: Float) {
        val startX = item.startX * width
        val startY = item.startY * height
        val endX = item.endX * width
        val endY = item.endY * height
        val strokePx = (item.strokeWidthNormalized * minDim).coerceAtLeast(3.5f)
        val alphaInt = (item.opacity * 255).toInt().coerceIn(0, 255)

        // 1. Measurement Line
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            alpha = (alphaInt * 0.7f).toInt()
            strokeWidth = strokePx + 3f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(startX, startY, endX, endY, shadowPaint)

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = item.colorArgb
            alpha = alphaInt
            strokeWidth = strokePx
            style = Paint.Style.STROKE
        }
        canvas.drawLine(startX, startY, endX, endY, linePaint)

        // 2. End-cap tick marks
        val angle = atan2((endY - startY).toDouble(), (endX - startX).toDouble())
        val tickLen = minDim * 0.025f
        val perpAngle = angle + Math.PI / 2

        val t1X = startX + tickLen * cos(perpAngle).toFloat()
        val t1Y = startY + tickLen * sin(perpAngle).toFloat()
        val t2X = startX - tickLen * cos(perpAngle).toFloat()
        val t2Y = startY - tickLen * sin(perpAngle).toFloat()
        canvas.drawLine(t1X, t1Y, t2X, t2Y, linePaint)

        val t3X = endX + tickLen * cos(perpAngle).toFloat()
        val t3Y = endY + tickLen * sin(perpAngle).toFloat()
        val t4X = endX - tickLen * cos(perpAngle).toFloat()
        val t4Y = endY - tickLen * sin(perpAngle).toFloat()
        canvas.drawLine(t3X, t3Y, t4X, t4Y, linePaint)

        // 3. Label Badge at midpoint
        val midX = (startX + endX) / 2f
        val midY = (startY + endY) / 2f
        val text = item.formattedDistance()

        val fontSize = (0.028f * minDim).coerceIn(20f, 60f)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = alphaInt
            textSize = fontSize
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val textWidth = textPaint.measureText(text)
        val padH = fontSize * 0.45f
        val padV = fontSize * 0.3f
        val pill = RectF(midX - textWidth / 2f - padH, midY - fontSize / 2f - padV, midX + textWidth / 2f + padH, midY + fontSize / 2f + padV)

        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            alpha = (alphaInt * 0.85f).toInt()
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = item.colorArgb
            alpha = alphaInt
            strokeWidth = 2.5f
            style = Paint.Style.STROKE
        }
        canvas.drawRoundRect(pill, padV * 1.5f, padV * 1.5f, pillPaint)
        canvas.drawRoundRect(pill, padV * 1.5f, padV * 1.5f, borderPaint)

        val textY = midY - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(text, midX, textY, textPaint)
    }

    private fun buildSmoothPath(points: List<Pair<Float, Float>>, width: Float, height: Float): Path {
        val path = Path()
        if (points.isEmpty()) return path
        val first = points.first()
        path.moveTo(first.first * width, first.second * height)
        if (points.size == 1) {
            path.lineTo(first.first * width + 0.5f, first.second * height + 0.5f)
            return path
        }
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val midX = (prev.first + curr.first) / 2f * width
            val midY = (prev.second + curr.second) / 2f * height
            path.quadTo(prev.first * width, prev.second * height, midX, midY)
        }
        val last = points.last()
        path.lineTo(last.first * width, last.second * height)
        return path
    }
}
