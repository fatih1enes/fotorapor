package com.fatihenes.photoreport.core.domain.usecase

import com.fatihenes.photoreport.core.domain.repository.TrashRepository
import com.fatihenes.photoreport.core.model.Photo
import com.fatihenes.photoreport.core.model.Project
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTrashItemsUseCase @Inject constructor(
    private val repository: TrashRepository
) {
    fun getProjects(): Flow<List<Project>> = repository.getDeletedProjects()
    fun getPhotos(): Flow<List<Photo>> = repository.getDeletedPhotos()
}

class RestoreTrashUseCase @Inject constructor(
    private val repository: TrashRepository
) {
    suspend fun restoreProject(projectId: Long) = repository.restoreProjectById(projectId)
    suspend fun restorePhoto(photoId: Long) = repository.restorePhoto(photoId)
}

class EmptyTrashUseCase @Inject constructor(
    private val repository: TrashRepository
) {
    suspend operator fun invoke() = repository.emptyTrash()
}
