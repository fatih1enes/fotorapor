package com.fatihenes.photoreport.manager

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.fatihenes.photoreport.util.result.OperationResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles all low-level physical file and directory operations.
 * Separates file system I/O from the domain/repository layers.
 */
interface FileManager {

    /**
     * Safely deletes a physical file given its path or URI.
     *
     * @param filePath The string representation of the file path or content URI.
     * @return [OperationResult.Success] containing a boolean indicating if deletion was successful,
     *         or [OperationResult.Error] if an exception occurred.
     */
    suspend fun deletePhysicalFile(filePath: String): OperationResult<Boolean>

    /**
     * Copies a file from a source URI to a destination File.
     * Handles content:// and file:// URIs securely.
     *
     * @param sourceUri The source URI to read from.
     * @param destFile The destination file to write to.
     * @return [OperationResult.Success] if copy was successful, or [OperationResult.Error] otherwise.
     */
    suspend fun copyFile(sourceUri: Uri, destFile: File): OperationResult<Boolean>

    /**
     * Creates a temporary directory for export or backup operations.
     *
     * @param dirPrefix The prefix name of the directory.
     * @return [OperationResult.Success] containing the created [File] directory.
     */
    suspend fun createTempDirectory(dirPrefix: String): OperationResult<File>

    /**
     * Recursively deletes a directory and all of its contents.
     *
     * @param directory The root directory to delete.
     * @return [OperationResult.Success] if fully deleted, or [OperationResult.Error].
     */
    suspend fun deleteDirectoryRecursively(directory: File): OperationResult<Boolean>
}

/**
 * Android-specific implementation of [FileManager] utilizing [Context.getContentResolver].
 */
@Singleton
class LocalFileManager @Inject constructor(
    @ApplicationContext private val context: Context
) : FileManager {

    override suspend fun deletePhysicalFile(filePath: String): OperationResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            val uri = filePath.toUri()
            val deleted = if (uri.scheme == "content") {
                context.contentResolver.delete(uri, null, null) > 0
            } else {
                val path = uri.path ?: filePath
                val file = File(path)
                if (file.exists()) file.delete() else true
            }
            OperationResult.Success(deleted)
        } catch (e: Exception) {
            OperationResult.Error(e, "Dosya silinirken hata oluştu: $filePath")
        }
    }

    override suspend fun copyFile(sourceUri: Uri, destFile: File): OperationResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            var success = false
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
                success = destFile.exists() && destFile.length() > 0
            }

            // Fallback to absolute path if content resolver fails
            val sourcePath = sourceUri.path
            if (!success && sourcePath != null) {
                val f = File(sourcePath)
                if (f.exists()) {
                    f.inputStream().use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    success = destFile.exists() && destFile.length() > 0
                }
            }

            if (success) {
                OperationResult.Success(true)
            } else {
                OperationResult.Error(Exception("Copy failed"), "Dosya kopyalanamadı: $sourceUri")
            }
        } catch (e: Exception) {
            OperationResult.Error(e, "Dosya kopyalanırken hata oluştu.")
        }
    }

    override suspend fun createTempDirectory(dirPrefix: String): OperationResult<File> = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.cacheDir, "${dirPrefix}_${System.currentTimeMillis()}")
            if (!dir.exists() && !dir.mkdirs()) {
                OperationResult.Error(Exception("Directory creation failed"), "Geçici klasör oluşturulamadı.")
            } else {
                OperationResult.Success(dir)
            }
        } catch (e: Exception) {
            OperationResult.Error(e, "Geçici klasör oluşturulurken hata oluştu.")
        }
    }

    override suspend fun deleteDirectoryRecursively(directory: File): OperationResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            val success = directory.deleteRecursively()
            OperationResult.Success(success)
        } catch (e: Exception) {
            OperationResult.Error(e, "Klasör temizlenirken hata oluştu.")
        }
    }
}
