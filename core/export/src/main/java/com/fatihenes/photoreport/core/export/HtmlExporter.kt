package com.fatihenes.photoreport.core.export

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.text.htmlEncode
import com.fatihenes.photoreport.core.common.util.FileNameUtils
import com.fatihenes.photoreport.core.database.*
import com.fatihenes.photoreport.core.media.CompanyLogoManager
import com.fatihenes.photoreport.core.media.ImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object HtmlExporter {

    private const val DIV_CLOSE = "</div>"
    private const val LOGO_ASSET_PATH = "assets/company_logo.png"

    suspend fun exportToHtmlZip(
        context: Context,
        project: ProjectEntity,
        logs: List<DailyLogEntity>,
        photos: List<PhotoEntity>,
        quality: Int = 100,
        language: String = "tr",
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val sanitizedName = FileNameUtils.sanitize(project.name, "proje")
            val zipNamePrefix = if (language == "en") "Report" else "Rapor"
            val zipFile = File(context.cacheDir, "${zipNamePrefix}_$sanitizedName.zip")
            if (zipFile.exists()) zipFile.delete()

            val photoMap = mutableMapOf<Long, String>()
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                val logoPath = streamLogoToZip(context, zos)
                streamMediaToZip(context, zos, photos, quality, photoMap)

                val htmlContent = generateHtmlContent(project, logs, photos, photoMap, logoPath, language)
                zos.putNextEntry(ZipEntry("index.html"))
                zos.write(htmlContent.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            return@withContext FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zipFile)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("HtmlExporter", "Export failed", e)
            null
        }
    }

    private fun streamLogoToZip(context: Context, zos: ZipOutputStream): String? {
        if (!CompanyLogoManager.hasLogo(context)) return null
        val logoUri = CompanyLogoManager.getLogoUri(context) ?: return null
        return try {
            zos.putNextEntry(ZipEntry(LOGO_ASSET_PATH))
            ImageProcessor.openInputStreamSafe(context, logoUri.toString())?.use { it.copyTo(zos, 8192) }
            zos.closeEntry()
            LOGO_ASSET_PATH
        } catch (e: Exception) {
            Log.e("HtmlExporter", "Logo stream failed", e)
            null
        }
    }

    private fun streamMediaToZip(
        context: Context,
        zos: ZipOutputStream,
        photos: List<PhotoEntity>,
        quality: Int,
        photoMap: MutableMap<Long, String>
    ) {
        photos.forEach { photo ->
            val isVideo = photo.filePath.endsWith(".mp4", ignoreCase = true)
            val fileName = if (isVideo) "video_${photo.id}.mp4" else "photo_${photo.id}.jpg"
            val entryPath = "assets/$fileName"
            try {
                zos.putNextEntry(ZipEntry(entryPath))
                val success = writeMediaContent(context, zos, photo.filePath, isVideo, quality)
                zos.closeEntry()
                if (success) photoMap[photo.id] = entryPath
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e("HtmlExporter", "Media stream failed: ${photo.id}", e)
            }
        }
    }

    private fun writeMediaContent(context: Context, zos: ZipOutputStream, path: String, isVideo: Boolean, quality: Int): Boolean {
        return if (isVideo || quality == 100) {
            ImageProcessor.openInputStreamSafe(context, path)?.use { it.copyTo(zos, 8192); true } ?: false
        } else {
            ImageProcessor.compressToStream(context, path, zos, quality) ||
                    ImageProcessor.openInputStreamSafe(context, path)?.use { it.copyTo(zos, 8192); true } ?: false
        }
    }

    private fun generateHtmlContent(
        project: ProjectEntity,
        logs: List<DailyLogEntity>,
        photos: List<PhotoEntity>,
        photoMap: Map<Long, String>,
        logoPath: String?,
        language: String,
    ): String {
        val locale = if (language == "en") Locale.US else Locale.forLanguageTag("tr-TR")
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", locale)
        val dateStr = dateFormat.format(Date())

        return buildString {
            append(generateHtmlHead(project.name, language))
            append("<body><div class=\"container\">")
            append(generateHtmlHeader(project.name, dateStr, logoPath, language))
            append("<div class=\"timeline\">")

            logs.sortedByDescending { it.date }.forEach { log ->
                val dayPhotos = photos.filter { it.logId == log.id }
                if (log.note.isNotBlank() || dayPhotos.isNotEmpty()) {
                    append(generateDayCard(log, dayPhotos, photoMap, dateFormat, language))
                }
            }

            append("</div></div></body></html>")
        }
    }

    private fun generateHtmlHead(title: String, lang: String): String = """
        <!DOCTYPE html>
        <html lang="$lang">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>$title - ${if (lang == "en") "Report" else "Rapor"}</title>
            <link rel="preconnect" href="https://fonts.googleapis.com">
            <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
            <link href="https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@600;700&family=Manrope:wght@400;500;600;700&display=swap" rel="stylesheet">
            <style>
                :root { --bg-color: #FAF9F6; --card-bg: #ffffff; --text-primary: #1C1915; --text-secondary: #5A5448; --primary: #2F386F; --accent: #A6712F; --border: #DDD9CF; --shadow: 0 8px 24px -8px rgba(21, 24, 47, 0.14); }
                @media (prefers-color-scheme: dark) { :root { --bg-color: #121009; --card-bg: #1C1915; --text-primary: #F5F3EE; --text-secondary: #C4BFB2; --primary: #8B95D8; --accent: #D2A25C; --border: #423D34; --shadow: 0 8px 24px -8px rgba(0, 0, 0, 0.45); } }
                body { font-family: 'Manrope', sans-serif; background-color: var(--bg-color); color: var(--text-primary); line-height: 1.6; margin: 0; padding: 0; }
                .container { max-width: 800px; margin: 0 auto; padding: 2rem 1rem; }
                .header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 3rem; background: var(--card-bg); padding: 2rem; border-radius: 16px; border-top: 4px solid var(--accent); box-shadow: var(--shadow); }
                .header-kicker { display: inline-block; font-size: 0.72rem; font-weight: 700; letter-spacing: 0.12em; text-transform: uppercase; color: var(--accent); margin-bottom: 0.6rem; }
                .header h1 { font-size: 2.3rem; font-weight: 700; margin: 0 0 0.5rem 0; color: var(--primary); }
                .company-logo { max-width: 120px; max-height: 120px; object-fit: contain; border-radius: 12px; margin-left: 2rem; }
                .day-card { background: var(--card-bg); border: 1px solid var(--border); border-radius: 12px; padding: 1.5rem; margin-bottom: 2rem; box-shadow: var(--shadow); }
                .day-header { border-bottom: 1px solid var(--border); margin-bottom: 1rem; padding-bottom: 1rem; }
                .day-title { font-size: 1.25rem; font-weight: 600; margin: 0; color: var(--primary); }
                .note-content { white-space: pre-wrap; margin-bottom: 1.5rem; }
                .media-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 1rem; }
                .media-item { border-radius: 8px; overflow: hidden; background: #000; aspect-ratio: 3/4; position: relative; }
                .media-item img, .media-item video { width: 100%; height: 100%; object-fit: contain; }
            </style>
        </head>
    """.trimIndent()

    private fun generateHtmlHeader(name: String, date: String, logo: String?, lang: String): String {
        val kicker = if (lang == "en") "Field Inspection Report" else "Saha Denetim Raporu"
        val label = if (lang == "en") "Report Date" else "Rapor Tarihi"
        val logoAlt = if (lang == "en") "Logo" else "Logosu"
        return """
            <div class="header">
                <div class="header-content">
                    <span class="header-kicker">$kicker</span>
                    <h1>$name</h1>
                    <p>$label: $date</p>
                </div>
                ${if (logo != null) "<img src=\"$logo\" class=\"company-logo\" alt=\"$logoAlt\">" else ""}
            </div>
        """.trimIndent()
    }

    private fun generateDayCard(log: DailyLogEntity, photos: List<PhotoEntity>, map: Map<Long, String>, df: SimpleDateFormat, lang: String): String {
        return buildString {
            append("<div class=\"day-card\"><div class=\"day-header\"><h2 class=\"day-title\">${df.format(Date(log.date))}</h2></div>")
            if (log.note.isNotBlank()) append("<div class=\"note-content\">${log.note.htmlEncode()}</div>")
            if (photos.isNotEmpty()) {
                append("<div class=\"media-grid\">")
                photos.forEach { photo -> map[photo.id]?.let { append(generateMediaItem(it, photo.rotation, lang)) } }
                append(DIV_CLOSE)
            }
            append(DIV_CLOSE)
        }
    }

    private fun generateMediaItem(path: String, rotation: Float, lang: String): String {
        val isVideo = path.endsWith(".mp4", ignoreCase = true)
        val videoMsg = if (lang == "en") "Video not supported" else "Video desteklenmiyor"
        val photoAlt = if (lang == "en") "Photo" else "Fotoğraf"
        return buildString {
            append("<div class=\"media-item\">")
            if (isVideo) {
                append("<video controls preload=\"metadata\"><source src=\"$path\" type=\"video/mp4\">$videoMsg</video>")
            } else {
                val style = if (rotation != 0f) "style=\"transform: rotate(${rotation}deg);\"" else ""
                append("<a href=\"$path\" target=\"_blank\"><img src=\"$path\" alt=\"$photoAlt\" loading=\"lazy\" $style></a>")
            }
            append(DIV_CLOSE)
        }
    }
}
