package com.fatihenes.photoreport.core.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.fatihenes.photoreport.core.ui.R
import java.io.File

object MediaShareUtils {

    fun shareSingleMedia(context: Context, filePath: String, onShowMessage: (String) -> Unit) {
        try {
            val (uri, extension) = resolveMediaUri(context, filePath)

            val mimeType = when (extension.lowercase()) {
                "mp4", "mov" -> "video/mp4"
                "png" -> "image/png"
                "webp", "avif" -> "image/*"
                else -> "image/jpeg"
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, context.getString(R.string.share_media))
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            android.util.Log.e("MediaShareUtils", "Could not share media", e)
            onShowMessage(context.getString(R.string.share_failed))
        }
    }

    fun shareMultipleMedia(context: Context, filePaths: List<String>, onShowMessage: (String) -> Unit) {
        try {
            val uris = ArrayList<Uri>()

            for (filePath in filePaths) {
                try {
                    val (uri, _) = resolveMediaUri(context, filePath)
                    uris.add(uri)
                } catch (e: Exception) {
                    android.util.Log.w("MediaShareUtils", "Skipping invalid media path: $filePath", e)
                }
            }

            if (uris.isEmpty()) {
                onShowMessage(context.getString(R.string.share_no_files))
                return
            }

            val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, context.getString(R.string.share_selected))
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            android.util.Log.e("MediaShareUtils", "Could not share media", e)
            onShowMessage(context.getString(R.string.share_failed))
        }
    }

    fun resolveMediaUri(context: Context, filePath: String): Pair<Uri, String> {
        return when {
            filePath.startsWith("content://") -> {
                val uri = filePath.toUri()
                val extension = filePath.substringAfterLast(".", "jpg")
                Pair(uri, extension)
            }
            filePath.startsWith("file://") -> {
                val file = File(filePath.toUri().path ?: "")
                validateFilePath(context, file)
                val authority = "${context.packageName}.fileprovider"
                val uri = FileProvider.getUriForFile(context, authority, file)
                Pair(uri, file.extension)
            }
            else -> {
                val file = File(filePath)
                validateFilePath(context, file)
                val authority = "${context.packageName}.fileprovider"
                val uri = FileProvider.getUriForFile(context, authority, file)
                Pair(uri, file.extension)
            }
        }
    }

    private fun validateFilePath(context: Context, file: File) {
        val canonicalPath = file.canonicalPath
        val isAllowed = (canonicalPath.startsWith(context.filesDir.canonicalPath) ||
                canonicalPath.startsWith(context.cacheDir.canonicalPath) ||
                context.externalCacheDir?.let { canonicalPath.startsWith(it.canonicalPath) } == true ||
                context.getExternalFilesDir(null)?.let { canonicalPath.startsWith(it.canonicalPath) } == true)

        if (!isAllowed) {
            throw SecurityException("Invalid file path: path traversal detected")
        }
        if (!file.exists()) {
            throw java.io.FileNotFoundException("File does not exist: $canonicalPath")
        }
    }
}
