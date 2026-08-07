@file:Suppress("LocalContextGetResourceValueCall")
package com.fatihenes.photoreport.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.fatihenes.photoreport.R
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporMotion
import com.fatihenes.photoreport.core.ui.state.UiState
import com.fatihenes.photoreport.feature.camera.ui.CameraScreen
import com.fatihenes.photoreport.feature.dashboard.ui.DashboardScreen
import com.fatihenes.photoreport.feature.dashboard.viewmodel.DashboardViewModel
import com.fatihenes.photoreport.feature.project.ui.ProjectDetailScreen
import com.fatihenes.photoreport.feature.project.viewmodel.ProjectDetailViewModel
import com.fatihenes.photoreport.feature.settings.ui.SettingsScreen
import com.fatihenes.photoreport.feature.settings.viewmodel.SettingsViewModel
import com.fatihenes.photoreport.feature.trash.ui.TrashScreen
import com.fatihenes.photoreport.ui.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// ─── Dashboard ─────────────────────────────────────────────────────
// Dashboard is the start destination; it inherits NavHost defaults.

fun NavGraphBuilder.dashboardRoute(
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    language: String
) {
    composable(Routes.DASHBOARD) {
        val dashboardViewModel: DashboardViewModel = hiltViewModel()
        val projects by dashboardViewModel.projects.collectAsStateWithLifecycle()
        val actionState by dashboardViewModel.projectActionState.collectAsStateWithLifecycle()
        val isTrashNotEmpty by dashboardViewModel.isTrashNotEmpty.collectAsStateWithLifecycle()
        val isRefreshing by dashboardViewModel.isRefreshing.collectAsStateWithLifecycle()

        LaunchedEffect(actionState) {
            val state = actionState
            if (state is UiState.Success) {
                val newProjectId = state.data
                navController.navigate(Routes.detail(newProjectId))
                dashboardViewModel.resetProjectActionState()
            } else if (state is UiState.Error) {
                snackbarHostState.showSnackbar(state.message)
                dashboardViewModel.resetProjectActionState()
            }
        }

        DashboardScreen(
            projects = projects,
            language = language,
            isTrashNotEmpty = isTrashNotEmpty,
            isRefreshing = isRefreshing,
            onProjectClick = { project -> navController.navigate(Routes.detail(project.id)) },
            onAddProject = { name, color -> dashboardViewModel.addProject(name, color) },
            onSettingsClick = { navController.navigate(Routes.SETTINGS) },
            onTrashClick = { navController.navigate(Routes.TRASH) },
            onRefresh = { dashboardViewModel.refresh() }
        )
    }
}

// ─── Settings ──────────────────────────────────────────────────────
// Utility screen → fast symmetric transitions (250ms).

fun NavGraphBuilder.settingsRoute(navController: NavController) {
    composable(
        route = Routes.SETTINGS,
        enterTransition = { FotoRaporMotion.navEnter(FotoRaporMotion.NavDurationFast) },
        exitTransition = { FotoRaporMotion.navExit(FotoRaporMotion.NavDurationFast) },
        popEnterTransition = { FotoRaporMotion.navPopEnter(FotoRaporMotion.NavDurationFast) },
        popExitTransition = { FotoRaporMotion.navPopExit(FotoRaporMotion.NavDurationFast) }
    ) {
        val settingsViewModel: SettingsViewModel = hiltViewModel()
        SettingsScreen(
            onBack = { navController.popBackStack() },
            viewModel = settingsViewModel
        )
    }
}

// ─── Project Detail ────────────────────────────────────────────────
// Content screen → standard transitions (300ms) — slightly more dramatic.

