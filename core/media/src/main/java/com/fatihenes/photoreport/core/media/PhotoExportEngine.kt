@file:Suppress("MagicNumber", "TooGenericExceptionCaught", "LongMethod")
package com.fatihenes.photoreport.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.util.Log
import com.fatihenes.photoreport.core.model.PhotoAdjustments
import com.fatihenes.photoreport.core.model.PhotoCropState
import com.fatihenes.photoreport.core.model.PhotoEditorSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class ExportResolution(val maxDimensionPx: Int) {
    ORIGINAL(0),
    UHD_4K(3840),
    QHD_2K(2560),
    FHD_1080P(1920)
}

enum class ExportFormat(val extension: String, val compressFormat: Bitmap.CompressFormat) {
    JPEG("jpg", Bitmap.CompressFormat.JPEG),
    PNG("png", Bitmap.CompressFormat.PNG),
    WEBP("webp", if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        Bitmap.CompressFormat.WEBP_LOSSY
    } else {
        @Suppress("DEPRECATION")
        Bitmap.CompressFormat.WEBP
    })
}

data class ExportOptions(
    val format: ExportFormat = ExportFormat.JPEG,
    val resolution: ExportResolution = ExportResolution.ORIGINAL,
    val quality: Int = 95,
    val preserveExif: Boolean = true,
    val stripGpsLocation: Boolean = false
)

data class PhotoExportResult(
    val success: Boolean,
    val outputFilePath: String,
    val errorMessage: String? = null
)

object PhotoExportEngine {

    private const val TAG = "PhotoExportEngine"

    suspend fun exportPhoto(
        context: Context,
        photoId: Long,
        sourceFilePath: String,
        session: PhotoEditorSession,
        options: ExportOptions = ExportOptions()
    ): PhotoExportResult = withContext(Dispatchers.IO) {
        var rawBitmap: Bitmap? = null
        var orientedBitmap: Bitmap? = null
        var croppedBitmap: Bitmap? = null
        var adjustedBitmap: Bitmap? = null
        var finalAnnotatedBitmap: Bitmap? = null
        var scaledBitmap: Bitmap? = null
        var tempFile: File? = null

        try {
            // 1. Decode Raw Bitmap
            rawBitmap = ImageProcessor.openInputStreamSafe(context, sourceFilePath)?.use {
                BitmapFactory.decodeStream(it)
            } ?: return@withContext PhotoExportResult(false, sourceFilePath, "Cannot decode source photo")

            // 2. Rotate to correct EXIF orientation
            val exifRotation = ImageProcessor.getExifRotation(context, sourceFilePath)
            orientedBitmap = ImageRotation.rotateIfNeeded(rawBitmap, exifRotation)

            // 3. Apply Crop & Transforms
            croppedBitmap = PhotoCropRenderer.applyCropAndTransform(orientedBitmap, session.cropState)

            // 4. Apply Adjustments (ColorMatrix, Vignette, etc.)
            adjustedBitmap = if (croppedBitmap.isMutable) croppedBitmap else croppedBitmap.copy(Bitmap.Config.ARGB_8888, true)
            PhotoAdjustEngine.applyAdjustmentsToBitmap(adjustedBitmap, session.adjustments)

            // 5. Apply Vector Markup & Destructive Redactions
            finalAnnotatedBitmap = PhotoMarkupRenderer.applyMarkupsToBitmap(adjustedBitmap, session.items)

            // 6. Scale to Target Resolution if requested
            scaledBitmap = if (options.resolution.maxDimensionPx > 0) {
                val curMax = maxOf(finalAnnotatedBitmap.width, finalAnnotatedBitmap.height)
                if (curMax > options.resolution.maxDimensionPx) {
                    val scale = options.resolution.maxDimensionPx.toFloat() / curMax.toFloat()
                    val targetW = (finalAnnotatedBitmap.width * scale).toInt().coerceAtLeast(1)
                    val targetH = (finalAnnotatedBitmap.height * scale).toInt().coerceAtLeast(1)
                    Bitmap.createScaledBitmap(finalAnnotatedBitmap, targetW, targetH, true)
                } else {
                    finalAnnotatedBitmap
                }
            } else {
                finalAnnotatedBitmap
            }

            // 7. Atomic Write to Temporary File
            val photosDir = File(context.filesDir, "photos").apply { if (!exists()) mkdirs() }
            val timestamp = System.currentTimeMillis()
            val unique = java.util.UUID.randomUUID().toString().take(8)
            tempFile = File(photosDir, "temp_export_${photoId}_${timestamp}_$unique.${options.format.extension}")
            val destFile = File(photosDir, "annotated_${photoId}_${timestamp}_$unique.${options.format.extension}")

            val compressOk = FileOutputStream(tempFile).use { out ->
                val ok = scaledBitmap.compress(options.format.compressFormat, options.quality.coerceIn(1, 100), out)
                out.flush()
                ok
            }
            if (!compressOk) {
                throw java.io.IOException("Bitmap compress failed for photo $photoId")
            }

            // 8. Copy EXIF metadata
            if (options.preserveExif && (options.format == ExportFormat.JPEG || options.format == ExportFormat.WEBP)) {
                copyExifMetadata(context, sourceFilePath, tempFile.absolutePath, options.stripGpsLocation)
            }

            // 9. Atomic Rename
            if (!tempFile.renameTo(destFile)) {
                // Fallback copy if atomic rename fails across mounts
                tempFile.copyTo(destFile, overwrite = true)
                tempFile.delete()
            }

            // 10. Clean up older annotated versions of this photo
            cleanupOldAnnotatedFiles(photosDir, photoId, destFile.name)

            PhotoExportResult(success = true, outputFilePath = destFile.absolutePath)
        } catch (e: kotlinx.coroutines.CancellationException) {
            tempFile?.delete()
            throw e
        } catch (e: Throwable) {
            // OOM dahil: crash yerine failure dön, temp temizle. Davranış korunur.
            Log.e(TAG, "Export failed for photo $photoId", e)
            tempFile?.delete()
            PhotoExportResult(false, sourceFilePath, e.localizedMessage)
        } finally {
            if (rawBitmap !== orientedBitmap) rawBitmap?.recycle()
            if (orientedBitmap !== croppedBitmap) orientedBitmap?.recycle()
            if (croppedBitmap !== adjustedBitmap) croppedBitmap?.recycle()
            if (adjustedBitmap !== finalAnnotatedBitmap) adjustedBitmap?.recycle()
            if (finalAnnotatedBitmap !== scaledBitmap) finalAnnotatedBitmap?.recycle()
            scaledBitmap?.recycle()
        }
    }

