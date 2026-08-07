package com.fatihenes.photoreport.core.domain.usecase

import com.fatihenes.photoreport.core.domain.repository.LogRepository
import com.fatihenes.photoreport.core.model.DailyLog
import com.fatihenes.photoreport.core.model.DailyLogWithPhotos
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify

class LogUseCasesTest {

    @Mock
    private lateinit var repository: LogRepository

    private lateinit var getLogsWithPhotosUseCase: GetLogsWithPhotosUseCase
    private lateinit var getLogForDateUseCase: GetLogForDateUseCase
    private lateinit var createDailyLogUseCase: CreateDailyLogUseCase
    private lateinit var updateLogNoteUseCase: UpdateLogNoteUseCase

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        getLogsWithPhotosUseCase = GetLogsWithPhotosUseCase(repository)
        getLogForDateUseCase = GetLogForDateUseCase(repository)
        createDailyLogUseCase = CreateDailyLogUseCase(repository)
        updateLogNoteUseCase = UpdateLogNoteUseCase(repository)
    }

    @Test
    fun `GetLogsWithPhotosUseCase should return flow`() = runTest {
        val projectId = 1L
        val logs = listOf(DailyLogWithPhotos(DailyLog(1, projectId, 0L, ""), emptyList()))
        `when`(repository.getLogsWithPhotosForProjectFlow(projectId)).thenReturn(flowOf(logs))

        getLogsWithPhotosUseCase(projectId).collect { result ->
            assertEquals(logs, result)
        }
    }

    @Test
    fun `GetLogForDateUseCase should return log`() = runTest {
        val projectId = 1L
        val date = 1000L
        val log = DailyLog(1, projectId, date, "Note")
        `when`(repository.getLogForDate(projectId, date)).thenReturn(log)

        val result = getLogForDateUseCase(projectId, date)
        assertEquals(log, result)
    }

    @Test
    fun `CreateDailyLogUseCase should return id`() = runTest {
        val projectId = 1L
        val date = 1000L
        val note = "Note"
        `when`(repository.insertLog(projectId, date, note)).thenReturn(10L)

        val result = createDailyLogUseCase(projectId, date, note)
        assertEquals(10L, result)
    }

    @Test
    fun `UpdateLogNoteUseCase should call repository`() = runTest {
        updateLogNoteUseCase(1L, "New Note")
        verify(repository).updateNote(1L, "New Note")
    }
}
