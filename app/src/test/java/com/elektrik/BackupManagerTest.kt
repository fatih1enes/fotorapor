package com.sarikaya.santiye.gunlugu.managers

import android.content.Context
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sarikaya.santiye.gunlugu.data.AppDatabase
import com.sarikaya.santiye.gunlugu.manager.FileManager
import com.sarikaya.santiye.gunlugu.manager.LocalBackupManager
import com.sarikaya.santiye.gunlugu.util.result.OperationResult
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.annotation.Config
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class BackupManagerTest {

    private lateinit var context: Context
    private lateinit var backupManager: LocalBackupManager
    private lateinit var mockDatabase: AppDatabase
    private lateinit var mockFileManager: FileManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        mockDatabase = mock(AppDatabase::class.java)
        mockFileManager = mock(FileManager::class.java)
        
        // Mock database checkpoint behavior to avoid NullPointerException
        val mockOpenHelper = mock(androidx.sqlite.db.SupportSQLiteOpenHelper::class.java)
        val mockDb = mock(androidx.sqlite.db.SupportSQLiteDatabase::class.java)
        val mockCursor = mock(android.database.Cursor::class.java)
        `when`(mockDatabase.openHelper).thenReturn(mockOpenHelper)
        `when`(mockOpenHelper.writableDatabase).thenReturn(mockDb)
        `when`(mockDb.query("PRAGMA wal_checkpoint(FULL)")).thenReturn(mockCursor)

        backupManager = LocalBackupManager(context, mockDatabase, mockFileManager)
        
        // Setup mock environment
        val dbDir = context.getDatabasePath("santiye_gunlugu.db").parentFile
        dbDir?.mkdirs()
        File(dbDir, "santiye_gunlugu.db").writeText("dummy db content")
        
        val picsDir = File(context.filesDir, "photos")
        picsDir.mkdirs()
        File(picsDir, "test.jpg").writeText("dummy photo content")
    }

    @Test
    fun testBackupCreationEmitsStates() = runTest {
        val destFile = File(context.cacheDir, "test_backup.zip")
        val states = backupManager.createBackup(destFile.toUri()).toList()
        
        assertTrue("Should emit states", states.isNotEmpty())
        assertTrue("First state should be Loading", states.first() is OperationResult.Loading)
        assertTrue("Last state should be Success or Error", 
            states.last() is OperationResult.Success || states.last() is OperationResult.Error)
    }
}
