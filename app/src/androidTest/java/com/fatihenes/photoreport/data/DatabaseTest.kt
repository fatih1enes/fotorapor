package com.fatihenes.photoreport.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fatihenes.photoreport.core.database.AppDatabase
import com.fatihenes.photoreport.core.database.DailyLogDao
import com.fatihenes.photoreport.core.database.DailyLogEntity
import com.fatihenes.photoreport.core.database.PhotoDao
import com.fatihenes.photoreport.core.database.PhotoEntity
import com.fatihenes.photoreport.core.database.ProjectDao
import com.fatihenes.photoreport.core.database.ProjectEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    private lateinit var projectDao: ProjectDao
    private lateinit var dailyLogDao: DailyLogDao
    private lateinit var photoDao: PhotoDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        projectDao = db.projectDao()
        dailyLogDao = db.dailyLogDao()
        photoDao = db.photoDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun writeProjectAndReadInList() = runBlocking {
        val project = ProjectEntity(name = "Test Project", colorHex = "#FF0000")
        projectDao.insertProject(project)
        val allProjects = projectDao.getAllProjects().first()
        assertEquals(allProjects[0].name, "Test Project")
    }

    @Test
    @Throws(Exception::class)
    fun testCascadeDelete() = runBlocking {
        // 1. Create Project
        val projectId = projectDao.insertProject(ProjectEntity(name = "Project X", colorHex = "#000"))

        // 2. Create Log
        val logId = dailyLogDao.insertLog(DailyLogEntity(projectId = projectId, date = System.currentTimeMillis(), note = "Note"))

        // 3. Create Photo
        photoDao.insertPhoto(PhotoEntity(logId = logId, filePath = "/path/to/img.jpg"))

        // 4. Verify linked data exists
        val logs = dailyLogDao.getLogsForProjectSuspend(projectId)
        assertEquals(1, logs.size)

        // 5. Hard delete project
        projectDao.hardDeleteProjectById(projectId)

        // 6. Verify logs and photos are gone due to CASCADE
        val logsAfter = dailyLogDao.getLogsForProjectSuspend(projectId)
        assertTrue("Logs should be deleted by cascade", logsAfter.isEmpty())

        val photosAfter = photoDao.getPhotosForLog(logId).first()
        assertTrue("Photos should be deleted by cascade", photosAfter.isEmpty())
    }

    @Test
    fun testSoftDelete() = runBlocking {
        val projectId = projectDao.insertProject(ProjectEntity(name = "Soft Delete Test", colorHex = "#000"))

        projectDao.softDeleteProjectById(projectId, System.currentTimeMillis())

        val activeProjects = projectDao.getAllProjects().first()
        assertTrue("Should not be in active projects", activeProjects.none { it.id == projectId })

        val deletedProjects = projectDao.getDeletedProjects().first()
        assertTrue("Should be in deleted projects", deletedProjects.any { it.id == projectId })
    }
}
