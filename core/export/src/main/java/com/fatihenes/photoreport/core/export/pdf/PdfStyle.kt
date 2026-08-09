@file:Suppress("MaxLineLength")
package com.fatihenes.photoreport.core.export.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import kotlin.math.min

data class HeaderParams(
    val canvas: Canvas,
    val projectName: String,
    val dateStr: String,
    val timeStr: String,
    val reportId: String,
    val logoBmp: Bitmap?,
    val language: String,
    val typography: PdfTypography
)

object PdfStyle {

    /**
     * Draws the report header with project metadata and optional logo.
     *
     * NOTE: External storage access for the logo and the final PDF file is handled via
     * app-private directories or standard Android MediaStore/FileProvider mechanisms.
     */
    fun drawHeader(params: HeaderParams): Float {
        val width = PdfTheme.PAGE_WIDTH.toFloat()
        val margin = PdfTheme.MARGIN

        params.canvas.drawRect(0f, 0f, width, 8f, params.typography.headerBannerPaint)
        var currentY = margin + 5f

        val reportSubtitle = if (params.language == "en") "FIELD INSPECTION & OBSERVATION REPORT" else "SAHA DENETİM VE GÖZLEM RAPORU"
        params.canvas.drawText(reportSubtitle, margin, currentY, params.typography.subtitlePaint)
        currentY += 22f

        val maxTitleChars = 32
        val displayTitle = if (params.projectName.length > maxTitleChars) params.projectName.take(maxTitleChars - 3) + "..." else params.projectName
        params.canvas.drawText(displayTitle, margin, currentY, params.typography.titlePaint)

        var rightEdge = width - margin
        params.logoBmp?.let { logo ->
            val logoHeight = 38f
            val scale = logoHeight / logo.height
            val logoWidth = logo.width * scale
            val rect = RectF(rightEdge - logoWidth, margin, rightEdge, margin + logoHeight)
            params.canvas.drawBitmap(logo, null, rect, params.typography.bitmapPaint)
            rightEdge -= (logoWidth + 12f)
        }

        val reportNoLabel = if (params.language == "en") "Doc No: #$params.reportId" else "Belge No: #$params.reportId"
        val dateTimeLabel = "${params.dateStr} - ${params.timeStr}"

        val metaY1 = margin + 12f
        val metaY2 = margin + 26f
        val w1 = params.typography.metaHeaderPaint.measureText(reportNoLabel)
        val w2 = params.typography.metaHeaderPaint.measureText(dateTimeLabel)

        params.canvas.drawText(reportNoLabel, rightEdge - w1, metaY1, params.typography.metaHeaderPaint)
        params.canvas.drawText(dateTimeLabel, rightEdge - w2, metaY2, params.typography.metaHeaderPaint)

        currentY += 15f
        params.canvas.drawLine(margin, currentY, width - margin, currentY, params.typography.dividerLinePaint)

        return currentY + 18f
    }

    data class PhotoFrameParams(
        val canvas: Canvas,
        val bitmap: Bitmap,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val captionText: String,
        val typography: PdfTypography
    )

    fun drawPhotoFrame(params: PhotoFrameParams) {
        val cardRect = RectF(params.x, params.y, params.x + params.width, params.y + params.height)
        params.canvas.drawRoundRect(cardRect, 6f, 6f, params.typography.cardBackgroundPaint)
        params.canvas.drawRoundRect(cardRect, 6f, 6f, params.typography.frameBorderPaint)

        val pad = 8f
        val captionSpace = if (params.captionText.isNotBlank()) 24f else 4f
        val availW = params.width - (pad * 2)
        val availH = params.height - (pad * 2) - captionSpace

        val scale = min(availW / params.bitmap.width, availH / params.bitmap.height)
        val w = params.bitmap.width * scale
        val h = params.bitmap.height * scale

        val drawX = params.x + pad + ((availW - w) / 2f)
        val drawY = params.y + pad + (availH - h) / 2f
        val imgRect = RectF(drawX, drawY, drawX + w, drawY + h)

        params.canvas.drawBitmap(params.bitmap, null, imgRect, params.typography.bitmapPaint)

        if (params.captionText.isNotBlank()) {
            val maxChars = (params.width / 7f).toInt().coerceAtLeast(20)
            val displayCaption = if (params.captionText.length > maxChars) params.captionText.take(maxChars - 3) + "..." else params.captionText
            val textWidth = params.typography.captionPaint.measureText(displayCaption)
            val capX = params.x + (params.width - textWidth) / 2f
            val capY = params.y + params.height - 10f
            params.canvas.drawText(displayCaption, capX, capY, params.typography.captionPaint)
        }
    }

