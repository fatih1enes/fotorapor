package com.fatihenes.photoreport.core.domain.repository

import com.fatihenes.photoreport.core.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun setThemeMode(mode: String)
    suspend fun setLanguage(lang: String)
    suspend fun setCameraOptimization(enabled: Boolean)
    suspend fun setAvifEnabled(enabled: Boolean)
    suspend fun setGpsWatermarkEnabled(enabled: Boolean)
    suspend fun setDisclosureShown(shown: Boolean)
    suspend fun importSettings(newSettings: Map<String, Any>)
}
