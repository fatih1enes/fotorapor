@file:Suppress("MaxLineLength", "MagicNumber")
package com.fatihenes.photoreport.core.export.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import android.text.StaticLayout
import androidx.core.graphics.withTranslation
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

object PdfStyle {

    fun drawHeader(params: HeaderParams): Float {
        val width = PdfTheme.PAGE_WIDTH.toFloat()
        val margin = PdfTheme.MARGIN

        // Top Brand Accent Stripe (Navy + Brass)
        params.canvas.drawRect(0f, 0f, width, 5f, params.typography.headerBannerPaint)
        params.canvas.drawRect(0f, 5f, width, 7f, params.typography.headerAccentBarPaint)

        val currentY = margin + 4f

        val reportSubtitle = if (params.language == "en") "FIELD INSPECTION & OBSERVATION REPORT" else "SAHA DENETİM VE GÖZLEM RAPORU"
        params.canvas.drawText(reportSubtitle, margin, currentY, params.typography.subtitlePaint)

        var rightEdge = width - margin
        params.logoBmp?.let { logo ->
            val logoHeight = 34f
            val scale = logoHeight / logo.height
            val logoWidth = logo.width * scale
            val rect = RectF(rightEdge - logoWidth, margin, rightEdge, margin + logoHeight)
            params.canvas.drawBitmap(logo, null, rect, params.typography.bitmapPaint)
            rightEdge -= (logoWidth + 12f)
        }

        val reportNoLabel = if (params.language == "en") "Doc No: #$params.reportId" else "Belge No: #$params.reportId"
        val dateTimeLabel = "${params.dateStr} • ${params.timeStr}"

        val w1 = params.typography.metaHeaderPaint.measureText(reportNoLabel)
        val w2 = params.typography.metaHeaderPaint.measureText(dateTimeLabel)
        val maxMetaW = maxOf(w1, w2)

        params.canvas.drawText(reportNoLabel, rightEdge - w1, currentY + 10f, params.typography.metaHeaderPaint)
        params.canvas.drawText(dateTimeLabel, rightEdge - w2, currentY + 22f, params.typography.metaHeaderPaint)

        val titleRightBound = rightEdge - maxMetaW - 16f
        val availTitleW = titleRightBound - margin
        val titleY = currentY + 18f

        var displayTitle = params.projectName
        if (params.typography.titlePaint.measureText(displayTitle) > availTitleW) {
            val count = params.typography.titlePaint.breakText(displayTitle, true, availTitleW - 10f, null)
            displayTitle = displayTitle.take(count) + "..."
        }
        params.canvas.drawText(displayTitle, margin, titleY, params.typography.titlePaint)

        val dividerY = currentY + 32f
        params.canvas.drawLine(margin, dividerY, width - margin, dividerY, params.typography.dividerLinePaint)

        return dividerY + 14f
    }

    fun drawDateSectionHeader(
        canvas: Canvas,
        startY: Float,
        dateStr: String,
        typography: PdfTypography
    ): Float {
        val margin = PdfTheme.MARGIN
        val textWidth = typography.datePillTextPaint.measureText(dateStr)
        val pillWidth = textWidth + 24f
        val pillHeight = 20f

        val pillRect = RectF(margin, startY, margin + pillWidth, startY + pillHeight)
        canvas.drawRoundRect(pillRect, 10f, 10f, typography.datePillBgPaint)

        canvas.drawText(dateStr, margin + 12f, startY + 14f, typography.datePillTextPaint)

        return startY + pillHeight + 10f
    }

    fun drawNoteCard(
        canvas: Canvas,
        startY: Float,
        noteLayout: StaticLayout,
        typography: PdfTypography
    ): Float {
        val margin = PdfTheme.MARGIN
        val cardWidth = PdfTheme.FULL_WIDTH_IMAGE_WIDTH
        val cardHeight = noteLayout.height + 16f

        val cardRect = RectF(margin, startY, margin + cardWidth, startY + cardHeight)
        canvas.drawRoundRect(cardRect, 6f, 6f, typography.noteCardBgPaint)

        val indicatorRect = RectF(margin, startY, margin + 4f, startY + cardHeight)
        canvas.drawRoundRect(indicatorRect, 2f, 2f, typography.noteCardAccentPaint)

        canvas.withTranslation(margin + 12f, startY + 8f) {
            noteLayout.draw(this)
        }

        return startY + cardHeight + 14f
    }

