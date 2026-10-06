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
import com.fatihenes.photoreport.core.model.ExportState
import com.fatihenes.photoreport.core.model.Photo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
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
    @Dispatcher(FotoRaporDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val projectIdFlow = savedStateHandle.getStateFlow("projectId", -1L)
    private val contentRefreshVersion = MutableStateFlow(0)

    fun setProjectId(id: Long) {
        savedStateHandle["projectId"] = id
    }

    val currentProject = projectIdFlow.flatMapLatest { id ->
        projectRepository.getProjectById(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedProject = currentProject

    val currentProjectLogs = combine(projectIdFlow, contentRefreshVersion) { id, _ -> id }
        .flatMapLatest { id ->
            logRepository.getLogsWithPhotosForProjectFlow(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProjectPhotos: StateFlow<List<Photo>> = currentProjectLogs.map { logs ->
        logs.flatMap { it.photos }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groupedPhotos = currentProjectLogs.map { logs ->
        logs.flatMap { it.photos }.groupBy { photo ->
            logs.firstOrNull { it.photos.any { p -> p.id == photo.id } }?.log?.date ?: 0L
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _calculatedFileSizes = MutableStateFlow<FileSizeInfo?>(null)
    val calculatedFileSizes: StateFlow<FileSizeInfo?> = _calculatedFileSizes.asStateFlow()
    val fileSizeInfo: StateFlow<FileSizeInfo?> = _calculatedFileSizes.asStateFlow()

    fun calculateFileSizes(photos: List<Photo>) {
        viewModelScope.launch {
            try {
                _calculatedFileSizes.value = reportRepository.calculateFileSizes(photos)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("ProjectDetailVM", "Failed to calculate file sizes", e)
            }
        }
    }

    private val _noteUpdates = MutableSharedFlow<Pair<Long, String>>(extraBufferCapacity = 64)

    init {
        viewModelScope.launch {
            _noteUpdates
                .debounce(300.milliseconds)
                .distinctUntilChanged()
                .collect { (logId, note) ->
                    withContext(ioDispatcher) {
                        logRepository.updateNote(logId, note)
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
            try {
                withContext(ioDispatcher) {
                    photoRepository.insertPhoto(logId = logId, filePath = filePath)
                }
                contentRefreshVersion.update { it + 1 }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("ProjectDetailVM", "Failed to add photo to log", e)
            }
        }
    }

    fun addLogForDate(projectId: Long, date: Long) {
        viewModelScope.launch(ioDispatcher) {
            try {
                val existing = logRepository.getLogForDate(projectId, date)
                if (existing == null) {
                    logRepository.insertLog(
                        projectId = projectId,
                        date = date,
                        note = ""
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("ProjectDetailVM", "Failed to add log for date", e)
            }
        }
    }

    fun deletePhoto(photo: Photo) {
        viewModelScope.launch(ioDispatcher) {
            try {
                photoRepository.softDeletePhoto(photo)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("ProjectDetailVM", "Failed to delete photo", e)
            }
        }
    }

    fun deletePhotos(photoIds: List<Long>) {
        viewModelScope.launch(ioDispatcher) {
            try {
                val photosToDelete = currentProjectLogs.value
                    .flatMap { it.photos }
                    .filter { it.id in photoIds }

                photoRepository.softDeletePhotos(photosToDelete)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("ProjectDetailVM", "Failed to delete photos", e)
            }
        }
    }

    fun updatePhotoRotation(photoId: Long, rotation: Float) {
        viewModelScope.launch {
            try {
                photoRepository.updatePhotoRotation(photoId, rotation)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("ProjectDetailVM", "Failed to update photo rotation", e)
            }
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
            reportRepository.observeExportState(workId).collect { state ->
                when (state) {
                    is ExportState.Success -> {
                        _isExporting.value = false
                        _exportResultUri.emit(state.uri)
                    }
                    is ExportState.Error -> {
                        _isExporting.value = false
                        _exportError.emit(state.message)
                    }
                    ExportState.Loading -> {
                        _isExporting.value = true
                    }
                }
            }
        }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            try {
                projectRepository.deleteProjectById(projectId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("ProjectDetailVM", "Failed to delete project", e)
            }
        }
    }
}
