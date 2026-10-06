package com.fatihenes.photoreport.core.media

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import com.fatihenes.photoreport.core.common.di.Dispatcher
import com.fatihenes.photoreport.core.common.di.FotoRaporDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import com.radzivon.bartoshyk.avif.coder.HeifCoder

@Singleton
class MediaProcessor @Inject constructor(
    @ApplicationContext private val appContext: Context,
    @Dispatcher(FotoRaporDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun processAndOptimize(
        originalUri: Uri,
        enableAvif: Boolean,
        projectName: String
    ): Uri = withContext(ioDispatcher) {
        if (enableAvif) {
            optimize(originalUri, projectName) ?: originalUri
        } else {
            originalUri
        }
    }

    private fun optimize(originalUri: Uri, projectName: String): Uri? {
        val uriString = originalUri.toString()
        val maxDimension = calculateMaxDimension(appContext, uriString)
        val bitmap = ImageScaler.loadScaledBitmap(appContext, uriString, maxDimension, maxDimension) ?: return null
        return try {
            val oldExif = extractOriginalExif(originalUri)
            val optimizedUri = saveOptimizedImage(bitmap, oldExif, projectName)
            if (optimizedUri != null && verifyImageWritten(optimizedUri)) {
                appContext.contentResolver.delete(originalUri, null, null)
                optimizedUri
            } else {
                android.util.Log.w("MediaProcessor", "AVIF file verification failed, keeping original photo")
                if (optimizedUri != null) {
                    try {
                        appContext.contentResolver.delete(optimizedUri, null, null)
                    } catch (_: Exception) { }
                }
                originalUri
            }
        } catch (e: Exception) {
            // Only recycle on failure; on success, the bitmap is consumed by the encoder
            // and will be GC'd naturally. Recycling too early can cause native crashes.
            bitmap.recycle()
            throw e
        }
    }

    private fun verifyImageWritten(uri: Uri): Boolean {
        return try {
            appContext.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                pfd.statSize > 0L
            } ?: false
        } catch (e: Exception) {
            android.util.Log.w("MediaProcessor", "Failed to verify written image: $uri", e)
            false
        }
    }

    private fun calculateMaxDimension(context: Context, pathString: String): Int {
        return try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val stream = context.contentResolver.openInputStream(pathString.toUri()) ?: return 2560
            stream.use { BitmapFactory.decodeStream(it, null, boundsOptions) }
            val maxPixels = 2560 * 2560 // ~6.5MP max
            val width = boundsOptions.outWidth
            val height = boundsOptions.outHeight
            if (width <= 0 || height <= 0) return 2560
            val currentPixels = width * height
            if (currentPixels <= maxPixels) return 2560
            // Calculate scale factor to reduce to maxPixels
            val scale = Math.sqrt(maxPixels.toDouble() / currentPixels)
            (2560 * scale).toInt().coerceAtLeast(1024).coerceAtMost(2560)
        } catch (e: Exception) {
            2560
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun extractOriginalExif(uri: Uri): ExifInterface? {
        return try {
            appContext.contentResolver.openInputStream(uri)?.use { ExifInterface(it) }
        } catch (e: Exception) {
            android.util.Log.w("MediaProcessor", "Exif read failed", e)
            null
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun saveOptimizedImage(bitmap: Bitmap, oldExif: ExifInterface?, projectName: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.avif")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/avif")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/PhotoReport")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val uri = appContext.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null

        return try {
            val avifBytes = HeifCoder().encodeAvif(bitmap)
            appContext.contentResolver.openOutputStream(uri)?.use { it.write(avifBytes) }

            updateExif(uri, oldExif, projectName)

            val pendingValues = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            appContext.contentResolver.update(uri, pendingValues, null, null)
            uri
        } catch (e: Exception) {
            @Suppress("TooGenericExceptionCaught")
            appContext.contentResolver.delete(uri, null, null)
            android.util.Log.e("MediaProcessor", "AVIF encoding failed", e)
            null
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun updateExif(uri: Uri, oldExif: ExifInterface?, projectName: String) {
        try {
            appContext.contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                val newExif = ExifInterface(pfd.fileDescriptor)
                oldExif?.let { copyTags(it, newExif) }
                newExif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                if (projectName.isNotBlank()) {
                    newExif.setAttribute(ExifInterface.TAG_USER_COMMENT, projectName)
                }
                newExif.saveAttributes()
            }
        } catch (e: Exception) {
            android.util.Log.w("MediaProcessor", "Exif update failed", e)
        }
    }

    private fun copyTags(old: ExifInterface, new: ExifInterface) {
        val tags = listOf(
            ExifInterface.TAG_DATETIME_ORIGINAL,
            ExifInterface.TAG_DATETIME,
            ExifInterface.TAG_DATETIME_DIGITIZED,
            ExifInterface.TAG_MAKE,
            ExifInterface.TAG_MODEL,
            ExifInterface.TAG_FOCAL_LENGTH,
            ExifInterface.TAG_GPS_LATITUDE,
            ExifInterface.TAG_GPS_LONGITUDE,
            ExifInterface.TAG_GPS_LATITUDE_REF,
            ExifInterface.TAG_GPS_LONGITUDE_REF
        )
        for (tag in tags) {
            old.getAttribute(tag)?.let { new.setAttribute(tag, it) }
        }
    }
}
