package com.fatihenes.photoreport.core.media

import android.content.Context
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface

object MetadataReader {
    fun getExifRotation(context: Context, pathString: String): Float {
        if (pathString.endsWith(".mp4", ignoreCase = true)) return 0f
        return try {
            val uri = pathString.toUri()
            val exif = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ExifInterface(inputStream)
            } ?: return 0f
            parseExifOrientation(exif)
        } catch (_: Exception) {
            try {
                val exif = ExifInterface(pathString)
                parseExifOrientation(exif)
            } catch (_: Exception) {
                0f
            }
        }
    }

    private fun parseExifOrientation(exif: ExifInterface): Float {
        return when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    }
}
