package com.sarikaya.santiye.gunlugu.repository

import com.sarikaya.santiye.gunlugu.data.DailyLogDao
import com.sarikaya.santiye.gunlugu.data.PhotoDao
import com.sarikaya.santiye.gunlugu.data.PhotoEntity
import com.sarikaya.santiye.gunlugu.data.ProjectDao
import com.sarikaya.santiye.gunlugu.data.ProjectEntity
import com.sarikaya.santiye.gunlugu.manager.FileManager
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
        val logsWithPhotos = dailyLogDao.getLogsWithPhotosForProjectSuspend(projectId)
        val allPhotos = logsWithPhotos.flatMap { it.photos }
        for (photo in allPhotos) {
            fileManager.deletePhysicalFile(photo.filePath)
            photoDao.hardDeletePhotoById(photo.id)
        }
        projectDao.hardDeleteProjectById(projectId)
        projectRepository.refreshWidgetData()
    }

    override suspend fun hardDeletePhoto(photo: PhotoEntity) = withContext(Dispatchers.IO) {
        fileManager.deletePhysicalFile(photo.filePath)
        photoDao.hardDeletePhotoById(photo.id)
    }

    override suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        val deletedProjects = projectDao.getDeletedProjects().first()
        for (project in deletedProjects) {
            val logsWithPhotos = dailyLogDao.getLogsWithPhotosForProjectSuspend(project.id)
            val allPhotos = logsWithPhotos.flatMap { it.photos }
            for (photo in allPhotos) {
                fileManager.deletePhysicalFile(photo.filePath)
            }
            projectDao.hardDeleteProjectById(project.id)
        }

        val deletedPhotos = photoDao.getDeletedPhotos().first()
        for (photo in deletedPhotos) {
            fileManager.deletePhysicalFile(photo.filePath)
            photoDao.hardDeletePhotoById(photo.id)
        }

        projectRepository.refreshWidgetData()
    }

    override suspend fun cleanOldTrash(threshold: Long) = withContext(Dispatchers.IO) {
        val deletedProjects = projectDao.getDeletedProjects().first()
        for (project in deletedProjects) {
            if (project.deletedAt != null && project.deletedAt < threshold) {
                val logsWithPhotos = dailyLogDao.getLogsWithPhotosForProjectSuspend(project.id)
                val allPhotos = logsWithPhotos.flatMap { it.photos }
                for (photo in allPhotos) {
                    fileManager.deletePhysicalFile(photo.filePath)
                }
                projectDao.hardDeleteProjectById(project.id)
            }
        }

        val deletedPhotos = photoDao.getDeletedPhotos().first()
        for (photo in deletedPhotos) {
            if (photo.deletedAt != null && photo.deletedAt < threshold) {
                fileManager.deletePhysicalFile(photo.filePath)
                photoDao.hardDeletePhotoById(photo.id)
            }
        }
    }
}
