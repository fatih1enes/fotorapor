package com.fatihenes.photoreport.manager.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import kotlin.math.min

/**
 * FotoRapor Kurumsal PDF Tasarım Sistemi - Şekil, Izgara, İmza Bloğu ve Alt/Üst Bilgi Çizim Servisi
 * PDF görselleştirme adımlarını modülerleştirir ve standart mühendislik taslak yapısını sunar.
 */
object PdfStyle {

    /**
     * Rapor üst bilgisi (Header) künyesini çizdirir ve içerik akışının başlayacağı yeni Y koordinatını döndürür.
     */
    fun drawHeader(
        canvas: Canvas,
        projectName: String,
        dateStr: String,
        timeStr: String,
        reportId: String,
        logoBmp: Bitmap?,
        language: String,
        typography: PdfTypography
    ): Float {
        val width = PdfTheme.PAGE_WIDTH.toFloat()
        val margin = PdfTheme.MARGIN
        var currentY = 15f

        // 1. Üst Kurumsal Laci Banner Çizgisi (Sayfa Üst Bilgi Bantı)
        canvas.drawRect(0f, 0f, width, 8f, typography.headerBannerPaint)
        currentY = margin + 5f

        // 2. Rapor Tipi Alt Başlığı
        val reportSubtitle = if (language == "en") "FIELD INSPECTION & OBSERVATION REPORT" else "SAHA DENETİM VE GÖZLEM RAPORU"
        canvas.drawText(reportSubtitle, margin, currentY, typography.subtitlePaint)
        currentY += 22f

        // 3. Proje Adı Ana Başlığı
        val maxTitleChars = 32
        val displayTitle = if (projectName.length > maxTitleChars) projectName.take(maxTitleChars - 3) + "..." else projectName
        canvas.drawText(displayTitle, margin, currentY, typography.titlePaint)
        
        // 4. Sağ Tarafa Logo ve Künye Yerleşimi
        var rightEdge = width - margin
        logoBmp?.let { logo ->
            val logoHeight = 38f
            val scale = logoHeight / logo.height
            val logoWidth = logo.width * scale
            val rect = RectF(rightEdge - logoWidth, margin, rightEdge, margin + logoHeight)
            canvas.drawBitmap(logo, null, rect, typography.bitmapPaint)
            rightEdge -= (logoWidth + 12f)
        }

        // 5. Tarih, Saat ve Rapor Numarası (Logonun veya Sağ Kenarın Hemen Soluna Yassılanır)
        val reportNoLabel = if (language == "en") "Doc No: #$reportId" else "Belge No: #$reportId"
        val dateTimeLabel = "$dateStr - $timeStr"
        
        val metaY1 = margin + 12f
        val metaY2 = margin + 26f
        val w1 = typography.metaHeaderPaint.measureText(reportNoLabel)
        val w2 = typography.metaHeaderPaint.measureText(dateTimeLabel)
        
        canvas.drawText(reportNoLabel, rightEdge - w1, metaY1, typography.metaHeaderPaint)
        canvas.drawText(dateTimeLabel, rightEdge - w2, metaY2, typography.metaHeaderPaint)

        // 6. Ayırıcı Çizgi (Divider)
        currentY += 15f
        canvas.drawLine(margin, currentY, width - margin, currentY, typography.dividerLinePaint)
        
        return currentY + 18f // İçerik akışı için tampon bırakarak döndür
    }

