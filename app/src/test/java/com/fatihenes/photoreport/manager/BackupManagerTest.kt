package com.fatihenes.photoreport.manager

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.fatihenes.photoreport.data.AppDatabase
import com.fatihenes.photoreport.data.PhotoDao
import com.fatihenes.photoreport.util.result.OperationResult
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.ByteArrayOutputStream
import java.io.File

class BackupManagerTest {

    private lateinit var context: Context
    private lateinit var backupManager: LocalBackupManager
    private lateinit var mockDatabase: AppDatabase
    private lateinit var mockFileManager: FileManager
    private lateinit var mockPhotoDao: PhotoDao
    private lateinit var mockContentResolver: ContentResolver
    private lateinit var mockOpenHelper: SupportSQLiteOpenHelper
    private lateinit var mockDb: SupportSQLiteDatabase

    @Before
    fun setup() {
        context = mock(Context::class.java)
        mockDatabase = mock(AppDatabase::class.java)
        mockFileManager = mock(FileManager::class.java)
        mockPhotoDao = mock(PhotoDao::class.java)
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

        backupManager = LocalBackupManager(context, mockDatabase, mockFileManager, mockPhotoDao)
    }

    @Test
    fun testBackupCreationEmitsStates() = runTest {
        val mockUri = mock(Uri::class.java)
        val outStream = ByteArrayOutputStream()
        `when`(mockContentResolver.openOutputStream(mockUri)).thenReturn(outStream)
        `when`(mockPhotoDao.getAllPhotosSuspend()).thenReturn(emptyList())

        val states = backupManager.createBackup(mockUri).toList()

        assertTrue("Should emit states", states.isNotEmpty())
        assertTrue("First state should be Loading", states.first() is OperationResult.Loading)
        assertTrue("Final state should be Success", states.last() is OperationResult.Success)
    }
}
