package com.fatihenes.photoreport.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fatihenes.photoreport.repository.AppRepository
import com.fatihenes.photoreport.repository.SettingsRepository
import com.fatihenes.photoreport.util.LocationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val isLoading: Boolean = true,
    val disclosureShown: Boolean = true,
    val themeMode: String = "system",
    val language: String = "tr",
    val gpsWatermarkEnabled: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val repository: AppRepository,
    private val locationManager: LocationManager,
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = settingsRepository.settings
        .map { settings ->
            MainUiState(
                isLoading = false,
                disclosureShown = settings.disclosureShown,
                themeMode = settings.themeMode,
                language = settings.language,
                gpsWatermarkEnabled = settings.gpsWatermarkEnabled
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = MainUiState(isLoading = true)
        )

    fun setDisclosureShown(shown: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDisclosureShown(shown)
        }
    }

    fun savePhotoInBackground(uri: Uri, projectId: Long, logId: Long, enableAvif: Boolean, projectName: String) {
        viewModelScope.launch {
            val isGpsEnabled = uiState.value.gpsWatermarkEnabled
            val watermarkData = if (isGpsEnabled) {
                locationManager.buildWatermarkData(projectName)
            } else {
                null
            }
            repository.processAndSavePhotoInBackground(uri, projectId, logId, enableAvif, projectName, watermarkData)
        }
    }
}

