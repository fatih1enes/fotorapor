package com.fatihenes.photoreport.core.media

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.fatihenes.photoreport.core.model.MarkupItem
import com.fatihenes.photoreport.core.model.RedactionMode

object PhotoBlurEngine {

    /**
     * Applies blur, mosaic, or solid blackout directly to bitmap pixels.
     * This destructively modifies the pixel buffer ensuring redacted content is unrecoverable.
     */
    fun applyRedaction(
        bitmap: Bitmap,
        item: MarkupItem.BlurRect,
        bitmapWidth: Float,
        bitmapHeight: Float
    ) {
        val l = (minOf(item.left, item.right) * bitmapWidth).toInt().coerceIn(0, bitmap.width - 1)
        val t = (minOf(item.top, item.bottom) * bitmapHeight).toInt().coerceIn(0, bitmap.height - 1)
        val r = (maxOf(item.left, item.right) * bitmapWidth).toInt().coerceIn(l + 1, bitmap.width)
        val b = (maxOf(item.top, item.bottom) * bitmapHeight).toInt().coerceIn(t + 1, bitmap.height)
        val rectW = r - l
        val rectH = b - t
        if (rectW <= 0 || rectH <= 0) return

        when (item.redactionType) {
            RedactionMode.MOSAIC -> applyMosaic(bitmap, l, t, rectW, rectH, item.strength)
            RedactionMode.GAUSSIAN_BLUR -> applyFastGaussianBlur(bitmap, l, t, rectW, rectH, item.strength)
            RedactionMode.SOLID_BLACK -> applySolidColor(bitmap, l, t, rectW, rectH, Color.BLACK)
            RedactionMode.SOLID_COLOR -> applySolidColor(bitmap, l, t, rectW, rectH, if (item.colorArgb != 0) item.colorArgb else Color.BLACK)
        }
    }

    /**
     * Applies a freehand blur brush stroke directly to the bitmap pixels.
     * Generates a stroke alpha mask and applies Gaussian blur to the masked area destructively.
     */
    fun applyFreehandBlur(
        bitmap: Bitmap,
        item: MarkupItem.BlurPath,
        bitmapWidth: Float,
        bitmapHeight: Float
    ) {
        if (item.points.size < 2) return

        val minDim = minOf(bitmapWidth, bitmapHeight)
        val strokePx = (item.strokeWidthNormalized * minDim).coerceAtLeast(10f)
        val halfStroke = strokePx / 2f

        val minX = item.points.minOf { it.first }
        val maxX = item.points.maxOf { it.first }
        val minY = item.points.minOf { it.second }
        val maxY = item.points.maxOf { it.second }

        val l = ((minX * bitmapWidth) - halfStroke).toInt().coerceIn(0, bitmap.width - 1)
        val t = ((minY * bitmapHeight) - halfStroke).toInt().coerceIn(0, bitmap.height - 1)
        val r = ((maxX * bitmapWidth) + halfStroke).toInt().coerceIn(l + 1, bitmap.width)
        val b = ((maxY * bitmapHeight) + halfStroke).toInt().coerceIn(t + 1, bitmap.height)
        val w = r - l
        val h = b - t
        if (w <= 0 || h <= 0) return

        // 1. Create sub-bitmap of original region & blur it
        val subPixels = IntArray(w * h)
        bitmap.getPixels(subPixels, 0, w, l, t, w, h)
        val blurredPixels = subPixels.clone()

        val radius = (minOf(w, h) / 12 * item.strength).toInt().coerceIn(4, 50)
        boxBlur(blurredPixels, w, h, radius)
        boxBlur(blurredPixels, w, h, radius)
        boxBlur(blurredPixels, w, h, radius)

        // 2. Render smooth mask for stroke path
        val maskBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val maskCanvas = Canvas(maskBitmap)
        val path = Path()
        val p0 = item.points.first()
        path.moveTo(p0.first * bitmapWidth - l, p0.second * bitmapHeight - t)
        for (i in 1 until item.points.size) {
            val prev = item.points[i - 1]
            val curr = item.points[i]
            val midX = (prev.first + curr.first) / 2f * bitmapWidth - l
            val midY = (prev.second + curr.second) / 2f * bitmapHeight - t
            path.quadTo(prev.first * bitmapWidth - l, prev.second * bitmapHeight - t, midX, midY)
        }
        val last = item.points.last()
        path.lineTo(last.first * bitmapWidth - l, last.second * bitmapHeight - t)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokePx
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = Color.BLACK
        }
        maskCanvas.drawPath(path, paint)

        val maskPixels = IntArray(w * h)
        maskBitmap.getPixels(maskPixels, 0, w, 0, 0, w, h)
        maskBitmap.recycle()

        // 3. Blend blurred pixels into bitmap where mask > 0
        for (i in 0 until (w * h)) {
            val maskAlpha = (maskPixels[i] ushr 24) and 0xFF
            if (maskAlpha > 0) {
                if (maskAlpha >= 250) {
                    subPixels[i] = blurredPixels[i]
                } else {
                    val a = maskAlpha / 255f
                    val invA = 1f - a
                    val orig = subPixels[i]
                    val blur = blurredPixels[i]
                    val red = (((blur ushr 16) and 0xFF) * a + ((orig ushr 16) and 0xFF) * invA).toInt().coerceIn(0, 255)
                    val green = (((blur ushr 8) and 0xFF) * a + ((orig ushr 8) and 0xFF) * invA).toInt().coerceIn(0, 255)
                    val blue = ((blur and 0xFF) * a + (orig and 0xFF) * invA).toInt().coerceIn(0, 255)
                    subPixels[i] = (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
                }
            }
        }

        bitmap.setPixels(subPixels, 0, w, l, t, w, h)
    }

