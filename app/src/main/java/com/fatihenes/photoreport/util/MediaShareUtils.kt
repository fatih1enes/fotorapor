package com.fatihenes.photoreport.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.fatihenes.photoreport.R
import java.io.File

object MediaShareUtils {

    fun shareSingleMedia(context: Context, filePath: String, onShowMessage: (String) -> Unit) {
        try {
            val resolvedFile = resolveFile(context, filePath)
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, resolvedFile)

            val mimeType = when (resolvedFile.extension.lowercase()) {
                "mp4" -> "video/mp4"
                "png" -> "image/png"
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
            onShowMessage(context.getString(R.string.share_failed))
        }
    }

    fun shareMultipleMedia(context: Context, filePaths: List<String>, onShowMessage: (String) -> Unit) {
        try {
            val uris = ArrayList<Uri>()
            val authority = "${context.packageName}.fileprovider"

            for (filePath in filePaths) {
                val resolvedFile = resolveFile(context, filePath)
                if (resolvedFile.exists()) {
                    val uri = FileProvider.getUriForFile(context, authority, resolvedFile)
                    uris.add(uri)
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
            onShowMessage(context.getString(R.string.share_failed))
        }
    }

    /**
     * Resolves a file path string (which may be a content:// URI, file:// URI, or absolute path)
     * to a java.io.File instance.
     */
    private fun resolveFile(context: Context, filePath: String): File {
        val file = when {
            filePath.startsWith("content://") -> {
                val filename = filePath.substringAfterLast("/")
                File(context.filesDir, filename)
            }
            filePath.startsWith("file://") -> {
                File(filePath.toUri().path ?: "")
            }
            else -> File(filePath)
        }

        val canonicalPath = file.canonicalPath
        if (!canonicalPath.startsWith(context.filesDir.canonicalPath) &&
            !canonicalPath.startsWith(context.cacheDir.canonicalPath)) {
            throw SecurityException("Invalid file path: path traversal detected")
        }

        return file
    }
}
