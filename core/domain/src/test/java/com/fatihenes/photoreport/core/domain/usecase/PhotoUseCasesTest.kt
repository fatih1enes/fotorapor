package com.fatihenes.photoreport.core.domain.usecase

import com.fatihenes.photoreport.core.domain.repository.PhotoRepository
import com.fatihenes.photoreport.core.model.Photo
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify

class PhotoUseCasesTest {

    @Mock
    private lateinit var repository: PhotoRepository

    private lateinit var getPhotosForLogUseCase: GetPhotosForLogUseCase
    private lateinit var getPhotosForProjectUseCase: GetPhotosForProjectUseCase
    private lateinit var processAndSavePhotoUseCase: ProcessAndSavePhotoUseCase
    private lateinit var softDeletePhotosUseCase: SoftDeletePhotosUseCase
    private lateinit var rotatePhotoUseCase: RotatePhotoUseCase

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        getPhotosForLogUseCase = GetPhotosForLogUseCase(repository)
        getPhotosForProjectUseCase = GetPhotosForProjectUseCase(repository)
        processAndSavePhotoUseCase = ProcessAndSavePhotoUseCase(repository)
        softDeletePhotosUseCase = SoftDeletePhotosUseCase(repository)
        rotatePhotoUseCase = RotatePhotoUseCase(repository)
    }

    @Test
    fun `GetPhotosForLogUseCase should return flow of photos`() = runTest {
        val logId = 1L
        val photos = listOf(Photo(1, logId, "path", 0f))
        `when`(repository.getPhotosForLog(logId)).thenReturn(flowOf(photos))

        getPhotosForLogUseCase(logId).collect { result ->
            assert(result == photos)
        }
    }

    @Test
    fun `GetPhotosForProjectUseCase should return flow of photos`() = runTest {
        val projectId = 1L
        val photos = listOf(Photo(1, 1L, "path", 0f))
        `when`(repository.getPhotosForProject(projectId)).thenReturn(flowOf(photos))

        getPhotosForProjectUseCase(projectId).collect { result ->
            assert(result == photos)
        }
    }

    @Test
    fun `ProcessAndSavePhotoUseCase should call repository background processing`() {
        processAndSavePhotoUseCase("uri", 1L, 2L, true, "Project", null)
        verify(repository).processAndSavePhotoInBackground("uri", 1L, 2L, true, "Project", null)
    }

    @Test
    fun `SoftDeletePhotosUseCase should call repository soft delete for list`() = runTest {
        val photos = listOf(Photo(1, 1L, "path", 0f))
        softDeletePhotosUseCase(photos)
        verify(repository).softDeletePhotos(photos)
    }

    @Test
    fun `SoftDeletePhotosUseCase should call repository soft delete for single`() = runTest {
        val photo = Photo(1, 1L, "path", 0f)
        softDeletePhotosUseCase(photo)
        verify(repository).softDeletePhoto(photo)
    }

    @Test
    fun `RotatePhotoUseCase should call repository update rotation`() = runTest {
        rotatePhotoUseCase(1L, 90f)
        verify(repository).updatePhotoRotation(1L, 90f)
    }
}
