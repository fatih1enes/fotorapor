package com.fatihenes.photoreport.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fatihenes.photoreport.manager.BackupManager
import com.fatihenes.photoreport.core.model.AppSettings
import com.fatihenes.photoreport.repository.SettingsRepository
import com.fatihenes.photoreport.core.common.util.result.OperationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val backupManager: BackupManager
) : ViewModel() {

    val settings: StateFlow<AppSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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

    fun setThemeMode(mode: String) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch { settingsRepository.setLanguage(lang) }
    }

    fun setCameraOptimization(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setCameraOptimization(enabled) }
    }

    fun setAvifEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAvifEnabled(enabled) }
    }

    fun setGpsWatermarkEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setGpsWatermarkEnabled(enabled) }
    }
}
