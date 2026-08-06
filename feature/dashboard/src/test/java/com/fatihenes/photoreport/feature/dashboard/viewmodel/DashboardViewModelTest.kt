package com.fatihenes.photoreport.feature.dashboard.viewmodel

import com.fatihenes.photoreport.core.domain.usecase.CreateProjectUseCase
import com.fatihenes.photoreport.core.domain.usecase.GetProjectsUseCase
import com.fatihenes.photoreport.core.domain.usecase.GetTrashItemsUseCase
import com.fatihenes.photoreport.core.model.Photo
import com.fatihenes.photoreport.core.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val mockGetProjectsUseCase = mock(GetProjectsUseCase::class.java)
    private val mockCreateProjectUseCase = mock(CreateProjectUseCase::class.java)
    private val mockGetTrashItemsUseCase = mock(GetTrashItemsUseCase::class.java)
    private val testDispatcher = StandardTestDispatcher()

    private val deletedProjectsFlow = MutableStateFlow<List<Project>>(emptyList())
    private val deletedPhotosFlow = MutableStateFlow<List<Photo>>(emptyList())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        deletedProjectsFlow.value = emptyList()
        deletedPhotosFlow.value = emptyList()
        
        `when`(mockGetProjectsUseCase()).thenReturn(flowOf(emptyList()))
        `when`(mockGetTrashItemsUseCase.getProjects()).thenReturn(deletedProjectsFlow)
        `when`(mockGetTrashItemsUseCase.getPhotos()).thenReturn(deletedPhotosFlow)
        
        viewModel = DashboardViewModel(mockGetProjectsUseCase, mockCreateProjectUseCase, mockGetTrashItemsUseCase)
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
        val projectList = listOf(Project(id = 1L, name = "Test", colorHex = "#FF0000"))
        `when`(mockGetProjectsUseCase()).thenReturn(flowOf(projectList))
        
        val collectJob = launch { viewModel.projects.collect() }
        viewModel.refresh()
        advanceUntilIdle()
        
        assertEquals(1, viewModel.projects.value.size)
        assertEquals("Test", viewModel.projects.value[0].name)
        collectJob.cancel()
    }

    @Test
    fun `isTrashNotEmpty returns true when projects are in trash`() = runTest {
        val collectJob = launch { viewModel.isTrashNotEmpty.collect() }
        advanceUntilIdle()
        assertEquals(false, viewModel.isTrashNotEmpty.value)
        
        deletedProjectsFlow.value = listOf(Project(id=1L, name="Deleted", colorHex="#000"))
        advanceUntilIdle()
        
        assertEquals(true, viewModel.isTrashNotEmpty.value)
        collectJob.cancel()
    }
}
