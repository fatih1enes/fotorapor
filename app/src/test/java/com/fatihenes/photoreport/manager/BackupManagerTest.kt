package com.fatihenes.photoreport.manager

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.fatihenes.photoreport.core.database.AppDatabase
import com.fatihenes.photoreport.core.database.FileManager
import com.fatihenes.photoreport.core.database.PhotoDao
import com.fatihenes.photoreport.repository.SettingsRepository
import com.fatihenes.photoreport.core.common.util.result.OperationResult
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupManagerTest {

    private lateinit var context: Context
    private lateinit var backupManager: LocalBackupManager
    private lateinit var mockDatabase: AppDatabase
    private lateinit var mockFileManager: FileManager
    private lateinit var mockPhotoDao: PhotoDao
    private lateinit var mockSettingsRepository: SettingsRepository
    private lateinit var mockContentResolver: ContentResolver
    private lateinit var mockOpenHelper: SupportSQLiteOpenHelper
    private lateinit var mockDb: SupportSQLiteDatabase

    @Before
    fun setup() {
        context = mock(Context::class.java)
        mockDatabase = mock(AppDatabase::class.java)
        mockFileManager = mock(FileManager::class.java)
        mockPhotoDao = mock(PhotoDao::class.java)
        mockSettingsRepository = mock(SettingsRepository::class.java)
        mockContentResolver = mock(ContentResolver::class.java)
        mockOpenHelper = mock(SupportSQLiteOpenHelper::class.java)
        mockDb = mock(SupportSQLiteDatabase::class.java)

        val tempDbFile = File.createTempFile("photoreport", ".db")
        tempDbFile.deleteOnExit()

        `when`(context.getDatabasePath("photoreport_database")).thenReturn(tempDbFile)
        val mockAppInfo = mock(android.content.pm.ApplicationInfo::class.java)
        mockAppInfo.dataDir = tempDbFile.parent
        `when`(context.applicationInfo).thenReturn(mockAppInfo)
        `when`(context.contentResolver).thenReturn(mockContentResolver)
        `when`(mockDatabase.openHelper).thenReturn(mockOpenHelper)
        `when`(mockOpenHelper.writableDatabase).thenReturn(mockDb)
        `when`(mockDb.query("PRAGMA wal_checkpoint(FULL)")).thenReturn(mock(android.database.Cursor::class.java))
        `when`(context.filesDir).thenReturn(tempDbFile.parentFile)

        backupManager = LocalBackupManager(context, mockDatabase, mockFileManager, mockPhotoDao, mockSettingsRepository)
    }

    @Test
    fun testBackupCreationEmitsStates() = runTest {
        val mockUri = mock(Uri::class.java)
        val outStream = ByteArrayOutputStream()
        `when`(mockContentResolver.openOutputStream(mockUri)).thenReturn(outStream)
        `when`(mockPhotoDao.getAllPhotosChunked(anyInt(), anyInt())).thenReturn(emptyList())

        val states = backupManager.createBackup(mockUri).toList()

        assertTrue("Should emit states", states.isNotEmpty())
        assertTrue("First state should be Loading", states.first() is OperationResult.Loading)
        assertTrue("Final state should be Success", states.last() is OperationResult.Success)
    }

    @Test
    fun testZipSlipPathTraversalProtection() = runTest {
        val mockUri = mock(Uri::class.java)
        val tempDir = File.createTempFile("test_restore", "").apply { delete(); mkdirs(); deleteOnExit() }
        `when`(mockFileManager.createTempDirectory("restore")).thenReturn(OperationResult.Success(tempDir))
        `when`(mockFileManager.deleteDirectoryRecursively(tempDir)).thenReturn(OperationResult.Success(true))

        val zipOutStream = ByteArrayOutputStream()
        ZipOutputStream(zipOutStream).use { zos ->
            zos.putNextEntry(ZipEntry("../../malicious_file.sh"))
            zos.write("echo 'malicious'".toByteArray())
            zos.closeEntry()
        }
        val zipInputStream = ByteArrayInputStream(zipOutStream.toByteArray())
        `when`(mockContentResolver.openInputStream(mockUri)).thenReturn(zipInputStream)

        val states = backupManager.restoreBackup(mockUri).toList()
        val errorState = states.last() as? OperationResult.Error
        assertTrue("Should emit error state on path traversal", errorState != null && errorState.error is SecurityException)
    }

    @Test
    fun testDataStoreBackupInclusion() = runTest {
        val mockUri = mock(Uri::class.java)
        val outStream = ByteArrayOutputStream()
        `when`(mockContentResolver.openOutputStream(mockUri)).thenReturn(outStream)
        `when`(mockPhotoDao.getAllPhotosChunked(anyInt(), anyInt())).thenReturn(emptyList())

        val dsDir = File(context.applicationInfo.dataDir, "datastore").apply { mkdirs(); deleteOnExit() }
        File(dsDir, "settings.preferences_pb").apply { 
            writeText("test datastore", Charsets.UTF_8)
            deleteOnExit() 
        }

        val states = backupManager.createBackup(mockUri).toList()
        assertTrue("Final state should be Success", states.last() is OperationResult.Success)

        var foundDataStoreEntry = false
        ZipInputStream(ByteArrayInputStream(outStream.toByteArray())).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name.contains("datastore_app/settings.preferences_pb")) {
                    foundDataStoreEntry = true
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        assertTrue("Zip archive should include datastore preference file", foundDataStoreEntry)
    }
}

