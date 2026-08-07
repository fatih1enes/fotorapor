package com.fatihenes.photoreport.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DailyLogDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var projectDao: ProjectDao
    private lateinit var logDao: DailyLogDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        projectDao = db.projectDao()
        logDao = db.dailyLogDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetLog() = runTest {
        val projectId = projectDao.insertProject(ProjectEntity(name = "P1", colorHex = "#000"))
        val log = DailyLogEntity(projectId = projectId, date = 1000L, note = "First log")
        val logId = logDao.insertLog(log)

        val result = logDao.getLogForDate(projectId, 1000L)
        assertEquals("First log", result?.note)
    }

    @Test
    fun updateNote_changesNote() = runTest {
        val projectId = projectDao.insertProject(ProjectEntity(name = "P1", colorHex = "#000"))
        val logId = logDao.insertLog(DailyLogEntity(projectId = projectId, date = 1000L, note = "Old"))

        logDao.updateNote(logId, "New Note")
        val result = logDao.getLogForDate(projectId, 1000L)
        assertEquals("New Note", result?.note)
    }

    @Test
    fun getLogsForProject_returnsAll() = runTest {
        val projectId = projectDao.insertProject(ProjectEntity(name = "P1", colorHex = "#000"))
        logDao.insertLog(DailyLogEntity(projectId = projectId, date = 1000L, note = "L1"))
        logDao.insertLog(DailyLogEntity(projectId = projectId, date = 2000L, note = "L2"))

        val logs = logDao.getLogsForProject(projectId).first()
        assertEquals(2, logs.size)
    }
}
