package com.fatihenes.photoreport.core.media

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
import com.fatihenes.photoreport.core.model.WatermarkData

/**
 * Renders GPS watermark overlay onto captured photos in :core:media.
 * Draws a semi-transparent panel with location, date/time and project info
 * at the bottom-left of the image.
 */
@Singleton
class WatermarkRenderer @Inject constructor() {

    companion object {
        private const val TAG = "WatermarkRenderer"
        private const val PANEL_ALPHA = 140
        private const val PANEL_CORNER_RADIUS = 16f
    }

    suspend fun applyWatermark(
        context: Context,
        originalUri: Uri,
        watermarkData: WatermarkData
    ): Uri = withContext(Dispatchers.IO) {
        var originalBitmap: Bitmap? = null
        var watermarkedBitmap: Bitmap? = null
        try {
            originalBitmap = ImageScaler.loadScaledBitmap(context, originalUri.toString(), 2560, 2560)
                ?: return@withContext originalUri

            watermarkedBitmap = drawWatermark(originalBitmap, watermarkData)
            if (originalBitmap !== watermarkedBitmap) {
                originalBitmap.recycle()
            }
            originalBitmap = null

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

    private fun drawWatermark(source: Bitmap, data: WatermarkData): Bitmap {
        val result = if (source.isMutable) source else source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val width = result.width.toFloat()
        val height = result.height.toFloat()

        val baseFontSize = (width.coerceAtMost(height) * 0.022f).coerceIn(20f, 60f)
        val smallFontSize = baseFontSize * 0.85f
        val padding = baseFontSize * 0.8f
        val lineSpacing = baseFontSize * 0.35f

        val lines = buildWatermarkLines(data)
        if (lines.isEmpty()) return result

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

        val allPaints = lines.map { (text, isTitle) ->
            val paint = if (isTitle) titlePaint else textPaint
            Triple(text, paint, paint.measureText(text))
        }
        val maxTextWidth = allPaints.maxOf { it.third }
        val totalTextHeight = allPaints.sumOf { it.second.textSize.toDouble() + lineSpacing }.toFloat()

        val panelWidth = maxTextWidth + padding * 2
        val panelHeight = totalTextHeight + padding * 1.5f

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            alpha = PANEL_ALPHA
            style = Paint.Style.FILL
        }

        val panelLeft = padding
        val panelTop = height - panelHeight - padding
        val panelRect = RectF(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight)
        canvas.drawRoundRect(panelRect, PANEL_CORNER_RADIUS, PANEL_CORNER_RADIUS, bgPaint)

        var yPos = panelTop + padding + allPaints.first().second.textSize
        for ((text, paint, _) in allPaints) {
            canvas.drawText(text, panelLeft + padding, yPos, paint)
            yPos += paint.textSize + lineSpacing
        }

        return result
    }

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
