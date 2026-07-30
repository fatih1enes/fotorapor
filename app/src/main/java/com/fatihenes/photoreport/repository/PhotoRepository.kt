package com.fatihenes.photoreport.repository

import android.net.Uri
import android.util.Log
import com.fatihenes.photoreport.data.DailyLogDao
import com.fatihenes.photoreport.data.DailyLogEntity
import com.fatihenes.photoreport.data.PhotoDao
import com.fatihenes.photoreport.data.PhotoEntity
import com.fatihenes.photoreport.util.DateUtils
import com.fatihenes.photoreport.util.MediaProcessor
import com.fatihenes.photoreport.util.WatermarkData
import com.fatihenes.photoreport.util.WatermarkRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

interface PhotoRepository {
    fun getPhotosForLog(logId: Long): Flow<List<PhotoEntity>>
    fun getPhotosForProject(projectId: Long): Flow<List<PhotoEntity>>
    suspend fun insertPhoto(photo: PhotoEntity)
    suspend fun deletePhoto(photo: PhotoEntity)
    suspend fun deletePhotosByIds(photoIds: List<Long>)
    suspend fun updatePhotoRotation(id: Long, rotation: Float)
    suspend fun softDeletePhoto(photo: PhotoEntity)
    suspend fun softDeletePhotos(photos: List<PhotoEntity>)
    fun processAndSavePhotoInBackground(uri: Uri, projectId: Long, logId: Long, enableWebp: Boolean, projectName: String, watermarkData: WatermarkData? = null)
}

@Singleton
class PhotoRepositoryImpl @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context,
    private val photoDao: PhotoDao,
    private val dailyLogDao: DailyLogDao,
    private val mediaProcessor: MediaProcessor,
    private val watermarkRenderer: WatermarkRenderer
) : PhotoRepository {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val logCreationMutex = Mutex()

    override fun getPhotosForLog(logId: Long): Flow<List<PhotoEntity>> = photoDao.getPhotosForLog(logId)

    override fun getPhotosForProject(projectId: Long): Flow<List<PhotoEntity>> = photoDao.getPhotosForProject(projectId)

    override suspend fun insertPhoto(photo: PhotoEntity) = photoDao.insertPhoto(photo)

    override suspend fun deletePhoto(photo: PhotoEntity) = photoDao.softDeletePhoto(photo.id, System.currentTimeMillis())

    override suspend fun deletePhotosByIds(photoIds: List<Long>) {
        val now = System.currentTimeMillis()
        for (id in photoIds) {
            photoDao.softDeletePhoto(id, now)
        }
    }

    override suspend fun updatePhotoRotation(id: Long, rotation: Float) = photoDao.updateRotation(id, rotation)

    override suspend fun softDeletePhoto(photo: PhotoEntity) {
        photoDao.softDeletePhoto(photo.id, System.currentTimeMillis())
    }

    override suspend fun softDeletePhotos(photos: List<PhotoEntity>) {
        val now = System.currentTimeMillis()
        for (photo in photos) {
            photoDao.softDeletePhoto(photo.id, now)
        }
    }

    override fun processAndSavePhotoInBackground(uri: Uri, projectId: Long, logId: Long, enableWebp: Boolean, projectName: String, watermarkData: WatermarkData?) {
        applicationScope.launch {
            try {
                // Apply watermark if data is available
                val watermarkedUri = if (watermarkData != null) {
                    watermarkRenderer.applyWatermark(appContext, uri, watermarkData)
                } else {
                    uri
                }

                val finalUri = mediaProcessor.processAndOptimize(watermarkedUri, enableWebp, projectName)

                val targetLogId = if (logId != -1L) {
                    logId
                } else {
                    val today = DateUtils.getStartOfDayEpochMillis()
                    logCreationMutex.withLock {
                        val log = dailyLogDao.getLogForDate(projectId, today)
                        log?.id ?: dailyLogDao.insertLog(
                            DailyLogEntity(projectId = projectId, date = today, note = "")
                        )
                    }
                }

                photoDao.insertPhoto(PhotoEntity(logId = targetLogId, filePath = finalUri.toString()))
            } catch (e: Exception) {
                Log.e("PhotoRepository", "Background photo save failed", e)
            }
        }
    }
}
