package com.fatihenes.photoreport.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fatihenes.photoreport.manager.BackupManager
import com.fatihenes.photoreport.repository.AppRepository
import com.fatihenes.photoreport.repository.SettingsRepository
import com.fatihenes.photoreport.util.LocationManager
import com.fatihenes.photoreport.util.result.OperationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val repository: AppRepository,
    private val locationManager: LocationManager,
    private val backupManager: BackupManager
) : ViewModel() {

    // Global settings for UI lifecycle
    val themeMode: StateFlow<String> = settingsRepository.settings
        .map { it.themeMode }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val language: StateFlow<String> = settingsRepository.settings
        .map { it.language }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "tr")

    val gpsWatermarkEnabled: StateFlow<Boolean> = settingsRepository.settings
        .map { it.gpsWatermarkEnabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val disclosureShown: StateFlow<Boolean> = settingsRepository.settings
        .map { it.disclosureShown }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setDisclosureShown(shown: Boolean) {
        viewModelScope.launch { settingsRepository.setDisclosureShown(shown) }
    }

    fun savePhotoInBackground(uri: Uri, projectId: Long, logId: Long, enableAvif: Boolean, projectName: String) {
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

    fun createBackup(uri: Uri) {
        viewModelScope.launch {
            backupManager.createBackup(uri).collect { result ->
                backupState.value = result
            }
        }
    }

    fun restoreBackup(uri: Uri) {
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
