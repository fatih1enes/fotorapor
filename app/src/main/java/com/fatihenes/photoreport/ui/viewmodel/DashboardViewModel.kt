package com.fatihenes.photoreport.ui.viewmodel

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fatihenes.photoreport.R
import com.fatihenes.photoreport.data.ProjectEntity
import com.fatihenes.photoreport.repository.AppRepository
import com.fatihenes.photoreport.repository.TrashRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @Suppress("CanBeParameter") @param:ApplicationContext private val appContext: Context,
    private val repository: AppRepository,
    private val trashRepository: TrashRepository
) : ViewModel() {

    private val _projectActionState = MutableStateFlow<UiState<Unit>?>(null)
    val projectActionState: StateFlow<UiState<Unit>?> = _projectActionState

    private val _refreshTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val projects = _refreshTrigger.flatMapLatest {
        repository.getAllProjects()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val isTrashNotEmpty: StateFlow<Boolean> = combine(
        trashRepository.getDeletedProjects(),
        trashRepository.getDeletedPhotos()
    ) { projects, photos ->
        projects.isNotEmpty() || photos.isNotEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _refreshTrigger.emit(Unit)
            delay(500) // Minimum animation time
            _isRefreshing.value = false
        }
    }

    fun addProject(name: String, color: Color) {
        viewModelScope.launch {
            _projectActionState.value = UiState.Loading
            try {
                val colorHex = String.format(Locale.US, "#%06X", 0xFFFFFF and color.toArgb())
                repository.insertProject(ProjectEntity(name = name, colorHex = colorHex))
                _projectActionState.value = UiState.Success(Unit)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _projectActionState.value = UiState.Error(
                    e.message ?: appContext.getString(R.string.error_unknown)
                )
            }
        }
    }

    fun resetProjectActionState() {
        _projectActionState.value = null
    }
}
