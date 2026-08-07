package com.fatihenes.photoreport.feature.dashboard.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fatihenes.photoreport.core.domain.usecase.CreateProjectUseCase
import com.fatihenes.photoreport.core.domain.usecase.GetProjectsUseCase
import com.fatihenes.photoreport.core.domain.usecase.GetTrashItemsUseCase
import com.fatihenes.photoreport.core.ui.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.Locale
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getProjectsUseCase: GetProjectsUseCase,
    private val createProjectUseCase: CreateProjectUseCase,
    getTrashItemsUseCase: GetTrashItemsUseCase,
) : ViewModel() {

    private val _projectActionState = MutableStateFlow<UiState<Unit>?>(null)
    val projectActionState: StateFlow<UiState<Unit>?> = _projectActionState

    private val _refreshTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val projects = _refreshTrigger.flatMapLatest {
        getProjectsUseCase()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRefreshing = MutableStateFlow(value = false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val isTrashNotEmpty: StateFlow<Boolean> = combine(
        getTrashItemsUseCase.getProjects(),
        getTrashItemsUseCase.getPhotos(),
    ) { projects, photos ->
        projects.isNotEmpty() || photos.isNotEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = false)

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _refreshTrigger.emit(Unit)
            delay(500.milliseconds) // Minimum animation time
            _isRefreshing.value = false
        }
    }

    fun addProject(name: String, color: Color) {
        viewModelScope.launch {
            _projectActionState.value = UiState.Loading
            try {
                val colorHex = String.format(Locale.US, "#%06X", 0xFFFFFF and color.toArgb())
                createProjectUseCase(name, colorHex)
                _projectActionState.value = UiState.Success(Unit)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _projectActionState.value = UiState.Error(
                    e.message ?: "Bilinmeyen bir hata oluştu",
                )
            }
        }
    }

    fun resetProjectActionState() {
        _projectActionState.value = null
    }
}
