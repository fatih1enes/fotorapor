package com.fatihenes.photoreport.core.domain.usecase

import com.fatihenes.photoreport.core.domain.repository.ReportRepository
import com.fatihenes.photoreport.core.model.FileSizeInfo
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify

class ReportUseCasesTest {

    @Mock
    private lateinit var repository: ReportRepository

    private lateinit var calculateFileSizesUseCase: CalculateFileSizesUseCase
    private lateinit var exportProjectUseCase: ExportProjectUseCase

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        calculateFileSizesUseCase = CalculateFileSizesUseCase(repository)
        exportProjectUseCase = ExportProjectUseCase(repository)
    }

    @Test
    fun `CalculateFileSizesUseCase should return size info`() = runTest {
        val expected = FileSizeInfo(100L, 0L, 1, 0, 100L, 85L, 75L)
        `when`(repository.calculateFileSizes(emptyList())).thenReturn(expected)

        val result = calculateFileSizesUseCase(emptyList())
        assertEquals(expected, result)
    }

    @Test
    fun `ExportProjectUseCase should call repository enqueue`() {
        exportProjectUseCase(1L, "P1", "PDF", 80, "tr")
        verify(repository).enqueueExportWork(1L, "P1", "PDF", 80, "tr")
    }
}
