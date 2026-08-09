@file:Suppress("LocalContextGetResourceValueCall", "MaxLineLength")
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
import com.fatihenes.photoreport.core.common.di.Dispatcher
import com.fatihenes.photoreport.core.common.di.FotoRaporDispatchers
import com.fatihenes.photoreport.core.common.util.DateUtils
import com.fatihenes.photoreport.core.common.util.FileNameUtils
import com.fatihenes.photoreport.core.common.util.result.OperationResult
import com.fatihenes.photoreport.core.database.DailyLogEntity
import com.fatihenes.photoreport.core.database.LogWithPhotos
import com.fatihenes.photoreport.core.database.PhotoEntity
import com.fatihenes.photoreport.core.database.ProjectEntity
import com.fatihenes.photoreport.core.media.CompanyLogoManager
import com.fatihenes.photoreport.core.media.ImageProcessor
import com.fatihenes.photoreport.core.export.pdf.AdaptivePdfLayoutHelper
import com.fatihenes.photoreport.core.export.pdf.HeaderParams
import com.fatihenes.photoreport.core.export.pdf.PdfRect
import com.fatihenes.photoreport.core.export.pdf.PdfStyle
import com.fatihenes.photoreport.core.export.pdf.PdfTheme
import com.fatihenes.photoreport.core.export.pdf.PdfTypography
import com.fatihenes.photoreport.core.export.pdf.PdfMetadataWriter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("AndroidLintAppBundleLocaleChanges")
fun interface PdfExportManager {
    suspend fun exportToPdf(
        project: ProjectEntity,
        logs: List<LogWithPhotos>,
        quality: Int,
        language: String,
        onProgress: ((current: Int, total: Int) -> Unit)?,
    ): OperationResult<Uri>
}

private const val REPORT_ID_PAD = 4
private const val REPORT_ID_RANDOM_LIMIT = 1000
private const val LOGO_MAX_DIMENSION = 500

