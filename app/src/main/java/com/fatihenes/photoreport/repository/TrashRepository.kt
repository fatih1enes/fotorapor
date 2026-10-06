package com.fatihenes.photoreport.repository

import com.fatihenes.photoreport.core.database.*
import com.fatihenes.photoreport.manager.FileManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

interface TrashRepository {
    fun getDeletedProjects(): Flow<List<ProjectEntity>>
    fun getDeletedPhotos(): Flow<List<PhotoEntity>>
    suspend fun restoreProjectById(projectId: Long)
    suspend fun restorePhoto(id: Long)
    suspend fun hardDeleteProject(projectId: Long)
    suspend fun hardDeletePhoto(photo: PhotoEntity)
    suspend fun emptyTrash()
    suspend fun cleanOldTrash(threshold: Long)
}

@Singleton
class TrashRepositoryImpl @Inject constructor(
    private val projectDao: ProjectDao,
    private val dailyLogDao: DailyLogDao,
    private val photoDao: PhotoDao,
    private val fileManager: FileManager,
    private val projectRepository: ProjectRepository
) : TrashRepository {

    override fun getDeletedProjects(): Flow<List<ProjectEntity>> = projectDao.getDeletedProjects()

    override fun getDeletedPhotos(): Flow<List<PhotoEntity>> = photoDao.getDeletedPhotos()

    override suspend fun restoreProjectById(projectId: Long) {
        projectDao.restoreProjectById(projectId)
        projectRepository.refreshWidgetData()
    }

    override suspend fun restorePhoto(id: Long) = photoDao.restorePhoto(id)

    override suspend fun hardDeleteProject(projectId: Long) {
        deleteProjectPermanently(projectId)
        projectRepository.refreshWidgetData()
    }

    override suspend fun hardDeletePhoto(photo: PhotoEntity) {
        deletePhotosPermanently(listOf(photo))
    }

    override suspend fun emptyTrash() {
        val projects = projectDao.getDeletedProjects().first()
        projects.forEach { project ->
            try {
                deleteProjectPermanently(project.id)
            } catch (e: Exception) {
                android.util.Log.e("TrashRepository", "Failed to permanently delete project ${project.id}", e)
            }
        }

        val photos = photoDao.getDeletedPhotos().first()
        try {
            deletePhotosPermanently(photos)
        } catch (e: Exception) {
            android.util.Log.e("TrashRepository", "Failed to permanently delete photos", e)
        }

        projectRepository.refreshWidgetData()
    }

    override suspend fun cleanOldTrash(threshold: Long) {
        val projects = projectDao.getDeletedProjects().first()
        projects.filter { (it.deletedAt ?: 0L) < threshold }.forEach { project ->
            try {
                deleteProjectPermanently(project.id)
            } catch (e: Exception) {
                android.util.Log.e("TrashRepository", "Failed to clean old project ${project.id}", e)
            }
        }

        val photos = photoDao.getDeletedPhotos().first()
        val oldPhotos = photos.filter { (it.deletedAt ?: 0L) < threshold }
        try {
            deletePhotosPermanently(oldPhotos)
        } catch (e: Exception) {
            android.util.Log.e("TrashRepository", "Failed to clean old photos", e)
        }
    }

    private suspend fun deleteProjectPermanently(projectId: Long) {
        val logsWithPhotos = dailyLogDao.getLogsWithPhotosForProjectSuspend(projectId)
        val allPhotos = logsWithPhotos.flatMap { it.photos }
        deletePhotosPermanently(allPhotos)
        projectDao.hardDeleteProjectById(projectId)
    }

    private suspend fun deletePhotosPermanently(photos: List<PhotoEntity>) {
        if (photos.isEmpty()) return
        photos.forEach { fileManager.deletePhysicalFile(it.filePath) }
        photoDao.hardDeletePhotosByIds(photos.map { it.id })
    }
}
