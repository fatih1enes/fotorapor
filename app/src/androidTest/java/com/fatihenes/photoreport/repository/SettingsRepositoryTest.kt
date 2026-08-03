package com.fatihenes.photoreport.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SettingsRepositoryTest {

    private lateinit var testDataStore: DataStore<Preferences>
    private lateinit var repository: SettingsRepository
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { context.preferencesDataStoreFile("test_settings") }
        )
        repository = SettingsRepositoryImpl(testDataStore)
    }

    @Test
    fun testDefaultSettings() = runTest(testDispatcher) {
        val settings = repository.settings.first()
        assertEquals("system", settings.themeMode)
        assertEquals("tr", settings.language)
    }

    @Test
    fun testSavingTheme() = runTest(testDispatcher) {
        repository.setThemeMode("dark")
        val settings = repository.settings.first()
        assertEquals("dark", settings.themeMode)
    }
}
