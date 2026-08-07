package com.fatihenes.photoreport.core.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.text.Layout
import android.text.StaticLayout
import androidx.core.content.FileProvider
import androidx.core.graphics.withTranslation
import com.fatihenes.photoreport.core.common.util.DateUtils
import com.fatihenes.photoreport.core.common.util.FileNameUtils
import com.fatihenes.photoreport.core.common.util.result.OperationResult
import com.fatihenes.photoreport.core.database.*
import com.fatihenes.photoreport.core.media.CompanyLogoManager
import com.fatihenes.photoreport.core.media.ImageProcessor
import com.fatihenes.photoreport.core.export.pdf.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("AndroidLintAppBundleLocaleChanges")
interface PdfExportManager {
    suspend fun exportToPdf(
        project: ProjectEntity,
        logs: List<LogWithPhotos>,
        quality: Int = 100,
        language: String = "tr",
        onProgress: ((current: Int, total: Int) -> Unit)? = null,
    ): OperationResult<Uri>
}

@Singleton
class NativePdfExportManager @Inject constructor(
    @ApplicationContext private val context: Context
) : PdfExportManager {

    private class ExportSession(
        val doc: PdfDocument,
        val project: ProjectEntity,
        val language: String,
        val typography: PdfTypography,
        val logo: Bitmap?,
        val reportId: String,
        val dateStr: String,
        val timeStr: String
    ) {
        var pageNumber = 1
        var pageInfo: PdfDocument.PageInfo = PdfDocument.PageInfo.Builder(PdfTheme.PAGE_WIDTH, PdfTheme.PAGE_HEIGHT, pageNumber).create()
        var currentPage: PdfDocument.Page = doc.startPage(pageInfo)
        var canvas: Canvas = currentPage.canvas
        var currentY = 0f
        val layout = AdaptivePdfLayoutHelper()

        fun startPage() {
            currentY = PdfStyle.drawHeader(canvas, project.name, dateStr, timeStr, reportId, logo, language, typography)
            layout.reset(currentY)
        }

        fun advancePage() {
            PdfStyle.drawFooter(canvas, pageNumber, language, typography)
            doc.finishPage(currentPage)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(PdfTheme.PAGE_WIDTH, PdfTheme.PAGE_HEIGHT, pageNumber).create()
            currentPage = doc.startPage(pageInfo)
            canvas = currentPage.canvas
            startPage()
        }

        fun finalizePage() {
            PdfStyle.drawFooter(canvas, pageNumber, language, typography)
            doc.finishPage(currentPage)
        }
    }

    override suspend fun exportToPdf(
        project: ProjectEntity,
        logs: List<LogWithPhotos>,
        quality: Int,
        language: String,
        onProgress: ((current: Int, total: Int) -> Unit)?
    ): OperationResult<Uri> = withContext(Dispatchers.IO) {
        var logoBmp: Bitmap? = null
        val doc = PdfDocument()
        try {
            setupLocale(language)
            logoBmp = loadLogo()

            val session = ExportSession(
                doc = doc,
                project = project,
                language = language,
                typography = PdfTypography(context),
                logo = logoBmp,
                reportId = "${project.id.toString().padStart(4, '0')}-${System.currentTimeMillis() % 1000}",
                dateStr = DateUtils.formatDate(System.currentTimeMillis(), language),
                timeStr = android.text.format.DateFormat.format("HH:mm", System.currentTimeMillis()).toString()
            )
            session.startPage()

            val totalPhotos = logs.sumOf { it.photos.size }
            var currentPhotoCount = 0
            var photoIndex = 1

            logs.sortedBy { it.log.date }.forEachIndexed { index, logWithPhotos ->
                if (index > 0) session.advancePage()
                renderLogEntry(session, logWithPhotos.log, language)
                currentPhotoCount = renderLogPhotos(context, session, logWithPhotos, language, photoIndex, quality, currentPhotoCount, totalPhotos, onProgress).also {
                    photoIndex += logWithPhotos.photos.filter { !it.filePath.endsWith(".mp4", true) }.size
                }
            }

            if (session.currentY + PdfTheme.SIGN_OFF_HEIGHT > PdfTheme.PAGE_HEIGHT - PdfTheme.MARGIN - PdfTheme.FOOTER_HEIGHT) {
                session.advancePage()
            }
            PdfStyle.drawSignOffBlock(session.canvas, session.currentY, language, session.typography)
            session.finalizePage()

            val file = generatePdfFile(doc, project.name, language)
            OperationResult.Success(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
        } catch (e: Exception) {
            OperationResult.Error(e, if (language == "en") "Error generating PDF." else "PDF oluşturma hatası.")
        } finally {
            logoBmp?.recycle()
            doc.close()
        }
    }

    private fun setupLocale(language: String) {
        val locale = if (language == "en") Locale.US else Locale.forLanguageTag("tr-TR")
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.createConfigurationContext(config)
    }

    private fun loadLogo(): Bitmap? {
        return CompanyLogoManager.getLogoUri(context)?.let {
            ImageProcessor.loadScaledBitmap(context, it.toString(), 500, 500)
        }
    }

    private fun renderLogEntry(session: ExportSession, log: DailyLogEntity, lang: String) {
        val dateLabel = if (lang == "en") "INSPECTION DATE:" else "DENETİM TARİHİ:"
        val dateStr = "$dateLabel ${DateUtils.formatDate(log.date, lang)}"
        session.canvas.drawText(dateStr, PdfTheme.MARGIN, session.currentY + 12f, session.typography.dateSectionPaint)
        session.currentY += 28f
        session.layout.updateY(session.currentY)

        if (log.note.isNotBlank()) {
            val width = (PdfTheme.PAGE_WIDTH - (PdfTheme.MARGIN * 2) - 10).toInt()
            val sl = StaticLayout.Builder.obtain(log.note, 0, log.note.length, session.typography.bodyPaint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(0f, 1.25f).build()

            if (session.currentY + sl.height > (PdfTheme.PAGE_HEIGHT - PdfTheme.MARGIN - PdfTheme.FOOTER_HEIGHT)) {
                session.advancePage()
                session.canvas.drawText(dateStr, PdfTheme.MARGIN, session.currentY + 12f, session.typography.dateSectionPaint)
                session.currentY += 28f
                session.layout.updateY(session.currentY)
            }
            session.canvas.withTranslation(PdfTheme.MARGIN + 4f, session.currentY) { sl.draw(this) }
            session.currentY += sl.height + 22f
            session.layout.updateY(session.currentY)
        }
    }

    private fun renderLogPhotos(
        context: Context,
        session: ExportSession,
        logWithPhotos: LogWithPhotos,
        lang: String,
        startIndex: Int,
        quality: Int,
        currentCount: Int,
        total: Int,
        onProgress: ((Int, Int) -> Unit)?
    ): Int {
        var count = currentCount
        var idx = startIndex
        val photos = logWithPhotos.photos.filter { !it.filePath.endsWith(".mp4", true) }

        for (photo in photos) {
            onProgress?.invoke(++count, total)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ImageProcessor.openInputStreamSafe(context, photo.filePath)?.use { BitmapFactory.decodeStream(it, null, options) }

            var res = session.layout.calculateSlot(options.outWidth, options.outHeight)
            if (res.isNewPageRequired) {
                session.advancePage()
                res = session.layout.calculateSlot(options.outWidth, options.outHeight)
            }
            drawPhoto(context, photo, res.rect, session.canvas, session.typography, logWithPhotos.log.date, lang, idx++, quality)
            session.layout.updateY(res.nextY)
            session.currentY = session.layout.getCurrentY()
        }

        if (session.layout.getCurrentColumn() > 0) {
            session.currentY += (PdfTheme.IMAGE_HEIGHT + PdfTheme.GRID_SPACING)
            session.layout.updateY(session.currentY)
        }
        return count
    }

    private fun generatePdfFile(doc: PdfDocument, projectName: String, lang: String): File {
        val dir = if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            context.getExternalFilesDir("PDFs")
        } else context.filesDir

        if (dir != null && !dir.exists()) dir.mkdirs()
        val sanitized = FileNameUtils.sanitize(projectName, "proje")
        val suffix = if (lang == "en") "daily_report" else "gunluk_rapor"
        val file = File(dir, "${sanitized}_$suffix.pdf")

        FileOutputStream(file).use { doc.writeTo(it) }
        PdfMetadataWriter.injectMetadata(file, "FotoRapor - $projectName", if (lang == "en") "Technical Report" else "Teknik Rapor")
        return file
    }

    private fun drawPhoto(
        context: Context,
        photo: PhotoEntity,
        rect: PdfRect,
        canvas: Canvas,
        typography: PdfTypography,
        date: Long,
        language: String,
        photoIndex: Int,
        quality: Int
    ) {
        var bmp: Bitmap? = null
        try {
            val dim = if (quality == 100) 1500 else 1000
            bmp = ImageProcessor.loadScaledBitmap(context, photo.filePath, dim, dim, Bitmap.Config.ARGB_8888)
            bmp?.let {
                val rot = (ImageProcessor.getExifRotation(context, photo.filePath) + photo.rotation) % 360f
                val finalBmp = if (rot != 0f) {
                    val m = android.graphics.Matrix().apply { postRotate(rot) }
                    Bitmap.createBitmap(it, 0, 0, it.width, it.height, m, true).also { r -> if (r !== it) it.recycle() }
                } else it

                val prefix = if (language == "en") "Photo" else "Görsel"
                PdfStyle.drawPhotoFrame(canvas, finalBmp, rect.left, rect.top, rect.width(), rect.height(), "$prefix #$photoIndex • ${DateUtils.formatDate(date, language)}", typography)
                if (finalBmp !== it) finalBmp.recycle()
            }
        } finally {
            bmp?.recycle()
        }
    }
}
