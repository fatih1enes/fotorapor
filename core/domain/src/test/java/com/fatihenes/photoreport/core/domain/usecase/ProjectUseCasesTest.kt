package com.fatihenes.photoreport.core.domain.usecase

import com.fatihenes.photoreport.core.domain.repository.ProjectRepository
import com.fatihenes.photoreport.core.model.Project
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify

class ProjectUseCasesTest {

    @Mock
    private lateinit var repository: ProjectRepository

    private lateinit var getProjectsUseCase: GetProjectsUseCase
    private lateinit var deleteProjectUseCase: DeleteProjectUseCase

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        getProjectsUseCase = GetProjectsUseCase(repository)
        deleteProjectUseCase = DeleteProjectUseCase(repository)
    }

    @Test
    fun `GetProjectsUseCase should return flow of projects`() = runTest {
        val projects = listOf(Project(1, "P1", "#FF0000"))
        `when`(repository.getAllProjects()).thenReturn(flowOf(projects))

        getProjectsUseCase().collect { result ->
            assert(result == projects)
        }
        verify(repository).getAllProjects()
    }

    @Test
    fun `DeleteProjectUseCase should call repository delete`() = runTest {
        val projectId = 1L
        deleteProjectUseCase(projectId)
        verify(repository).deleteProjectById(projectId)
    }
}