    /**
     * Çift sütunlu zemin tamponuna oturan kurumsal çerçeveli fotoğraf ve alt yazı etiketi basar.
     * Adaptive Layout desteği ile dinamik genişlik ve yükseklik kabul eder.
     */
    fun drawPhotoFrame(
        canvas: Canvas,
        bitmap: Bitmap,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        captionText: String,
        typography: PdfTypography
    ) {
        // 1. Zemin Tamponu ve İnce Çerçeve
        val cardRect = RectF(x, y, x + width, y + height)
        canvas.drawRoundRect(cardRect, 6f, 6f, typography.cardBackgroundPaint)
        canvas.drawRoundRect(cardRect, 6f, 6f, typography.frameBorderPaint)

        // 2. Görsel Boyutlandırma ve Merkezileştirme
        val pad = 8f
        val captionSpace = if (captionText.isNotBlank()) 24f else 4f
        val availW = width - (pad * 2)
        val availH = height - (pad * 2) - captionSpace
        
        // Bitmap'in en boy oranını koruyarak kullanılabilir alana sığdır (Letterbox)
        val scale = min(availW / bitmap.width, availH / bitmap.height)
        val w = bitmap.width * scale
        val h = bitmap.height * scale
        
        // Çerçeve içinde tam orta noktaya yerleştir
        val drawX = x + pad + (availW - w) / 2f
        val drawY = y + pad + (availH - h) / 2f
        val imgRect = RectF(drawX, drawY, drawX + w, drawY + h)
        
        canvas.drawBitmap(bitmap, null, imgRect, typography.bitmapPaint)

        // 3. Fotoğraf Alt Açıklama (Caption) Etiketi
        if (captionText.isNotBlank()) {
            // Dinamik genişliğe göre kırpma sınırını belirle
            val maxChars = (width / 7f).toInt().coerceAtLeast(20)
            val displayCaption = if (captionText.length > maxChars) captionText.take(maxChars - 3) + "..." else captionText
            val textWidth = typography.captionPaint.measureText(displayCaption)
            val capX = x + (width - textWidth) / 2f
            val capY = y + height - 10f
            canvas.drawText(displayCaption, capX, capY, typography.captionPaint)
        }
    }

    /**
     * Sayfa altbilgisini (Footer) ve kurumsal tescil ibaresini yazdırır.
     */
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

        // İnce Alt Ayırıcı Çizgi
        canvas.drawLine(margin, footerY - 12f, width - margin, footerY - 12f, typography.dividerLinePaint)

        // Sol Tarafta Kurumsal Sistem Tescil Bildirimi
        val sysText = if (language == "en") "FotoRapor Executive Field Inspection System" else "FotoRapor Kurumsal Saha Denetim Mimarisi"
        canvas.drawText(sysText, margin, footerY + 2f, typography.pageNumPaint)

        // Sağ Tarafta Sayfa Numarası
        val pageText = if (language == "en") "Page $pageNumber" else "Sayfa $pageNumber"
        val pageW = typography.pageNumPaint.measureText(pageText)
        canvas.drawText(pageText, width - margin - pageW, footerY + 2f, typography.pageNumPaint)
    }

    /**
     * 4 Sütunlu Resmi Kurum ve Müşteri Teslimatı İmza / Onay ve Kaşe Bloğu (Sign-Off Table)
     * Raporun en son sayfasına uygulanır.
     */
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
        
        // Bloğun Başlığı İçin Üst Çizgi
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
            
            // Başlıkları sütun içinde ortala
            val tX = colX + (colWidth - titleW) / 2f
            val sX = colX + (colWidth - subW) / 2f
            
            canvas.drawText(title, tX, currentY + 22f, typography.signOffHeaderPaint)
            canvas.drawText(sub, sX, currentY + 36f, typography.signOffSubPaint)

            if (idx == 3) {
                // 4. Sütun (Firma Kaşesi) için zarif bir kare kutu çizdir
                val boxPad = 8f
                val boxRect = RectF(colX + boxPad, currentY + 45f, colX + colWidth - boxPad, currentY + 95f)
                canvas.drawRoundRect(boxRect, 4f, 4f, typography.frameBorderPaint)
            } else {
                // İlk 3 Sütun için kesik çizgili imza ve tarih yuvaları çizdir
                val lineStart = colX + 12f
                val lineEnd = colX + colWidth - 12f
                // İmza Satırı
                canvas.drawLine(lineStart, currentY + 75f, lineEnd, currentY + 75f, typography.signLinePaint)
                // Tarih Satırı
                val datePrompt = if (language == "en") "Date: .... / .... / 20..." else "Tarih: .... / .... / 20..."
                val dW = typography.signOffSubPaint.measureText(datePrompt)
                canvas.drawText(datePrompt, colX + (colWidth - dW) / 2f, currentY + 92f, typography.signOffSubPaint)
            }
        }

        return currentY + 105f
    }
}
