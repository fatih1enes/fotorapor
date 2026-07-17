package com.sarikaya.santiye.gunlugu.managers

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sarikaya.santiye.gunlugu.data.AppDatabase
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File
import org.mockito.Mockito.mock

@RunWith(AndroidJUnit4::class)
@Config(manifest=Config.NONE)
class BackupManagerTest {

    private lateinit context: Context
    private lateinit backupManager: LocalBackupManager
    private lateinit mockDatabase: AppDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        mockDatabase = mock(AppDatabase::class.java)
        backupManager = LocalBackupManager(context, mockDatabase)
        
        // Setup mock environment
        val dbDir = context.getDatabasePath("test.db").parentFile
        dbDir?.mkdirs()
        File(dbDir, "test.db").writeText("dummy db content")
        
        val picsDir = File(context.filesDir, "photos")
        picsDir.mkdirs()
        File(picsDir, "test.jpg").writeText("dummy photo content")
    }

    @Test
    fun testBackupCreationEmitsStates() = runTest {
        val destFile = File(context.cacheDir, "test_backup.zip")
        val states = backupManager.createBackup(destFile).toList()
        
        assertTrue("Should emit states", states.isNotEmpty())
        assertTrue("First state should be Loading", states.first() is com.sarikaya.santiye.gunlugu.util.OperationResult.Loading)
        assertTrue("Last state should be Success or Error", states.last() is com.sarikaya.santiye.gunlugu.util.OperationResult.Success || states.last() is com.sarikaya.santiye.gunlugu.util.OperationResult.Error)
    }
}
