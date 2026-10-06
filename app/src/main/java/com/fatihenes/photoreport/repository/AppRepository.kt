package com.fatihenes.photoreport.repository

import com.fatihenes.photoreport.core.database.DailyLogEntity
import com.fatihenes.photoreport.core.database.LogWithPhotos
import com.fatihenes.photoreport.core.database.PhotoEntity
import com.fatihenes.photoreport.core.database.ProjectEntity
import com.fatihenes.photoreport.core.database.mapper.toDomain
import com.fatihenes.photoreport.core.database.mapper.toEntity
import com.fatihenes.photoreport.core.model.DailyLog
import com.fatihenes.photoreport.core.model.Photo
import com.fatihenes.photoreport.core.model.Project
import com.fatihenes.photoreport.core.domain.repository.PhotoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
    suspend fun insertPhoto(photo: PhotoEntity): Long
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
    fun processAndSavePhotoInBackground(uri: android.net.Uri, projectId: Long, logId: Long, enableWebp: Boolean, projectName: String, watermarkData: com.fatihenes.photoreport.core.model.WatermarkData? = null)
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

    override fun getPhotosForLog(logId: Long): Flow<List<PhotoEntity>> = photoRepository.getPhotosForLog(logId).map { it.map { it.toEntity() } }

    override fun getPhotosForProject(projectId: Long): Flow<List<PhotoEntity>> = photoRepository.getPhotosForProject(projectId).map { it.map { it.toEntity() } }

    override suspend fun insertPhoto(photo: PhotoEntity): Long = photoRepository.insertPhoto(photo.logId, photo.filePath)

    override suspend fun deletePhoto(photo: PhotoEntity) = photoRepository.deletePhoto(photo.toDomain())

    override suspend fun deletePhotosByIds(photoIds: List<Long>) = photoRepository.deletePhotosByIds(photoIds)

    override suspend fun updatePhotoRotation(id: Long, rotation: Float) = photoRepository.updatePhotoRotation(id, rotation)

    override fun getProjectById(projectId: Long): Flow<ProjectEntity?> = projectRepository.getProjectById(projectId)

    override suspend fun getProjectByIdSuspend(projectId: Long): ProjectEntity? = projectRepository.getProjectByIdSuspend(projectId)

    override suspend fun getLatestProjectSuspend(): ProjectEntity? = projectRepository.getLatestProjectSuspend()

    override fun getLogsWithPhotosForProjectFlow(projectId: Long): Flow<List<LogWithPhotos>> = logRepository.getLogsWithPhotosForProjectFlow(projectId)

    override suspend fun getLogsWithPhotosForProject(projectId: Long): List<LogWithPhotos> = logRepository.getLogsWithPhotosForProject(projectId)

    override suspend fun softDeletePhoto(photo: PhotoEntity) = photoRepository.softDeletePhoto(photo.toDomain())

    override suspend fun softDeletePhotos(photos: List<PhotoEntity>) = photoRepository.softDeletePhotos(photos.map { it.toDomain() })

    override fun processAndSavePhotoInBackground(uri: android.net.Uri, projectId: Long, logId: Long, enableWebp: Boolean, projectName: String, watermarkData: com.fatihenes.photoreport.core.model.WatermarkData?) {
        photoRepository.processAndSavePhotoInBackground(uri.toString(), projectId, logId, enableWebp, projectName, watermarkData)
    }
}
