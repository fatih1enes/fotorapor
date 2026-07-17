package com.sarikaya.santiye.gunlugu.ui.viewmodel

import android.content.Context
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.sarikaya.santiye.gunlugu.data.DailyLogEntity
import com.sarikaya.santiye.gunlugu.data.PhotoEntity
import com.sarikaya.santiye.gunlugu.repository.AppRepository
import com.sarikaya.santiye.gunlugu.util.DateUtils
import com.sarikaya.santiye.gunlugu.util.groupBy
import com.sarikaya.santiye.gunlugu.worker.ExportWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val repository: AppRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val projectIdFlow = savedStateHandle.getStateFlow<Long?>("projectId", null)

    fun setProjectId(id: Long) {
        savedStateHandle["projectId"] = id
    }

    private val _fileSizeInfo = MutableStateFlow<FileSizeInfo?>(null)
    val fileSizeInfo: StateFlow<FileSizeInfo?> = _fileSizeInfo

    fun calculateFileSizes(photos: List<PhotoEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            _fileSizeInfo.value = calculateFileSizesInternal(appContext, photos)
        }
    }

    private val _exportState = MutableStateFlow<UiState<Unit>?>(null)
    val exportState: StateFlow<UiState<Unit>?> = _exportState

    fun resetExportState() {
        _exportState.value = null
    }

    val selectedProject = projectIdFlow.flatMapLatest { id ->
        if (id == null) flowOf(null)
        else repository.getProjectById(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Optimized: Using Room Relation natively to prevent UI glitches and N+1 queries
    val currentProjectLogs = projectIdFlow.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else repository.getLogsWithPhotosForProjectFlow(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Note Update Debouncing ---
    private val _noteUpdates = MutableSharedFlow<Pair<Long, String>>(extraBufferCapacity = 10)

    init {
        // Collect note updates, debounce them by logId, and save to DB safely
        viewModelScope.launch {
            @OptIn(kotlinx.coroutines.FlowPreview::class)
            _noteUpdates
                .groupBy { it.first } // Group by logId
                .collect { groupedFlow ->
                    launch {
                        groupedFlow
                            .debounce(400.milliseconds) // Debounce for each log independently
                            .collect { (logId, note) ->
                                repository.updateNote(logId, note)
                            }
                    }
                }
        }
    }




    fun updateNote(logId: Long, note: String) {
        val success = _noteUpdates.tryEmit(logId to note)
        if (!success) {
            // Fallback if buffer is full, though unlikely
            viewModelScope.launch { repository.updateNote(logId, note) }
        }
    }

    // Mutex for safe concurrent log creation
    private val logCreationMutex = Mutex()

    fun addPhotoToToday(projectId: Long, filePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val today = DateUtils.getStartOfDayEpochMillis()
            // 3. Race Condition Önlemi: Aynı anda çoklu tıklamada mükerrer gün oluşumunu Mutex ile engelle
            val logId = logCreationMutex.withLock {
                val log = repository.getLogForDate(projectId, today)
                log?.id ?: repository.insertLog(
                    DailyLogEntity(projectId = projectId, date = today, note = "")
                )
            }
            repository.insertPhoto(PhotoEntity(logId = logId, filePath = filePath))
        }
    }


    fun addPhotoToLog(logId: Long, filePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertPhoto(PhotoEntity(logId = logId, filePath = filePath))
        }
    }

    fun addLogForDate(projectId: Long, date: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.getLogForDate(projectId, date)
            if (existing == null) {
                repository.insertLog(
                    DailyLogEntity(projectId = projectId, date = date, note = "")
                )
            }
        }
    }

    fun deletePhoto(photo: PhotoEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.softDeletePhoto(photo)
        }
    }

    fun deletePhotos(photoIds: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            val photosToDelete = currentProjectLogs.value
                .flatMap { it.photos }
                .filter { it.id in photoIds }
            
            repository.softDeletePhotos(photosToDelete)
        }
    }

    fun updatePhotoRotation(photoId: Long, rotation: Float) {
        viewModelScope.launch {
            repository.updatePhotoRotation(photoId, rotation)
        }
    }

    fun exportProject(context: Context, projectId: Long, projectName: String, format: String, quality: Int) {
        val workRequest = OneTimeWorkRequestBuilder<ExportWorker>()
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.EXPONENTIAL,
                10,
                java.util.concurrent.TimeUnit.SECONDS
            )
            .setInputData(
                workDataOf(
                    "project_id" to projectId,
                    "project_name" to projectName,
                    "format" to format,
                    "quality" to quality
                )
            )
            .build()
            
        val workManager = WorkManager.getInstance(context)
        workManager.enqueue(workRequest)
        
        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(workRequest.id).collect { workInfo ->
                if (workInfo != null) {
                    when (workInfo.state) {
                        androidx.work.WorkInfo.State.ENQUEUED, androidx.work.WorkInfo.State.RUNNING -> {
                            _exportState.value = UiState.Loading
                        }
                        androidx.work.WorkInfo.State.SUCCEEDED -> {
                            _exportState.value = UiState.Success(Unit)
                        }
                        androidx.work.WorkInfo.State.FAILED -> {
                            val errorMsg = workInfo.outputData.getString("error") ?: context.getString(com.sarikaya.santiye.gunlugu.R.string.error_unknown)
                            _exportState.value = UiState.Error(errorMsg)
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            repository.deleteProjectById(projectId)
        }
    }

    private fun calculateFileSizesInternal(context: Context, photos: List<PhotoEntity>): FileSizeInfo {
        var totalPhotoBytes = 0L
        var totalVideoBytes = 0L
        var photoCount = 0
        var videoCount = 0

        var estimatedQ100 = 0L
        var estimatedQ85 = 0L
        var estimatedQ75 = 0L

        val maxBytesQ100 = (1.5 * 1024 * 1024).toLong() // 1.5 MB max for Q100 downsampled
        val maxBytesQ85 = (0.4 * 1024 * 1024).toLong()  // 400 KB max for Q85 downsampled
        val maxBytesQ75 = (0.15 * 1024 * 1024).toLong() // 150 KB max for Q75 downsampled

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

        return FileSizeInfo(
            totalPhotoBytes, 
            totalVideoBytes, 
            photoCount, 
            videoCount,
            estimatedQ100,
            estimatedQ85,
            estimatedQ75
        )
    }
}

data class FileSizeInfo(
    val totalPhotoBytes: Long,
    val totalVideoBytes: Long,
    val photoCount: Int,
    val videoCount: Int,
    val estimatedQ100Bytes: Long,
    val estimatedQ85Bytes: Long,
    val estimatedQ75Bytes: Long
)
