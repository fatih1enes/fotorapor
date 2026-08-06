package com.fatihenes.photoreport.feature.trash.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fatihenes.photoreport.core.model.Photo
import com.fatihenes.photoreport.core.model.Project
import com.fatihenes.photoreport.core.domain.repository.TrashRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val repository: TrashRepository
) : ViewModel() {

    val deletedProjects: Flow<List<Project>> = repository.getDeletedProjects()
    val deletedPhotos: Flow<List<Photo>> = repository.getDeletedPhotos()

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

    fun hardDeletePhoto(photo: Photo) {
        viewModelScope.launch {
            repository.hardDeletePhoto(photo)
        }
    }
}
