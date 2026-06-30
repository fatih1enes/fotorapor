package com.elektrik.ui.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.elektrik.data.DailyLogEntity
import com.elektrik.data.PhotoEntity
import com.elektrik.repository.AppRepository
import com.elektrik.util.DateUtils
import com.elektrik.worker.ExportWorker
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
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val projectIdFlow = savedStateHandle.getStateFlow<Long?>("projectId", null)

    fun setProjectId(id: Long) {
        savedStateHandle["projectId"] = id
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

    // Helper extension to group Flow by key for per-item debouncing
    private fun <T, K> Flow<T>.groupBy(keySelector: (T) -> K): Flow<Flow<T>> = flow {
        val groups = mutableMapOf<K, MutableSharedFlow<T>>()
        collect { item ->
            val key = keySelector(item)
            var groupFlow = groups[key]
            if (groupFlow == null) {
                groupFlow = MutableSharedFlow(extraBufferCapacity = 10)
                groups[key] = groupFlow
                emit(groupFlow)
            }
            groupFlow.emit(item)
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
            repository.deletePhotoWithFile(photo)
        }
    }

    fun deletePhotos(photoIds: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            val photosToDelete = currentProjectLogs.value
                .flatMap { it.photos }
                .filter { it.id in photoIds }
            
            repository.deletePhotosWithFiles(photosToDelete)
        }
    }

    fun updatePhotoRotation(photoId: Long, rotation: Float) {
        viewModelScope.launch {
            repository.updatePhotoRotation(photoId, rotation)
        }
    }

    fun exportProject(context: Context, projectId: Long, projectName: String, format: String, quality: Int) {
        val workRequest = OneTimeWorkRequestBuilder<ExportWorker>()
            .setInputData(
                workDataOf(
                    "project_id" to projectId,
                    "project_name" to projectName,
                    "format" to format,
                    "quality" to quality
                )
            )
            .build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            repository.deleteProjectById(projectId)
        }
    }
}
