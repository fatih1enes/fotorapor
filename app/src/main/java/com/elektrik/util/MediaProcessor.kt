package com.elektrik.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaProcessor @Inject constructor(
    @get:ApplicationContext private val appContext: Context,
) {
    suspend fun processAndConvertToWebpIfNeeded(
        originalUri: Uri,
        enableWebp: Boolean,
        projectName: String
    ): Uri = withContext(Dispatchers.IO) {
        if (!enableWebp || (Build.VERSION.SDK_INT < Build.VERSION_CODES.R)) {
            return@withContext originalUri
        }

        val bitmapToCompress = ImageUtils.loadScaledBitmap(appContext, originalUri.toString(), 4000, 4000)
            ?: return@withContext originalUri

        try {
            var orientation = 0
            var dateTime = ""
            appContext.contentResolver.openInputStream(originalUri)?.use { input ->
                val oldExif = ExifInterface(input)
                orientation = oldExif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)
                dateTime = oldExif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) ?: ""
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.webp")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/webp")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/Elektrik")
            }

            val webpUri = appContext.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (webpUri != null) {
                appContext.contentResolver.openOutputStream(webpUri)?.use { out ->
                    bitmapToCompress.compress(Bitmap.CompressFormat.WEBP_LOSSY, 100, out)
                }

                appContext.contentResolver.openFileDescriptor(webpUri, "rw")?.use { rwPfd ->
                    val newExif = ExifInterface(rwPfd.fileDescriptor)
                    newExif.setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
                    if (dateTime.isNotEmpty()) {
                        newExif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, dateTime)
                    }
                    if (projectName.isNotBlank()) {
                        newExif.setAttribute(ExifInterface.TAG_USER_COMMENT, projectName)
                    }
                    newExif.saveAttributes()
                }
                appContext.contentResolver.delete(originalUri, null, null)
                return@withContext webpUri
            }
        } finally {
            bitmapToCompress.recycle()
        }

        return@withContext originalUri
    }
}
