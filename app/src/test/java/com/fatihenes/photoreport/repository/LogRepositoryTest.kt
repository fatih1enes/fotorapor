package com.fatihenes.photoreport.repository

import com.fatihenes.photoreport.core.database.DailyLogDao
import com.fatihenes.photoreport.core.database.DailyLogEntity
import com.fatihenes.photoreport.core.database.LogWithPhotos
import com.fatihenes.photoreport.core.database.PhotoEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class LogRepositoryTest {

    private lateinit var logRepository: LogRepositoryImpl
    private val mockDailyLogDao = mock(DailyLogDao::class.java)

    @Before
    fun setup() {
        logRepository = LogRepositoryImpl(mockDailyLogDao)
    }

    @Test
    fun `getLogsWithPhotosForProject filters out soft deleted photos`() = runTest {
        val projectId = 100L
        val dummyLog = DailyLogEntity(id = 1L, projectId = projectId, date = System.currentTimeMillis(), note = "Test Log")
        val activePhoto = PhotoEntity(id = 10L, logId = 1L, filePath = "file://10.jpg", isDeleted = false)
        val deletedPhoto = PhotoEntity(id = 11L, logId = 1L, filePath = "file://11.jpg", isDeleted = true)

        val logWithPhotosList = listOf(LogWithPhotos(log = dummyLog, photos = listOf(activePhoto, deletedPhoto)))

        `when`(mockDailyLogDao.getLogsWithPhotosForProjectSuspend(projectId)).thenReturn(logWithPhotosList)
        `when`(mockDailyLogDao.getLogsWithPhotosForProject(projectId)).thenReturn(flowOf(logWithPhotosList))

        val suspendResult = logRepository.getLogsWithPhotosForProject(projectId)
        assertEquals(1, suspendResult.size)
        assertEquals(1, suspendResult[0].photos.size)
        assertEquals(10L, suspendResult[0].photos[0].id)

        val flowResult = logRepository.getLogsWithPhotosForProjectFlow(projectId).first()
        assertEquals(1, flowResult.size)
        assertEquals(1, flowResult[0].photos.size)
        assertEquals(10L, flowResult[0].photos[0].id)
    }
}
