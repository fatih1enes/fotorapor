package com.elektrik.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri

import android.os.Environment
import android.provider.MediaStore
import androidx.camera.core.ImageCapture
import androidx.camera.video.MediaStoreOutputOptions
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
object PhotoManager {
    
    fun getCaptureOutputOptions(context: Context): ImageCapture.OutputFileOptions {
        val name = "IMG_${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/Elektrik")
        }
        val metadata = ImageCapture.Metadata()
        return ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).setMetadata(metadata).build()
    }

    fun getVideoOutputOptions(context: Context): MediaStoreOutputOptions {
        val name = "VID_${System.currentTimeMillis()}.mp4"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/Elektrik")
        }
        return MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()
    }

    fun createPhotoUri(context: Context): Uri {
        val file = File(context.filesDir, "IMG_${System.currentTimeMillis()}.jpg")
        return Uri.fromFile(file)
    }

    fun copyUriToFile(context: Context, uri: Uri): String? {
        return try {
            val isVideo = context.contentResolver.getType(uri)?.contains("video") == true
            val extension = if (isVideo) ".mp4" else ".jpg"
            val prefix = if (isVideo) "VID_" else "IMG_"
            val destFile = File(context.filesDir, "${prefix}${System.currentTimeMillis()}$extension")
            
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun copyUriToInternalStorage(context: Context, uri: Uri): Uri? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val extension = when {
                context.contentResolver.getType(uri)?.contains("video") == true -> "mp4"
                else -> "jpg"
            }
            val file = File(
                context.filesDir, 
                "imported_${System.currentTimeMillis()}_${UUID.randomUUID()}.$extension"
            )
            inputStream.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(file)
        } catch (e: Exception) {
            android.util.Log.e("PhotoManager", "Dosya kopyalama hatasÃƒâ€žÃ‚Â±: $uri", e)
            null
        }
    }
}
