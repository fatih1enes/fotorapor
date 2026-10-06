package com.fatihenes.photoreport.repository

import com.fatihenes.photoreport.core.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository : com.fatihenes.photoreport.core.domain.repository.SettingsRepository {
    override val settings: Flow<AppSettings>
    override suspend fun setThemeMode(mode: String)
    override suspend fun setLanguage(lang: String)
    override suspend fun setCameraOptimization(enabled: Boolean)
    override suspend fun setAvifEnabled(enabled: Boolean)
    override suspend fun setGpsWatermarkEnabled(enabled: Boolean)
    override suspend fun setDisclosureShown(shown: Boolean)
    override suspend fun importSettings(newSettings: Map<String, Any>)
}
