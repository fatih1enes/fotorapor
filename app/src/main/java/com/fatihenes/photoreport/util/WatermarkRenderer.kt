package com.fatihenes.photoreport.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Renders GPS watermark overlay onto captured photos.
 * Draws a semi-transparent panel with location, date/time and project info
 * at the bottom-left of the image.
 */
@Singleton
class WatermarkRenderer @Inject constructor() {

    companion object {
        private const val TAG = "WatermarkRenderer"
        private const val PANEL_ALPHA = 140 // 0-255 transparency for background
        private const val PANEL_CORNER_RADIUS = 16f
    }

    /**
     * Applies watermark to a bitmap and saves the result as a new file in MediaStore.
     * Returns the URI of the watermarked image, or the original URI if watermarking fails.
     */
    suspend fun applyWatermark(
        context: Context,
        originalUri: Uri,
        watermarkData: WatermarkData
    ): Uri = withContext(Dispatchers.IO) {
        var originalBitmap: Bitmap? = null
        var watermarkedBitmap: Bitmap? = null
        try {
            originalBitmap = ImageUtils.loadScaledBitmap(context, originalUri.toString(), 2560, 2560)
                ?: return@withContext originalUri

            watermarkedBitmap = drawWatermark(originalBitmap, watermarkData)
            if (originalBitmap !== watermarkedBitmap) {
                originalBitmap.recycle()
            }
            originalBitmap = null

            // Save watermarked bitmap back to MediaStore
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_WM_${System.currentTimeMillis()}.jpg")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/PhotoReport")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val watermarkedUri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
            )

            if (watermarkedUri != null) {
                context.contentResolver.openOutputStream(watermarkedUri)?.use { out ->
                    watermarkedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }

                val pendingValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                context.contentResolver.update(watermarkedUri, pendingValues, null, null)

                // Delete original
                context.contentResolver.delete(originalUri, null, null)
                return@withContext watermarkedUri
            }

            originalUri
        } catch (e: Exception) {
            Log.e(TAG, "Watermark application failed", e)
            originalUri
        } finally {
            if (originalBitmap !== watermarkedBitmap) {
                originalBitmap?.recycle()
            }
            watermarkedBitmap?.recycle()
        }
    }

    /**
     * Draws a watermark panel onto a bitmap.
     * The panel is positioned in the bottom-left corner.
     */
    private fun drawWatermark(source: Bitmap, data: WatermarkData): Bitmap {
        val result = if (source.isMutable) source else source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val width = result.width.toFloat()
        val height = result.height.toFloat()

        // Scale text size relative to image dimensions for consistent appearance
        val baseFontSize = (width.coerceAtMost(height) * 0.022f).coerceIn(20f, 60f)
        val smallFontSize = baseFontSize * 0.85f
        val padding = baseFontSize * 0.8f
        val lineSpacing = baseFontSize * 0.35f

        // Build watermark lines
        val lines = buildWatermarkLines(data)
        if (lines.isEmpty()) return result

        // Paint for text
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = smallFontSize
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            setShadowLayer(3f, 1f, 1f, Color.BLACK)
        }

        val titlePaint = Paint(textPaint).apply {
            textSize = baseFontSize
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        // Calculate panel dimensions
        val allPaints = lines.map { (text, isTitle) ->
            val paint = if (isTitle) titlePaint else textPaint
            Triple(text, paint, paint.measureText(text))
        }
        val maxTextWidth = allPaints.maxOf { it.third }
        val totalTextHeight = allPaints.sumOf { it.second.textSize.toDouble() + lineSpacing }.toFloat()

        val panelWidth = maxTextWidth + padding * 2
        val panelHeight = totalTextHeight + padding * 1.5f

        // Panel background paint
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            alpha = PANEL_ALPHA
            style = Paint.Style.FILL
        }

        // Draw panel at bottom-left
        val panelLeft = padding
        val panelTop = height - panelHeight - padding
        val panelRect = RectF(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight)
        canvas.drawRoundRect(panelRect, PANEL_CORNER_RADIUS, PANEL_CORNER_RADIUS, bgPaint)

        // Draw text lines
        var yPos = panelTop + padding + allPaints.first().second.textSize
        for ((text, paint, _) in allPaints) {
            canvas.drawText(text, panelLeft + padding, yPos, paint)
            yPos += paint.textSize + lineSpacing
        }

        return result
    }

    /**
     * Builds the list of (text, isTitle) pairs for the watermark overlay.
     */
    private fun buildWatermarkLines(data: WatermarkData): List<Pair<String, Boolean>> {
        val lines = mutableListOf<Pair<String, Boolean>>()

        if (data.projectName.isNotBlank()) {
            lines.add(data.projectName to true)
        }

        if (data.dateTime.isNotBlank()) {
            lines.add("📅 ${data.dateTime}" to false)
        }

        if (data.latitude != null && data.longitude != null) {
            lines.add(String.format(Locale.US, "📍 %.6f, %.6f", data.latitude, data.longitude) to false)
        }

        if (!data.address.isNullOrBlank()) {
            lines.add("🏠 ${data.address}" to false)
        }

        return lines
    }
}
