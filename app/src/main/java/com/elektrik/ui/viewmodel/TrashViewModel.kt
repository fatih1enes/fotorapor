package com.elektrik.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elektrik.data.PhotoEntity
import com.elektrik.data.ProjectEntity
import com.elektrik.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    val deletedProjects: Flow<List<ProjectEntity>> = repository.getDeletedProjects()
    val deletedPhotos: Flow<List<PhotoEntity>> = repository.getDeletedPhotos()

    fun restoreProject(projectId: Long) {
        viewModelScope.launch {
            repository.restoreProjectById(projectId)
        }
    }

    fun hardDeleteProject(context: Context, projectId: Long) {
        viewModelScope.launch {
            repository.hardDeleteProject(context, projectId)
        }
    }

    fun restorePhoto(photoId: Long) {
        viewModelScope.launch {
            repository.restorePhoto(photoId)
        }
    }

    fun hardDeletePhoto(context: Context, photo: PhotoEntity) {
        viewModelScope.launch {
            repository.hardDeletePhoto(context, photo)
        }
    }
}
