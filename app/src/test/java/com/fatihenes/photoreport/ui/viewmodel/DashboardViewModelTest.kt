package com.fatihenes.photoreport.ui.viewmodel

import android.content.Context
import com.fatihenes.photoreport.data.ProjectEntity
import com.fatihenes.photoreport.repository.AppRepository
import com.fatihenes.photoreport.repository.TrashRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private lateinit var viewModel: DashboardViewModel
    private val mockRepository = mock(AppRepository::class.java)
    private val mockTrashRepository = mock(TrashRepository::class.java)
    private val mockContext = mock(Context::class.java)
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        `when`(mockRepository.getAllProjects()).thenReturn(flowOf(emptyList()))
        `when`(mockTrashRepository.getDeletedProjects()).thenReturn(flowOf(emptyList()))
        `when`(mockTrashRepository.getDeletedPhotos()).thenReturn(flowOf(emptyList()))
        
        viewModel = DashboardViewModel(mockContext, mockRepository, mockTrashRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial projects state is empty`() = runTest {
        val collectJob = launch { viewModel.projects.collect() }
        advanceUntilIdle()
        assertEquals(0, viewModel.projects.value.size)
        collectJob.cancel()
    }

    @Test
    fun `refresh updates projects flow`() = runTest {
        val projectList = listOf(ProjectEntity(id = 1L, name = "Test", colorHex = "#FF0000"))
        `when`(mockRepository.getAllProjects()).thenReturn(flowOf(projectList))
        
        val collectJob = launch { viewModel.projects.collect() }
        viewModel.refresh()
        advanceUntilIdle()
        
        assertEquals(1, viewModel.projects.value.size)
        assertEquals("Test", viewModel.projects.value[0].name)
        collectJob.cancel()
    }

    @Test
    fun `isTrashNotEmpty returns true when projects are in trash`() = runTest {
        `when`(mockTrashRepository.getDeletedProjects()).thenReturn(flowOf(listOf(ProjectEntity(name="Deleted", colorHex="#000"))))
        
        val collectJob = launch { viewModel.isTrashNotEmpty.collect() }
        advanceUntilIdle()
        
        assertEquals(true, viewModel.isTrashNotEmpty.value)
        collectJob.cancel()
    }
}
