package com.fatihenes.photoreport.repository.domain

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.fatihenes.photoreport.manager.FileManager
import com.fatihenes.photoreport.core.domain.datasource.LocalLogDataSource
import com.fatihenes.photoreport.core.domain.datasource.LocalPhotoDataSource
import com.fatihenes.photoreport.core.domain.datasource.LocalProjectDataSource
import com.fatihenes.photoreport.core.domain.repository.*
import com.fatihenes.photoreport.core.model.*
import com.fatihenes.photoreport.repository.SettingsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DomainProjectRepositoryImpl @Inject constructor(
    private val localProjectDataSource: LocalProjectDataSource
) : ProjectRepository {
    override fun getAllProjects(): Flow<List<Project>> = localProjectDataSource.getAllProjects()
    override suspend fun insertProject(name: String, colorHex: String): Long {
        return localProjectDataSource.insertProject(
            Project(name = name, colorHex = colorHex)
        )
    }
    override suspend fun deleteProjectById(projectId: Long) {
        localProjectDataSource.softDeleteProjectById(projectId, System.currentTimeMillis())
    }
    override fun getProjectById(projectId: Long): Flow<Project?> = localProjectDataSource.getProjectById(projectId)
    override suspend fun getProjectByIdSuspend(projectId: Long): Project? = localProjectDataSource.getProjectByIdSuspend(projectId)
    override suspend fun getLatestProjectSuspend(): Project? = localProjectDataSource.getLatestProjectSuspend()
}

@Singleton
class DomainTrashRepositoryImpl @Inject constructor(
    private val localProjectDataSource: LocalProjectDataSource,
    private val localPhotoDataSource: LocalPhotoDataSource,
    private val fileManager: FileManager
) : TrashRepository {
    override fun getDeletedProjects(): Flow<List<Project>> = localProjectDataSource.getDeletedProjects()
    override fun getDeletedPhotos(): Flow<List<Photo>> = localPhotoDataSource.getDeletedPhotos()
    override suspend fun restoreProjectById(projectId: Long) = localProjectDataSource.restoreProjectById(projectId)
    override suspend fun restorePhoto(id: Long) = localPhotoDataSource.restorePhoto(id)
    override suspend fun hardDeleteProject(projectId: Long) {
        localProjectDataSource.hardDeleteProjectById(projectId)
    }
    override suspend fun hardDeletePhoto(photo: Photo) {
        fileManager.deletePhysicalFile(photo.filePath)
        localPhotoDataSource.hardDeletePhotoById(photo.id)
    }
    override suspend fun emptyTrash() {
        localProjectDataSource.getDeletedProjects().first().forEach { project ->
            hardDeleteProject(project.id)
        }
        localPhotoDataSource.getDeletedPhotos().first().forEach { photo ->
            hardDeletePhoto(photo)
        }
    }
    override suspend fun cleanOldTrash(threshold: Long) {
        localProjectDataSource.getDeletedProjects().first().filter { (it.deletedAt ?: 0L) < threshold }.forEach {
            hardDeleteProject(it.id)
        }
        localPhotoDataSource.getDeletedPhotos().first().filter { (it.deletedAt ?: 0L) < threshold }.forEach {
            hardDeletePhoto(it)
        }
    }
}

@Singleton
class DomainLogRepositoryImpl @Inject constructor(
    private val localLogDataSource: LocalLogDataSource
) : LogRepository {
    override fun getLogsForProject(projectId: Long): Flow<List<DailyLog>> = localLogDataSource.getLogsForProject(projectId)
    override suspend fun getLogForDate(projectId: Long, date: Long): DailyLog? = localLogDataSource.getLogForDate(projectId, date)
    override suspend fun insertLog(projectId: Long, date: Long, note: String): Long {
        return localLogDataSource.insertLog(DailyLog(projectId = projectId, date = date, note = note))
    }
    override suspend fun updateNote(id: Long, note: String) = localLogDataSource.updateNote(id, note)
    override fun getLogsWithPhotosForProjectFlow(projectId: Long): Flow<List<DailyLogWithPhotos>> = localLogDataSource.getLogsWithPhotosForProject(projectId)
    override suspend fun getLogsWithPhotosForProject(projectId: Long): List<DailyLogWithPhotos> = localLogDataSource.getLogsWithPhotosForProjectSuspend(projectId)
}

