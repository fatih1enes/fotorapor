package com.fatihenes.photoreport.core.media

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import com.radzivon.bartoshyk.avif.coder.HeifCoder

@Singleton
class MediaProcessor @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {
    suspend fun processAndOptimize(
        originalUri: Uri,
        enableAvif: Boolean,
        projectName: String
    ): Uri = withContext(Dispatchers.IO) {
        if (enableAvif) {
            optimize(originalUri, projectName) ?: originalUri
        } else {
            originalUri
        }
    }

    private fun optimize(originalUri: Uri, projectName: String): Uri? {
        val bitmap = ImageScaler.loadScaledBitmap(appContext, originalUri.toString(), 2560, 2560) ?: return null
        return try {
            val oldExif = extractOriginalExif(originalUri)
            val optimizedUri = saveOptimizedImage(bitmap, oldExif, projectName)
            optimizedUri?.also { appContext.contentResolver.delete(originalUri, null, null) }
        } finally {
            bitmap.recycle()
        }
    }

    private fun extractOriginalExif(uri: Uri): ExifInterface? {
        return try {
            appContext.contentResolver.openInputStream(uri)?.use { ExifInterface(it) }
        } catch (e: Exception) {
            android.util.Log.w("MediaProcessor", "Exif read failed", e)
            null
        }
    }

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
            appContext.contentResolver.delete(uri, null, null)
            android.util.Log.e("MediaProcessor", "AVIF encoding failed", e)
            null
        }
    }

    private fun updateExif(uri: Uri, oldExif: ExifInterface?, projectName: String) {
        try {
            appContext.contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                val newExif = ExifInterface(pfd.fileDescriptor)
                oldExif?.let { copyTags(it, newExif) }
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
            ExifInterface.TAG_ORIENTATION,
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
