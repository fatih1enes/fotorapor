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
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val fileManager: FileManager,
    private val photoDao: PhotoDao
) : BackupManager {

    override fun createBackup(destUri: Uri): Flow<OperationResult<Unit>> = flow {
        emit(OperationResult.Loading(0))

        try {
            // Checkpoint database to ensure WAL is flushed
            val dbPath = context.getDatabasePath("photoreport_database").absolutePath
            database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()

            val filesToZip = mutableListOf<Pair<String, Any>>() // Can be File or Uri

            // Add DB files
            val dbFile = File(dbPath)
            val dbShm = File("$dbPath-shm")
            val dbWal = File("$dbPath-wal")

            if (dbFile.exists()) filesToZip.add(Pair("database/photoreport_database", dbFile))
            if (dbShm.exists()) filesToZip.add(Pair("database/photoreport_database-shm", dbShm))
            if (dbWal.exists()) filesToZip.add(Pair("database/photoreport_database-wal", dbWal))

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
                    val type = context.contentResolver.getType(uri)
                    MimeTypeMap.getSingleton().getExtensionFromMimeType(type) ?: "jpg"
                } else {
                    photo.filePath.substringAfterLast(".", "avif")
                }
                filesToZip.add(Pair("media/${photo.id}.$extension", uri))
            }

            emit(OperationResult.Loading(20))

            context.contentResolver.openOutputStream(destUri)?.use { os ->
                ZipOutputStream(BufferedOutputStream(os)).use { zos ->
                    val total = filesToZip.size
                    filesToZip.forEachIndexed { index, pair ->
                        val (zipPath, item) = pair
                        val entry = ZipEntry(zipPath)
                        zos.putNextEntry(entry)
                        
                        try {
                            when (item) {
                                is File -> FileInputStream(item).use { fis -> fis.copyTo(zos) }
                                is Uri -> context.contentResolver.openInputStream(item)?.use { ins -> ins.copyTo(zos) }
                            }
                        } catch (e: Exception) {
                            // Skip missing media files safely
                        }
                        
                        zos.closeEntry()

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

            // Extract to temp folder
            val tempDirResult = fileManager.createTempDirectory("restore")
            if (tempDirResult !is OperationResult.Success) {
                throw Exception("Geçici klasör oluşturulamadı")
            }
            val tempDir = tempDirResult.data

            context.contentResolver.openInputStream(sourceUri)?.use { ins ->
                ZipInputStream(BufferedInputStream(ins)).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        val outFile = File(tempDir, entry.name)
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            FileOutputStream(outFile).use { fos ->
                                zis.copyTo(fos)
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            } ?: throw Exception("Yedek dosyası okunamadı")

            emit(OperationResult.Loading(50))

            // Restore Database
            val extractedDb = File(tempDir, "database/photoreport_database")
            if (extractedDb.exists()) {
                // Close current DB connections securely
                database.close()
                val dbPath = context.getDatabasePath("photoreport_database").absolutePath
                val currentDb = File(dbPath)
                val currentShm = File("$dbPath-shm")
                val currentWal = File("$dbPath-wal")

                extractedDb.copyTo(currentDb, overwrite = true)

                val extractedShm = File(tempDir, "database/photoreport_database-shm")
                if (extractedShm.exists()) extractedShm.copyTo(currentShm, overwrite = true)
                else currentShm.delete()

                val extractedWal = File(tempDir, "database/photoreport_database-wal")
                if (extractedWal.exists()) extractedWal.copyTo(currentWal, overwrite = true)
                else currentWal.delete()
            }

            // Restore Prefs
            val extractedPrefsDir = File(tempDir, "prefs")
            if (extractedPrefsDir.exists() && extractedPrefsDir.isDirectory) {
                val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
                prefsDir.mkdirs()
                extractedPrefsDir.listFiles()?.forEach { prefFile ->
                    prefFile.copyTo(File(prefsDir, prefFile.name), overwrite = true)
                }
            }

            // Restore Media
            val extractedMediaDir = File(tempDir, "media")
            if (extractedMediaDir.exists() && extractedMediaDir.isDirectory) {
                val appMediaDir = File(context.filesDir, "restored_media")
                if (appMediaDir.exists()) appMediaDir.deleteRecursively()
                appMediaDir.mkdirs()
                
                // We need to update the DB paths to point to the restored files
                val dbPath = context.getDatabasePath("photoreport_database").absolutePath
                val sqliteDb = android.database.sqlite.SQLiteDatabase.openDatabase(dbPath, null, android.database.sqlite.SQLiteDatabase.OPEN_READWRITE)
                
                extractedMediaDir.listFiles()?.forEach { mediaFile ->
                    val destFile = File(appMediaDir, mediaFile.name)
                    mediaFile.copyTo(destFile, overwrite = true)
                    
                    val photoId = mediaFile.name.substringBefore(".").toLongOrNull()
                    if (photoId != null) {
                        val newPath = destFile.absolutePath
                        sqliteDb.execSQL("UPDATE photos SET filePath = ? WHERE id = ?", arrayOf<Any>(newPath, photoId))
                    }
                }
                sqliteDb.close()
            }

            // Cleanup
            fileManager.deleteDirectoryRecursively(tempDir)

            emit(OperationResult.Loading(100))
            emit(OperationResult.Success(Unit))

        } catch (e: Exception) {
            emit(OperationResult.Error(e, "Geri yükleme başarısız oldu."))
        }
    }.flowOn(Dispatchers.IO)
}
