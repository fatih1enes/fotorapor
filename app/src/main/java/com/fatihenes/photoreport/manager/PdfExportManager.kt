package com.fatihenes.photoreport.manager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
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

import com.fatihenes.photoreport.manager.pdf.PdfMetadataWriter
import com.fatihenes.photoreport.manager.pdf.PdfStyle
import com.fatihenes.photoreport.manager.pdf.PdfTheme
import com.fatihenes.photoreport.manager.pdf.PdfTypography

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
        language: String = "tr",
        onProgress: ((current: Int, total: Int) -> Unit)? = null
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
        language: String,
        onProgress: ((current: Int, total: Int) -> Unit)?
    ): OperationResult<Uri> = withContext(Dispatchers.IO) {
        var pdfDocument: PdfDocument? = null
        var logoBmp: Bitmap? = null
        try {
            val locale = if (language == "en") Locale.US else Locale("tr", "TR")
            val config = android.content.res.Configuration(context.resources.configuration)
            config.setLocale(locale)
            context.createConfigurationContext(config)

            pdfDocument = PdfDocument()
            val sortedLogs = logs.sortedBy { it.log.date }

            val typography = PdfTypography()
            val pageWidth = PdfTheme.PAGE_WIDTH
            val pageHeight = PdfTheme.PAGE_HEIGHT
            val margin = PdfTheme.MARGIN
            var pageNumber = 1

            // Preload logo bitmap
            val logoUri = CompanyLogoManager.getLogoUri(context)
            if (logoUri != null) {
                logoBmp = ImageUtils.loadScaledBitmap(context, logoUri.toString(), 500, 500)
            }

            val reportIdStr = "${project.id.toString().padStart(4, '0')}-${System.currentTimeMillis() % 1000}"
            val generatedDateStr = DateUtils.formatDate(System.currentTimeMillis(), language)
            val timeStr = android.text.format.DateFormat.format("HH:mm", System.currentTimeMillis()).toString()

            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var currentPage = pdfDocument.startPage(pageInfo)
            var canvas = currentPage.canvas
            var currentY: Float

            val layoutHelper = com.fatihenes.photoreport.manager.pdf.AdaptivePdfLayoutHelper()

            fun renderPageHeader(): Float {
                return PdfStyle.drawHeader(
                    canvas = canvas,
                    projectName = project.name,
                    dateStr = generatedDateStr,
                    timeStr = timeStr,
                    reportId = reportIdStr,
                    logoBmp = logoBmp,
                    language = language,
                    typography = typography
                )
            }

            fun closeAndStartNewPage(): Float {
                PdfStyle.drawFooter(canvas, pageNumber, language, typography)
                pdfDocument.finishPage(currentPage)
                
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                currentPage = pdfDocument.startPage(pageInfo)
                canvas = currentPage.canvas
                return renderPageHeader()
            }

            currentY = renderPageHeader()
            layoutHelper.reset(currentY)

            var isFirstLog = true
            var photoIndex = 1
            val totalPhotos = logs.sumOf { it.photos.size }
            var currentPhotoCount = 0

            for (logWithPhotos in sortedLogs) {
                val log = logWithPhotos.log
                val photos = logWithPhotos.photos

                if (!isFirstLog) {
                    currentY = closeAndStartNewPage()
                    layoutHelper.reset(currentY)
                }
                isFirstLog = false

                // Date section header
                val dateLabel = if (language == "en") "INSPECTION DATE:" else "DENETİM TARİHİ:"
                val dateStr = "$dateLabel ${DateUtils.formatDate(log.date, language)}"
                canvas.drawText(dateStr, margin, currentY + 12f, typography.dateSectionPaint)
                currentY += 28f
                layoutHelper.updateY(currentY)

                // Note with multi-line wrapping and native Turkish character support
                if (log.note.isNotBlank()) {
                    val noteWidth = (pageWidth - (margin * 2) - 10).toInt()
                    val staticLayout = StaticLayout.Builder
                        .obtain(log.note, 0, log.note.length, typography.bodyPaint, noteWidth)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(0f, 1.25f)
                        .build()

                    if (currentY + staticLayout.height > pageHeight - margin - PdfTheme.FOOTER_HEIGHT) {
                        currentY = closeAndStartNewPage()
                        layoutHelper.reset(currentY)
                        canvas.drawText(dateStr, margin, currentY + 12f, typography.dateSectionPaint)
                        currentY += 28f
                        layoutHelper.updateY(currentY)
                    }

                    canvas.withTranslation(margin + 4f, currentY) {
                        staticLayout.draw(this)
                    }

                    currentY += staticLayout.height + 22f
                    layoutHelper.updateY(currentY)
                }

                // Photos adaptive grid
                if (photos.isNotEmpty()) {
                    for (photo in photos) {
                        if (photo.filePath.endsWith(".mp4", ignoreCase = true)) continue

                        onProgress?.invoke(++currentPhotoCount, totalPhotos)

                        try {
                            // First pass to get dimensions for layout calculation
                            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            ImageUtils.openInputStreamSafe(context, photo.filePath)?.use { input ->
                                BitmapFactory.decodeStream(input, null, boundsOptions)
                            }
                            
                            val photoW = boundsOptions.outWidth
                            val photoH = boundsOptions.outHeight
                            
                            val layoutResult = layoutHelper.calculateSlot(photoW, photoH)
                            if (layoutResult.isNewPageRequired) {
                                currentY = closeAndStartNewPage()
                                layoutHelper.reset(currentY)
                                // If it was a landscape photo that triggered new page, we might want to re-calculate 
                                // but for simplicity and safety, the second attempt will definitely fit.
                                val retryResult = layoutHelper.calculateSlot(photoW, photoH)
                                drawPhoto(context, photo, retryResult.rect, canvas, typography, log.date, language, photoIndex++, quality)
                                layoutHelper.updateY(retryResult.nextY)
                            } else {
                                drawPhoto(context, photo, layoutResult.rect, canvas, typography, log.date, language, photoIndex++, quality)
                                layoutHelper.updateY(layoutResult.nextY)
                            }
                            currentY = layoutHelper.getCurrentY()

                        } catch (e: Exception) {
                            android.util.Log.e("PdfExport", "Photo rendering failed", e)
                        }
                    }
                    
                    // Final spacing after log's photos if we ended on a partial row
                    if (layoutHelper.getCurrentColumn() > 0) {
                        currentY += (PdfTheme.IMAGE_HEIGHT + PdfTheme.GRID_SPACING)
                        layoutHelper.updateY(currentY)
                    }
                }
            }

            // Son Sayfa İmza ve Onay Bloğu Kontrolü (Sign-Off Table)
            if (currentY + PdfTheme.SIGN_OFF_HEIGHT > pageHeight - margin - PdfTheme.FOOTER_HEIGHT) {
                currentY = closeAndStartNewPage()
            }
            PdfStyle.drawSignOffBlock(canvas, currentY, language, typography)

            // Final Sayfa Altbilgisi (Footer)
            PdfStyle.drawFooter(canvas, pageNumber, language, typography)
            pdfDocument.finishPage(currentPage)

            val directory = context.getExternalFilesDir("PDFs")
            if (directory != null && !directory.exists()) directory.mkdirs()
            val sanitizedProjectName = FileNameUtils.sanitize(project.name, "proje")
            val filenameSuffix = if (language == "en") "daily_report" else "gunluk_rapor"
            val file = File(directory, "${sanitizedProjectName}_$filenameSuffix.pdf")

            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }

            // Adobe Reader uyurlu resmi PDF Metadata bilgisi ekleniyor
            val subjectTitle = if (language == "en") "Field Inspection & Technical Report" else "Saha Denetim ve Teknik Gözlem Raporu"
            PdfMetadataWriter.injectMetadata(
                pdfFile = file,
                title = "FotoRapor - ${project.name}",
                subject = subjectTitle
            )

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            OperationResult.Success(uri)
        } catch (e: Exception) {
            val errMsg = if (language == "en") "Error occurred while generating PDF report." else "PDF raporu oluşturulurken bir hata meydana geldi."
            OperationResult.Error(e, errMsg)
        } finally {
            logoBmp?.recycle()
            pdfDocument?.close()
        }
    }

    private fun drawPhoto(
        context: Context,
        photo: com.fatihenes.photoreport.data.PhotoEntity,
        rect: com.fatihenes.photoreport.manager.pdf.PdfRect,
        canvas: android.graphics.Canvas,
        typography: com.fatihenes.photoreport.manager.pdf.PdfTypography,
        date: Long,
        language: String,
        photoIndex: Int,
        quality: Int
    ) {
        var activeBitmap: Bitmap? = null
        try {
            val reqDim = if (quality == 100) 1500 else 1000
            val loadedBmp = ImageUtils.loadScaledBitmap(
                context, photo.filePath, reqDim, reqDim, Bitmap.Config.ARGB_8888
            )
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

                activeBitmap.let { bmp ->
                    val captionPrefix = if (language == "en") "Photo" else "Görsel"
                    val captionText = "$captionPrefix #$photoIndex • ${DateUtils.formatDate(date, language)}"
                    
                    PdfStyle.drawPhotoFrame(
                        canvas = canvas,
                        bitmap = bmp,
                        x = rect.left,
                        y = rect.top,
                        width = rect.width(),
                        height = rect.height(),
                        captionText = captionText,
                        typography = typography
                    )
                }
            }
        } finally {
            activeBitmap?.recycle()
        }
    }
}
