package com.fatihenes.photoreport.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.text.htmlEncode
import com.fatihenes.photoreport.data.DailyLogEntity
import com.fatihenes.photoreport.data.PhotoEntity
import com.fatihenes.photoreport.data.ProjectEntity
import com.fatihenes.photoreport.util.FileNameUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import com.fatihenes.photoreport.R

object HtmlExporter {

    suspend fun exportToHtmlZip(
        context: Context,
        project: ProjectEntity,
        logs: List<DailyLogEntity>,
        photos: List<PhotoEntity>,
        quality: Int = 100,
        language: String = "tr"
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val sanitizedName = FileNameUtils.sanitize(project.name, "proje")
            val exportDir = File(context.cacheDir, "export_${project.id}_${System.currentTimeMillis()}")
            Log.d("HtmlExporter", "Creating export directory: ${exportDir.absolutePath}")
            if (!exportDir.exists()) exportDir.mkdirs()

            val assetsDir = File(exportDir, "assets")
            if (!assetsDir.exists()) assetsDir.mkdirs()

            var logoAssetPath: String? = null
            if (CompanyLogoManager.hasLogo(context)) {
                val logoUri = CompanyLogoManager.getLogoUri(context)
                if (logoUri != null) {
                    val destLogo = File(assetsDir, "company_logo.png")
                    copyFile(context, logoUri, logoUri.path ?: "", destLogo)
                    if (destLogo.exists()) {
                        logoAssetPath = "assets/company_logo.png"
                    }
                }
            }

            val photoMap = mutableMapOf<Long, String>()

            photos.forEach { photo ->
                Log.d("HtmlExporter", "Processing photo: ${photo.id}, path: ${photo.filePath}")
                val isVideo = photo.filePath.endsWith(".mp4", ignoreCase = true)
                val fileName = if (isVideo) "video_${photo.id}.mp4" else "photo_${photo.id}.jpg"
                val destFile = File(assetsDir, fileName)

                try {
                    val sourceUri = photo.filePath.toUri()
                    if (isVideo) {
                        // Videoları doğrudan kopyalıyoruz
                        copyFile(context, sourceUri, photo.filePath, destFile)
                    } else {
                        if (quality == 100) {
                            // Orijinal kaliteyi koru ve HTML/ZIP içerisinde eksiksiz aktar
                            copyFile(context, sourceUri, photo.filePath, destFile)
                        } else {
                            // Sıkıştırma seçildiyse compressAndSaveImage kullan
                            val success = ImageUtils.compressAndSaveImage(context, photo.filePath, destFile, quality)
                            if (!success) {
                                // Sıkıştırma başarısız olursa orijinali kopyala
                                copyFile(context, sourceUri, photo.filePath, destFile)
                            }
                        }
                    }

                    if (destFile.exists() && destFile.length() > 0) {
                        photoMap[photo.id] = "assets/$fileName"
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e("HtmlExporter", "Error processing media: ${photo.id}", e)
                }
            }

            val htmlFile = File(exportDir, "index.html")
            val htmlContent = generateHtmlContent(context, project, logs, photos, photoMap, logoAssetPath, language)
            htmlFile.writeText(htmlContent)
            Log.d("HtmlExporter", "HTML content written to: ${htmlFile.absolutePath}")

            val zipNamePrefix = if (language == "en") "Report" else "Rapor"
            val zipFile = File(context.cacheDir, "${zipNamePrefix}_${sanitizedName}.zip")
            if (zipFile.exists()) zipFile.delete() // Eskisini sil

            Log.d("HtmlExporter", "Creating ZIP file: ${zipFile.absolutePath}")
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                // Önce index.html'i ekle (opsiyonel ama düzenli olur)
                val htmlEntry = ZipEntry("index.html")
                zos.putNextEntry(htmlEntry)
                htmlFile.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()

                // Sonra assets klasörünü ve içindekileri ekle
                val assetsFiles = assetsDir.listFiles()
                if (assetsFiles != null) {
                    for (file in assetsFiles) {
                        val entry = ZipEntry("assets/${file.name}")
                        zos.putNextEntry(entry)
                        file.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                        Log.d("HtmlExporter", "Added to ZIP: assets/${file.name}")
                    }
                }
            }

            exportDir.deleteRecursively()

            return@withContext FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                zipFile
            )

        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("HtmlExporter", "Error exporting to HTML ZIP", e)
            return@withContext null
        }
    }

    private fun generateHtmlContent(
        context: Context,
        project: ProjectEntity,
        logs: List<DailyLogEntity>,
        photos: List<PhotoEntity>,
        photoMap: Map<Long, String>,
        logoAssetPath: String?,
        language: String
    ): String {
        val locale = if (language == "en") Locale.US else Locale("tr", "TR")
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(locale)
        val localizedContext = context.createConfigurationContext(config)

        val dateFormat = SimpleDateFormat("dd MMMM yyyy", locale)
        val dateStr = dateFormat.format(Date())

        val builder = StringBuilder()
        builder.append("""
            <!DOCTYPE html>
            <html lang="$language">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>${project.name} - ${if(language == "en") "Report" else "Rapor"}</title>
                <style>
                    :root {
                        --bg-color: #f8fafc;
                        --card-bg: #ffffff;
                        --text-primary: #1e293b;
                        --text-secondary: #64748b;
                        --accent: #2563eb;
                        --border: #e2e8f0;
                    }
                    @media (prefers-color-scheme: dark) {
                        :root {
                            --bg-color: #0f172a;
                            --card-bg: #1e293b;
                            --text-primary: #f8fafc;
                            --text-secondary: #94a3b8;
                            --accent: #3b82f6;
                            --border: #334155;
                        }
                    }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                        background-color: var(--bg-color);
                        color: var(--text-primary);
                        line-height: 1.6;
                        margin: 0;
                        padding: 0;
                    }
                    .container {
                        max-width: 800px;
                        margin: 0 auto;
                        padding: 2rem 1rem;
                    }
                    .header {
                        display: flex;
                        align-items: center;
                        justify-content: space-between;
                        margin-bottom: 3rem;
                        background: var(--card-bg);
                        padding: 2rem;
                        border-radius: 16px;
                        box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
                    }
                    .header-content {
                        flex: 1;
                    }
                    .header h1 {
                        font-size: 2.5rem;
                        margin: 0 0 0.5rem 0;
                        color: var(--accent);
                    }
                    .header p {
                        color: var(--text-secondary);
                        font-size: 1.1rem;
                        margin: 0;
                    }
                    .company-logo {
                        max-width: 120px;
                        max-height: 120px;
                        object-fit: contain;
                        border-radius: 12px;
                        margin-left: 2rem;
                    }
                    .timeline {
                        position: relative;
                    }
                    .day-card {
                        background-color: var(--card-bg);
                        border: 1px solid var(--border);
                        border-radius: 12px;
                        padding: 1.5rem;
                        margin-bottom: 2rem;
                        box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
                    }
                    .day-header {
                        display: flex;
                        align-items: center;
                        margin-bottom: 1rem;
                        padding-bottom: 1rem;
                        border-bottom: 1px solid var(--border);
                    }
                    .day-title {
                        font-size: 1.25rem;
                        font-weight: 600;
                        margin: 0;
                    }
                    .note-content {
                        font-size: 1rem;
                        white-space: pre-wrap;
                        margin-bottom: 1.5rem;
                    }
                    .media-grid {
                        display: grid;
                        grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
                        gap: 1rem;
                    }
                    .media-item {
                        border-radius: 8px;
                        overflow: hidden;
                        background: #000;
                        aspect-ratio: 3/4;
                        position: relative;
                    }
                    .media-item img, .media-item video {
                        width: 100%;
                        height: 100%;
                        object-fit: contain;
                        background-color: #000;
                    }
                    video[controls] {
                        max-height: 100%;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="header-content">
                            <h1>${project.name}</h1>
                            <p>${localizedContext.getString(R.string.report_date)}: $dateStr</p>
                        </div>
                        ${if (logoAssetPath != null) "<img src=\"$logoAssetPath\" class=\"company-logo\" alt=\"${localizedContext.getString(R.string.company_logo_alt)}\">" else ""}
                    </div>
                    <div class="timeline">
        """.trimIndent())

        logs.sortedByDescending { it.date }.forEach { log ->
            val logDateStr = dateFormat.format(Date(log.date))
            val dayPhotos = photos.filter { it.logId == log.id }

            if (log.note.trim().isNotEmpty() || dayPhotos.isNotEmpty()) {
                builder.append("""
                    <div class="day-card">
                        <div class="day-header">
                            <h2 class="day-title">$logDateStr</h2>
                        </div>
                """.trimIndent())

                if (log.note.trim().isNotEmpty()) {
                    builder.append("<div class=\"note-content\">${log.note.htmlEncode()}</div>")
                }

                if (dayPhotos.isNotEmpty()) {
                    builder.append("<div class=\"media-grid\">")
                    dayPhotos.forEach { photo ->
                        val assetPath = photoMap[photo.id]
                        if (assetPath != null) {
                            val isVideo = assetPath.endsWith(".mp4", ignoreCase = true)
                            builder.append("<div class=\"media-item\">")
                            if (isVideo) {
                                builder.append("<video controls preload=\"metadata\"><source src=\"$assetPath\" type=\"video/mp4\">${localizedContext.getString(R.string.video_not_supported)}</video>")
                            } else {
                                // Kullanıcının galeride yaptığı döndürme işlemini CSS ile uyguluyoruz (orijinal dosyayı bozmadan)
                                val transform = if (photo.rotation != 0f) "transform: rotate(${photo.rotation}deg);" else ""
                                builder.append("<a href=\"$assetPath\" target=\"_blank\"><img src=\"$assetPath\" alt=\"${localizedContext.getString(R.string.photo_alt)}\" loading=\"lazy\" style=\"$transform\"></a>")
                            }
                            builder.append("</div>")
                        }
                    }
                    builder.append("</div>")
                }

                builder.append("</div>") // End day-card
            }
        }

        builder.append("""
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent())

        return builder.toString()
    }



    private fun copyFile(context: Context, sourceUri: Uri, fallbackPath: String, destFile: File) {
        var success = false

        // 1. Öncelik: ContentResolver ile aç (content:// ve file:// URI'lar için)
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
                success = destFile.exists() && destFile.length() > 0
            }
        } catch (e: Exception) {
            Log.w("HtmlExporter", "ContentResolver failed for $sourceUri: ${e.message}")
        }

        // 2. Fallback: URI'nin path kısmını doğrudan dosya olarak aç
        val uriPath = sourceUri.path
        if (!success && uriPath != null) {
            try {
                val f = File(uriPath)
                if (f.exists()) {
                    f.inputStream().use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    success = destFile.exists() && destFile.length() > 0
                }
            } catch (e: Exception) {
                Log.w("HtmlExporter", "Direct path failed: ${e.message}")
            }
        }

        // 3. Son çare: fallbackPath'i doğrudan dosya olarak aç
        if (!success) {
            try {
                val f = File(fallbackPath)
                if (f.exists()) {
                    f.inputStream().use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    success = destFile.exists() && destFile.length() > 0
                }
            } catch (e: Exception) {
                Log.e("HtmlExporter", "All copy attempts failed for: $fallbackPath", e)
            }
        }

        if (!success) {
            Log.e("HtmlExporter", "COPY COMPLETELY FAILED: uri=$sourceUri, path=$fallbackPath")
        }
    }

}
