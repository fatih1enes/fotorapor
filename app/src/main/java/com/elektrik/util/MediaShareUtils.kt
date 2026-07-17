package com.sarikaya.santiye.gunlugu.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.sarikaya.santiye.gunlugu.R
import java.io.File

object MediaShareUtils {

    fun shareSingleMedia(context: Context, filePath: String) {
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
            Toast.makeText(context, context.getString(R.string.share_failed), Toast.LENGTH_SHORT).show()
        }
    }

    fun shareMultipleMedia(context: Context, filePaths: List<String>) {
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
                Toast.makeText(context, context.getString(R.string.share_no_files), Toast.LENGTH_SHORT).show()
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
            Toast.makeText(context, context.getString(R.string.share_failed), Toast.LENGTH_SHORT).show()
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
