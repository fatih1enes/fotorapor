package com.fatihenes.photoreport.ui.navigation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fatihenes.photoreport.R
import com.fatihenes.photoreport.ui.CameraScreen
import com.fatihenes.photoreport.ui.DashboardScreen
import com.fatihenes.photoreport.ui.ProjectDetailScreen
import com.fatihenes.photoreport.ui.SettingsScreen
import com.fatihenes.photoreport.ui.TrashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.fatihenes.photoreport.ui.viewmodel.AppViewModel
import com.fatihenes.photoreport.ui.viewmodel.DashboardViewModel
import com.fatihenes.photoreport.ui.viewmodel.ProjectDetailViewModel
import com.fatihenes.photoreport.ui.viewmodel.UiState

/**
 * Main navigation graph for the Şantiye Günlüğü app.
 * Extracted from MainActivity to follow SRP — the Activity only sets up
 * the theme and calls this composable.
 */
@Composable
fun AppNavGraph(
    viewModel: AppViewModel,
    initialCameraProjectId: Long = -1L,
    initialProjectDetailId: Long = -1L
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    // --- Permission Handling ---
    var pendingLogId by remember { mutableStateOf<Long?>(null) }
    var pendingProjectId by remember { mutableStateOf<Long?>(null) }
    var showPermissionRationale by remember { mutableStateOf(false) }
    var initialNavDone by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(initialCameraProjectId, initialProjectDetailId) {
        if (!initialNavDone) {
            if (initialCameraProjectId != -1L) {
                navController.navigate(Routes.camera(null, initialCameraProjectId))
                initialNavDone = true
            } else if (initialProjectDetailId != -1L) {
                navController.navigate(Routes.detail(initialProjectDetailId))
                initialNavDone = true
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            navController.navigate(Routes.camera(pendingLogId, pendingProjectId))
        } else {
            val activity = context as? android.app.Activity
            if (activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)) {
                showPermissionRationale = true
            } else {
                Toast.makeText(context, context.getString(R.string.camera_permission_required), Toast.LENGTH_SHORT).show()
            }
        }
        pendingLogId = null
        pendingProjectId = null
    }

    if (showPermissionRationale) {
        CameraPermissionRationaleDialog(
            onDismiss = { showPermissionRationale = false },
            onGoToSettings = {
                showPermissionRationale = false
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        )
    }

    // --- Navigation Host ---
    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD,
        modifier = Modifier,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(400, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(400))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 3 },
                animationSpec = tween(400, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(400))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 3 },
                animationSpec = tween(400, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(400))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(400, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(400))
        }
    ) {
        composable(Routes.DASHBOARD) {
            val dashboardViewModel: DashboardViewModel = hiltViewModel()
            val projects by dashboardViewModel.projects.collectAsStateWithLifecycle()
            val actionState by dashboardViewModel.projectActionState.collectAsStateWithLifecycle()

            LaunchedEffect(actionState) {
                if (actionState is UiState.Success) {
                    Toast.makeText(context, context.getString(R.string.project_added_success), Toast.LENGTH_SHORT).show()
                    dashboardViewModel.resetProjectActionState()
                } else if (actionState is UiState.Error) {
                    Toast.makeText(context, (actionState as UiState.Error).message, Toast.LENGTH_SHORT).show()
                    dashboardViewModel.resetProjectActionState()
                }
            }

            DashboardScreen(
                projects = projects,
                onProjectClick = { project ->
                    navController.navigate(Routes.detail(project.id))
                },
                onAddProject = { name, color ->
                    dashboardViewModel.addProject(name, color)
                },
                onSettingsClick = {
                    navController.navigate(Routes.SETTINGS)
                },
                onTrashClick = { navController.navigate(Routes.TRASH) }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                viewModel = viewModel
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
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
                        pendingLogId = logId
                        pendingProjectId = projectId
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                onDeletePhoto = { photo ->
                    detailViewModel.deletePhoto(photo)
                },
                onDeletePhotos = { ids ->
                    detailViewModel.deletePhotos(ids)
                },
                onNoteChange = { logId, note ->
                    detailViewModel.updateNote(logId, note)
                },
                onUpdateRotation = { photoId, rotation ->
                    detailViewModel.updatePhotoRotation(photoId, rotation)
                },
                onAddLogForDate = { date ->
                    project?.let { detailViewModel.addLogForDate(it.id, date) }
                },
                onImportPhotoToLog = { logId, filePath ->
                    detailViewModel.addPhotoToLog(logId, filePath)
                },
                onExportProject = { format, quality ->
                    project?.let { detailViewModel.exportProject(context, it.id, it.name, format, quality) }
                }
            )
        }

        composable(
            route = Routes.CAMERA,
            arguments = listOf(
                navArgument("logId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("projectId") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val logId = backStackEntry.arguments?.getLong("logId") ?: -1L
            val cameraProjectId = backStackEntry.arguments?.getLong("projectId") ?: -1L

            val detailViewModel: ProjectDetailViewModel = hiltViewModel()
            val cameraOpt by viewModel.cameraOptimizationEnabled.collectAsStateWithLifecycle()
            val avifEnabled by viewModel.avifEnabled.collectAsStateWithLifecycle()

            val projectState by detailViewModel.selectedProject.collectAsStateWithLifecycle()
            val projectName = projectState?.name ?: ""

            // Set projectId so the ViewModel knows which project we're working with
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
                onToggleOptimization = { viewModel.setCameraOptimization(it) },
                onToggleAvif = { viewModel.setAvifEnabled(it) }
            )
        }

        composable(route = Routes.TRASH) {
            TrashScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

/**
 * Dialog shown when camera permission has been permanently denied.
 */
@Composable
private fun CameraPermissionRationaleDialog(
    onDismiss: () -> Unit,
    onGoToSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.camera_permission_rationale_title)) },
        text = { Text(stringResource(R.string.camera_permission_rationale_desc)) },
        confirmButton = {
            Button(onClick = onGoToSettings) {
                Text(stringResource(R.string.camera_permission_go_settings))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_btn))
            }
        }
    )
}
