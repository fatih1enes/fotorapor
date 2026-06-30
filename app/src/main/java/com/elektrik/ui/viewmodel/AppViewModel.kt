package com.elektrik.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

import com.elektrik.repository.AppRepository

@HiltViewModel
class AppViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: AppRepository
) : ViewModel() {

    // --- Theme ---
    val themeMode = savedStateHandle.getStateFlow("theme_mode", "system")

    fun setThemeMode(mode: String) {
        savedStateHandle["theme_mode"] = mode
    }

    // --- Camera Settings ---
    val cameraOptimizationEnabled = savedStateHandle.getStateFlow("camera_opt", true)
    val videoStabilizationEnabled = savedStateHandle.getStateFlow("video_stab", false)
    val webpEnabled = savedStateHandle.getStateFlow("webp_enabled", false)

    fun setCameraOptimization(enabled: Boolean) {
        savedStateHandle["camera_opt"] = enabled
    }

    fun setVideoStabilization(enabled: Boolean) {
        savedStateHandle["video_stab"] = enabled
    }


    fun setWebpEnabled(enabled: Boolean) {
        savedStateHandle["webp_enabled"] = enabled
    }

    fun savePhotoInBackground(uri: android.net.Uri, projectId: Long, logId: Long, enableWebp: Boolean, projectName: String) {
        repository.processAndSavePhotoInBackground(uri, projectId, logId, enableWebp, projectName)
    }
}