    private fun applySolidColor(bitmap: Bitmap, l: Int, t: Int, w: Int, h: Int, color: Int) {
        val pixels = IntArray(w * h) { color }
        bitmap.setPixels(pixels, 0, w, l, t, w, h)
    }

    private fun applyMosaic(bitmap: Bitmap, l: Int, t: Int, w: Int, h: Int, strength: Float) {
        val baseBlock = (minOf(w, h) / 10).coerceIn(8, 64)
        val blockSize = (baseBlock * strength).toInt().coerceIn(4, 96)

        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, l, t, w, h)

        for (by in 0 until h step blockSize) {
            for (bx in 0 until w step blockSize) {
                val curBlockW = minOf(blockSize, w - bx)
                val curBlockH = minOf(blockSize, h - by)

                var totalR = 0
                var totalG = 0
                var totalB = 0
                var count = 0
                for (y in 0 until curBlockH) {
                    for (x in 0 until curBlockW) {
                        val p = pixels[(by + y) * w + (bx + x)]
                        totalR += (p ushr 16) and 0xFF
                        totalG += (p ushr 8) and 0xFF
                        totalB += p and 0xFF
                        count++
                    }
                }
                if (count == 0) continue
                val avgColor = (0xFF shl 24) or
                    ((totalR / count).coerceIn(0, 255) shl 16) or
                    ((totalG / count).coerceIn(0, 255) shl 8) or
                    (totalB / count).coerceIn(0, 255)

                for (y in 0 until curBlockH) {
                    for (x in 0 until curBlockW) {
                        pixels[(by + y) * w + (bx + x)] = avgColor
                    }
                }
            }
        }
        bitmap.setPixels(pixels, 0, w, l, t, w, h)
    }

    /**
     * Stack-box blur approximation of Gaussian Blur with 3 passes.
     * High performance, O(N), smooth visual quality.
     */
    private fun applyFastGaussianBlur(bitmap: Bitmap, l: Int, t: Int, w: Int, h: Int, strength: Float) {
        val radius = (minOf(w, h) / 15 * strength).toInt().coerceIn(4, 50)
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, l, t, w, h)

        // 3 passes of box blur approximate true Gaussian blur
        boxBlur(pixels, w, h, radius)
        boxBlur(pixels, w, h, radius)
        boxBlur(pixels, w, h, radius)

        bitmap.setPixels(pixels, 0, w, l, t, w, h)
    }

    internal fun boxBlur(pixels: IntArray, w: Int, h: Int, radius: Int) {
        boxBlurHorizontal(pixels, w, h, radius)
        boxBlurVertical(pixels, w, h, radius)
    }

    private fun boxBlurHorizontal(pixels: IntArray, w: Int, h: Int, r: Int) {
        val div = 2 * r + 1
        val rArr = IntArray(w)
        val gArr = IntArray(w)
        val bArr = IntArray(w)

        for (y in 0 until h) {
            val yOffset = y * w
            for (x in 0 until w) {
                val p = pixels[yOffset + x]
                rArr[x] = (p ushr 16) and 0xFF
                gArr[x] = (p ushr 8) and 0xFF
                bArr[x] = p and 0xFF
            }

            var sumR = 0
            var sumG = 0
            var sumB = 0

            for (i in -r..r) {
                val clamped = i.coerceIn(0, w - 1)
                sumR += rArr[clamped]
                sumG += gArr[clamped]
                sumB += bArr[clamped]
            }

            for (x in 0 until w) {
                pixels[yOffset + x] = (0xFF shl 24) or
                    ((sumR / div).coerceIn(0, 255) shl 16) or
                    ((sumG / div).coerceIn(0, 255) shl 8) or
                    (sumB / div).coerceIn(0, 255)

                val leftIndex = (x - r).coerceIn(0, w - 1)
                val rightIndex = (x + r + 1).coerceIn(0, w - 1)

                sumR += rArr[rightIndex] - rArr[leftIndex]
                sumG += gArr[rightIndex] - gArr[leftIndex]
                sumB += bArr[rightIndex] - bArr[leftIndex]
            }
        }
    }

    private fun boxBlurVertical(pixels: IntArray, w: Int, h: Int, r: Int) {
        val div = 2 * r + 1
        val rArr = IntArray(h)
        val gArr = IntArray(h)
        val bArr = IntArray(h)

        for (x in 0 until w) {
            for (y in 0 until h) {
                val p = pixels[y * w + x]
                rArr[y] = (p ushr 16) and 0xFF
                gArr[y] = (p ushr 8) and 0xFF
                bArr[y] = p and 0xFF
            }

            var sumR = 0
            var sumG = 0
            var sumB = 0

            for (i in -r..r) {
                val clamped = i.coerceIn(0, h - 1)
                sumR += rArr[clamped]
                sumG += gArr[clamped]
                sumB += bArr[clamped]
            }

            for (y in 0 until h) {
                pixels[y * w + x] = (0xFF shl 24) or
                    ((sumR / div).coerceIn(0, 255) shl 16) or
                    ((sumG / div).coerceIn(0, 255) shl 8) or
                    (sumB / div).coerceIn(0, 255)

                val topIndex = (y - r).coerceIn(0, h - 1)
                val bottomIndex = (y + r + 1).coerceIn(0, h - 1)

                sumR += rArr[bottomIndex] - rArr[topIndex]
                sumG += gArr[bottomIndex] - gArr[topIndex]
                sumB += bArr[bottomIndex] - bArr[topIndex]
            }
        }
    }
}
