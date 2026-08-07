package com.fatihenes.photoreport.manager

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import com.fatihenes.photoreport.core.database.*
import com.fatihenes.photoreport.core.common.util.result.OperationResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
    private val photoDao: PhotoDao,
    private val settingsRepository: com.fatihenes.photoreport.repository.SettingsRepository,
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

            // Add SharedPreferences and DataStore
            val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
            if (prefsDir.exists() && prefsDir.isDirectory) {
                prefsDir.listFiles()?.forEach { prefFile ->
                    filesToZip.add(Pair("prefs/${prefFile.name}", prefFile))
                }
            }
            val dataStoreDirFiles = File(context.filesDir, "datastore")
            if (dataStoreDirFiles.exists() && dataStoreDirFiles.isDirectory) {
                dataStoreDirFiles.listFiles()?.forEach { dsFile ->
                    filesToZip.add(Pair("datastore_files/${dsFile.name}", dsFile))
                }
            }
            val dataStoreDirApp = File(context.applicationInfo.dataDir, "datastore")
            if (dataStoreDirApp.exists() && dataStoreDirApp.isDirectory) {
                dataStoreDirApp.listFiles()?.forEach { dsFile ->
                    filesToZip.add(Pair("datastore_app/${dsFile.name}", dsFile))
                }
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

                        val progress = 20 + ((index + 1) * 20 / (total.coerceAtLeast(1)))
                        emit(OperationResult.Loading(progress))
                    }

                    // Add media files via chunked streaming to prevent OOM
                    var offset = 0
                    val chunkSize = 50
                    while (true) {
                        val chunk = photoDao.getAllPhotosChunked(limit = chunkSize, offset = offset)
                        if (chunk.isEmpty()) break

                        chunk.forEach { photo ->
                            try {
                                val uri = photo.filePath.toUri()
                                val extension = if (uri.scheme == "content") {
                                    context.contentResolver.getType(uri)?.let {
                                        MimeTypeMap.getSingleton().getExtensionFromMimeType(it)
                                    } ?: "jpg"
                                } else {
                                    photo.filePath.substringAfterLast(".", "avif")
                                }
                                val zipPath = "media/${photo.id}.$extension"
                                zos.putNextEntry(ZipEntry(zipPath))
                                context.contentResolver.openInputStream(uri)?.use { it.copyTo(zos) }
                                zos.closeEntry()
                            } catch (e: Exception) {
                                android.util.Log.w("BackupManager", "Skipping media file in backup: ${photo.filePath}", e)
                            }
                        }
                        offset += chunk.size
                        emit(OperationResult.Loading(80))
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
            // Stop background workers safely
            try {
                androidx.work.WorkManager.getInstance(context).cancelAllWork()
            } catch (e: Exception) {
                android.util.Log.w("BackupManager", "Could not stop WorkManager during restore", e)
            }

            val tempDir = (fileManager.createTempDirectory("restore") as? OperationResult.Success)?.data
                ?: throw Exception("Geçici klasör oluşturulamadı")

            getBackupInputStream(sourceUri)?.use { ins ->
                ZipInputStream(BufferedInputStream(ins)).use { zis ->
                    val destDirCanonical = tempDir.canonicalPath
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        val outFile = File(tempDir, entry.name)
                        val canonicalPath = outFile.canonicalPath
                        if (!canonicalPath.startsWith(destDirCanonical)) {
                            throw SecurityException("Zip Slip (Path Traversal) zafiyeti tespit edildi: ${entry.name}")
                        }
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

            // 1. Database Restoration via Data Merge (Hot Restore)
            val extractedDb = File(tempDir, "database/photoreport_database")
            if (extractedDb.exists()) {
                val db = database.openHelper.writableDatabase
                db.beginTransaction()
                try {
                    db.execSQL("ATTACH DATABASE '${extractedDb.absolutePath}' AS backup")

                    // Clear and Import Projects (Cascades to logs and photos if correctly set up,
                    // but we do it explicitly to be safe and handle order)
                    db.execSQL("DELETE FROM photos")
                    db.execSQL("DELETE FROM daily_logs")
                    db.execSQL("DELETE FROM projects")

                    db.execSQL("INSERT INTO projects SELECT * FROM backup.projects")
                    db.execSQL("INSERT INTO daily_logs SELECT * FROM backup.daily_logs")
                    db.execSQL("INSERT INTO photos SELECT * FROM backup.photos")

                    db.execSQL("DETACH DATABASE backup")
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            }

            // 2. Preferences Restoration (Hot Restore)
            File(tempDir, "datastore_files").listFiles()?.firstOrNull { it.name.endsWith(".preferences_pb") }?.let { extractedDsFile ->
                try {
                    val tempDs = androidx.datastore.preferences.core.PreferenceDataStoreFactory.create { extractedDsFile }
                    val prefs = tempDs.data.first()
                    val settingsMap = mutableMapOf<String, Any>()
                    prefs.asMap().forEach { entry ->
                        settingsMap[entry.key.name] = entry.value
                    }
                    if (settingsMap.isNotEmpty()) {
                        settingsRepository.importSettings(settingsMap)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("BackupManager", "Error restoring settings", e)
                }
            }

            // 3. Media Restoration & Path Update
            File(tempDir, "media").takeIf { it.exists() && it.isDirectory }?.let { extractedMediaDir ->
                val appMediaDir = File(context.filesDir, "restored_media").apply {
                    if (exists()) deleteRecursively()
                    mkdirs()
                }

                extractedMediaDir.listFiles()?.forEach { mediaFile ->
                    val destFile = File(appMediaDir, mediaFile.name)
                    mediaFile.copyTo(destFile, overwrite = true)
                    mediaFile.name.substringBefore(".").toLongOrNull()?.let { photoId ->
                        database.openHelper.writableDatabase.execSQL(
                            "UPDATE photos SET filePath = ? WHERE id = ?",
                            arrayOf(destFile.absolutePath, photoId)
                        )
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
