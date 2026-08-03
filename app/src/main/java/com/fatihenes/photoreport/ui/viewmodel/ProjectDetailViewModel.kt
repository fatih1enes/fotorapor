package com.fatihenes.photoreport.ui.viewmodel

import android.content.Context
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.fatihenes.photoreport.data.DailyLogEntity
import com.fatihenes.photoreport.data.PhotoEntity
import com.fatihenes.photoreport.repository.AppRepository
import com.fatihenes.photoreport.repository.ReportRepository
import com.fatihenes.photoreport.util.groupBy
import com.fatihenes.photoreport.worker.ExportWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    private val repository: AppRepository,
    private val reportRepository: ReportRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val projectIdFlow = savedStateHandle.getStateFlow<Long?>("projectId", null)

    fun setProjectId(id: Long) {
        savedStateHandle["projectId"] = id
    }

    private val _fileSizeInfo = MutableStateFlow<FileSizeInfo?>(null)
    val fileSizeInfo: StateFlow<FileSizeInfo?> = _fileSizeInfo

    fun calculateFileSizes(photos: List<PhotoEntity>) {
        viewModelScope.launch {
            _fileSizeInfo.value = reportRepository.calculateFileSizes(photos)
        }
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

    val allProjectPhotos = currentProjectLogs.map { logs ->
        logs.flatMap { it.photos }.sortedByDescending { it.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // --- Note Update Debouncing ---
    private val _noteUpdates = MutableSharedFlow<Pair<Long, String>>(extraBufferCapacity = 10)

    init {
        observeNoteUpdates()
    }

    private fun observeNoteUpdates() {
        viewModelScope.launch {
            @OptIn(kotlinx.coroutines.FlowPreview::class)
            _noteUpdates
                .groupBy { it.first } // Group by logId for independent debouncing
                .collect { groupedFlow ->
                    launch {
                        groupedFlow
                            .debounce(400.milliseconds)
                            .distinctUntilChanged()
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

    fun exportProject(context: Context, projectId: Long, projectName: String, format: String, quality: Int, language: String) {
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
                    "quality" to quality,
                    "language" to language
                )
            )
            .build()

        val workManager = WorkManager.getInstance(context)
        workManager.enqueue(workRequest)

    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            repository.deleteProjectById(projectId)
        }
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
