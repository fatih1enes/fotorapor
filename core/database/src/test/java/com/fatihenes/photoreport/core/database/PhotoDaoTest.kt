package com.fatihenes.photoreport.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PhotoDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var projectDao: ProjectDao
    private lateinit var logDao: DailyLogDao
    private lateinit var photoDao: PhotoDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        projectDao = db.projectDao()
        logDao = db.dailyLogDao()
        photoDao = db.photoDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetPhoto() = runTest {
        val projectId = projectDao.insertProject(ProjectEntity(name = "P1", colorHex = "#000"))
        val logId = logDao.insertLog(DailyLogEntity(projectId = projectId, date = 1000L, note = "L1"))
        val photo = PhotoEntity(logId = logId, filePath = "path/to/photo.jpg", rotation = 90f)
        val photoId = photoDao.insertPhoto(photo)

        val photos = photoDao.getPhotosForLogSuspend(logId)
        assertEquals(1, photos.size)
        assertEquals(90f, photos[0].rotation)
    }

    @Test
    fun softDelete_excludesFromActivePhotos() = runTest {
        val projectId = projectDao.insertProject(ProjectEntity(name = "P1", colorHex = "#000"))
        val logId = logDao.insertLog(DailyLogEntity(projectId = projectId, date = 1000L, note = "L1"))
        val photoId = photoDao.insertPhoto(PhotoEntity(logId = logId, filePath = "p1"))

        photoDao.softDeletePhoto(photoId, System.currentTimeMillis())

        val activePhotos = photoDao.getPhotosForLog(logId).first()
        assertTrue(activePhotos.isEmpty())

        val deletedPhotos = photoDao.getDeletedPhotos().first()
        assertEquals(1, deletedPhotos.size)
    }

    @Test
    fun updateRotation_works() = runTest {
        val projectId = projectDao.insertProject(ProjectEntity(name = "P1", colorHex = "#000"))
        val logId = logDao.insertLog(DailyLogEntity(projectId = projectId, date = 1000L, note = "L1"))
        val photoId = photoDao.insertPhoto(PhotoEntity(logId = logId, filePath = "p1"))

        photoDao.updateRotation(photoId, 180f)
        val photos = photoDao.getPhotosForLogSuspend(logId)
        assertEquals(180f, photos[0].rotation)
    }
}
