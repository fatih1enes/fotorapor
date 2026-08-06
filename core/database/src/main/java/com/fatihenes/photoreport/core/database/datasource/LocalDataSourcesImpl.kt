package com.fatihenes.photoreport.core.database.datasource

import com.fatihenes.photoreport.core.database.*
import com.fatihenes.photoreport.core.database.mapper.*
import com.fatihenes.photoreport.core.domain.datasource.*
import com.fatihenes.photoreport.core.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalProjectDataSourceImpl @Inject constructor(
    private val projectDao: ProjectDao
) : LocalProjectDataSource {
    override fun getAllProjects(): Flow<List<Project>> =
        projectDao.getAllProjects().map { list -> list.map { it.toDomain() } }

    override suspend fun insertProject(project: Project): Long =
        projectDao.insertProject(project.toEntity())

    override suspend fun softDeleteProjectById(id: Long, deletedAt: Long) =
        projectDao.softDeleteProjectById(id, deletedAt)

    override suspend fun restoreProjectById(id: Long) =
        projectDao.restoreProjectById(id)

    override suspend fun hardDeleteProjectById(id: Long) =
        projectDao.hardDeleteProjectById(id)

    override fun getDeletedProjects(): Flow<List<Project>> =
        projectDao.getDeletedProjects().map { list -> list.map { it.toDomain() } }

    override fun getProjectById(id: Long): Flow<Project?> =
        projectDao.getProjectById(id).map { it?.toDomain() }

    override suspend fun getProjectByIdSuspend(id: Long): Project? =
        projectDao.getProjectByIdSuspend(id)?.toDomain()

    override suspend fun getLatestProjectSuspend(): Project? =
        projectDao.getLatestProjectSuspend()?.toDomain()
}

@Singleton
class LocalLogDataSourceImpl @Inject constructor(
    private val dailyLogDao: DailyLogDao
) : LocalLogDataSource {
    override fun getLogsForProject(projectId: Long): Flow<List<DailyLog>> =
        dailyLogDao.getLogsForProject(projectId).map { list -> list.map { it.toDomain() } }

    override suspend fun getLogForDate(projectId: Long, date: Long): DailyLog? =
        dailyLogDao.getLogForDate(projectId, date)?.toDomain()

    override suspend fun insertLog(log: DailyLog): Long =
        dailyLogDao.insertLog(log.toEntity())

    override suspend fun updateNote(id: Long, note: String) =
        dailyLogDao.updateNote(id, note)

    override fun getLogsWithPhotosForProject(projectId: Long): Flow<List<DailyLogWithPhotos>> =
        dailyLogDao.getLogsWithPhotosForProject(projectId).map { list -> list.map { it.toDomain() } }

    override suspend fun getLogsWithPhotosForProjectSuspend(projectId: Long): List<DailyLogWithPhotos> =
        dailyLogDao.getLogsWithPhotosForProjectSuspend(projectId).map { it.toDomain() }
}

@Singleton
class LocalPhotoDataSourceImpl @Inject constructor(
    private val photoDao: PhotoDao
) : LocalPhotoDataSource {
    override fun getPhotosForLog(logId: Long): Flow<List<Photo>> =
        photoDao.getPhotosForLog(logId).map { list -> list.map { it.toDomain() } }

    override fun getPhotosForProject(projectId: Long): Flow<List<Photo>> =
        photoDao.getPhotosForProject(projectId).map { list -> list.map { it.toDomain() } }

    override suspend fun insertPhoto(photo: Photo): Long =
        photoDao.insertPhoto(photo.toEntity())

    override suspend fun softDeletePhoto(id: Long, deletedAt: Long) =
        photoDao.softDeletePhoto(id, deletedAt)

    override suspend fun restorePhoto(id: Long) =
        photoDao.restorePhoto(id)

    override suspend fun hardDeletePhotoById(id: Long) =
        photoDao.hardDeletePhotoById(id)

    override suspend fun hardDeletePhotosByIds(ids: List<Long>) =
        photoDao.hardDeletePhotosByIds(ids)

    override suspend fun updateRotation(id: Long, rotation: Float) =
        photoDao.updateRotation(id, rotation)

    override fun getDeletedPhotos(): Flow<List<Photo>> =
        photoDao.getDeletedPhotos().map { list -> list.map { it.toDomain() } }
}
