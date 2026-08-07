package com.fatihenes.photoreport.core.domain.usecase

import com.fatihenes.photoreport.core.domain.repository.ProjectRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.verify

class CreateProjectUseCaseTest {

    @Mock
    private lateinit var repository: ProjectRepository

    private lateinit var createProjectUseCase: CreateProjectUseCase

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        createProjectUseCase = CreateProjectUseCase(repository)
    }

    @Test
    fun `when name is valid then calls repository and returns id`() = runTest {
        val projectName = "Test Project"
        val projectColor = "#FFFFFF"
        val expectedId = 1L

        `when`(repository.insertProject(projectName, projectColor)).thenReturn(expectedId)

        val result = createProjectUseCase(projectName, projectColor)

        assertEquals(expectedId, result)
        verify(repository).insertProject(projectName, projectColor)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `when name is empty then throws exception`() = runTest {
        createProjectUseCase("", "#FFFFFF")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `when name is blank then throws exception`() = runTest {
        createProjectUseCase("   ", "#FFFFFF")
    }

    @Test
    fun `when name has whitespace then trims and calls repository`() = runTest {
        val projectName = "  Trimmed Project  "
        val expectedName = "Trimmed Project"
        val projectColor = "#FFFFFF"

        `when`(repository.insertProject(expectedName, projectColor)).thenReturn(1L)

        createProjectUseCase(projectName, projectColor)

        verify(repository).insertProject(expectedName, projectColor)
    }
}
