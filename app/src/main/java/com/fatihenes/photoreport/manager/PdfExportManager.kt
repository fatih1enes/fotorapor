package com.fatihenes.photoreport.manager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.fatihenes.photoreport.data.LogWithPhotos
import com.fatihenes.photoreport.data.ProjectEntity
import com.fatihenes.photoreport.util.CompanyLogoManager
import com.fatihenes.photoreport.util.DateUtils
import com.fatihenes.photoreport.util.FileNameUtils
import com.fatihenes.photoreport.util.ImageUtils
import com.fatihenes.photoreport.util.result.OperationResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

import com.fatihenes.photoreport.R
import androidx.core.graphics.withTranslation

/**
 * Handles PDF generation operations asynchronously.
 */
interface PdfExportManager {
    /**
     * Exports the project and its logs to a PDF file with full native UTF-8 (Turkish) character support.
     *
     * @param project The project to export.
     * @param logs The chronological list of logs and their photos.
     * @param quality The compression quality (1-100).
     * @return [OperationResult.Success] containing the content URI of the generated PDF.
     */
    suspend fun exportToPdf(
        project: ProjectEntity,
        logs: List<LogWithPhotos>,
        quality: Int = 100,
        language: String = "tr"
    ): OperationResult<Uri>
}

@Singleton
class NativePdfExportManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) : PdfExportManager {

