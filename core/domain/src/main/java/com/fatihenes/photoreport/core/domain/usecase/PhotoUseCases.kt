package com.fatihenes.photoreport.core.domain.usecase

import com.fatihenes.photoreport.core.domain.repository.PhotoRepository
import com.fatihenes.photoreport.core.model.Photo
import com.fatihenes.photoreport.core.model.WatermarkData
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPhotosForLogUseCase @Inject constructor(
    private val repository: PhotoRepository
) {
    operator fun invoke(logId: Long): Flow<List<Photo>> = repository.getPhotosForLog(logId)
}

class GetPhotosForProjectUseCase @Inject constructor(
    private val repository: PhotoRepository
) {
    operator fun invoke(projectId: Long): Flow<List<Photo>> = repository.getPhotosForProject(projectId)
}

class ProcessAndSavePhotoUseCase @Inject constructor(
    private val repository: PhotoRepository
) {
    operator fun invoke(
        uriString: String,
        projectId: Long,
        logId: Long,
        enableWebp: Boolean,
        projectName: String,
        watermarkData: WatermarkData? = null
    ) {
        repository.processAndSavePhotoInBackground(uriString, projectId, logId, enableWebp, projectName, watermarkData)
    }
}

class SoftDeletePhotosUseCase @Inject constructor(
    private val repository: PhotoRepository
) {
    suspend operator fun invoke(photos: List<Photo>) = repository.softDeletePhotos(photos)
    suspend operator fun invoke(photo: Photo) = repository.softDeletePhoto(photo)
}

class RotatePhotoUseCase @Inject constructor(
    private val repository: PhotoRepository
) {
    suspend operator fun invoke(photoId: Long, rotation: Float) = repository.updatePhotoRotation(photoId, rotation)
}
