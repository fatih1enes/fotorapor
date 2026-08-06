package com.fatihenes.photoreport.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fatihenes.photoreport.core.model.AppSettings
import com.fatihenes.photoreport.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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
