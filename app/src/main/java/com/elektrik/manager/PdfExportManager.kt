package com.sarikaya.santiye.gunlugu.manager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import com.sarikaya.santiye.gunlugu.data.LogWithPhotos
import com.sarikaya.santiye.gunlugu.data.ProjectEntity
import com.sarikaya.santiye.gunlugu.util.CompanyLogoManager
import com.sarikaya.santiye.gunlugu.util.DateUtils
import com.sarikaya.santiye.gunlugu.util.ImageUtils
import com.sarikaya.santiye.gunlugu.util.result.OperationResult
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/**
 * Handles PDF generation operations asynchronously.
 */
interface PdfExportManager {
    /**
     * Exports the project and its logs to a PDF file using chunked generation to prevent OOM.
     * 
     * @param project The project to export.
     * @param logs The chronological list of logs and their photos.
     * @param quality The compression quality (1-100).
     * @return [OperationResult.Success] containing the content URI of the generated PDF.
     */
    suspend fun exportToPdf(
        project: ProjectEntity,
        logs: List<LogWithPhotos>,
        quality: Int = 100
    ): OperationResult<Uri>
}

@Singleton
class PdfBoxExportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileManager: FileManager
) : PdfExportManager {

    override suspend fun exportToPdf(
        project: ProjectEntity,
        logs: List<LogWithPhotos>,
        quality: Int
    ): OperationResult<Uri> = withContext(Dispatchers.IO) {
        var document: PDDocument? = null
        try {
            document = PDDocument()
            val sortedLogs = logs.sortedBy { it.log.date }
            
            // A4 Size in points (1 pt = 1/72 inch) -> 595 x 842
            val pageWidth: Float = PDRectangle.A4.width
            val pageHeight: Float = PDRectangle.A4.height
            var pageNumber = 1
            
            val margin = 40f
            var currentY: Float = pageHeight - margin
            
            // Pre-load company logo if exists
            val companyLogoPath = CompanyLogoManager.getLogoUri(context)?.path
            var logoImage: com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject? = null
            companyLogoPath?.let { path ->
                val f = File(path)
                if (f.exists()) {
                    val bmp = BitmapFactory.decodeFile(path)
                    if (bmp != null) {
                        logoImage = JPEGFactory.createFromImage(document, bmp)
                        bmp.recycle()
                    }
                }
            }

            var currentPage = PDPage(PDRectangle.A4)
            document.addPage(currentPage)
            var contentStream = PDPageContentStream(document, currentPage)

            fun drawHeader() {
                // Title
                contentStream.beginText()
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 24f)
                // Note: PDFBox standard fonts don't support Turkish chars well.
                // For a production app, TTF fonts should be embedded. Here we map or strip if needed, 
                // but for simplicity we rely on Helvetica.
                val safeTitle = project.name.replace("ı", "i").replace("ğ", "g")
                    .replace("ü", "u").replace("ş", "s").replace("ö", "o").replace("ç", "c")
                    .replace("İ", "I").replace("Ğ", "G").replace("Ü", "U")
                    .replace("Ş", "S").replace("Ö", "O").replace("Ç", "C")
                
                val titleWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(safeTitle) / 1000 * 24f
                contentStream.newLineAtOffset((pageWidth - titleWidth) / 2, pageHeight - margin - 24f)
                contentStream.showText(safeTitle)
                contentStream.endText()
                
                currentY = pageHeight - margin - 50f
                
                // Draw Line
                contentStream.moveTo(margin, currentY)
                contentStream.lineTo(pageWidth - margin, currentY)
                contentStream.stroke()
                currentY -= 20f

                // Draw Logo
                logoImage?.let {
                    val scale = 40f / it.height
                    val w = it.width * scale
                    contentStream.drawImage(it, pageWidth - margin - w, pageHeight - margin - 30f, w, 40f)
                }
            }

            fun closeAndStartNewPage() {
                contentStream.beginText()
                contentStream.setFont(PDType1Font.HELVETICA, 10f)
                contentStream.newLineAtOffset(pageWidth / 2 - 15f, margin / 2)
                contentStream.showText("Sayfa $pageNumber")
                contentStream.endText()
                contentStream.close()
                
                pageNumber++
                currentPage = PDPage(PDRectangle.A4)
                document.addPage(currentPage)
                contentStream = PDPageContentStream(document, currentPage)
                currentY = pageHeight - margin
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

                val dateStr = DateUtils.formatDate(log.date)
                contentStream.beginText()
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14f)
                contentStream.newLineAtOffset(margin, currentY)
                contentStream.showText("Tarih: $dateStr")
                contentStream.endText()
                currentY -= 30f
                
                if (log.note.isNotBlank()) {
                    val safeNote = log.note.replace("ı", "i").replace("ğ", "g")
                        .replace("ü", "u").replace("ş", "s").replace("ö", "o").replace("ç", "c")
                        .replace("İ", "I").replace("Ğ", "G").replace("Ü", "U")
                        .replace("Ş", "S").replace("Ö", "O").replace("Ç", "C")
                    
                    contentStream.beginText()
                    contentStream.setFont(PDType1Font.HELVETICA, 12f)
                    contentStream.newLineAtOffset(margin + 10f, currentY)
                    // Simplified: We assume short notes for now. In full implementation, text wrapping is needed.
                    // To keep it memory safe and simple:
                    val chunks = safeNote.chunked(70)
                    for (chunk in chunks) {
                        if (currentY < margin + 40f) {
                            contentStream.endText()
                            closeAndStartNewPage()
                            contentStream.beginText()
                            contentStream.setFont(PDType1Font.HELVETICA, 12f)
                            contentStream.newLineAtOffset(margin + 10f, currentY)
                        }
                        contentStream.showText(chunk.replace("\n", " "))
                        contentStream.newLineAtOffset(0f, -14f)
                        currentY -= 14f
                    }
                    contentStream.endText()
                    currentY -= 20f
                }
                
                if (photos.isNotEmpty()) {
                    var col = 0
                    val imgWidth = 230f
                    val imgHeight = 300f
                    val spacing = 20f
                    
                    for (photo in photos) {
                        val isVideo = photo.filePath.endsWith(".mp4", ignoreCase = true)
                        if (isVideo) continue // Skip videos in PDF to save memory/processing
                        
                        if (currentY - imgHeight < margin + 30f) {
                            closeAndStartNewPage()
                            col = 0
                        }
                        
                        val x = margin + col * (imgWidth + spacing)
                        
                        try {
                            // Memory efficient loading
                            val reqDim = if (quality == 100) 1200 else 800
                            var bmp = ImageUtils.loadScaledBitmap(context, photo.filePath, reqDim, reqDim)
                            if (bmp != null) {
                                // PDFBox rotation
                                val exifRotation = ImageUtils.getExifRotation(context, photo.filePath)
                                val totalRotation = (exifRotation + photo.rotation) % 360f
                                
                                if (totalRotation != 0f) {
                                    val matrix = android.graphics.Matrix()
                                    matrix.postRotate(totalRotation)
                                    val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                                    if (rotated != bmp) {
                                        bmp.recycle()
                                        bmp = rotated
                                    }
                                }
                                
                                val pdfImage = JPEGFactory.createFromImage(document, bmp, 0.75f)
                                bmp.recycle()
                                
                                val scale = min(imgWidth / pdfImage.width, imgHeight / pdfImage.height)
                                val w = pdfImage.width * scale
                                val h = pdfImage.height * scale
                                
                                val drawX = x + (imgWidth - w) / 2
                                val drawY = currentY - imgHeight + (imgHeight - h) / 2
                                
                                contentStream.drawImage(pdfImage, drawX, drawY, w, h)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("PdfBox", "Image draw failed", e)
                        }
                        
                        col++
                        if (col > 1) {
                            col = 0
                            currentY -= (imgHeight + spacing)
                        }
                    }
                    
                    if (col > 0) {
                        currentY -= (imgHeight + spacing)
                    }
                }
            }

            contentStream.beginText()
            contentStream.setFont(PDType1Font.HELVETICA, 10f)
            contentStream.newLineAtOffset(pageWidth / 2 - 15f, margin / 2)
            contentStream.showText("Sayfa $pageNumber")
            contentStream.endText()
            contentStream.close()
            
            val directory = context.getExternalFilesDir("PDFs")
            if (directory != null && !directory.exists()) directory.mkdirs()
            val safeName = project.name.replace(Regex("[^a-zA-Z0-9]"), "_")
            val file = File(directory, "${safeName}_gunluk_rapor.pdf")
            
            document.save(file)
            
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            OperationResult.Success(uri)
        } catch (e: Exception) {
            OperationResult.Error(e, "PDF oluşturulurken kritik bir hata oluştu.")
        } finally {
            (document as? java.io.Closeable)?.close()
        }
    }
}