    fun drawPhotoFrame(params: PhotoFrameParams) {
        val radius = PdfTheme.CARD_CORNER_RADIUS
        val cardRect = RectF(params.x, params.y, params.x + params.width, params.y + params.height)

        params.canvas.drawRoundRect(cardRect, radius, radius, params.typography.cardBackgroundPaint)
        params.canvas.drawRoundRect(cardRect, radius, radius, params.typography.frameBorderPaint)

        val pad = 6f
        val badgeHeight = if (params.captionText.isNotBlank()) 22f else 0f
        val availW = params.width - (pad * 2)
        val availH = params.height - (pad * 2) - badgeHeight

        val scale = min(availW / params.bitmap.width, availH / params.bitmap.height)
        val w = params.bitmap.width * scale
        val h = params.bitmap.height * scale

        val drawX = params.x + pad + ((availW - w) / 2f)
        val drawY = params.y + pad + ((availH - h) / 2f)
        val imgRect = RectF(drawX, drawY, drawX + w, drawY + h)

        params.canvas.drawBitmap(params.bitmap, null, imgRect, params.typography.bitmapPaint)

        if (params.captionText.isNotBlank()) {
            val badgeY = params.y + params.height - badgeHeight - 2f
            val badgeRect = RectF(params.x + 1f, badgeY, params.x + params.width - 1f, params.y + params.height - 1f)
            params.canvas.drawRoundRect(badgeRect, 0f, 0f, params.typography.photoBadgeBgPaint)

            params.canvas.drawLine(
                params.x + 1f, badgeY,
                params.x + params.width - 1f, badgeY,
                params.typography.dividerLinePaint
            )

            val maxW = params.width - 16f
            var displayCap = params.captionText
            if (params.typography.photoBadgeTextPaint.measureText(displayCap) > maxW) {
                val count = params.typography.photoBadgeTextPaint.breakText(displayCap, true, maxW - 8f, null)
                displayCap = displayCap.take(count) + "..."
            }

            val capW = params.typography.photoBadgeTextPaint.measureText(displayCap)
            val capX = params.x + (params.width - capW) / 2f
            params.canvas.drawText(displayCap, capX, badgeY + 14f, params.typography.photoBadgeTextPaint)
        }
    }

    fun drawFooter(
        canvas: Canvas,
        pageNumber: Int,
        totalPages: Int,
        language: String,
        typography: PdfTypography
    ) {
        val width = PdfTheme.PAGE_WIDTH.toFloat()
        val height = PdfTheme.PAGE_HEIGHT.toFloat()
        val margin = PdfTheme.MARGIN
        val footerY = height - margin + 12f

        canvas.drawLine(margin, footerY - 12f, width - margin, footerY - 12f, typography.dividerLinePaint)

        val sysText = if (language == "en") "FotoRapor Executive Field Inspection System" else "FotoRapor Kurumsal Saha Denetim Mimarisi"
        canvas.drawText(sysText, margin, footerY + 2f, typography.pageNumPaint)

        val pageText = if (language == "en") "Page $pageNumber of $totalPages" else "Sayfa $pageNumber / $totalPages"
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
        val currentY = startY + 10f

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

            canvas.drawText(title, tX, currentY + 20f, typography.signOffHeaderPaint)
            canvas.drawText(sub, sX, currentY + 32f, typography.signOffSubPaint)

            if (idx == 3) {
                val boxPad = 6f
                val boxRect = RectF(colX + boxPad, currentY + 40f, colX + colWidth - boxPad, currentY + 90f)
                canvas.drawRoundRect(boxRect, 4f, 4f, typography.frameBorderPaint)
            } else {
                val lineStart = colX + 10f
                val lineEnd = colX + colWidth - 10f
                canvas.drawLine(lineStart, currentY + 70f, lineEnd, currentY + 70f, typography.signLinePaint)
                val datePrompt = if (language == "en") "Date: .... / .... / 20..." else "Tarih: .... / .... / 20..."
                val dW = typography.signOffSubPaint.measureText(datePrompt)
                canvas.drawText(datePrompt, colX + (colWidth - dW) / 2f, currentY + 86f, typography.signOffSubPaint)
            }
        }

        return currentY + 98f
    }
}