    fun drawFooter(
        canvas: Canvas,
        pageNumber: Int,
        language: String,
        typography: PdfTypography
    ) {
        val width = PdfTheme.PAGE_WIDTH.toFloat()
        val height = PdfTheme.PAGE_HEIGHT.toFloat()
        val margin = PdfTheme.MARGIN
        val footerY = height - margin + 10f

        canvas.drawLine(margin, footerY - 12f, width - margin, footerY - 12f, typography.dividerLinePaint)

        val sysText = if (language == "en") "FotoRapor Executive Field Inspection System" else "FotoRapor Kurumsal Saha Denetim Mimarisi"
        canvas.drawText(sysText, margin, footerY + 2f, typography.pageNumPaint)

        val pageText = if (language == "en") "Page $pageNumber" else "Sayfa $pageNumber"
        val pageW = typography.pageNumPaint.measureText(pageText)
        canvas.drawText(pageText, width - margin - pageW, footerY + 2f, typography.pageNumPaint)
    }

    fun drawSignOffBlock(
        canvas: Canvas,
        startY: Float,
        language: String,
        typography: PdfTypography
    ): Float {
        val margin = PdfTheme.MARGIN
        val totalW = PdfTheme.PAGE_WIDTH.toFloat() - (margin * 2)
        val colWidth = totalW / 4f
        val currentY = startY + 15f

        canvas.drawLine(margin, currentY, margin + totalW, currentY, typography.dividerLinePaint)

        val colTitles = if (language == "en") {
            listOf(
                "PREPARED BY" to "Field Engineer / Tech",
                "CHECKED BY" to "Project Lead / Supervisor",
                "APPROVED BY" to "Client / Authority Rep",
                "COMPANY STAMP" to "Official Seal Box"
            )
        } else {
            listOf(
                "HAZIRLAYAN" to "Saha Mühendisi / Tekniker",
                "KONTROL EDEN" to "Proje / Şantiye Şefi",
                "ONAYLAYAN" to "Müşteri / İdare Temsilcisi",
                "FİRMA KAŞESİ" to "Resmi Kurum / Şirket Kaşesi"
            )
        }

        colTitles.forEachIndexed { idx, (title, sub) ->
            val colX = margin + (idx * colWidth)
            val titleW = typography.signOffHeaderPaint.measureText(title)
            val subW = typography.signOffSubPaint.measureText(sub)

            val tX = colX + (colWidth - titleW) / 2f
            val sX = colX + (colWidth - subW) / 2f

            canvas.drawText(title, tX, currentY + 22f, typography.signOffHeaderPaint)
            canvas.drawText(sub, sX, currentY + 36f, typography.signOffSubPaint)

            if (idx == 3) {
                val boxPad = 8f
                val boxRect = RectF(colX + boxPad, currentY + 45f, colX + colWidth - boxPad, currentY + 95f)
                canvas.drawRoundRect(boxRect, 4f, 4f, typography.frameBorderPaint)
            } else {
                val lineStart = colX + 12f
                val lineEnd = colX + colWidth - 12f
                canvas.drawLine(lineStart, currentY + 75f, lineEnd, currentY + 75f, typography.signLinePaint)
                val datePrompt = if (language == "en") "Date: .... / .... / 20..." else "Tarih: .... / .... / 20..."
                val dW = typography.signOffSubPaint.measureText(datePrompt)
                canvas.drawText(datePrompt, colX + (colWidth - dW) / 2f, currentY + 92f, typography.signOffSubPaint)
            }
        }

        return currentY + 105f
    }
}
