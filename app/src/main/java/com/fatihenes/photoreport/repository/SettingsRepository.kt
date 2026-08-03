package com.fatihenes.photoreport.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class AppSettings(
    val themeMode: String,
    val language: String,
    val cameraOptimization: Boolean,
    val avifEnabled: Boolean,
    val gpsWatermarkEnabled: Boolean,
    val disclosureShown: Boolean
)

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun setThemeMode(mode: String)
    suspend fun setLanguage(lang: String)
    suspend fun setCameraOptimization(enabled: Boolean)
    suspend fun setAvifEnabled(enabled: Boolean)
    suspend fun setGpsWatermarkEnabled(enabled: Boolean)
    suspend fun setDisclosureShown(shown: Boolean)
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val CAMERA_OPT = booleanPreferencesKey("camera_opt")
        val AVIF_ENABLED = booleanPreferencesKey("avif_enabled")
        val GPS_WATERMARK = booleanPreferencesKey("gps_watermark_enabled")
        val DISCLOSURE_SHOWN = booleanPreferencesKey("disclosure_shown")
    }

    override val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[Keys.THEME_MODE] ?: "system",
            language = prefs[Keys.LANGUAGE] ?: "tr",
            cameraOptimization = prefs[Keys.CAMERA_OPT] ?: true,
            avifEnabled = prefs[Keys.AVIF_ENABLED] ?: true,
            gpsWatermarkEnabled = prefs[Keys.GPS_WATERMARK] ?: false,
            disclosureShown = prefs[Keys.DISCLOSURE_SHOWN] ?: false
        )
    }

    override suspend fun setThemeMode(mode: String) {
        dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    override suspend fun setLanguage(lang: String) {
        dataStore.edit { it[Keys.LANGUAGE] = lang }
    }

    override suspend fun setCameraOptimization(enabled: Boolean) {
        dataStore.edit { it[Keys.CAMERA_OPT] = enabled }
    }

    override suspend fun setAvifEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.AVIF_ENABLED] = enabled }
    }

    override suspend fun setGpsWatermarkEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.GPS_WATERMARK] = enabled }
    }

    override suspend fun setDisclosureShown(shown: Boolean) {
        dataStore.edit { it[Keys.DISCLOSURE_SHOWN] = shown }
    }
}