    private fun copyExifMetadata(
        context: Context,
        srcPath: String,
        destPath: String,
        stripGps: Boolean
    ) {
        try {
            val srcExif = (if (srcPath.startsWith("content://")) {
                context.contentResolver.openInputStream(android.net.Uri.parse(srcPath))?.use {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        ExifInterface(it)
                    } else null
                }
            } else if (srcPath.startsWith("file://")) {
                val path = android.net.Uri.parse(srcPath).path ?: srcPath.removePrefix("file://")
                ExifInterface(path)
            } else {
                ExifInterface(srcPath)
            }) ?: return

            val destExif = ExifInterface(destPath)

            val tagsToCopy = arrayOf(
                ExifInterface.TAG_DATETIME,
                ExifInterface.TAG_DATETIME_ORIGINAL,
                ExifInterface.TAG_DATETIME_DIGITIZED,
                ExifInterface.TAG_MAKE,
                ExifInterface.TAG_MODEL,
                ExifInterface.TAG_FLASH,
                ExifInterface.TAG_WHITE_BALANCE,
                ExifInterface.TAG_EXPOSURE_TIME,
                ExifInterface.TAG_FOCAL_LENGTH,
                ExifInterface.TAG_ISO_SPEED_RATINGS
            )

            for (tag in tagsToCopy) {
                srcExif.getAttribute(tag)?.let { value ->
                    destExif.setAttribute(tag, value)
                }
            }

            if (!stripGps) {
                val gpsTags = arrayOf(
                    ExifInterface.TAG_GPS_LATITUDE,
                    ExifInterface.TAG_GPS_LATITUDE_REF,
                    ExifInterface.TAG_GPS_LONGITUDE,
                    ExifInterface.TAG_GPS_LONGITUDE_REF,
                    ExifInterface.TAG_GPS_ALTITUDE,
                    ExifInterface.TAG_GPS_ALTITUDE_REF,
                    ExifInterface.TAG_GPS_TIMESTAMP,
                    ExifInterface.TAG_GPS_DATESTAMP
                )
                for (tag in gpsTags) {
                    srcExif.getAttribute(tag)?.let { value ->
                        destExif.setAttribute(tag, value)
                    }
                }
            }

            // Reset orientation to Normal (1) since bitmap was rotated before export
            destExif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
            destExif.saveAttributes()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to copy EXIF metadata", e)
        }
    }

    private fun cleanupOldAnnotatedFiles(photosDir: File, photoId: Long, currentFileName: String) {
        try {
            val prefix = "annotated_${photoId}_"
            photosDir.listFiles { _, name ->
                name.startsWith(prefix) && name != currentFileName
            }?.forEach { oldFile ->
                oldFile.delete()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cleanup old annotated files for photo $photoId", e)
        }
    }
}
