package com.fatihenes.photoreport.repository

import com.fatihenes.photoreport.core.datastore.SettingsPreferencesDataSource
import com.fatihenes.photoreport.core.model.AppSettings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

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

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataSource: SettingsPreferencesDataSource
) : SettingsRepository {

    override val settings: Flow<AppSettings> = dataSource.settings

    override suspend fun setThemeMode(mode: String) = dataSource.setThemeMode(mode)
    override suspend fun setLanguage(lang: String) = dataSource.setLanguage(lang)
    override suspend fun setCameraOptimization(enabled: Boolean) = dataSource.setCameraOptimization(enabled)
    override suspend fun setAvifEnabled(enabled: Boolean) = dataSource.setAvifEnabled(enabled)
    override suspend fun setGpsWatermarkEnabled(enabled: Boolean) = dataSource.setGpsWatermarkEnabled(enabled)
    override suspend fun setDisclosureShown(shown: Boolean) = dataSource.setDisclosureShown(shown)
    override suspend fun importSettings(newSettings: Map<String, Any>) = dataSource.importSettings(newSettings)
}
