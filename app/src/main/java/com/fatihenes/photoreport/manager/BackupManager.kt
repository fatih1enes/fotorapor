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
    fun createBackup(destUri: Uri): Flow<OperationResult<Unit>>
    fun restoreBackup(sourceUri: Uri): Flow<OperationResult<Unit>>
}

@Singleton
@Suppress("TooManyFunctions")
class LocalBackupManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val fileManager: FileManager,
    private val photoDao: PhotoDao,
    private val settingsRepository: com.fatihenes.photoreport.repository.SettingsRepository,
) : BackupManager {

    private fun getBackupOutputStream(destUri: Uri): java.io.OutputStream? =
        context.contentResolver.openOutputStream(destUri)

    private fun getBackupInputStream(sourceUri: Uri): java.io.InputStream? =
        context.contentResolver.openInputStream(sourceUri)

    override fun createBackup(destUri: Uri): Flow<OperationResult<Unit>> = flow {
        emit(OperationResult.Loading(0))
        try {
            checkpointDatabase()
            val systemFiles = collectSystemFiles()
            emit(OperationResult.Loading(20))

            getBackupOutputStream(destUri)?.use { os ->
                ZipOutputStream(BufferedOutputStream(os)).use { zos ->
                    writeZipEntries(zos, systemFiles) { progress ->
                        emit(OperationResult.Loading(20 + progress * 20 / 100))
                    }
                    zipMediaInChunks(zos) { progress ->
                        emit(OperationResult.Loading(progress))
                    }
                }
            } ?: throw Exception("Hedef dosya açılamadı")

            emit(OperationResult.Success(Unit))
        } catch (e: Exception) {
            emit(OperationResult.Error(e, "Yedekleme başarısız oldu."))
        }
    }.flowOn(Dispatchers.IO)

    private fun checkpointDatabase() {
        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
    }

    private fun collectSystemFiles(): List<Pair<String, File>> {
        val files = mutableListOf<Pair<String, File>>()
        val dbPath = context.getDatabasePath("photoreport_database").absolutePath
        val dbFile = File(dbPath)
        if (dbFile.exists()) files.add("database/photoreport_database" to dbFile)
        listOf("-shm", "-wal").forEach { suffix ->
            val f = File(dbPath + suffix)
            if (f.exists()) files.add("database/photoreport_database$suffix" to f)
        }

        val dataDir = context.applicationInfo.dataDir
        File(dataDir, "shared_prefs")
            .takeIf { it.exists() && it.isDirectory }
            ?.listFiles()?.forEach {
                files.add("prefs/${it.name}" to it)
            }
        File(context.filesDir, "datastore")
            .takeIf { it.exists() && it.isDirectory }
            ?.listFiles()?.forEach {
                files.add("datastore_files/${it.name}" to it)
            }
        File(dataDir, "datastore")
            .takeIf { it.exists() && it.isDirectory }
            ?.listFiles()?.forEach {
                files.add("datastore_app/${it.name}" to it)
            }
        return files
    }

    private suspend fun writeZipEntries(
        zos: ZipOutputStream,
        files: List<Pair<String, File>>,
        onProgress: suspend (Int) -> Unit
    ) {
        files.forEachIndexed { index, (path, file) ->
            try {
                zos.putNextEntry(ZipEntry(path))
                FileInputStream(file).use { it.copyTo(zos) }
                zos.closeEntry()
            } catch (e: Exception) {
                android.util.Log.w("BackupManager", "Skipping file: $path", e)
            }
            onProgress((index + 1) * 100 / files.size.coerceAtLeast(1))
        }
    }

    @Suppress("NestedBlockDepth")
    private suspend fun zipMediaInChunks(zos: ZipOutputStream, onProgress: suspend (Int) -> Unit) {
        var offset = 0
        val chunkSize = 50
        var processed = 0
        while (true) {
            val chunk = photoDao.getAllPhotosChunked(chunkSize, offset)
            if (chunk.isEmpty()) break
            chunk.forEach { photo ->
                try {
                    val uri = photo.filePath.toUri()
                    val ext = if (uri.scheme == "content") {
                        context.contentResolver.getType(uri)?.let {
                            MimeTypeMap.getSingleton().getExtensionFromMimeType(it)
                        } ?: "jpg"
                    } else photo.filePath.substringAfterLast(".", "avif")

                    // Boş stream'de boş zip entry yazma (0-byte dosya oluşurdu).
                    val input = try {
                        context.contentResolver.openInputStream(uri)
                    } catch (e: Exception) {
                        android.util.Log.w("BackupManager", "Skipping media: ${photo.filePath}", e)
                        null
                    }
                    if (input != null) {
                        input.use {
                            zos.putNextEntry(ZipEntry("media/${photo.id}.$ext"))
                            it.copyTo(zos)
                            zos.closeEntry()
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("BackupManager", "Skipping media: ${photo.filePath}", e)
                }
            }
            offset += chunk.size
            processed += chunk.size
            // Sabit 80 yerine ilerleyen progress: 40->80 bandında ilerle.
            onProgress((40 + (processed / 10)).coerceAtMost(80))
        }
    }

    override fun restoreBackup(sourceUri: Uri): Flow<OperationResult<Unit>> = flow {
        emit(OperationResult.Loading(0))
        try {
            cancelAllWork()
            val tempDir = (fileManager.createTempDirectory("restore") as? OperationResult.Success)?.data
                ?: throw Exception("Geçici klasör oluşturulamadı")

            unzipToTempDirectory(sourceUri, tempDir)
            emit(OperationResult.Loading(50))

            restoreDatabaseFromBackup(tempDir)
            restoreSettingsFromBackup(tempDir)
            restoreMediaFiles(tempDir)

            fileManager.deleteDirectoryRecursively(tempDir)
            emit(OperationResult.Loading(100))
            emit(OperationResult.Success(Unit))
        } catch (e: Exception) {
            emit(OperationResult.Error(e, "Geri yükleme başarısız oldu."))
        }
    }.flowOn(Dispatchers.IO)

    private fun cancelAllWork() {
        try {
            androidx.work.WorkManager.getInstance(context).cancelAllWork()
        } catch (e: Exception) {
            android.util.Log.w("BackupManager", "WorkManager stop failed", e)
        }
    }

    @Suppress("NestedBlockDepth")
    private fun unzipToTempDirectory(sourceUri: Uri, tempDir: File) {
        getBackupInputStream(sourceUri)?.use { ins ->
            ZipInputStream(BufferedInputStream(ins)).use { zis ->
                val destDirCanonical = tempDir.canonicalPath
                var entry = zis.nextEntry
                while (entry != null) {
                    val outFile = File(tempDir, entry.name)
                    if (!outFile.canonicalPath.startsWith(destDirCanonical)) {
                        throw SecurityException("Zip Slip detected: ${entry.name}")
                    }
                    if (entry.isDirectory) outFile.mkdirs() else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { zis.copyTo(it) }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        } ?: throw Exception("Yedek dosyası okunamadı")
    }

    private fun restoreDatabaseFromBackup(tempDir: File) {
        val extractedDb = File(tempDir, "database/photoreport_database")
        if (!extractedDb.exists()) return

        prepareAndMigrateBackupDatabase(extractedDb)

        database.openHelper.writableDatabase.apply {
            beginTransaction()
            try {
                // ATTACH bind desteklemez: tek tırnak escape'le. Davranış aynı, SQL bozulması önlenir.
                val escapedPath = extractedDb.absolutePath.replace("'", "''")
                execSQL("ATTACH DATABASE '$escapedPath' AS backup")
                execSQL("DELETE FROM photos")
                execSQL("DELETE FROM daily_logs")
                execSQL("DELETE FROM projects")
                execSQL("INSERT INTO projects (id, name, colorHex, isDeleted, deletedAt) SELECT id, name, colorHex, isDeleted, deletedAt FROM backup.projects")
                execSQL("INSERT INTO daily_logs (id, projectId, date, note) SELECT id, projectId, date, note FROM backup.daily_logs")
                execSQL("INSERT INTO photos (id, logId, filePath, rotation, isDeleted, deletedAt) SELECT id, logId, filePath, rotation, isDeleted, deletedAt FROM backup.photos")
                execSQL("DETACH DATABASE backup")
                setTransactionSuccessful()
            } finally {
                endTransaction()
            }
        }
    }

    private fun prepareAndMigrateBackupDatabase(dbFile: File) {
        val sqliteDb = android.database.sqlite.SQLiteDatabase.openDatabase(
            dbFile.absolutePath,
            null,
            android.database.sqlite.SQLiteDatabase.OPEN_READWRITE
        )
        try {
            val currentVersion = 6
            val backupVersion = sqliteDb.version
            if (backupVersion > currentVersion) {
                throw IllegalStateException("Yedek dosyası daha yeni bir uygulama sürümüne ait (v$backupVersion > v$currentVersion)")
            }

            if (backupVersion < 2) {
                try {
                    sqliteDb.execSQL("ALTER TABLE photos ADD COLUMN rotation REAL NOT NULL DEFAULT 0.0")
                } catch (_: Exception) { }
            }
            if (backupVersion < 3) {
                try {
                    sqliteDb.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_logs_projectId` ON `daily_logs` (`projectId`)")
                } catch (_: Exception) { }
            }
            if (backupVersion < 4) {
                try {
                    sqliteDb.execSQL("ALTER TABLE projects ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) { }
                try {
                    sqliteDb.execSQL("ALTER TABLE projects ADD COLUMN deletedAt INTEGER")
                } catch (_: Exception) { }
                try {
                    sqliteDb.execSQL("ALTER TABLE photos ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) { }
                try {
                    sqliteDb.execSQL("ALTER TABLE photos ADD COLUMN deletedAt INTEGER")
                } catch (_: Exception) { }
            }
            if (backupVersion < 5) {
                try {
                    sqliteDb.execSQL("CREATE INDEX IF NOT EXISTS `index_projects_isDeleted` ON `projects` (`isDeleted`)")
                    sqliteDb.execSQL("CREATE INDEX IF NOT EXISTS `index_projects_deletedAt` ON `projects` (`deletedAt`)")
                    sqliteDb.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_logs_date` ON `daily_logs` (`date`)")
                    sqliteDb.execSQL("CREATE INDEX IF NOT EXISTS `index_photos_isDeleted` ON `photos` (`isDeleted`)")
                    sqliteDb.execSQL("CREATE INDEX IF NOT EXISTS `index_photos_deletedAt` ON `photos` (`deletedAt`)")
                } catch (_: Exception) { }
            }
            if (backupVersion < 6) {
                try {
                    sqliteDb.execSQL("CREATE INDEX IF NOT EXISTS `index_projects_isDeleted_deletedAt` ON `projects` (`isDeleted`, `deletedAt`)")
                    sqliteDb.execSQL("CREATE INDEX IF NOT EXISTS `index_photos_isDeleted_deletedAt` ON `photos` (`isDeleted`, `deletedAt`)")
                } catch (_: Exception) { }
            }
            sqliteDb.version = currentVersion
        } finally {
            sqliteDb.close()
        }
    }

    private suspend fun restoreSettingsFromBackup(tempDir: File) {
        File(tempDir, "datastore_files").listFiles()?.firstOrNull { it.name.endsWith(".preferences_pb") }?.let { file ->
            try {
                val tempDs = androidx.datastore.preferences.core.PreferenceDataStoreFactory.create { file }
                val prefs = tempDs.data.first()
                val settingsMap = prefs.asMap().mapKeys { it.key.name }
                if (settingsMap.isNotEmpty()) settingsRepository.importSettings(settingsMap)
            } catch (e: Exception) {
                android.util.Log.e("BackupManager", "Settings restore failed", e)
            }
        }
    }

    private fun restoreMediaFiles(tempDir: File) {
        val extractedMediaDir = File(tempDir, "media").takeIf { it.exists() && it.isDirectory } ?: return
        val appMediaDir = File(context.filesDir, "restored_media").apply {
            if (exists()) deleteRecursively()
            mkdirs()
        }

        extractedMediaDir.listFiles()?.forEach { mediaFile ->
            try {
                val destFile = File(appMediaDir, mediaFile.name)
                mediaFile.copyTo(destFile, overwrite = true)
                mediaFile.name.substringBefore(".").toLongOrNull()?.let { photoId ->
                    database.openHelper.writableDatabase.execSQL(
                        "UPDATE photos SET filePath = ? WHERE id = ?",
                        arrayOf(destFile.absolutePath, photoId)
                    )
                }
            } catch (e: Exception) {
                // Disk dolması vb: tek dosya fail'i tüm restore'u bozmasın, devam et.
                android.util.Log.w("BackupManager", "Skipping restore media: ${mediaFile.name}", e)
            }
        }
    }
}