    override suspend fun exportToPdf(
        project: ProjectEntity,
        logs: List<LogWithPhotos>,
        quality: Int,
        language: String
    ): OperationResult<Uri> = withContext(Dispatchers.IO) {
        var pdfDocument: PdfDocument? = null
        var logoBmp: Bitmap? = null
        try {
            val locale = if (language == "en") Locale.US else Locale("tr", "TR")
            val config = android.content.res.Configuration(context.resources.configuration)
            config.setLocale(locale)
            val localizedContext = context.createConfigurationContext(config)

            pdfDocument = PdfDocument()
            val sortedLogs = logs.sortedBy { it.log.date }

            val pageWidth = 595 // A4 width at 72 DPI
            val pageHeight = 842 // A4 height at 72 DPI
            val margin = 40f
            var pageNumber = 1

            // Paints
            val titlePaint = TextPaint().apply {
                color = Color.BLACK
                textSize = 22f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val headerLinePaint = Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 1.5f
                isAntiAlias = true
            }

            val datePaint = TextPaint().apply {
                color = Color.DKGRAY
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val notePaint = TextPaint().apply {
                color = Color.BLACK
                textSize = 11f
                typeface = Typeface.DEFAULT
                isAntiAlias = true
            }

            val pageNumPaint = TextPaint().apply {
                color = Color.GRAY
                textSize = 10f
                typeface = Typeface.DEFAULT
                isAntiAlias = true
            }

            val bitmapPaint = Paint().apply {
                isFilterBitmap = true
                isAntiAlias = true
                isDither = true
            }

            // Preload logo bitmap
            val logoUri = CompanyLogoManager.getLogoUri(context)
            if (logoUri != null) {
                logoBmp = ImageUtils.loadScaledBitmap(context, logoUri.toString(), 500, 500)
            }

            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var currentPage = pdfDocument.startPage(pageInfo)
            var canvas = currentPage.canvas
            var currentY = margin

            fun drawHeader() {
                val title = project.name
                val titleWidth = titlePaint.measureText(title)
                val startX = (pageWidth - titleWidth) / 2f
                canvas.drawText(title, startX, margin + 20f, titlePaint)

                currentY = margin + 40f
                canvas.drawLine(margin, currentY, pageWidth - margin, currentY, headerLinePaint)
                currentY += 20f

                logoBmp?.let { logo ->
                    val logoHeight = 35f
                    val scale = logoHeight / logo.height
                    val logoWidth = logo.width * scale
                    val rect = RectF(pageWidth - margin - logoWidth, margin, pageWidth - margin, margin + logoHeight)
                    canvas.drawBitmap(logo, null, rect, bitmapPaint)
                }
            }

            fun closeAndStartNewPage() {
                val footerText = "${localizedContext.getString(R.string.page_label)} $pageNumber"
                val footerWidth = pageNumPaint.measureText(footerText)
                canvas.drawText(footerText, (pageWidth - footerWidth) / 2f, pageHeight - margin / 2f, pageNumPaint)

                pdfDocument.finishPage(currentPage)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                currentPage = pdfDocument.startPage(pageInfo)
                canvas = currentPage.canvas
                currentY = margin
                drawHeader()
            }

            drawHeader()

            var isFirstLog = true
            for (logWithPhotos in sortedLogs) {
                val log = logWithPhotos.log
                val photos = logWithPhotos.photos

                if (!isFirstLog) {
                    closeAndStartNewPage()
                }
                isFirstLog = false

                // Date
                val dateStr = "${localizedContext.getString(R.string.date_label)} ${DateUtils.formatDate(log.date, language)}"
                canvas.drawText(dateStr, margin, currentY + 14f, datePaint)
                currentY += 30f

                // Note with multi-line wrapping and native Turkish character support
                if (log.note.isNotBlank()) {
                    val noteWidth = (pageWidth - (margin * 2) - 20).toInt()
                    val staticLayout = StaticLayout.Builder
                        .obtain(log.note, 0, log.note.length, notePaint, noteWidth)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(0f, 1.15f)
                        .build()

                    if (currentY + staticLayout.height > pageHeight - margin - 50f) {
                        closeAndStartNewPage()
                    }

                    canvas.withTranslation(margin + 10f, currentY) {
                        staticLayout.draw(this)
                    }

                    currentY += staticLayout.height + 20f
                }

                // Photos grid (2 columns)
                if (photos.isNotEmpty()) {
                    var col = 0
                    val imgWidth = 230f
                    val imgHeight = 280f
                    val spacing = 15f

                    for (photo in photos) {
                        if (photo.filePath.endsWith(".mp4", ignoreCase = true)) continue

                        if (currentY + imgHeight > pageHeight - margin - 30f) {
                            closeAndStartNewPage()
                            col = 0
                        }

                        val x = margin + col * (imgWidth + spacing)

                        var activeBitmap: Bitmap? = null
                        try {
                            val reqDim = if (quality == 100) 1200 else 800
                            val loadedBmp = ImageUtils.loadScaledBitmap(context, photo.filePath, reqDim, reqDim)
                            if (loadedBmp != null) {
                                activeBitmap = loadedBmp
                                val exifRotation = ImageUtils.getExifRotation(context, photo.filePath)
                                val totalRotation = (exifRotation + photo.rotation) % 360f

                                if (totalRotation != 0f) {
                                    val matrix = android.graphics.Matrix().apply { postRotate(totalRotation) }
                                    val rotated = Bitmap.createBitmap(loadedBmp, 0, 0, loadedBmp.width, loadedBmp.height, matrix, true)
                                    if (rotated !== loadedBmp) {
                                        loadedBmp.recycle()
                                        activeBitmap = rotated
                                    }
                                }

                                activeBitmap?.let { bmp ->
                                    val scale = min(imgWidth / bmp.width, imgHeight / bmp.height)
                                    val w = bmp.width * scale
                                    val h = bmp.height * scale
                                    val drawX = x + (imgWidth - w) / 2f
                                    val drawY = currentY + (imgHeight - h) / 2f
                                    val destRect = RectF(drawX, drawY, drawX + w, drawY + h)
                                    canvas.drawBitmap(bmp, null, destRect, bitmapPaint)
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("PdfExport", "Photo rendering failed", e)
                        } finally {
                            activeBitmap?.recycle()
                        }

                        col++
                        if (col > 1) {
                            col = 0
                            currentY += (imgHeight + spacing)
                        }
                    }

                    if (col > 0) {
                        currentY += (imgHeight + spacing)
                    }
                }
            }

            // Footer for final page
            val footerText = "${localizedContext.getString(R.string.page_label)} $pageNumber"
            val footerWidth = pageNumPaint.measureText(footerText)
            canvas.drawText(footerText, (pageWidth - footerWidth) / 2f, pageHeight - margin / 2f, pageNumPaint)

            pdfDocument.finishPage(currentPage)

            val directory = context.getExternalFilesDir("PDFs")
            if (directory != null && !directory.exists()) directory.mkdirs()
            val sanitizedProjectName = FileNameUtils.sanitize(project.name, "proje")
            val filenameSuffix = if (language == "en") "daily_report" else "gunluk_rapor"
            val file = File(directory, "${sanitizedProjectName}_$filenameSuffix.pdf")

            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            OperationResult.Success(uri)
        } catch (e: Exception) {
            OperationResult.Error(e, "PDF oluşturulurken bir hata oluştu.")
        } finally {
            logoBmp?.recycle()
            pdfDocument?.close()
        }
    }
}
