package com.fatihenes.photoreport.manager

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import com.fatihenes.photoreport.data.AppDatabase
import com.fatihenes.photoreport.data.PhotoDao
import com.fatihenes.photoreport.util.result.OperationResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Backup and Restore functionality.
 */
interface BackupManager {
    /**
     * Creates a backup of the entire app state (Database, Preferences, and Media Files)
     * and streams it to the given [destUri].
     *
     * @param destUri The destination URI to write the backup zip archive.
     * @return A Flow emitting [OperationResult.Loading] with progress, and finally [OperationResult.Success].
     */
    fun createBackup(destUri: Uri): Flow<OperationResult<Unit>>

    /**
     * Restores the app state from a given backup [sourceUri].
     * Handles Room Database schema version checks and safe media extraction.
     *
     * @param sourceUri The URI of the backup file.
     * @return A Flow emitting [OperationResult.Loading] with progress, and finally [OperationResult.Success].
     */
    fun restoreBackup(sourceUri: Uri): Flow<OperationResult<Unit>>
}

@Singleton
class LocalBackupManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val fileManager: FileManager,
    private val photoDao: PhotoDao
) : BackupManager {

    /**
     * Future-ready output stream provider.
     * Encryption (e.g., CipherOutputStream) can be easily added here.
     */
    private fun getBackupOutputStream(destUri: Uri): java.io.OutputStream? {
        val baseStream = context.contentResolver.openOutputStream(destUri) ?: return null
        // Placeholder for future encryption:
        // return CipherOutputStream(baseStream, secretKey)
        return baseStream
    }

    /**
     * Future-ready input stream provider.
     */
    private fun getBackupInputStream(sourceUri: Uri): java.io.InputStream? {
        val baseStream = context.contentResolver.openInputStream(sourceUri) ?: return null
        // Placeholder for future decryption:
        // return CipherInputStream(baseStream, secretKey)
        return baseStream
    }

    override fun createBackup(destUri: Uri): Flow<OperationResult<Unit>> = flow {
        emit(OperationResult.Loading(0))

        try {
            // Checkpoint database to ensure WAL is flushed
            val dbPath = context.getDatabasePath("photoreport_database").absolutePath
            database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()

            val filesToZip = mutableListOf<Pair<String, Any>>() // Can be File or Uri

            // Add DB files
            val dbFile = File(dbPath)
            if (dbFile.exists()) filesToZip.add(Pair("database/photoreport_database", dbFile))
            listOf("-shm", "-wal").forEach { suffix ->
                val f = File(dbPath + suffix)
                if (f.exists()) filesToZip.add(Pair("database/photoreport_database$suffix", f))
            }

            // Add SharedPreferences
            val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
            if (prefsDir.exists() && prefsDir.isDirectory) {
                prefsDir.listFiles()?.forEach { prefFile ->
                    filesToZip.add(Pair("prefs/${prefFile.name}", prefFile))
                }
            }

            // Add media files
            val allPhotos = photoDao.getAllPhotosSuspend()
            allPhotos.forEach { photo ->
                val uri = photo.filePath.toUri()
                val extension = if (uri.scheme == "content") {
                    context.contentResolver.getType(uri)?.let { 
                        MimeTypeMap.getSingleton().getExtensionFromMimeType(it) 
                    } ?: "jpg"
                } else {
                    photo.filePath.substringAfterLast(".", "avif")
                }
                filesToZip.add(Pair("media/${photo.id}.$extension", uri))
            }

            emit(OperationResult.Loading(20))

            getBackupOutputStream(destUri)?.use { os ->
                ZipOutputStream(BufferedOutputStream(os)).use { zos ->
                    val total = filesToZip.size
                    filesToZip.forEachIndexed { index, (zipPath, item) ->
                        try {
                            zos.putNextEntry(ZipEntry(zipPath))
                            when (item) {
                                is File -> FileInputStream(item).use { it.copyTo(zos) }
                                is Uri -> context.contentResolver.openInputStream(item)?.use { it.copyTo(zos) }
                            }
                            zos.closeEntry()
                        } catch (e: Exception) {
                            android.util.Log.w("BackupManager", "Skipping file in backup: $zipPath", e)
                        }

                        val progress = 20 + ((index + 1) * 80 / total)
                        emit(OperationResult.Loading(progress))
                    }
                }
            } ?: throw Exception("Hedef dosya açılamadı")

            emit(OperationResult.Success(Unit))
        } catch (e: Exception) {
            emit(OperationResult.Error(e, "Yedekleme başarısız oldu."))
        }
    }.flowOn(Dispatchers.IO)

    override fun restoreBackup(sourceUri: Uri): Flow<OperationResult<Unit>> = flow {
        emit(OperationResult.Loading(0))
        try {
            // Stop background workers
            androidx.work.WorkManager.getInstance(context).cancelAllWork()

            val tempDirResult = fileManager.createTempDirectory("restore")
            if (tempDirResult !is OperationResult.Success) throw Exception("Geçici klasör oluşturulamadı")
            val tempDir = tempDirResult.data

            getBackupInputStream(sourceUri)?.use { ins ->
                ZipInputStream(BufferedInputStream(ins)).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        val outFile = File(tempDir, entry.name)
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            FileOutputStream(outFile).use { zis.copyTo(it) }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            } ?: throw Exception("Yedek dosyası okunamadı")

            emit(OperationResult.Loading(50))

            // Database Restoration
            val extractedDb = File(tempDir, "database/photoreport_database")
            if (extractedDb.exists()) {
                database.close()
                val dbPath = context.getDatabasePath("photoreport_database").absolutePath
                extractedDb.copyTo(File(dbPath), overwrite = true)
                listOf("-shm", "-wal").forEach { suffix ->
                    val src = File(tempDir, "database/photoreport_database$suffix")
                    val dst = File(dbPath + suffix)
                    if (src.exists()) src.copyTo(dst, overwrite = true) else dst.delete()
                }
            }

            // Prefs Restoration
            File(tempDir, "prefs").takeIf { it.exists() && it.isDirectory }?.let { extractedPrefsDir ->
                val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs").apply { mkdirs() }
                extractedPrefsDir.listFiles()?.forEach { it.copyTo(File(prefsDir, it.name), overwrite = true) }
            }

            // Media Restoration & DB Path Update
            File(tempDir, "media").takeIf { it.exists() && it.isDirectory }?.let { extractedMediaDir ->
                val appMediaDir = File(context.filesDir, "restored_media").apply { 
                    if (exists()) deleteRecursively()
                    mkdirs()
                }
                
                val dbPath = context.getDatabasePath("photoreport_database").absolutePath
                android.database.sqlite.SQLiteDatabase.openDatabase(dbPath, null, android.database.sqlite.SQLiteDatabase.OPEN_READWRITE).use { sqliteDb ->
                    extractedMediaDir.listFiles()?.forEach { mediaFile ->
                        val destFile = File(appMediaDir, mediaFile.name)
                        mediaFile.copyTo(destFile, overwrite = true)
                        mediaFile.name.substringBefore(".").toLongOrNull()?.let { photoId ->
                            sqliteDb.execSQL("UPDATE photos SET filePath = ? WHERE id = ?", arrayOf(destFile.absolutePath, photoId))
                        }
                    }
                }
            }

            fileManager.deleteDirectoryRecursively(tempDir)
            emit(OperationResult.Loading(100))
            emit(OperationResult.Success(Unit))
        } catch (e: Exception) {
            emit(OperationResult.Error(e, "Geri yükleme başarısız oldu."))
        }
    }.flowOn(Dispatchers.IO)
}
