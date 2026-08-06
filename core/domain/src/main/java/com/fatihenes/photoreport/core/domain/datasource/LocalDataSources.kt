package com.fatihenes.photoreport.core.domain.datasource

import com.fatihenes.photoreport.core.model.AppSettings
import com.fatihenes.photoreport.core.model.DailyLog
import com.fatihenes.photoreport.core.model.DailyLogWithPhotos
import com.fatihenes.photoreport.core.model.Photo
import com.fatihenes.photoreport.core.model.Project
import kotlinx.coroutines.flow.Flow

interface LocalProjectDataSource {
    fun getAllProjects(): Flow<List<Project>>
    suspend fun insertProject(project: Project): Long
    suspend fun softDeleteProjectById(id: Long, deletedAt: Long)
    suspend fun restoreProjectById(id: Long)
    suspend fun hardDeleteProjectById(id: Long)
    fun getDeletedProjects(): Flow<List<Project>>
    fun getProjectById(id: Long): Flow<Project?>
    suspend fun getProjectByIdSuspend(id: Long): Project?
    suspend fun getLatestProjectSuspend(): Project?
}

interface LocalLogDataSource {
    fun getLogsForProject(projectId: Long): Flow<List<DailyLog>>
    suspend fun getLogForDate(projectId: Long, date: Long): DailyLog?
    suspend fun insertLog(log: DailyLog): Long
    suspend fun updateNote(id: Long, note: String)
    fun getLogsWithPhotosForProject(projectId: Long): Flow<List<DailyLogWithPhotos>>
    suspend fun getLogsWithPhotosForProjectSuspend(projectId: Long): List<DailyLogWithPhotos>
}

interface LocalPhotoDataSource {
    fun getPhotosForLog(logId: Long): Flow<List<Photo>>
    fun getPhotosForProject(projectId: Long): Flow<List<Photo>>
    suspend fun insertPhoto(photo: Photo): Long
    suspend fun softDeletePhoto(id: Long, deletedAt: Long)
    suspend fun restorePhoto(id: Long)
    suspend fun hardDeletePhotoById(id: Long)
    suspend fun hardDeletePhotosByIds(ids: List<Long>)
    suspend fun updateRotation(id: Long, rotation: Float)
    fun getDeletedPhotos(): Flow<List<Photo>>
}

interface LocalSettingsDataSource {
    val settings: Flow<AppSettings>
    suspend fun setThemeMode(mode: String)
    suspend fun setLanguage(lang: String)
    suspend fun setCameraOptimization(enabled: Boolean)
    suspend fun setAvifEnabled(enabled: Boolean)
    suspend fun setGpsWatermarkEnabled(enabled: Boolean)
    suspend fun setDisclosureShown(shown: Boolean)
    suspend fun importSettings(newSettings: Map<String, Any>)
}
