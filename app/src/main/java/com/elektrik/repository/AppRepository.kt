package com.elektrik.repository

import com.elektrik.data.*
import kotlinx.coroutines.flow.Flow
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
    suspend fun hardDeleteProject(context: android.content.Context, projectId: Long)
    suspend fun hardDeletePhoto(context: android.content.Context, photo: PhotoEntity)
    suspend fun emptyTrash()
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
    suspend fun deletePhotoWithFile(photo: PhotoEntity)
    suspend fun deletePhotosWithFiles(photos: List<PhotoEntity>)
    fun processAndSavePhotoInBackground(uri: android.net.Uri, projectId: Long, logId: Long, enableWebp: Boolean, projectName: String)
}

@Singleton
class AppRepositoryImpl @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context,
    private val projectDao: ProjectDao,
    private val dailyLogDao: DailyLogDao,
    private val photoDao: PhotoDao
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

    override suspend fun hardDeleteProject(context: android.content.Context, projectId: Long) {
        val logsWithPhotos = dailyLogDao.getLogsWithPhotosForProjectSuspend(projectId)
        val allPhotos = logsWithPhotos.flatMap { it.photos }
        for (photo in allPhotos) {
            deletePhysicalFile(context, photo.filePath)
            photoDao.hardDeletePhotoById(photo.id) // Hard delete photo from DB
        }
        projectDao.hardDeleteProjectById(projectId)
        refreshWidgetData()
    }

    override suspend fun hardDeletePhoto(context: android.content.Context, photo: PhotoEntity) {
        deletePhysicalFile(context, photo.filePath)
        photoDao.hardDeletePhotoById(photo.id)
    }

    override suspend fun emptyTrash() {
        // Find old projects

        // We will just do a simple empty trash for now, let's say it deletes EVERYTHING in trash regardless of 30 days.
        // Actually user clicks "Empty Trash". So delete all where isDeleted = 1.
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
    private suspend fun refreshWidgetData() {
        val latest = projectDao.getLatestProjectSuspend()
        if (latest != null) {
            com.elektrik.widget.WidgetDataHelper.saveLatestProject(appContext, latest.id, latest.name)
        } else {
            com.elektrik.widget.WidgetDataHelper.saveLatestProject(appContext, -1L, "Proje Yok")
        }
    }

    override fun getLogsWithPhotosForProjectFlow(projectId: Long): Flow<List<LogWithPhotos>> {
        return dailyLogDao.getLogsWithPhotosForProject(projectId)
    }

    override suspend fun getLogsWithPhotosForProject(projectId: Long): List<LogWithPhotos> {
        return dailyLogDao.getLogsWithPhotosForProjectSuspend(projectId)
    }

    override suspend fun deletePhotoWithFile(photo: PhotoEntity) {
        // Soft Delete
        photoDao.softDeletePhoto(photo.id, System.currentTimeMillis())
    }

    override suspend fun deletePhotosWithFiles(photos: List<PhotoEntity>) {
        val now = System.currentTimeMillis()
        for (photo in photos) {
            photoDao.softDeletePhoto(photo.id, now)
        }
    }

    private fun deletePhysicalFile(context: android.content.Context, filePath: String): Boolean {
        return try {
            val uri = android.net.Uri.parse(filePath)
            if (uri.scheme == "content") {
                context.contentResolver.delete(uri, null, null) > 0
            } else {
                val path = uri.path ?: filePath
                val file = java.io.File(path)
                if (file.exists()) file.delete() else true
            }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Physical file deletion failed: $filePath", e)
            false
        }
    }

    private val logCreationMutex = kotlinx.coroutines.sync.Mutex()

    override fun processAndSavePhotoInBackground(uri: android.net.Uri, projectId: Long, logId: Long, enableWebp: Boolean, projectName: String) {
        applicationScope.launch {
            try {
                var finalUri = uri

                if (enableWebp && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    var bitmapToCompress: android.graphics.Bitmap? = null
                    appContext.contentResolver.openInputStream(uri)?.use { input ->
                        bitmapToCompress = android.graphics.BitmapFactory.decodeStream(input)
                    }

                    if (bitmapToCompress != null) {
                        var orientation = 0
                        var dateTime = ""
                        appContext.contentResolver.openInputStream(uri)?.use { input ->
                            val oldExif = androidx.exifinterface.media.ExifInterface(input)
                            orientation = oldExif.getAttributeInt(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION, 0)
                            dateTime = oldExif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME_ORIGINAL) ?: ""
                        }

                        val contentValues = android.content.ContentValues().apply {
                            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.webp")
                            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/webp")
                            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DCIM + "/Elektrik")
                        }

                        val webpUri = appContext.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                        if (webpUri != null) {
                            appContext.contentResolver.openOutputStream(webpUri)?.use { out ->
                                bitmapToCompress?.compress(android.graphics.Bitmap.CompressFormat.WEBP_LOSSY, 100, out)
                            }

                            appContext.contentResolver.openFileDescriptor(webpUri, "rw")?.use { rwPfd ->
                                if (rwPfd != null) {
                                    val newExif = androidx.exifinterface.media.ExifInterface(rwPfd.fileDescriptor)
                                    newExif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION, orientation.toString())
                                    if (dateTime.isNotEmpty()) {
                                        newExif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME_ORIGINAL, dateTime)
                                    }
                                    if (projectName.isNotBlank()) {
                                        newExif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_USER_COMMENT, projectName)
                                    }
                                    newExif.saveAttributes()
                                }
                            }
                            appContext.contentResolver.delete(uri, null, null)
                            finalUri = webpUri
                        }
                    }
                }

                val targetLogId = if (logId != -1L) {
                    logId
                } else {
                    val today = com.elektrik.util.DateUtils.getStartOfDayEpochMillis()
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
