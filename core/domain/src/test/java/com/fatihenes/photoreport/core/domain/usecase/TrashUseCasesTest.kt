package com.fatihenes.photoreport.core.domain.usecase

import com.fatihenes.photoreport.core.domain.repository.TrashRepository
import com.fatihenes.photoreport.core.model.Photo
import com.fatihenes.photoreport.core.model.Project
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify

class TrashUseCasesTest {

    @Mock
    private lateinit var repository: TrashRepository

    private lateinit var getTrashItemsUseCase: GetTrashItemsUseCase
    private lateinit var restoreTrashUseCase: RestoreTrashUseCase
    private lateinit var emptyTrashUseCase: EmptyTrashUseCase

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        getTrashItemsUseCase = GetTrashItemsUseCase(repository)
        restoreTrashUseCase = RestoreTrashUseCase(repository)
        emptyTrashUseCase = EmptyTrashUseCase(repository)
    }

    @Test
    fun `GetTrashItemsUseCase getProjects should return flow`() = runTest {
        val projects = listOf(Project(1, "P1", "#000"))
        `when`(repository.getDeletedProjects()).thenReturn(flowOf(projects))

        getTrashItemsUseCase.getProjects().collect { result ->
            assertEquals(projects, result)
        }
    }

    @Test
    fun `GetTrashItemsUseCase getPhotos should return flow`() = runTest {
        val photos = listOf(Photo(1, 1, "path", 0f))
        `when`(repository.getDeletedPhotos()).thenReturn(flowOf(photos))

        getTrashItemsUseCase.getPhotos().collect { result ->
            assertEquals(photos, result)
        }
    }

    @Test
    fun `RestoreTrashUseCase restoreProject should call repository`() = runTest {
        restoreTrashUseCase.restoreProject(1L)
        verify(repository).restoreProjectById(1L)
    }

    @Test
    fun `RestoreTrashUseCase restorePhoto should call repository`() = runTest {
        restoreTrashUseCase.restorePhoto(1L)
        verify(repository).restorePhoto(1L)
    }

    @Test
    fun `EmptyTrashUseCase should call repository`() = runTest {
        emptyTrashUseCase()
        verify(repository).emptyTrash()
    }
}
