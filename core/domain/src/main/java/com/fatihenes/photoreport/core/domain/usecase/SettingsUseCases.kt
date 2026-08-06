package com.fatihenes.photoreport.core.domain.usecase

import com.fatihenes.photoreport.core.domain.repository.SettingsRepository
import com.fatihenes.photoreport.core.model.AppSettings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAppSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<AppSettings> = repository.settings
}

class UpdateAppSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend fun setThemeMode(mode: String) = repository.setThemeMode(mode)
    suspend fun setLanguage(lang: String) = repository.setLanguage(lang)
    suspend fun setCameraOptimization(enabled: Boolean) = repository.setCameraOptimization(enabled)
    suspend fun setAvifEnabled(enabled: Boolean) = repository.setAvifEnabled(enabled)
    suspend fun setGpsWatermarkEnabled(enabled: Boolean) = repository.setGpsWatermarkEnabled(enabled)
    suspend fun setDisclosureShown(shown: Boolean) = repository.setDisclosureShown(shown)
}