@Singleton
class DomainPhotoRepositoryImpl @Inject constructor(
    private val localPhotoDataSource: LocalPhotoDataSource,
    private val legacyPhotoRepository: com.fatihenes.photoreport.repository.PhotoRepository
) : PhotoRepository {
    override fun getPhotosForLog(logId: Long): Flow<List<Photo>> = localPhotoDataSource.getPhotosForLog(logId)
    override fun getPhotosForProject(projectId: Long): Flow<List<Photo>> = localPhotoDataSource.getPhotosForProject(projectId)
    override suspend fun insertPhoto(logId: Long, filePath: String): Long {
        return localPhotoDataSource.insertPhoto(Photo(logId = logId, filePath = filePath))
    }
    override suspend fun deletePhoto(photo: Photo) = localPhotoDataSource.softDeletePhoto(photo.id, System.currentTimeMillis())
    override suspend fun deletePhotosByIds(photoIds: List<Long>) {
        val now = System.currentTimeMillis()
        photoIds.forEach { localPhotoDataSource.softDeletePhoto(it, now) }
    }
    override suspend fun updatePhotoRotation(id: Long, rotation: Float) = localPhotoDataSource.updateRotation(id, rotation)
    override suspend fun softDeletePhoto(photo: Photo) = localPhotoDataSource.softDeletePhoto(photo.id, System.currentTimeMillis())
    override suspend fun softDeletePhotos(photos: List<Photo>) {
        val now = System.currentTimeMillis()
        photos.forEach { localPhotoDataSource.softDeletePhoto(it.id, now) }
    }
    override fun processAndSavePhotoInBackground(uriString: String, projectId: Long, logId: Long, enableWebp: Boolean, projectName: String, watermarkData: WatermarkData?) {
        legacyPhotoRepository.processAndSavePhotoInBackground(uriString.toUri(), projectId, logId, enableWebp, projectName, watermarkData)
    }
}

@Singleton
class DomainBackupRepositoryImpl @Inject constructor(
    private val backupManager: com.fatihenes.photoreport.manager.BackupManager
) : BackupRepository {
    override fun createBackup(destUri: Uri) = backupManager.createBackup(destUri)
    override fun restoreBackup(sourceUri: Uri) = backupManager.restoreBackup(sourceUri)
}

@Singleton
class DomainReportRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ReportRepository {

    override suspend fun calculateFileSizes(photos: List<Photo>): FileSizeInfo = withContext(Dispatchers.IO) {
        var totalPhotoBytes = 0L
        var totalVideoBytes = 0L
        var photoCount = 0
        var videoCount = 0
        var estimatedQ100 = 0L
        var estimatedQ85 = 0L
        var estimatedQ75 = 0L

        val maxBytesQ100 = (1.5 * 1024 * 1024).toLong()
        val maxBytesQ85 = (0.4 * 1024 * 1024).toLong()
        val maxBytesQ75 = (0.15 * 1024 * 1024).toLong()

        photos.forEach { photo ->
            try {
                val uri = photo.filePath.toUri()
                val size = if (photo.filePath.startsWith("content://")) {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use {
                        it.statSize
                    } ?: 0L
                } else {
                    val path = if (photo.filePath.startsWith("file://")) uri.path else photo.filePath
                    path?.let { java.io.File(it).length() } ?: 0L
                }

                if (photo.filePath.endsWith(".mp4", ignoreCase = true)) {
                    totalVideoBytes += size
                    videoCount++
                } else {
                    totalPhotoBytes += size
                    photoCount++
                    estimatedQ100 += minOf(size, maxBytesQ100)
                    estimatedQ85 += minOf(size, maxBytesQ85)
                    estimatedQ75 += minOf(size, maxBytesQ75)
                }
            } catch (_: Exception) {}
        }

        FileSizeInfo(
            totalPhotoBytes,
            totalVideoBytes,
            photoCount,
            videoCount,
            estimatedQ100,
            estimatedQ85,
            estimatedQ75
        )
    }

    override fun enqueueExportWork(projectId: Long, projectName: String, format: String, quality: Int, language: String) {
        val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.fatihenes.photoreport.worker.ExportWorker>()
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.EXPONENTIAL,
                10,
                java.util.concurrent.TimeUnit.SECONDS
            )
            .setInputData(
                androidx.work.workDataOf(
                    "project_id" to projectId,
                    "project_name" to projectName,
                    "format" to format,
                    "quality" to quality,
                    "language" to language
                )
            )
            .build()

        val workManager = androidx.work.WorkManager.getInstance(context)
        workManager.enqueue(workRequest)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DomainRepositoryModule {
    @Binds
    abstract fun bindDomainProjectRepository(impl: DomainProjectRepositoryImpl): ProjectRepository

    @Binds
    abstract fun bindDomainTrashRepository(impl: DomainTrashRepositoryImpl): TrashRepository

    @Binds
    abstract fun bindDomainLogRepository(impl: DomainLogRepositoryImpl): LogRepository

    @Binds
    abstract fun bindDomainPhotoRepository(impl: DomainPhotoRepositoryImpl): PhotoRepository

    @Binds
    abstract fun bindDomainSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    abstract fun bindDomainBackupRepository(impl: DomainBackupRepositoryImpl): BackupRepository

    @Binds
    abstract fun bindDomainReportRepository(impl: DomainReportRepositoryImpl): ReportRepository
}
