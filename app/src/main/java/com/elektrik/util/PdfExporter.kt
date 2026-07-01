package com.elektrik.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.graphics.scale
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withSave
import androidx.core.graphics.withTranslation
import androidx.core.net.toUri
import com.elektrik.data.LogWithPhotos
import com.elektrik.data.ProjectEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

object PdfExporter {

    suspend fun exportToPdf(
        context: Context,
        project: ProjectEntity,
        logs: List<LogWithPhotos>,
        quality: Int = 100
    ) = withContext(Dispatchers.IO) {
        val sortedLogs = logs.sortedBy { it.log.date } // Chronological order
        val pdfDocument = PdfDocument()
        
        // Page dimensions (A4 size at 120 dpi - balanced resolution)
        val pageWidth = 992
        val pageHeight = 1403
        var pageNumber = 1
        
        val companyLogo = CompanyLogoManager.getLogoBitmap(context)?.let {
            if (it.width > 0 && it.height > 0) {
                try {
                    val maxHeight = 64
                    val ratio = it.width.toFloat() / it.height.toFloat()
                    val newWidth = (maxHeight * ratio).toInt().coerceAtLeast(1)
                    it.scale(newWidth, maxHeight, true)
                } catch (_: Exception) {
                    android.util.Log.e("PdfExporter", "Logo scale error")
                    null
                }
            } else null
        }
        
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        
        // Paints
        val headerDatePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 22f
            isFakeBoldText = true
            isAntiAlias = true
        }
        
