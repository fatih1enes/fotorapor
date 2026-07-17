package com.sarikaya.santiye.gunlugu.repository

import android.content.Context
import androidx.core.net.toUri
import com.sarikaya.santiye.gunlugu.data.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
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
    fun processAndSavePhotoInBackground(uri: android.net.Uri, projectId: Long, logId: Long, enableWebp: Boolean, projectName: String)
}

@Singleton
class AppRepositoryImpl @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val projectDao: ProjectDao,
    private val dailyLogDao: DailyLogDao,
    private val photoDao: PhotoDao,
    private val mediaProcessor: com.sarikaya.santiye.gunlugu.util.MediaProcessor,
    private val fileManager: com.sarikaya.santiye.gunlugu.manager.FileManager
) : AppRepository {

    private val applicationScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)

    override fun getAllProjects(): Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    override suspend fun insertProject(project: ProjectEntity): Long {
        val id = projectDao.insertProject(project)
        refreshWidgetData()
        return id
    }

    override suspend fun deleteProjectById(projectId: Long) {
        // Soft delete: Do not delete physical files yet.
        projectDao.softDeleteProjectById(projectId, System.currentTimeMillis())
        refreshWidgetData()
    }

    override fun getDeletedProjects(): Flow<List<ProjectEntity>> = projectDao.getDeletedProjects()
    override fun getDeletedPhotos(): Flow<List<PhotoEntity>> = photoDao.getDeletedPhotos()
    
    override suspend fun restoreProjectById(projectId: Long) {
        projectDao.restoreProjectById(projectId)
        refreshWidgetData()
    }
    override suspend fun restorePhoto(id: Long) = photoDao.restorePhoto(id)

    override suspend fun hardDeleteProject(projectId: Long) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val logsWithPhotos = dailyLogDao.getLogsWithPhotosForProjectSuspend(projectId)
        val allPhotos = logsWithPhotos.flatMap { it.photos }
        for (photo in allPhotos) {
            fileManager.deletePhysicalFile(photo.filePath)
            photoDao.hardDeletePhotoById(photo.id) // Hard delete photo from DB
        }
        projectDao.hardDeleteProjectById(projectId)
        refreshWidgetData()
    }

    override suspend fun hardDeletePhoto(photo: PhotoEntity) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        fileManager.deletePhysicalFile(photo.filePath)
        photoDao.hardDeletePhotoById(photo.id)
    }

    override suspend fun emptyTrash() = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        // 1. Önce isDeleted = 1 olan tüm projeleri bul
        val deletedProjects = projectDao.getDeletedProjects().first()
        
        for (project in deletedProjects) {
            // 2. Her projenin tüm fotoğraf dosyalarını deletePhysicalFile() ile sil
            val logsWithPhotos = dailyLogDao.getLogsWithPhotosForProjectSuspend(project.id)
            val allPhotos = logsWithPhotos.flatMap { it.photos }
            for (photo in allPhotos) {
                fileManager.deletePhysicalFile(photo.filePath)
            }
            // 3. projectDao.hardDeleteProjectById() ile DB'den sil
            projectDao.hardDeleteProjectById(project.id)
        }
        
        // 4. Ardından logla ilişkili olmayan orphan isDeleted fotoğrafları bul ve temizle
        val deletedPhotos = photoDao.getDeletedPhotos().first()
        for (photo in deletedPhotos) {
            fileManager.deletePhysicalFile(photo.filePath)
            photoDao.hardDeletePhotoById(photo.id)
        }

        // 5. refreshWidgetData() çağır
        refreshWidgetData()
    }

    override suspend fun cleanOldTrash(threshold: Long) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
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

    override fun getLogsForProject(projectId: Long): Flow<List<DailyLogEntity>> = dailyLogDao.getLogsForProject(projectId)

    override suspend fun getLogForDate(projectId: Long, date: Long): DailyLogEntity? = dailyLogDao.getLogForDate(projectId, date)

    override suspend fun insertLog(log: DailyLogEntity): Long = dailyLogDao.insertLog(log)

    override suspend fun updateNote(id: Long, note: String) = dailyLogDao.updateNote(id, note)

    override fun getPhotosForLog(logId: Long): Flow<List<PhotoEntity>> = photoDao.getPhotosForLog(logId)

    override fun getPhotosForProject(projectId: Long): Flow<List<PhotoEntity>> = photoDao.getPhotosForProject(projectId)

    override suspend fun insertPhoto(photo: PhotoEntity) = photoDao.insertPhoto(photo)

    override suspend fun deletePhoto(photo: PhotoEntity) = photoDao.softDeletePhoto(photo.id, System.currentTimeMillis())

    override suspend fun deletePhotosByIds(photoIds: List<Long>) {
        val now = System.currentTimeMillis()
        for (id in photoIds) {
            photoDao.softDeletePhoto(id, now)
        }
    }

    override suspend fun updatePhotoRotation(id: Long, rotation: Float) = photoDao.updateRotation(id, rotation)
    
    override fun getProjectById(projectId: Long): Flow<ProjectEntity?> = projectDao.getProjectById(projectId)

    override suspend fun getProjectByIdSuspend(projectId: Long): ProjectEntity? = projectDao.getProjectByIdSuspend(projectId)
    
    override suspend fun getLatestProjectSuspend(): ProjectEntity? = projectDao.getLatestProjectSuspend()

    /** Fetch latest project from DB and push to SharedPreferences for the widget. */
    private suspend fun refreshWidgetData() = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val latest = projectDao.getLatestProjectSuspend()
        if (latest != null) {
            com.sarikaya.santiye.gunlugu.widget.WidgetDataHelper.saveLatestProject(appContext, latest.id, latest.name)
        } else {
            com.sarikaya.santiye.gunlugu.widget.WidgetDataHelper.saveLatestProject(appContext, -1L, "Proje Yok")
        }
    }

    override fun getLogsWithPhotosForProjectFlow(projectId: Long): Flow<List<LogWithPhotos>> {
        return dailyLogDao.getLogsWithPhotosForProject(projectId)
    }

    override suspend fun getLogsWithPhotosForProject(projectId: Long): List<LogWithPhotos> {
        return dailyLogDao.getLogsWithPhotosForProjectSuspend(projectId)
    }

    override suspend fun softDeletePhoto(photo: PhotoEntity) {
        // Soft Delete
        photoDao.softDeletePhoto(photo.id, System.currentTimeMillis())
    }

    override suspend fun softDeletePhotos(photos: List<PhotoEntity>) {
        val now = System.currentTimeMillis()
        for (photo in photos) {
            photoDao.softDeletePhoto(photo.id, now)
        }
    }



    private val logCreationMutex = kotlinx.coroutines.sync.Mutex()

    override fun processAndSavePhotoInBackground(uri: android.net.Uri, projectId: Long, logId: Long, enableWebp: Boolean, projectName: String) {
        applicationScope.launch {
            try {
                val finalUri = mediaProcessor.processAndOptimize(uri, enableWebp, projectName)

                val targetLogId = if (logId != -1L) {
                    logId
                } else {
                    val today = com.sarikaya.santiye.gunlugu.util.DateUtils.getStartOfDayEpochMillis()
                    logCreationMutex.withLock {
                        val log = dailyLogDao.getLogForDate(projectId, today)
                        log?.id ?: dailyLogDao.insertLog(
                            DailyLogEntity(projectId = projectId, date = today, note = "")
                        )
                    }
                }

                photoDao.insertPhoto(PhotoEntity(logId = targetLogId, filePath = finalUri.toString()))
            } catch (e: Exception) {
                android.util.Log.e("AppRepository", "Background photo save failed", e)
            }
        }
    }
}
