package com.fatihenes.photoreport.ui.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.fatihenes.photoreport.repository.AppRepository
import com.fatihenes.photoreport.manager.BackupManager
import com.fatihenes.photoreport.util.result.OperationResult

@HiltViewModel
class AppViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val repository: AppRepository,
    private val locationManager: com.fatihenes.photoreport.util.LocationManager,
    private val backupManager: BackupManager
) : ViewModel() {

    companion object {
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val LANGUAGE = stringPreferencesKey("language")
        private val CAMERA_OPT = booleanPreferencesKey("camera_opt")
        private val AVIF_ENABLED = booleanPreferencesKey("avif_enabled")
        private val GPS_WATERMARK = booleanPreferencesKey("gps_watermark_enabled")
        private val DISCLOSURE_SHOWN = booleanPreferencesKey("disclosure_shown")
    }

    val themeMode: StateFlow<String> = dataStore.data
        .map { it[THEME_MODE] ?: "system" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val language: StateFlow<String> = dataStore.data
        .map { it[LANGUAGE] ?: "tr" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "tr")

    val cameraOptimizationEnabled: StateFlow<Boolean> = dataStore.data
        .map { it[CAMERA_OPT] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = true)

    val avifEnabled: StateFlow<Boolean> = dataStore.data
        .map { it[AVIF_ENABLED] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = true)

    val gpsWatermarkEnabled: StateFlow<Boolean> = dataStore.data
        .map { it[GPS_WATERMARK] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = false)

    val disclosureShown: StateFlow<Boolean> = dataStore.data
        .map { it[DISCLOSURE_SHOWN] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = false)

    fun setThemeMode(mode: String) {
        viewModelScope.launch { dataStore.edit { it[THEME_MODE] = mode } }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch { dataStore.edit { it[LANGUAGE] = lang } }
    }

    fun setCameraOptimization(enabled: Boolean) {
        viewModelScope.launch { dataStore.edit { it[CAMERA_OPT] = enabled } }
    }

    fun setAvifEnabled(enabled: Boolean) {
        viewModelScope.launch { dataStore.edit { it[AVIF_ENABLED] = enabled } }
    }

    fun setGpsWatermarkEnabled(enabled: Boolean) {
        viewModelScope.launch { dataStore.edit { it[GPS_WATERMARK] = enabled } }
    }

    fun setDisclosureShown(shown: Boolean) {
        viewModelScope.launch { dataStore.edit { it[DISCLOSURE_SHOWN] = shown } }
    }

    fun savePhotoInBackground(uri: android.net.Uri, projectId: Long, logId: Long, enableAvif: Boolean, projectName: String) {
        viewModelScope.launch {
            val isGpsEnabled = gpsWatermarkEnabled.value
            val watermarkData = if (isGpsEnabled) {
                locationManager.buildWatermarkData(projectName)
            } else {
                null
            }
            repository.processAndSavePhotoInBackground(uri, projectId, logId, enableAvif, projectName, watermarkData)
        }
    }

    val backupState = MutableStateFlow<OperationResult<Unit>?>(null)
    val restoreState = MutableStateFlow<OperationResult<Unit>?>(null)

    fun createBackup(uri: android.net.Uri) {
        viewModelScope.launch {
            backupManager.createBackup(uri).collect { result ->
                backupState.value = result
            }
        }
    }

    fun restoreBackup(uri: android.net.Uri) {
        viewModelScope.launch {
            backupManager.restoreBackup(uri).collect { result ->
                restoreState.value = result
            }
        }
    }

    fun resetBackupState() {
        backupState.value = null
    }

    fun resetRestoreState() {
        restoreState.value = null
    }
}
