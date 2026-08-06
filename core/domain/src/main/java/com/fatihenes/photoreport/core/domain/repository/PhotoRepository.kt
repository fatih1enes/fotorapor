package com.fatihenes.photoreport.core.domain.repository

import com.fatihenes.photoreport.core.model.Photo
import com.fatihenes.photoreport.core.model.WatermarkData
import kotlinx.coroutines.flow.Flow

interface PhotoRepository {
    fun getPhotosForLog(logId: Long): Flow<List<Photo>>
    fun getPhotosForProject(projectId: Long): Flow<List<Photo>>
    suspend fun insertPhoto(logId: Long, filePath: String): Long
    suspend fun deletePhoto(photo: Photo)
    suspend fun deletePhotosByIds(photoIds: List<Long>)
    suspend fun updatePhotoRotation(id: Long, rotation: Float)
    suspend fun softDeletePhoto(photo: Photo)
    suspend fun softDeletePhotos(photos: List<Photo>)
    fun processAndSavePhotoInBackground(uriString: String, projectId: Long, logId: Long, enableWebp: Boolean, projectName: String, watermarkData: WatermarkData? = null)
}
