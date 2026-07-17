package com.sarikaya.santiye.gunlugu.ui.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.sarikaya.santiye.gunlugu.repository.AppRepository

@HiltViewModel
class AppViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val repository: AppRepository
) : ViewModel() {

    companion object {
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val CAMERA_OPT = booleanPreferencesKey("camera_opt")
        private val AVIF_ENABLED = booleanPreferencesKey("avif_enabled")
    }

    val themeMode: StateFlow<String> = dataStore.data
        .map { it[THEME_MODE] ?: "system" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "system")

    val cameraOptimizationEnabled: StateFlow<Boolean> = dataStore.data
        .map { it[CAMERA_OPT] ?: true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = true)

    val avifEnabled: StateFlow<Boolean> = dataStore.data
        .map { it[AVIF_ENABLED] ?: true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = true)

    fun setThemeMode(mode: String) {
        viewModelScope.launch { dataStore.edit { it[THEME_MODE] = mode } }
    }

    fun setCameraOptimization(enabled: Boolean) {
        viewModelScope.launch { dataStore.edit { it[CAMERA_OPT] = enabled } }
    }

    fun setAvifEnabled(enabled: Boolean) {
        viewModelScope.launch { dataStore.edit { it[AVIF_ENABLED] = enabled } }
    }

    fun savePhotoInBackground(uri: android.net.Uri, projectId: Long, logId: Long, enableAvif: Boolean, projectName: String) {
        repository.processAndSavePhotoInBackground(uri, projectId, logId, enableAvif, projectName)
    }
}
