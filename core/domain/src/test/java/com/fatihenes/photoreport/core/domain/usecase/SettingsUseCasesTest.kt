package com.fatihenes.photoreport.core.domain.usecase

import com.fatihenes.photoreport.core.domain.repository.SettingsRepository
import com.fatihenes.photoreport.core.model.AppSettings
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify

class SettingsUseCasesTest {

    @Mock
    private lateinit var repository: SettingsRepository

    private lateinit var getAppSettingsUseCase: GetAppSettingsUseCase
    private lateinit var updateAppSettingsUseCase: UpdateAppSettingsUseCase

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        getAppSettingsUseCase = GetAppSettingsUseCase(repository)
        updateAppSettingsUseCase = UpdateAppSettingsUseCase(repository)
    }

    @Test
    fun `GetAppSettingsUseCase should return flow`() = runTest {
        val settings = AppSettings("system", "tr", true, false, true, true)
        `when`(repository.settings).thenReturn(flowOf(settings))

        getAppSettingsUseCase().collect { result ->
            assertEquals(settings, result)
        }
    }

    @Test
    fun `UpdateAppSettingsUseCase should call repository methods`() = runTest {
        updateAppSettingsUseCase.setThemeMode("dark")
        verify(repository).setThemeMode("dark")

        updateAppSettingsUseCase.setLanguage("en")
        verify(repository).setLanguage("en")

        updateAppSettingsUseCase.setCameraOptimization(true)
        verify(repository).setCameraOptimization(true)

        updateAppSettingsUseCase.setAvifEnabled(true)
        verify(repository).setAvifEnabled(true)

        updateAppSettingsUseCase.setGpsWatermarkEnabled(true)
        verify(repository).setGpsWatermarkEnabled(true)

        updateAppSettingsUseCase.setDisclosureShown(true)
        verify(repository).setDisclosureShown(true)
    }
}
