package com.fatihenes.photoreport.core.media

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.fatihenes.photoreport.core.model.PhotoAdjustments

object PhotoAdjustEngine {

    /**
     * Builds an Android ColorMatrix representing the combined non-destructive adjustments.
     */
    fun createColorMatrix(adjustments: PhotoAdjustments): ColorMatrix {
        val result = ColorMatrix()

        if (adjustments.isDefault()) return result

        // 1. Contrast
        if (adjustments.contrast != 0f) {
            val scale = 1f + adjustments.contrast
            val translate = (-0.5f * scale + 0.5f) * 255f
            val contrastMatrix = ColorMatrix(floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
            result.postConcat(contrastMatrix)
        }

        // 2. Brightness & Exposure
        val totalBrightness = (adjustments.brightness + adjustments.exposure * 0.5f) * 255f
        if (totalBrightness != 0f) {
            val brightMatrix = ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, totalBrightness,
                0f, 1f, 0f, 0f, totalBrightness,
                0f, 0f, 1f, 0f, totalBrightness,
                0f, 0f, 0f, 1f, 0f
            ))
            result.postConcat(brightMatrix)
        }

        // 3. Saturation
        if (adjustments.saturation != 0f) {
            val satMatrix = ColorMatrix()
            satMatrix.setSaturation(1f + adjustments.saturation)
            result.postConcat(satMatrix)
        }

        // 4. Temperature (Warmth) & Tint
        if (adjustments.temperature != 0f || adjustments.tint != 0f) {
            val rShift = (adjustments.temperature * 30f).coerceIn(-50f, 50f)
            val bShift = (-adjustments.temperature * 30f).coerceIn(-50f, 50f)
            val gShift = (-adjustments.tint * 25f).coerceIn(-50f, 50f)
            val mShift = (adjustments.tint * 25f).coerceIn(-50f, 50f)

            val tempTintMatrix = ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, rShift + mShift,
                0f, 1f, 0f, 0f, gShift,
                0f, 0f, 1f, 0f, bShift + mShift,
                0f, 0f, 0f, 1f, 0f
            ))
            result.postConcat(tempTintMatrix)
        }

        return result
    }

    /**
     * Applies all adjustments (ColorMatrix + Vignette + Fade) to a bitmap in place.
     */
    fun applyAdjustmentsToBitmap(bitmap: Bitmap, adjustments: PhotoAdjustments) {
        if (adjustments.isDefault()) return

        val colorMatrix = createColorMatrix(adjustments)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }

        // Canvas(bitmap).drawBitmap(bitmap) tanımsızdır (self-draw).
        // Snapshot üzerinden çiz: aynı görsel sonuç, stabil davranış.
        val snapshot = bitmap.copy(Bitmap.Config.ARGB_8888, false)
        val canvas = Canvas(bitmap)
        try {
            canvas.drawBitmap(snapshot, 0f, 0f, paint)
        } finally {
            snapshot.recycle()
        }

        // Apply Vignette if enabled
        if (adjustments.vignette > 0f) {
            val w = bitmap.width.toFloat()
            val h = bitmap.height.toFloat()
            val radius = maxOf(w, h) * 0.75f
            val alpha = (adjustments.vignette * 180).toInt().coerceIn(0, 255)
            val vignetteShader = RadialGradient(
                w / 2f, h / 2f, radius,
                intArrayOf(Color.TRANSPARENT, Color.argb(alpha, 0, 0, 0)),
                floatArrayOf(0.4f, 1.0f),
                Shader.TileMode.CLAMP
            )
            val vPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = vignetteShader
            }
            canvas.drawRect(0f, 0f, w, h, vPaint)
        }
    }
}
