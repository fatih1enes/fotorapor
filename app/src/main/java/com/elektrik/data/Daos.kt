package com.sarikaya.santiye.gunlugu.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE isDeleted = 0")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :projectId AND isDeleted = 0")
    fun getProjectById(projectId: Long): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE id = :projectId AND isDeleted = 0")
    suspend fun getProjectByIdSuspend(projectId: Long): ProjectEntity?

    @Query("SELECT * FROM projects WHERE isDeleted = 0 ORDER BY id DESC LIMIT 1")
    suspend fun getLatestProjectSuspend(): ProjectEntity?

    @Insert
    suspend fun insertProject(project: ProjectEntity): Long

    @Query("UPDATE projects SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :projectId")
    suspend fun softDeleteProjectById(projectId: Long, deletedAt: Long)

    @Query("SELECT * FROM projects WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedProjects(): Flow<List<ProjectEntity>>

    @Query("UPDATE projects SET isDeleted = 0, deletedAt = NULL WHERE id = :projectId")
    suspend fun restoreProjectById(projectId: Long)

    @Query("DELETE FROM projects WHERE isDeleted = 1 AND deletedAt < :threshold")
    suspend fun deleteOldProjects(threshold: Long)

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun hardDeleteProjectById(projectId: Long)
}

@Dao
interface DailyLogDao {
    @Query("SELECT * FROM daily_logs WHERE projectId = :projectId ORDER BY date DESC")
    fun getLogsForProject(projectId: Long): Flow<List<DailyLogEntity>>

    @Query("SELECT * FROM daily_logs WHERE projectId = :projectId ORDER BY date DESC")
    suspend fun getLogsForProjectSuspend(projectId: Long): List<DailyLogEntity>

    @Transaction
    @Query("SELECT * FROM daily_logs WHERE projectId = :projectId ORDER BY date DESC")
    fun getLogsWithPhotosForProject(projectId: Long): Flow<List<LogWithPhotos>>

    @Transaction
    @Query("SELECT * FROM daily_logs WHERE projectId = :projectId ORDER BY date DESC")
    suspend fun getLogsWithPhotosForProjectSuspend(projectId: Long): List<LogWithPhotos>

    @Query("SELECT * FROM daily_logs WHERE projectId = :projectId AND date = :date LIMIT 1")
    suspend fun getLogForDate(projectId: Long, date: Long): DailyLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DailyLogEntity): Long

    @Query("UPDATE daily_logs SET note = :note WHERE id = :id")
    suspend fun updateNote(id: Long, note: String)
}

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos WHERE logId = :logId AND isDeleted = 0 ORDER BY id DESC")
    fun getPhotosForLog(logId: Long): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE logId = :logId AND isDeleted = 0 ORDER BY id DESC")
    suspend fun getPhotosForLogSuspend(logId: Long): List<PhotoEntity>

    @Query("SELECT photos.* FROM photos INNER JOIN daily_logs ON photos.logId = daily_logs.id WHERE daily_logs.projectId = :projectId AND photos.isDeleted = 0 ORDER BY photos.id DESC")
    fun getPhotosForProject(projectId: Long): Flow<List<PhotoEntity>>

    @Insert
    suspend fun insertPhoto(photo: PhotoEntity)

    @Query("UPDATE photos SET rotation = :rotation WHERE id = :id")
    suspend fun updateRotation(id: Long, rotation: Float)

    @Query("UPDATE photos SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeletePhoto(id: Long, deletedAt: Long)

    @Query("SELECT * FROM photos WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedPhotos(): Flow<List<PhotoEntity>>

    @Query("UPDATE photos SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restorePhoto(id: Long)

    @Query("DELETE FROM photos WHERE isDeleted = 1 AND deletedAt < :threshold")
    suspend fun deleteOldPhotos(threshold: Long): Int

    @Delete
    suspend fun hardDeletePhoto(photo: PhotoEntity)

    @Query("DELETE FROM photos WHERE id = :id")
    suspend fun hardDeletePhotoById(id: Long)

    @Query("DELETE FROM photos WHERE id IN (:photoIds)")
    suspend fun hardDeletePhotosByIds(photoIds: List<Long>)
}
