package com.fatihenes.photoreport.repository

import android.net.Uri
import com.fatihenes.photoreport.data.PhotoDao
import com.fatihenes.photoreport.data.PhotoEntity
import com.fatihenes.photoreport.util.WatermarkData
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.fatihenes.photoreport.worker.PhotoProcessingWorker

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
) : PhotoRepository {

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
        val dataBuilder = Data.Builder()
            .putString("uri", uri.toString())
            .putLong("projectId", projectId)
            .putLong("logId", logId)
            .putBoolean("enableAvif", enableWebp)
            .putString("projectName", projectName)
            .putBoolean("hasWatermark", watermarkData != null)

        if (watermarkData != null) {
            watermarkData.latitude?.let { dataBuilder.putDouble("latitude", it) }
            watermarkData.longitude?.let { dataBuilder.putDouble("longitude", it) }
            dataBuilder.putString("address", watermarkData.address)
            dataBuilder.putString("dateTime", watermarkData.dateTime)
        }

        val workRequest = OneTimeWorkRequestBuilder<PhotoProcessingWorker>()
            .setInputData(dataBuilder.build())
            .build()

        WorkManager.getInstance(appContext).enqueue(workRequest)
    }
}
