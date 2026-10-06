package com.fatihenes.photoreport.feature.camera.viewmodel

import com.fatihenes.photoreport.feature.camera.capture.PhotoCaptureController
import com.fatihenes.photoreport.feature.camera.capture.VideoRecordingController
import com.fatihenes.photoreport.feature.camera.engine.AspectRatioSelection
import com.fatihenes.photoreport.feature.camera.engine.CameraEngine
import com.fatihenes.photoreport.feature.camera.engine.CameraKeyEventDispatcher
import com.fatihenes.photoreport.feature.camera.engine.CameraMode
import com.fatihenes.photoreport.feature.camera.engine.CameraSessionState
import com.fatihenes.photoreport.feature.camera.engine.FlashMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class CameraViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val cameraEngine: CameraEngine = mock()
    private val photoCaptureController: PhotoCaptureController = mock()
    private val videoRecordingController: VideoRecordingController = mock()

    private val sessionStateFlow = MutableStateFlow<CameraSessionState>(CameraSessionState.Uninitialized)
    private val zoomStateFlow = MutableStateFlow<androidx.camera.core.ZoomState?>(null)
    private val exposureStateFlow = MutableStateFlow<androidx.camera.core.ExposureState?>(null)
    private val isFocusLockedFlow = MutableStateFlow(false)
    private val activeCapturesCountFlow = MutableStateFlow(0)
    private val isRecordingFlow = MutableStateFlow(false)
    private val isPausedFlow = MutableStateFlow(false)
    private val durationSecondsFlow = MutableStateFlow(0)

    private lateinit var viewModel: CameraViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        whenever(cameraEngine.sessionState).thenReturn(sessionStateFlow)
        whenever(cameraEngine.zoomState).thenReturn(zoomStateFlow)
        whenever(cameraEngine.exposureState).thenReturn(exposureStateFlow)
        whenever(cameraEngine.isFocusLocked).thenReturn(isFocusLockedFlow)

        whenever(photoCaptureController.activeCapturesCount).thenReturn(activeCapturesCountFlow)
        whenever(photoCaptureController.capturedPhotos).thenReturn(MutableSharedFlow())
        whenever(videoRecordingController.isRecording).thenReturn(isRecordingFlow)
        whenever(videoRecordingController.isPaused).thenReturn(isPausedFlow)
        whenever(videoRecordingController.durationSeconds).thenReturn(durationSecondsFlow)
        whenever(videoRecordingController.recordedVideos).thenReturn(MutableSharedFlow())

        viewModel = CameraViewModel(
            cameraEngine = cameraEngine,
            photoCaptureController = photoCaptureController,
            videoRecordingController = videoRecordingController,
            keyEventDispatcher = CameraKeyEventDispatcher()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialUiState_hasSensibleDefaults() = runTest {
        val state = viewModel.uiState.value
        assertEquals(CameraMode.PHOTO, state.cameraMode)
        assertEquals(FlashMode.OFF, state.flashMode)
        assertEquals(AspectRatioSelection.RATIO_4_3, state.aspectRatio)
        assertEquals(1f, state.zoomRatio)
        assertFalse(state.isGridVisible)
        assertFalse(state.isLevelVisible)
        assertFalse(state.isRecording)
    }

    @Test
    fun setCameraMode_updatesUiState() = runTest {
        viewModel.setCameraMode(CameraMode.VIDEO)
        advanceUntilIdle()
        assertEquals(CameraMode.VIDEO, viewModel.uiState.value.cameraMode)
    }

    @Test
    fun cycleFlashMode_cyclesThroughAllModesSequentially() = runTest {
        assertEquals(FlashMode.OFF, viewModel.uiState.value.flashMode)

        viewModel.cycleFlashMode()
        advanceUntilIdle()
        assertEquals(FlashMode.AUTO, viewModel.uiState.value.flashMode)

        viewModel.cycleFlashMode()
        advanceUntilIdle()
        assertEquals(FlashMode.ON, viewModel.uiState.value.flashMode)

        viewModel.cycleFlashMode()
        advanceUntilIdle()
        assertEquals(FlashMode.TORCH, viewModel.uiState.value.flashMode)

        viewModel.cycleFlashMode()
        advanceUntilIdle()
        assertEquals(FlashMode.OFF, viewModel.uiState.value.flashMode)
    }

    @Test
    fun toggleAspectRatio_switchesBetween4_3And16_9() = runTest {
        assertEquals(AspectRatioSelection.RATIO_4_3, viewModel.uiState.value.aspectRatio)

        viewModel.toggleAspectRatio()
        advanceUntilIdle()
        assertEquals(AspectRatioSelection.RATIO_16_9, viewModel.uiState.value.aspectRatio)

        viewModel.toggleAspectRatio()
        advanceUntilIdle()
        assertEquals(AspectRatioSelection.RATIO_4_3, viewModel.uiState.value.aspectRatio)
    }

    @Test
    fun toggleGridAndLevel_togglesVisibilityFlags() = runTest {
        assertFalse(viewModel.uiState.value.isGridVisible)
        viewModel.toggleGrid()
        assertTrue(viewModel.uiState.value.isGridVisible)

        assertFalse(viewModel.uiState.value.isLevelVisible)
        viewModel.toggleLevel()
        assertTrue(viewModel.uiState.value.isLevelVisible)
    }

    @Test
    fun setQuickSettingsAndSessionReview_opensAndClosesSheets() = runTest {
        assertFalse(viewModel.uiState.value.isQuickSettingsOpen)
        viewModel.setQuickSettingsOpen(true)
        assertTrue(viewModel.uiState.value.isQuickSettingsOpen)

        assertFalse(viewModel.uiState.value.isSessionReviewOpen)
        viewModel.setSessionReviewOpen(true)
        assertTrue(viewModel.uiState.value.isSessionReviewOpen)
    }
}
