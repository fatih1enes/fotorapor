package com.fatihenes.photoreport.repository

import com.fatihenes.photoreport.core.database.*
import com.fatihenes.photoreport.manager.FileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
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

    override suspend fun hardDeleteProject(projectId: Long) = withContext(Dispatchers.IO) {
        deleteProjectPermanently(projectId)
        projectRepository.refreshWidgetData()
    }

    override suspend fun hardDeletePhoto(photo: PhotoEntity) = withContext(Dispatchers.IO) {
        deletePhotosPermanently(listOf(photo))
    }

    override suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        val projects = projectDao.getDeletedProjects().first()
        projects.forEach { deleteProjectPermanently(it.id) }

        val photos = photoDao.getDeletedPhotos().first()
        deletePhotosPermanently(photos)

        projectRepository.refreshWidgetData()
    }

    override suspend fun cleanOldTrash(threshold: Long) = withContext(Dispatchers.IO) {
        val projects = projectDao.getDeletedProjects().first()
        projects.filter { (it.deletedAt ?: 0L) < threshold }.forEach { deleteProjectPermanently(it.id) }

        val photos = photoDao.getDeletedPhotos().first()
        val oldPhotos = photos.filter { (it.deletedAt ?: 0L) < threshold }
        deletePhotosPermanently(oldPhotos)
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
