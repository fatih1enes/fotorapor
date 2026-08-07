@file:Suppress("LocalContextGetResourceValueCall")
package com.fatihenes.photoreport.ui.navigation

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.fatihenes.photoreport.R
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporMotion
import com.fatihenes.photoreport.core.ui.navigation.LocalSnackbarHostState
import com.fatihenes.photoreport.ui.components.DisclosureDialog
import com.fatihenes.photoreport.ui.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun AppNavGraph(
    viewModel: MainViewModel,
    initialCameraProjectId: Long = -1L,
    initialProjectDetailId: Long = -1L,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        val navController = rememberNavController()
        val scope = rememberCoroutineScope()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        // Wait until settings are loaded before rendering anything
        if (uiState.isLoading) return@CompositionLocalProvider

        if (!uiState.disclosureShown) {
            DisclosureDialog { viewModel.setDisclosureShown(shown = true) }
        }

        // --- Permission & Initial Nav Handling ---
        var pendingLogId by remember { mutableStateOf<Long?>(null) }
        var pendingProjectId by remember { mutableStateOf<Long?>(null) }

        HandleInitialNavigation(navController, initialCameraProjectId, initialProjectDetailId)

        val permissionLauncher = rememberCameraPermissionLauncher(
            navController = navController,
            snackbarHostState = snackbarHostState,
            scope = scope,
            onResetPending = {
                pendingLogId = null
                pendingProjectId = null
            },
            getPendingLogId = { pendingLogId },
            getPendingProjectId = { pendingProjectId }
        )

        // --- Navigation Host ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            AppNavHost(
                navController = navController,
                viewModel = viewModel,
                snackbarHostState = snackbarHostState,
                scope = scope,
                language = uiState.language,
                permissionLauncher = permissionLauncher,
                onSetPending = { logId, projectId ->
                    pendingLogId = logId
                    pendingProjectId = projectId
                }
            )

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .navigationBarsPadding()
            )
        }
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    viewModel: MainViewModel,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    language: String,
    permissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    onSetPending: (Long?, Long?) -> Unit
) {
    // Default NavHost transitions — per-route overrides are in NavGraphExtensions.
    // NavDurationStandard (300ms) is the default; utility routes override with NavDurationFast.
    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD,
        enterTransition = { FotoRaporMotion.navEnter() },
        exitTransition = { FotoRaporMotion.navExit() },
        popEnterTransition = { FotoRaporMotion.navPopEnter() },
        popExitTransition = { FotoRaporMotion.navPopExit() }
    ) {
        dashboardRoute(navController, snackbarHostState, scope, language)
        settingsRoute(navController)
        detailRoute(navController, language, permissionLauncher, onSetPending)
        cameraRoute(navController, viewModel)
        trashRoute(navController, language)
    }
}

@Composable
private fun HandleInitialNavigation(
    navController: NavHostController,
    initialCameraProjectId: Long,
    initialProjectDetailId: Long
) {
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
}

@Composable
private fun rememberCameraPermissionLauncher(
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    onResetPending: () -> Unit,
    getPendingLogId: () -> Long?,
    getPendingProjectId: () -> Long?
): ManagedActivityResultLauncher<String, Boolean> {
    val context = LocalContext.current
    var showPermissionRationale by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            navController.navigate(Routes.camera(getPendingLogId(), getPendingProjectId()))
        } else {
            val activity = context as? android.app.Activity
            if (activity != null && (!ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA))) {
                showPermissionRationale = true
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.camera_permission_required))
                }
            }
        }
        onResetPending()
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

    return permissionLauncher
}

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