fun NavGraphBuilder.detailRoute(
    navController: NavController,
    language: String,
    permissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    onSetPending: (Long?, Long?) -> Unit
) {
    composable(
        route = Routes.DETAIL,
        arguments = listOf(navArgument("projectId") { type = NavType.LongType }),
        enterTransition = { FotoRaporMotion.navEnter(FotoRaporMotion.NavDurationStandard) },
        exitTransition = { FotoRaporMotion.navExit(FotoRaporMotion.NavDurationStandard) },
        popEnterTransition = { FotoRaporMotion.navPopEnter(FotoRaporMotion.NavDurationStandard) },
        popExitTransition = { FotoRaporMotion.navPopExit(FotoRaporMotion.NavDurationStandard) }
    ) { backStackEntry ->
        val context = LocalContext.current
        val projectId = backStackEntry.arguments?.getLong("projectId") ?: -1L
        val detailViewModel: ProjectDetailViewModel = hiltViewModel()

        LaunchedEffect(projectId) {
            detailViewModel.setProjectId(projectId)
        }

        val project by detailViewModel.selectedProject.collectAsStateWithLifecycle()
        val currentLogs by detailViewModel.currentProjectLogs.collectAsStateWithLifecycle()

        ProjectDetailScreen(
            project = project,
            logs = currentLogs,
            viewModel = detailViewModel,
            onBack = { navController.popBackStack() },
            onDeleteProject = {
                project?.let { detailViewModel.deleteProject(it.id) }
                navController.popBackStack()
            },
            onAddPhoto = { logId ->
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    navController.navigate(Routes.camera(logId, projectId))
                } else {
                    onSetPending(logId, projectId)
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            onDeletePhoto = { photo -> detailViewModel.deletePhoto(photo) },
            onDeletePhotos = { ids -> detailViewModel.deletePhotos(ids) },
            onNoteChange = { logId, note -> detailViewModel.updateNote(logId, note) },
            onUpdateRotation = { photoId, rotation -> detailViewModel.updatePhotoRotation(photoId, rotation) },
            onAddLogForDate = { date -> project?.let { detailViewModel.addLogForDate(it.id, date) } },
            onImportPhotoToLog = { logId, filePath -> detailViewModel.addPhotoToLog(logId, filePath) },
            onExportProject = { format, quality, lang ->
                project?.let { detailViewModel.exportProject(it.id, it.name, format, quality, lang) }
            },
            language = language
        )
    }
}

// ─── Camera ────────────────────────────────────────────────────────
// Immersive fullscreen → instant cut (no animation), avoids jarring
// slide over the camera preview.

fun NavGraphBuilder.cameraRoute(
    navController: NavController,
    viewModel: MainViewModel
) {
    composable(
        route = Routes.CAMERA,
        arguments = listOf(
            navArgument("logId") { type = NavType.LongType; defaultValue = -1L },
            navArgument("projectId") { type = NavType.LongType; defaultValue = -1L }
        ),
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) { backStackEntry ->
        val logId = backStackEntry.arguments?.getLong("logId") ?: -1L
        val cameraProjectId = backStackEntry.arguments?.getLong("projectId") ?: -1L

        val detailViewModel: ProjectDetailViewModel = hiltViewModel()
        val settingsViewModel: SettingsViewModel = hiltViewModel()

        val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
        val cameraOpt = settings?.cameraOptimization ?: true
        val avifEnabled = settings?.avifEnabled ?: true

        val projectState by detailViewModel.selectedProject.collectAsStateWithLifecycle()
        val projectName = projectState?.name ?: ""

        LaunchedEffect(cameraProjectId) {
            if (cameraProjectId != -1L) {
                detailViewModel.setProjectId(cameraProjectId)
            }
        }

        CameraScreen(
            onPhotoCaptured = { uri ->
                if (logId != -1L) {
                    viewModel.savePhotoInBackground(uri, cameraProjectId, logId, avifEnabled, projectName)
                } else if (cameraProjectId != -1L) {
                    viewModel.savePhotoInBackground(uri, cameraProjectId, -1L, avifEnabled, projectName)
                }
            },
            onClose = { navController.popBackStack() },
            enableOptimization = cameraOpt,
            enableAvif = avifEnabled,
            onToggleOptimization = { settingsViewModel.setCameraOptimization(it) },
            onToggleAvif = { settingsViewModel.setAvifEnabled(it) }
        )
    }
}

// ─── Trash ─────────────────────────────────────────────────────────
// Utility screen → fast symmetric transitions (250ms) — same as Settings.

fun NavGraphBuilder.trashRoute(navController: NavController, language: String) {
    composable(
        route = Routes.TRASH,
        enterTransition = { FotoRaporMotion.navEnter(FotoRaporMotion.NavDurationFast) },
        exitTransition = { FotoRaporMotion.navExit(FotoRaporMotion.NavDurationFast) },
        popEnterTransition = { FotoRaporMotion.navPopEnter(FotoRaporMotion.NavDurationFast) },
        popExitTransition = { FotoRaporMotion.navPopExit(FotoRaporMotion.NavDurationFast) }
    ) {
        TrashScreen(
            onBack = { navController.popBackStack() },
            language = language
        )
    }
}