@Singleton
class NativePdfExportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(FotoRaporDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : PdfExportManager {

    private class ReportMetadata(
        val reportId: String,
        val dateStr: String,
        val timeStr: String,
    )

    private class ExportSessionParams(
        val doc: PdfDocument,
        val project: ProjectEntity,
        val language: String,
        val typography: PdfTypography,
        val logo: Bitmap?,
        val metadata: ReportMetadata,
    )

    private class ExportSession(params: ExportSessionParams) {
        val doc = params.doc
        val project = params.project
        val language = params.language
        val typography = params.typography
        val logo = params.logo
        val reportId = params.metadata.reportId
        val dateStr = params.metadata.dateStr
        val timeStr = params.metadata.timeStr

        var pageNumber = 1
        var pageInfo: PdfDocument.PageInfo = PdfDocument.PageInfo.Builder(PdfTheme.PAGE_WIDTH, PdfTheme.PAGE_HEIGHT, pageNumber).create()
        var currentPage: PdfDocument.Page = doc.startPage(pageInfo)
        var canvas: Canvas = currentPage.canvas
        var currentY = 0f
        val layout = AdaptivePdfLayoutHelper()

        fun startPage() {
            currentY = PdfStyle.drawHeader(
                HeaderParams(canvas, project.name, dateStr, timeStr, reportId, logo, language, typography),
            )
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
        onProgress: ((current: Int, total: Int) -> Unit)?,
    ): OperationResult<Uri> = withContext(ioDispatcher) {
        var logoBmp: Bitmap? = null
        val doc = PdfDocument()
        try {
            setupLocale(language)
            logoBmp = loadLogo()
            val session = createExportSession(doc, project, language, logoBmp)
            session.startPage()

            val total = logs.sumOf { it.photos.size }
            var curCount = 0
            var pIdx = 1

            logs.sortedBy { it.log.date }.forEachIndexed { i, lwp ->
                if (i > 0) session.advancePage()
                renderLogEntry(session, lwp.log, language)
                val renderParams = RenderPhotosParams(
                    session = session,
                    logWithPhotos = lwp,
                    lang = language,
                    startIndex = pIdx,
                    quality = quality,
                    currentCount = curCount,
                    total = total,
                    onProgress = onProgress,
                )
                curCount = renderLogPhotos(renderParams)
                pIdx += lwp.photos.filter { !it.filePath.endsWith(".mp4", ignoreCase = true) }.size
            }

            finalizeExport(session, language)
            val file = generatePdfFile(doc, project.name, language)
            OperationResult.Success(
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file),
            )
        } catch (e: java.io.IOException) {
            OperationResult.Error(e, if (language == "en") "Error generating PDF." else "PDF oluşturma hatası.")
        } catch (e: SecurityException) {
            OperationResult.Error(e, if (language == "en") "Security error." else "Güvenlik hatası.")
        } finally {
            logoBmp?.recycle()
            doc.close()
        }
    }

    private fun createExportSession(
        doc: PdfDocument,
        project: ProjectEntity,
        language: String,
        logo: Bitmap?,
    ): ExportSession {
        val now = System.currentTimeMillis()
        val reportId = "${project.id.toString().padStart(REPORT_ID_PAD, '0')}-${now % REPORT_ID_RANDOM_LIMIT}"
        val dateStr = DateUtils.formatDate(now, language)
        val timeStr = android.text.format.DateFormat.format("HH:mm", now).toString()
        val metadata = ReportMetadata(reportId, dateStr, timeStr)
        return ExportSession(
            ExportSessionParams(doc, project, language, PdfTypography(context), logo, metadata),
        )
    }

    private fun finalizeExport(session: ExportSession, language: String) {
        val remainingSpace = PdfTheme.PAGE_HEIGHT - PdfTheme.MARGIN - PdfTheme.FOOTER_HEIGHT
        if ((session.currentY + PdfTheme.SIGN_OFF_HEIGHT) > remainingSpace) {
            session.advancePage()
        }
        PdfStyle.drawSignOffBlock(session.canvas, session.currentY, language, session.typography)
        session.finalizePage()
    }

    @Suppress("kotlin:S5324")
    private fun generatePdfFile(doc: PdfDocument, projectName: String, lang: String): File {
        val dir = if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            context.getExternalFilesDir("PDFs") ?: context.filesDir
        } else {
            context.filesDir
        }
        if (!dir.exists()) dir.mkdirs()
        val sanitized = FileNameUtils.sanitize(projectName, "proje")
        val suffix = if (lang == "en") "daily_report" else "gunluk_rapor"
        val file = File(dir, "${sanitized}_$suffix.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        PdfMetadataWriter.injectMetadata(
            file,
            "FotoRapor - $projectName",
            if (lang == "en") "Technical Report" else "Teknik Rapor",
        )
        return file
    }

    private fun setupLocale(language: String) {
        val locale = if (language == "en") Locale.US else Locale.forLanguageTag("tr-TR")
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.createConfigurationContext(config)
    }

    private fun loadLogo(): Bitmap? = CompanyLogoManager.getLogoUri(context)?.let {
        ImageProcessor.loadScaledBitmap(context, it.toString(), LOGO_MAX_DIMENSION, LOGO_MAX_DIMENSION)
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
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.25f)
                .build()
            val footerLimit = PdfTheme.PAGE_HEIGHT - PdfTheme.MARGIN - PdfTheme.FOOTER_HEIGHT
            if (session.currentY + sl.height > footerLimit) {
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

    private data class RenderPhotosParams(
        val session: ExportSession,
        val logWithPhotos: LogWithPhotos,
        val lang: String,
        val startIndex: Int,
        val quality: Int,
        val currentCount: Int,
        val total: Int,
        val onProgress: ((Int, Int) -> Unit)?,
    )

    private fun renderLogPhotos(params: RenderPhotosParams): Int {
        var count = params.currentCount
        var idx = params.startIndex
        val photos = params.logWithPhotos.photos.filter { !it.filePath.endsWith(".mp4", ignoreCase = true) }
        for (photo in photos) {
            params.onProgress?.invoke(++count, params.total)
            val opt = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            try {
                ImageProcessor.openInputStreamSafe(context, photo.filePath)?.use {
                    BitmapFactory.decodeStream(it, null, opt)
                }
            } catch (e: Exception) {
                android.util.Log.w("PdfExportManager", "Failed to decode bounds for ${photo.filePath}", e)
            }
            val w = if (opt.outWidth > 0) opt.outWidth else 1000
            val h = if (opt.outHeight > 0) opt.outHeight else 1000
            var res = params.session.layout.calculateSlot(w, h)
            if (res.isNewPageRequired) {
                params.session.advancePage()
                res = params.session.layout.calculateSlot(w, h)
            }
            val drawParams = DrawPhotoParams(
                photo = photo,
                rect = res.rect,
                canvas = params.session.canvas,
                typography = params.session.typography,
                date = params.logWithPhotos.log.date,
                language = params.lang,
                photoIndex = idx++,
                quality = params.quality,
            )
            drawPhoto(drawParams)
            params.session.layout.updateY(res.nextY)
            params.session.currentY = params.session.layout.currentY
        }
        if (params.session.layout.currentColumn > 0) {
            params.session.currentY += (PdfTheme.IMAGE_HEIGHT + PdfTheme.GRID_SPACING)
            params.session.layout.updateY(params.session.currentY)
        }
        return count
    }

    private data class DrawPhotoParams(
        val photo: PhotoEntity,
        val rect: PdfRect,
        val canvas: Canvas,
        val typography: PdfTypography,
        val date: Long,
        val language: String,
        val photoIndex: Int,
        val quality: Int,
    )

    @Suppress("TooGenericExceptionCaught")
    private fun drawPhoto(p: DrawPhotoParams) {
        var bmp: Bitmap? = null
        try {
            val dim = if (p.quality == 100) 1500 else 1000
            bmp = ImageProcessor.loadScaledBitmap(context, p.photo.filePath, dim, dim, Bitmap.Config.ARGB_8888)
            bmp?.let { processAndDrawPhoto(p, it) }
        } catch (e: Throwable) {
            android.util.Log.e("PdfExportManager", "Error rendering photo ${p.photo.filePath}", e)
        } finally {
            bmp?.recycle()
        }
    }

    private fun processAndDrawPhoto(p: DrawPhotoParams, original: Bitmap) {
        val exifRot = ImageProcessor.getExifRotation(context, p.photo.filePath)
        val rot = (exifRot + p.photo.rotation) % 360f
        val finalBmp = if (rot != 0f) {
            val m = android.graphics.Matrix().apply { postRotate(rot) }
            Bitmap.createBitmap(original, 0, 0, original.width, original.height, m, true)
                .also { r -> if (r !== original) original.recycle() }
        } else {
            original
        }

        val prefix = if (p.language == "en") "Photo" else "Görsel"
        val foot = "$prefix #${p.photoIndex} • ${DateUtils.formatDate(p.date, p.language)}"

        PdfStyle.drawPhotoFrame(
            PdfStyle.PhotoFrameParams(
                p.canvas,
                finalBmp,
                p.rect.left,
                p.rect.top,
                p.rect.width(),
                p.rect.height(),
                foot,
                p.typography,
            ),
        )
        if (finalBmp !== original) finalBmp.recycle()
    }
}
