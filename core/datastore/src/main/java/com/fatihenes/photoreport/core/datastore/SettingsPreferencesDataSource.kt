package com.fatihenes.photoreport.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.fatihenes.photoreport.core.domain.datasource.LocalSettingsDataSource
import com.fatihenes.photoreport.core.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : LocalSettingsDataSource {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val CAMERA_OPT = booleanPreferencesKey("camera_opt")
        val AVIF_ENABLED = booleanPreferencesKey("avif_enabled")
        val GPS_WATERMARK = booleanPreferencesKey("gps_watermark_enabled")
        val DISCLOSURE_SHOWN = booleanPreferencesKey("disclosure_shown")
    }

    override val settings: Flow<AppSettings> = dataStore.data
        .catch { e ->
            // Bozuk preferences_pb'de splash'ta takılmayı önle: varsayılanları yayınla.
            // Davranış korunur: sadece IOException yutulur, diğerleri yukarı taşınır.
            if (e is IOException) {
                emit(emptyPreferences())
            } else {
                throw e
            }
        }
        .map { prefs ->
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

    override suspend fun importSettings(newSettings: Map<String, Any>) {
        dataStore.edit { prefs ->
            newSettings.forEach { (key, value) ->
                when (key) {
                    // Güvenli cast: yanlış tip gelirse ClassCastException yerine yoksay.
                    // Davranış korunur: geçerli değerler aynen yazılır.
                    "theme_mode" -> (value as? String)?.let { prefs[Keys.THEME_MODE] = it }
                    "language" -> (value as? String)?.let { prefs[Keys.LANGUAGE] = it }
                    "camera_opt" -> (value as? Boolean)?.let { prefs[Keys.CAMERA_OPT] = it }
                    "avif_enabled" -> (value as? Boolean)?.let { prefs[Keys.AVIF_ENABLED] = it }
                    "gps_watermark_enabled" -> (value as? Boolean)?.let { prefs[Keys.GPS_WATERMARK] = it }
                    "disclosure_shown" -> (value as? Boolean)?.let { prefs[Keys.DISCLOSURE_SHOWN] = it }
                }
            }
        }
    }
}
