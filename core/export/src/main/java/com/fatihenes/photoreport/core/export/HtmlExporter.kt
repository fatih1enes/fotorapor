@file:Suppress("MaxLineLength")
package com.fatihenes.photoreport.core.export

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.text.htmlEncode
import com.fatihenes.photoreport.core.common.util.FileNameUtils
import com.fatihenes.photoreport.core.database.DailyLogEntity
import com.fatihenes.photoreport.core.database.PhotoEntity
import com.fatihenes.photoreport.core.database.ProjectEntity
import com.fatihenes.photoreport.core.media.CompanyLogoManager
import com.fatihenes.photoreport.core.media.ImageProcessor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object HtmlExporter {

    private const val TAG = "HtmlExporter"
    private const val DIV_CLOSE = "</div>"
    private const val LOGO_ASSET_PATH = "assets/company_logo.png"
    private const val COPY_BUFFER_SIZE = 8192
    private const val QUALITY_LOSSLESS = 100
    private const val LOGO_MAX_DIMENSION = 500

    data class ExportParams(
        val context: Context,
        val project: ProjectEntity,
        val logs: List<DailyLogEntity>,
        val photos: List<PhotoEntity>,
        val quality: Int = 100,
        val language: String = "tr",
        val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    )

    suspend fun exportToHtmlZip(params: ExportParams): Uri? = withContext(params.dispatcher) {
        try {
            val sanitizedName = FileNameUtils.sanitize(params.project.name, "proje")
            val zipPrefix = if (params.language == "en") "Report" else "Rapor"
            val zipFile = File(params.context.cacheDir, "${zipPrefix}_$sanitizedName.zip")
            if (zipFile.exists() && !zipFile.delete()) {
                Log.w("HtmlExporter", "Failed to delete existing zip file: ${zipFile.name}")
            }

            val photoMap = mutableMapOf<Long, String>()
            val fos = FileOutputStream(zipFile)
            ZipOutputStream(fos).use { zos ->
                val logoPath = streamLogoToZip(params.context, zos)
                streamMediaToZip(
                    StreamMediaParams(params.context, zos, params.photos, params.quality, photoMap),
                )

                val html = generateHtmlContent(
                    HtmlContentParams(params.project, params.logs, params.photos, photoMap, logoPath, params.language),
                )
                zos.putNextEntry(ZipEntry("index.html"))
                zos.write(html.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            val authority = "${params.context.packageName}.fileprovider"
            return@withContext FileProvider.getUriForFile(params.context, authority, zipFile)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "Export failed (IO)", e)
            null
        } catch (e: SecurityException) {
            Log.e(TAG, "Export failed (Security)", e)
            null
        }
    }

    @Suppress("ReturnCount")
    private fun streamLogoToZip(context: Context, zos: ZipOutputStream): String? {
        if (!CompanyLogoManager.hasLogo(context)) return null
        val logoUri = CompanyLogoManager.getLogoUri(context) ?: return null
        return try {
            zos.putNextEntry(ZipEntry(LOGO_ASSET_PATH))
            val scaledBmp = ImageProcessor.loadScaledBitmap(
                context,
                logoUri.toString(),
                LOGO_MAX_DIMENSION,
                LOGO_MAX_DIMENSION,
            )
            if (scaledBmp != null) {
                scaledBmp.compress(Bitmap.CompressFormat.PNG, QUALITY_LOSSLESS, zos)
                scaledBmp.recycle()
            } else {
                ImageProcessor.openInputStreamSafe(context, logoUri.toString())?.use {
                    it.copyTo(zos, COPY_BUFFER_SIZE)
                }
            }
            zos.closeEntry()
            LOGO_ASSET_PATH
        } catch (e: java.io.IOException) {
            Log.e(TAG, "Logo stream failed", e)
            null
        }
    }

    private data class StreamMediaParams(
        val context: Context,
        val zos: ZipOutputStream,
        val photos: List<PhotoEntity>,
        val quality: Int,
        val photoMap: MutableMap<Long, String>,
    )

    private fun streamMediaToZip(params: StreamMediaParams) {
        params.photos.forEach { photo ->
            val isVideo = photo.filePath.endsWith(".mp4", ignoreCase = true)
            val fileName = if (isVideo) "video_${photo.id}.mp4" else "photo_${photo.id}.jpg"
            val entryPath = "assets/$fileName"
            try {
                params.zos.putNextEntry(ZipEntry(entryPath))
                val success = writeMediaContent(
                    WriteMediaParams(params.context, params.zos, photo.filePath, isVideo, params.quality),
                )
                params.zos.closeEntry()
                if (success) params.photoMap[photo.id] = entryPath
            } catch (e: java.io.IOException) {
                Log.e(TAG, "Media stream failed: ${photo.id}", e)
            }
        }
    }

    private data class WriteMediaParams(
        val context: Context,
        val zos: ZipOutputStream,
        val path: String,
        val isVideo: Boolean,
        val quality: Int,
    )

    private fun writeMediaContent(params: WriteMediaParams): Boolean {
        val isLossless = params.quality == QUALITY_LOSSLESS
        return if (params.isVideo || isLossless) {
            ImageProcessor.openInputStreamSafe(params.context, params.path)?.use {
                it.copyTo(params.zos, COPY_BUFFER_SIZE)
                true
            } ?: false
        } else {
            val res = ImageProcessor.compressToStream(params.context, params.path, params.zos, params.quality)
            res || (
                ImageProcessor.openInputStreamSafe(params.context, params.path)?.use {
                    it.copyTo(params.zos, COPY_BUFFER_SIZE)
                    true
                } ?: false
            )
        }
    }

    private data class HtmlContentParams(val project: ProjectEntity, val logs: List<DailyLogEntity>, val photos: List<PhotoEntity>, val photoMap: Map<Long, String>, val logoPath: String?, val language: String)

    private fun generateHtmlContent(p: HtmlContentParams): String {
        val locale = if (p.language == "en") Locale.US else Locale.forLanguageTag("tr-TR")
        val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", locale).withZone(ZoneId.systemDefault())
        val dateStr = formatter.format(Instant.now())
        return buildString {
            append(generateHtmlHead(p.project.name, p.language))
            append("<body><div class=\"container\">")
            append(generateHtmlHeader(p.project.name, dateStr, p.logoPath, p.language))
            append("<div class=\"timeline\">")
            p.logs.sortedByDescending { it.date }.forEach { log ->
                val dayPhotos = p.photos.filter { it.logId == log.id }
                if (log.note.isNotBlank() || dayPhotos.isNotEmpty()) append(generateDayCard(log, dayPhotos, p.photoMap, formatter, p.language))
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

    private fun generateDayCard(log: DailyLogEntity, photos: List<PhotoEntity>, map: Map<Long, String>, formatter: DateTimeFormatter, lang: String): String {
        return buildString {
            append("<div class=\"day-card\">")
            append("<div class=\"day-header\">")
            append("<h2 class=\"day-title\">${formatter.format(Instant.ofEpochMilli(log.date))}</h2>")
            append("</div>")
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
                append("<video controls preload=\"metadata\">")
                append("<source src=\"$path\" type=\"video/mp4\">")
                append("$videoMsg</video>")
            } else {
                val style = if (rotation != 0f) "style=\"transform: rotate(${rotation}deg);\"" else ""
                append("<a href=\"$path\" target=\"_blank\">")
                append("<img src=\"$path\" alt=\"$photoAlt\" loading=\"lazy\" $style>")
                append("</a>")
            }
            append(DIV_CLOSE)
        }
    }
}