        val cardPaint = Paint().apply {
            color = Color.argb(12, 0, 0, 0)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        val borderPaint = Paint().apply {
            color = Color.argb(30, 0, 0, 0)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            isAntiAlias = true
        }

        val centerTitlePaint = Paint().apply {
            color = project.colorHex.toColorInt()
            textSize = 34f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        var currentY = 140
        val margin = 50
        val bottomMargin = 70
        
        fun drawHeader(canvas: Canvas) {
            canvas.drawText(project.name, (pageWidth / 2).toFloat(), 80f, centerTitlePaint)
            canvas.drawLine(margin.toFloat(), 110f, (pageWidth - margin).toFloat(), 110f, borderPaint)
            
            companyLogo?.let { logo ->
                canvas.drawBitmap(logo, (pageWidth - margin - logo.width).toFloat(), 25f, null)
            }
        }

        drawHeader(canvas)

        
        fun startNewPage() {
            val footerPaint = Paint().apply {
                color = Color.GRAY
                textSize = 14f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Sayfa $pageNumber", (pageWidth / 2).toFloat(), (pageHeight - 40).toFloat(), footerPaint)
            
            pdfDocument.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            
            drawHeader(canvas)
            currentY = 140
        }
        
        var isFirstLog = true
        for (logWithPhotos in sortedLogs) {
            val log = logWithPhotos.log
            val photos = logWithPhotos.photos
            // Her yeni gÃƒÆ’Ã‚Â¼n raporu yeni bir sayfadan baÃƒâ€¦Ã…Â¸lar (Ãƒâ€žÃ‚Â°lk sayfa hariÃƒÆ’Ã‚Â§)
            if (!isFirstLog) {
                startNewPage()
            }
            isFirstLog = false
            
            if (currentY + 100 > pageHeight - bottomMargin) {
                startNewPage()
            }
            
            val formattedDate = DateUtils.formatDate(log.date)
            canvas.drawText("Tarih: $formattedDate", margin.toFloat(), currentY.toFloat(), headerDatePaint)
            currentY += 25
            
            if (log.note.trim().isNotEmpty()) {
                val maxTextWidth = pageWidth - (margin * 2) - 30
                
                val textPaint = TextPaint().apply {
                    color = Color.BLACK
                    textSize = 18f
                    isAntiAlias = true
                }
                
                val staticLayout = StaticLayout.Builder.obtain(log.note, 0, log.note.length, textPaint, maxTextWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1.2f)
                    .setIncludePad(false)
                    .build()
                
                val cardHeight = staticLayout.height + 60
                if (currentY + cardHeight > pageHeight - bottomMargin) {
                    startNewPage()
                }
                
                val rect = RectF(
                    margin.toFloat(),
                    currentY.toFloat(),
                    (pageWidth - margin).toFloat(),
                    (currentY + cardHeight).toFloat()
                )
                canvas.drawRoundRect(rect, 12f, 12f, cardPaint)
                canvas.drawRoundRect(rect, 12f, 12f, borderPaint)
                
                canvas.withTranslation((margin + 15).toFloat(), (currentY + 30).toFloat()) {
                    staticLayout.draw(this)
                }
                
                currentY += cardHeight + 25
            } else {
                currentY += 10
            }
            
            if (photos.isNotEmpty()) {
                val hasNote = log.note.trim().isNotEmpty()
                var maxPhotosForThisPage = if (hasNote) 2 else 4
                var photosDrawnOnCurrentPage = 0
                
                val slotWidth = 431
                val slotHeight = 550 // Sayfaya tam sÃƒâ€Ã‚Â±Ãƒâ€Ã…Â¸acak ve yayÃƒâ€Ã‚Â±lacak optimum ÃƒÆ’Ã‚Â¶lÃƒÆ’Ã‚Â§ÃƒÆ’Ã‚Â¼
                val spacing = 30 // Increased spacing to spread photos
                var col = 0
                
                // Ãƒâ€Ã‚Â°lk sayfalardaki (tarih olan) yÃƒâ€Ã‚Â±Ãƒâ€Ã…Â¸Ãƒâ€Ã‚Â±lmayÃƒâ€Ã‚Â± ÃƒÆ’Ã‚Â¶nlemek iÃƒÆ’Ã‚Â§in baÃƒâ€¦Ã…Â¸a biraz boÃƒâ€¦Ã…Â¸luk ekle
                if (currentY < 250) {
                    currentY += 20
                }
                
                for (photo in photos) {
                    if (photosDrawnOnCurrentPage >= maxPhotosForThisPage) {
                        startNewPage()
                        photosDrawnOnCurrentPage = 0
                        maxPhotosForThisPage = 4
                        col = 0
                    }
                    
                    val x = margin + col * (slotWidth + spacing)
                    if (currentY + slotHeight > pageHeight - bottomMargin) {
                        startNewPage()
                        photosDrawnOnCurrentPage = 0
                        maxPhotosForThisPage = 4
                        col = 0
                    }
                    
                    val exifRotation = ImageUtils.getExifRotation(context, photo.filePath)
                    val totalRotation = (exifRotation + photo.rotation) % 360f

                    val sourceBitmap = if (photo.filePath.endsWith(".mp4", ignoreCase = true)) {
                        loadVideoFrame(context, photo.filePath)
                    } else {
                        // Orijinal kalite (100) iÃƒÆ’Ã‚Â§in 2000px sÃƒâ€Ã‚Â±nÃƒâ€Ã‚Â±rÃƒâ€Ã‚Â±. A4 PDF'te 2000px "kayÃƒâ€Ã‚Â±psÃƒâ€Ã‚Â±z" print kalitesi saÃƒâ€Ã…Â¸lar 
                        // ve devasa 181MB (OOM) yerine UI'da yazan tahmini boyutu (ÃƒÆ’Ã‚Â¶rn: 41MB) tam tutturur.
                        val reqDim = when (quality) {
                            100 -> 2000 
                            85 -> 1200
                            else -> 800
                        }
                        val bitmap = ImageUtils.loadScaledBitmap(context, photo.filePath, reqDim, reqDim)
                        if (bitmap != null) {
                            try {
                                val maxDim = reqDim.toFloat()
                                val bw = bitmap.width.toFloat()
                                val bh = bitmap.height.toFloat()
                                // inSampleSize ikinin katlarÃƒâ€Ã‚Â± olduÃƒâ€Ã…Â¸u iÃƒÆ’Ã‚Â§in tam boyuta (ÃƒÆ’Ã‚Â¶rneÃƒâ€Ã…Â¸in 2000'e) ÃƒÆ’Ã‚Â§ekiyoruz
                                if (bw > maxDim || bh > maxDim) {
                                    val scale = min(maxDim / bw, maxDim / bh)
                                    val scaled = bitmap.scale((bw * scale).toInt(), (bh * scale).toInt(), true)
                                    if (scaled != bitmap) bitmap.recycle()
                                    scaled
                                } else {
                                    bitmap
                                }
                            } catch (_: OutOfMemoryError) {
                                android.util.Log.e("PdfExporter", "OOM during bitmap scaling")
                                bitmap.recycle()
                                null
                            }
                        } else null
                    }
                    
                    if (sourceBitmap != null) {
                        val bitmapWidth = sourceBitmap.width.toFloat()
                        val bitmapHeight = sourceBitmap.height.toFloat()
                        
                        // Swap dimensions if rotated 90 or 270 degrees
                        val isRotated = totalRotation == 90f || totalRotation == 270f
                        val effectiveBitmapWidth = if (isRotated) bitmapHeight else bitmapWidth
                        val effectiveBitmapHeight = if (isRotated) bitmapWidth else bitmapHeight

                        val viewWidth = slotWidth.toFloat()
                        val viewHeight = slotHeight.toFloat()
                        
                        val scale = min(viewWidth / effectiveBitmapWidth, viewHeight / effectiveBitmapHeight)
                        val finalWidth = effectiveBitmapWidth * scale
                        val finalHeight = effectiveBitmapHeight * scale
                        
                        val drawX = x + (slotWidth - finalWidth) / 2
                        val drawY = currentY + (slotHeight - finalHeight) / 2
                        
                        val matrix = Matrix()
                        matrix.postTranslate(-bitmapWidth / 2f, -bitmapHeight / 2f)
                        if (totalRotation != 0f) {
                            matrix.postRotate(totalRotation)
                        }
                        matrix.postScale(scale, scale)
                        matrix.postTranslate(drawX + finalWidth / 2f, drawY + finalHeight / 2f)

                        canvas.withSave {
                            val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
                            drawBitmap(sourceBitmap, matrix, paint)
                        }
                        
                        if (photo.filePath.endsWith(".mp4", ignoreCase = true)) {
                            val playPaint = Paint().apply {
                                color = Color.WHITE
                                alpha = 180
                                style = Paint.Style.FILL
                                isAntiAlias = true
                            }
                            val trianglePaint = Paint().apply {
                                color = Color.BLACK
                                style = Paint.Style.FILL
                                isAntiAlias = true
                            }
                            
                            val centerX = x + slotWidth / 2
                            val centerY = currentY + slotHeight / 2
                            canvas.drawCircle(centerX.toFloat(), centerY.toFloat(), 30f, playPaint)
                            
                            val path = Path().apply {
                                moveTo((centerX - 10).toFloat(), (centerY - 12).toFloat())
                                lineTo((centerX + 18).toFloat(), centerY.toFloat())
                                lineTo((centerX - 10).toFloat(), (centerY + 12).toFloat())
                                close()
                            }
                            canvas.drawPath(path, trianglePaint)
                        }
                        
                        sourceBitmap.recycle()
                    } else {
                        val rect = RectF(
                            x.toFloat(),
                            currentY.toFloat(),
                            (x + slotWidth).toFloat(),
                            (currentY + slotHeight).toFloat()
                        )
                        canvas.drawRoundRect(rect, 8f, 8f, cardPaint)
                        canvas.drawRoundRect(rect, 8f, 8f, borderPaint)
                    }
                    
                    col++
                    photosDrawnOnCurrentPage++
                    
                    if (col >= 2) {
                        col = 0
                        currentY += slotHeight + spacing
                    }
                }
                
                if (col > 0) {
                    currentY += slotHeight + spacing
                }
                currentY += 15
            }
            
            currentY += 25
        }
        
        val footerPaint = Paint().apply {
            color = Color.GRAY
            textSize = 14f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Sayfa $pageNumber", (pageWidth / 2).toFloat(), (pageHeight - 40).toFloat(), footerPaint)
        
        pdfDocument.finishPage(page)
        
        val directory = context.getExternalFilesDir("PDFs")
        if (directory != null && !directory.exists()) {
            directory.mkdirs()
        }
        val safeName = project.name.replace(Regex("[^a-zA-Z0-9]"), "_")
        val file = File(directory, "${safeName}_gunluk_rapor.pdf")
        
        try {
            FileOutputStream(file).use { fos ->
                pdfDocument.writeTo(fos)
            }
            pdfDocument.close()
            
            withContext(Dispatchers.Main) {
                val authority = "${context.packageName}.fileprovider"
                val uri = FileProvider.getUriForFile(context, authority, file)
                uri
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            pdfDocument.close()
            throw e
        } catch (_: Exception) {
            android.util.Log.e("PdfExporter", "PDF Generation failed")
            pdfDocument.close()
            null
        }
    }
    
    private fun loadVideoFrame(context: Context, videoPath: String): Bitmap? {
        return try {
            val retriever = android.media.MediaMetadataRetriever()
            if (videoPath.startsWith("content://")) {
                retriever.setDataSource(context, videoPath.toUri())
            } else {
                retriever.setDataSource(videoPath)
            }
            val frame = retriever.getFrameAtTime(0, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            retriever.release()
            frame
        } catch (_: Exception) {
            null
        }
    }
}

