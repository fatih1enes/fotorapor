package com.fatihenes.photoreport.core.domain.repository

import com.fatihenes.photoreport.core.model.Photo
import com.fatihenes.photoreport.core.model.Project
import kotlinx.coroutines.flow.Flow

interface TrashRepository {
    fun getDeletedProjects(): Flow<List<Project>>
    fun getDeletedPhotos(): Flow<List<Photo>>
    suspend fun restoreProjectById(projectId: Long)
    suspend fun restorePhoto(id: Long)
    suspend fun hardDeleteProject(projectId: Long)
    suspend fun hardDeletePhoto(photo: Photo)
    suspend fun emptyTrash()
    suspend fun cleanOldTrash(threshold: Long)
}
