package com.fatihenes.photoreport.feature.project.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fatihenes.photoreport.core.common.model.FileSizeInfo
import com.fatihenes.photoreport.core.common.util.groupBy
import com.fatihenes.photoreport.core.common.di.Dispatcher
import com.fatihenes.photoreport.core.common.di.FotoRaporDispatchers
import com.fatihenes.photoreport.core.domain.repository.LogRepository
import com.fatihenes.photoreport.core.domain.repository.PhotoRepository
import com.fatihenes.photoreport.core.domain.repository.ProjectRepository
import com.fatihenes.photoreport.core.domain.repository.ReportRepository
import com.fatihenes.photoreport.core.model.Photo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
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
    @Dispatcher(FotoRaporDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
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
                            .debounce(400.milliseconds)
                            .distinctUntilChanged()
                            .collect { (logId, note) ->
                                logRepository.updateNote(logId, note)
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
            withContext(ioDispatcher) {
                photoRepository.insertPhoto(logId = logId, filePath = filePath)
            }
            contentRefreshVersion.update { it + 1 }
        }
    }

    fun addLogForDate(projectId: Long, date: Long) {
        viewModelScope.launch(ioDispatcher) {
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
        viewModelScope.launch(ioDispatcher) {
            photoRepository.softDeletePhoto(photo)
        }
    }

    fun deletePhotos(photoIds: List<Long>) {
        viewModelScope.launch(ioDispatcher) {
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

    private val _exportResultUri = MutableSharedFlow<android.net.Uri>(extraBufferCapacity = 1)
    val exportResultUri: SharedFlow<android.net.Uri> = _exportResultUri.asSharedFlow()

    private val _exportError = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val exportError: SharedFlow<String> = _exportError.asSharedFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    fun exportProject(projectId: Long, projectName: String, format: String, quality: Int, language: String) {
        _isExporting.value = true
        val workId = reportRepository.enqueueExportWork(projectId, projectName, format, quality, language)
        viewModelScope.launch {
            reportRepository.observeExportWork(workId).collect { uri ->
                if (uri != null) {
                    _isExporting.value = false
                    _exportResultUri.emit(uri)
                }
            }
        }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            projectRepository.deleteProjectById(projectId)
        }
    }
}
