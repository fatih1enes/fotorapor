package com.fatihenes.photoreport.util

import android.content.ContentValues
import android.content.Context
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
        if (!enableAvif) {
            return@withContext originalUri
        }

        val bitmapToCompress = ImageUtils.loadScaledBitmap(appContext, originalUri.toString(), 4000, 4000)
            ?: return@withContext originalUri

        try {
            var oldExif: ExifInterface? = null
            appContext.contentResolver.openInputStream(originalUri)?.use { input ->
                oldExif = ExifInterface(input)
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.avif")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/avif")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/PhotoReport")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val optimizedUri = appContext.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (optimizedUri != null) {
                try {
                    // Encode to AVIF using avif-coder
                    val coder = HeifCoder()
                    val avifBytes = coder.encodeAvif(bitmapToCompress)

                    appContext.contentResolver.openOutputStream(optimizedUri)?.use { out ->
                        out.write(avifBytes)
                    }

                    // Copy all EXIF data
                    appContext.contentResolver.openFileDescriptor(optimizedUri, "rw")?.use { rwPfd ->
                        val newExif = ExifInterface(rwPfd.fileDescriptor)

                        oldExif?.let { old ->
                            // Copy essential tags
                            val tagsToCopy = listOf(
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
                            for (tag in tagsToCopy) {
                                old.getAttribute(tag)?.let { value ->
                                    newExif.setAttribute(tag, value)
                                }
                            }
                        }

                        if (projectName.isNotBlank()) {
                            newExif.setAttribute(ExifInterface.TAG_USER_COMMENT, projectName)
                        }
                        newExif.saveAttributes()
                    }

                    val pendingValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.IS_PENDING, 0)
                    }
                    appContext.contentResolver.update(optimizedUri, pendingValues, null, null)

                    // Delete original ONLY if optimization was successful
                    appContext.contentResolver.delete(originalUri, null, null)
                    return@withContext optimizedUri
                } catch (e: Throwable) {
                    // If encoding fails, delete the empty optimized file and keep original
                    appContext.contentResolver.delete(optimizedUri, null, null)
                    android.util.Log.e("MediaProcessor", "AVIF encoding failed", e)
                }
            }
        } finally {
            bitmapToCompress.recycle()
        }

        return@withContext originalUri
    }
}
