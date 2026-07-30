package com.sarikaya.santiye.gunlugu.ui.viewmodel

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarikaya.santiye.gunlugu.R
import com.sarikaya.santiye.gunlugu.data.ProjectEntity
import com.sarikaya.santiye.gunlugu.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val repository: AppRepository,
) : ViewModel() {

    private val _projectActionState = MutableStateFlow<UiState<Unit>?>(null)
    val projectActionState: StateFlow<UiState<Unit>?> = _projectActionState

    val projects = repository.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            _projectActionState.value = UiState.Loading
            try {
                repository.deleteProjectById(projectId)
                _projectActionState.value = UiState.Success(Unit)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _projectActionState.value = UiState.Error(
                    e.message ?: appContext.getString(R.string.error_delete_failed)
                )
            }
        }
    }

    fun resetProjectActionState() {
        _projectActionState.value = null
    }
}
