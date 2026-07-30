package com.fatihenes.photoreport.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fatihenes.photoreport.data.PhotoEntity
import com.fatihenes.photoreport.data.ProjectEntity
import com.fatihenes.photoreport.repository.AppRepository
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

    fun hardDeleteProject(projectId: Long) {
        viewModelScope.launch {
            repository.hardDeleteProject(projectId)
        }
    }

    fun restorePhoto(photoId: Long) {
        viewModelScope.launch {
            repository.restorePhoto(photoId)
        }
    }

    fun hardDeletePhoto(photo: PhotoEntity) {
        viewModelScope.launch {
            repository.hardDeletePhoto(photo)
        }
    }
}
