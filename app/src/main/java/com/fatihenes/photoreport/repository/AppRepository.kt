package com.fatihenes.photoreport.repository

import com.fatihenes.photoreport.data.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface AppRepository {
    fun getAllProjects(): Flow<List<ProjectEntity>>
    suspend fun insertProject(project: ProjectEntity): Long
    suspend fun deleteProjectById(projectId: Long)
    fun getDeletedProjects(): Flow<List<ProjectEntity>>
    fun getDeletedPhotos(): Flow<List<PhotoEntity>>
    suspend fun restoreProjectById(projectId: Long)
    suspend fun restorePhoto(id: Long)
    suspend fun hardDeleteProject(projectId: Long)
    suspend fun hardDeletePhoto(photo: PhotoEntity)
    suspend fun emptyTrash()
    suspend fun cleanOldTrash(threshold: Long)
    fun getLogsForProject(projectId: Long): Flow<List<DailyLogEntity>>
    suspend fun getLogForDate(projectId: Long, date: Long): DailyLogEntity?
    suspend fun insertLog(log: DailyLogEntity): Long
    suspend fun updateNote(id: Long, note: String)
    fun getPhotosForLog(logId: Long): Flow<List<PhotoEntity>>
    fun getPhotosForProject(projectId: Long): Flow<List<PhotoEntity>>
    suspend fun insertPhoto(photo: PhotoEntity)
    suspend fun deletePhoto(photo: PhotoEntity)
    suspend fun deletePhotosByIds(photoIds: List<Long>)
    suspend fun updatePhotoRotation(id: Long, rotation: Float)
    fun getProjectById(projectId: Long): Flow<ProjectEntity?>
    suspend fun getProjectByIdSuspend(projectId: Long): ProjectEntity?
    suspend fun getLatestProjectSuspend(): ProjectEntity?
    fun getLogsWithPhotosForProjectFlow(projectId: Long): Flow<List<LogWithPhotos>>
    suspend fun getLogsWithPhotosForProject(projectId: Long): List<LogWithPhotos>
    suspend fun softDeletePhoto(photo: PhotoEntity)
    suspend fun softDeletePhotos(photos: List<PhotoEntity>)
    fun processAndSavePhotoInBackground(uri: android.net.Uri, projectId: Long, logId: Long, enableWebp: Boolean, projectName: String, watermarkData: com.fatihenes.photoreport.util.WatermarkData? = null)
}

/**
 * Composite Facade Repository delegating domain tasks to specialized domain repositories:
 * ProjectRepository, LogRepository, PhotoRepository, TrashRepository.
 */
@Singleton
class AppRepositoryImpl @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val logRepository: LogRepository,
    private val photoRepository: PhotoRepository,
    private val trashRepository: TrashRepository
) : AppRepository {

    override fun getAllProjects(): Flow<List<ProjectEntity>> = projectRepository.getAllProjects()

    override suspend fun insertProject(project: ProjectEntity): Long = projectRepository.insertProject(project)

    override suspend fun deleteProjectById(projectId: Long) = projectRepository.deleteProjectById(projectId)

    override fun getDeletedProjects(): Flow<List<ProjectEntity>> = trashRepository.getDeletedProjects()

    override fun getDeletedPhotos(): Flow<List<PhotoEntity>> = trashRepository.getDeletedPhotos()

    override suspend fun restoreProjectById(projectId: Long) = trashRepository.restoreProjectById(projectId)

    override suspend fun restorePhoto(id: Long) = trashRepository.restorePhoto(id)

    override suspend fun hardDeleteProject(projectId: Long) = trashRepository.hardDeleteProject(projectId)

    override suspend fun hardDeletePhoto(photo: PhotoEntity) = trashRepository.hardDeletePhoto(photo)

    override suspend fun emptyTrash() = trashRepository.emptyTrash()

    override suspend fun cleanOldTrash(threshold: Long) = trashRepository.cleanOldTrash(threshold)

    override fun getLogsForProject(projectId: Long): Flow<List<DailyLogEntity>> = logRepository.getLogsForProject(projectId)

    override suspend fun getLogForDate(projectId: Long, date: Long): DailyLogEntity? = logRepository.getLogForDate(projectId, date)

    override suspend fun insertLog(log: DailyLogEntity): Long = logRepository.insertLog(log)

    override suspend fun updateNote(id: Long, note: String) = logRepository.updateNote(id, note)

    override fun getPhotosForLog(logId: Long): Flow<List<PhotoEntity>> = photoRepository.getPhotosForLog(logId)

    override fun getPhotosForProject(projectId: Long): Flow<List<PhotoEntity>> = photoRepository.getPhotosForProject(projectId)

    override suspend fun insertPhoto(photo: PhotoEntity) = photoRepository.insertPhoto(photo)

    override suspend fun deletePhoto(photo: PhotoEntity) = photoRepository.deletePhoto(photo)

    override suspend fun deletePhotosByIds(photoIds: List<Long>) = photoRepository.deletePhotosByIds(photoIds)

    override suspend fun updatePhotoRotation(id: Long, rotation: Float) = photoRepository.updatePhotoRotation(id, rotation)

    override fun getProjectById(projectId: Long): Flow<ProjectEntity?> = projectRepository.getProjectById(projectId)

    override suspend fun getProjectByIdSuspend(projectId: Long): ProjectEntity? = projectRepository.getProjectByIdSuspend(projectId)

    override suspend fun getLatestProjectSuspend(): ProjectEntity? = projectRepository.getLatestProjectSuspend()

    override fun getLogsWithPhotosForProjectFlow(projectId: Long): Flow<List<LogWithPhotos>> = logRepository.getLogsWithPhotosForProjectFlow(projectId)

    override suspend fun getLogsWithPhotosForProject(projectId: Long): List<LogWithPhotos> = logRepository.getLogsWithPhotosForProject(projectId)

    override suspend fun softDeletePhoto(photo: PhotoEntity) = photoRepository.softDeletePhoto(photo)

    override suspend fun softDeletePhotos(photos: List<PhotoEntity>) = photoRepository.softDeletePhotos(photos)

    override fun processAndSavePhotoInBackground(uri: android.net.Uri, projectId: Long, logId: Long, enableWebp: Boolean, projectName: String, watermarkData: com.fatihenes.photoreport.util.WatermarkData?) {
        photoRepository.processAndSavePhotoInBackground(uri, projectId, logId, enableWebp, projectName, watermarkData)
    }
}
