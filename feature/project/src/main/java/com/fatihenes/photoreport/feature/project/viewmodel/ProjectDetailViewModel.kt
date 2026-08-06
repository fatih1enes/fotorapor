package com.fatihenes.photoreport.feature.project.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fatihenes.photoreport.core.common.model.FileSizeInfo
import com.fatihenes.photoreport.core.common.util.groupBy
import com.fatihenes.photoreport.core.domain.repository.LogRepository
import com.fatihenes.photoreport.core.domain.repository.PhotoRepository
import com.fatihenes.photoreport.core.domain.repository.ProjectRepository
import com.fatihenes.photoreport.core.domain.repository.ReportRepository
import com.fatihenes.photoreport.core.model.Photo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val logRepository: LogRepository,
    private val photoRepository: PhotoRepository,
    private val reportRepository: ReportRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val projectIdFlow = savedStateHandle.getStateFlow<Long?>("projectId", null)
    private val contentRefreshVersion = MutableStateFlow(0)

    fun setProjectId(id: Long) {
        savedStateHandle["projectId"] = id
    }

    private val _fileSizeInfo = MutableStateFlow<FileSizeInfo?>(null)
    val fileSizeInfo: StateFlow<FileSizeInfo?> = _fileSizeInfo

    fun calculateFileSizes(photos: List<Photo>) {
        viewModelScope.launch {
            _fileSizeInfo.value = reportRepository.calculateFileSizes(photos)
        }
    }

    val selectedProject = projectIdFlow.flatMapLatest { id ->
        if (id == null) flowOf(null)
        else projectRepository.getProjectById(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentProjectLogs = combine(projectIdFlow, contentRefreshVersion) { id, _ -> id }.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else logRepository.getLogsWithPhotosForProjectFlow(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProjectPhotos = currentProjectLogs.map { logs ->
        logs.flatMap { it.photos }.sortedByDescending { it.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _noteUpdates = MutableSharedFlow<Pair<Long, String>>(extraBufferCapacity = 10)
    private val _isSavingNotes = MutableStateFlow(false)
    val isSavingNotes: StateFlow<Boolean> = _isSavingNotes.asStateFlow()

    init {
        observeNoteUpdates()
    }

    private fun observeNoteUpdates() {
        viewModelScope.launch {
            @OptIn(kotlinx.coroutines.FlowPreview::class)
            _noteUpdates
                .groupBy { it.first }
                .collect { groupedFlow ->
                    launch {
                        groupedFlow
                            .onEach { _isSavingNotes.value = true }
                            .debounce(400.milliseconds)
                            .distinctUntilChanged()
                            .collect { (logId, note) ->
                                logRepository.updateNote(logId, note)
                                _isSavingNotes.value = false
                            }
                    }
                }
        }
    }

    fun updateNote(logId: Long, note: String) {
        val success = _noteUpdates.tryEmit(logId to note)
        if (!success) {
            viewModelScope.launch { logRepository.updateNote(logId, note) }
        }
    }

    fun addPhotoToLog(logId: Long, filePath: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                photoRepository.insertPhoto(logId = logId, filePath = filePath)
            }
            contentRefreshVersion.update { it + 1 }
        }
    }

    fun addLogForDate(projectId: Long, date: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = logRepository.getLogForDate(projectId, date)
            if (existing == null) {
                logRepository.insertLog(
                    projectId = projectId,
                    date = date,
                    note = ""
                )
            }
        }
    }

    fun deletePhoto(photo: Photo) {
        viewModelScope.launch(Dispatchers.IO) {
            photoRepository.softDeletePhoto(photo)
        }
    }

    fun deletePhotos(photoIds: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            val photosToDelete = currentProjectLogs.value
                .flatMap { it.photos }
                .filter { it.id in photoIds }

            photoRepository.softDeletePhotos(photosToDelete)
        }
    }

    fun updatePhotoRotation(photoId: Long, rotation: Float) {
        viewModelScope.launch {
            photoRepository.updatePhotoRotation(photoId, rotation)
        }
    }

    fun exportProject(projectId: Long, projectName: String, format: String, quality: Int, language: String) {
        reportRepository.enqueueExportWork(projectId, projectName, format, quality, language)
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            projectRepository.deleteProjectById(projectId)
        }
    }
}
