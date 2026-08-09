@file:Suppress("LocalContextGetResourceValueCall", "MatchingDeclarationName", "MaxLineLength")
package com.fatihenes.photoreport.feature.project.ui

import android.content.Context
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fatihenes.photoreport.core.common.util.DateUtils
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporMotion
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporTokens
import com.fatihenes.photoreport.core.model.DailyLogWithPhotos
import com.fatihenes.photoreport.core.model.Photo
import com.fatihenes.photoreport.core.model.Project
import com.fatihenes.photoreport.core.ui.R
import com.fatihenes.photoreport.core.ui.components.AppEmptyState
import com.fatihenes.photoreport.core.ui.navigation.LocalSnackbarHostState
import com.fatihenes.photoreport.feature.project.viewmodel.ProjectDetailViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

data class ProjectDetailScreenParams(
    val project: Project?,
    val logs: List<DailyLogWithPhotos>,
    val onBack: () -> Unit,
    val onDeleteProject: () -> Unit,
    val onAddPhoto: (Long?) -> Unit,
    val onDeletePhoto: (Photo) -> Unit,
    val onDeletePhotos: (List<Long>) -> Unit,
    val onNoteChange: (Long, String) -> Unit,
    val onUpdateRotation: (Long, Float) -> Unit,
    val onAddLogForDate: (Long) -> Unit,
    val onImportPhotoToLog: (Long, String) -> Unit,
    val onExportProject: (String, Int, String) -> Unit,
    val viewModel: ProjectDetailViewModel,
    val language: String = "tr",
)

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod", "FunctionName")
@Composable
fun ProjectDetailScreen(params: ProjectDetailScreenParams) {
    val displayProject = params.project ?: Project(id = -1L, name = "", colorHex = "#808080")
    val projectColor = remember(displayProject.colorHex) {
        runCatching { Color(displayProject.colorHex.toColorInt()) }.getOrDefault(Color.Gray)
    }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHost = LocalSnackbarHostState.current

    var showDeleteConfirm by remember { mutableStateOf(value = false) }
    var showExportDialog by remember { mutableStateOf(value = false) }
    var selectedPhotoForFullView by remember { mutableStateOf<Long?>(null) }
    var showFullGalleryByLogId by remember { mutableStateOf<Long?>(null) }
    var isTransitionFinished by remember { mutableStateOf(value = false) }

    LaunchedEffect(Unit) {
        delay(FotoRaporMotion.DURATION_SHORT.milliseconds)
        isTransitionFinished = true
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (!isGranted) scope.launch { snackbarHost.showSnackbar("Bildirim izni verilmedi.") }
        showExportDialog = true
    }

    val allProjectPhotos by params.viewModel.allProjectPhotos.collectAsStateWithLifecycle()
    val viewState = ProjectDetailViewState(
        project = displayProject,
        logs = params.logs,
        allProjectPhotos = allProjectPhotos,
        showFullGalleryByLogId = showFullGalleryByLogId,
        showDeleteConfirm = showDeleteConfirm,
        showExportDialog = showExportDialog,
        selectedPhotoForFullView = selectedPhotoForFullView,
        language = params.language,
    )
    val actions = ProjectDetailActions(
        onGalleryDismiss = { showFullGalleryByLogId = null },
        onPhotoClick = { selectedPhotoForFullView = it.id },
        onDeletePhotos = params.onDeletePhotos,
        onDeleteConfirmDismiss = { showDeleteConfirm = false },
        onDeleteProject = { showDeleteConfirm = false; params.onDeleteProject() },
        onExportDismiss = { showExportDialog = false },
        onExportProject = params.onExportProject,
        onPhotoViewDismiss = { selectedPhotoForFullView = null },
        onDeletePhoto = params.onDeletePhoto,
        onUpdateRotation = params.onUpdateRotation,
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ProjectDetailTopBar(
                params = TopBarParams(
                    state = viewState,
                    projectColor = projectColor,
                    onBack = params.onBack,
                    onShareClick = {
                        handleShareClick(context, notificationLauncher) { showExportDialog = true }
                    },
                    haptic = haptic,
                    actions = actions,
                ),
            )
        },
        floatingActionButton = { DetailFab(projectColor, haptic) { params.onAddPhoto(null) } },
    ) { padding ->
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = FotoRaporTokens.ScreenPaddingHorizontal)
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding(),
            contentPadding = PaddingValues(top = FotoRaporTokens.SpacingS),
        ) {
            item {
                ProjectHeroBanner(
                    projectName = displayProject.name,
                    logCount = params.logs.size,
                    photoCount = allProjectPhotos.size,
                    projectColor = projectColor,
                ) {
                    showExportDialog = true
                }
            }
            item {
                WeekCalendar(
                    projectColor = projectColor,
                    onDateSelected = { date ->
                        scope.launch {
                            val targetMillis = DateUtils.getStartOfDayEpochMillis(date)
                            val idx = params.logs.indexOfFirst { it.log.date == targetMillis }
                            if (idx != -1) {
                                val headerOffset = 3
                                listState.animateScrollToItem(idx + headerOffset)
                            }
                        }
                    },
                )
            }
            item {
                AddDateCard(
                    projectColor = projectColor,
                    onAddLogForDate = params.onAddLogForDate,
                )
            }
            timelineSection(
                params = TimelineSectionParams(
                    state = viewState,
                    isTransitionFinished = isTransitionFinished,
                    onNoteChange = params.onNoteChange,
                    onAddPhoto = params.onAddPhoto,
                    onImportPhotoToLog = params.onImportPhotoToLog,
                    timelineActions = ProjectDetailTimelineActions(
                        onPhotoClick = { selectedPhotoForFullView = it.id },
                        onMorePhotosClick = { showFullGalleryByLogId = it },
                    ),
                ),
            )
            item { Spacer(modifier = Modifier.height(96.dp)) }
        }
    }
    ProjectDetailDialogs(viewState, actions)
}

private fun handleShareClick(context: Context, launcher: ManagedActivityResultLauncher<String, Boolean>, onShowDialog: () -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val hasPerm = ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (hasPerm) onShowDialog() else launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    } else onShowDialog()
}

@Suppress("FunctionName")
@Composable
private fun DetailFab(color: Color, haptic: HapticFeedback, onClick: () -> Unit) {
    FloatingActionButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        containerColor = color,
        contentColor = Color.White,
        shape = RoundedCornerShape(FotoRaporTokens.RadiusM),
    ) {
        Icon(
            Icons.Default.CameraAlt,
            contentDescription = stringResource(R.string.acc_shutter),
            modifier = Modifier.size(FotoRaporTokens.IconSizeM),
        )
    }
}

private data class ProjectDetailTimelineActions(
    val onPhotoClick: (Photo) -> Unit,
    val onMorePhotosClick: (Long) -> Unit,
)

private data class TimelineSectionParams(
    val state: ProjectDetailViewState,
    val isTransitionFinished: Boolean,
    val onNoteChange: (Long, String) -> Unit,
    val onAddPhoto: (Long?) -> Unit,
    val onImportPhotoToLog: (Long, String) -> Unit,
    val timelineActions: ProjectDetailTimelineActions,
)

private fun LazyListScope.timelineSection(params: TimelineSectionParams) {
    item { TimelineHeader() }
    if (params.isTransitionFinished) {
        if (params.state.logs.isEmpty()) {
            item { TimelineEmptyState { params.onAddPhoto(null) } }
        } else {
            items(params.state.logs, key = { it.log.id }) { logWithPhotos ->
                TimelineItemWrapper(
                    TimelineWrapperParams(
                        logWithPhotos = logWithPhotos,
                        color = remember(params.state.project.colorHex) {
                            runCatching { Color(params.state.project.colorHex.toColorInt()) }
                                .getOrDefault(Color.Gray)
                        },
                        language = params.state.language,
                        onNoteChange = params.onNoteChange,
                        onAddPhoto = params.onAddPhoto,
                        onImportPhotoToLog = params.onImportPhotoToLog,
                        timelineActions = params.timelineActions,
                    ),
                )
            }
        }
    }
}

@Composable
private fun TimelineHeader() {
    Column {
        Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingXXL))
        Text(
            stringResource(R.string.timeline_label).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingL))
    }
}

@Composable
private fun TimelineEmptyState(onAddPhoto: () -> Unit) {
    var visible by remember { mutableStateOf(value = false) }
    LaunchedEffect(Unit) { visible = true }
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(FotoRaporMotion.NAV_DURATION_STANDARD)) + slideInVertically(animationSpec = tween(FotoRaporMotion.NAV_DURATION_STANDARD), initialOffsetY = { 30 })
    ) {
        AppEmptyState(
            icon = Icons.Default.CameraAlt,
            title = stringResource(R.string.empty_photos_title),
            description = stringResource(R.string.empty_photos_desc),
            actionLabel = stringResource(R.string.empty_photos_action),
            onActionClick = onAddPhoto
        )
    }
}

private data class TimelineWrapperParams(
    val logWithPhotos: DailyLogWithPhotos,
    val color: Color,
    val language: String,
    val onNoteChange: (Long, String) -> Unit,
    val onAddPhoto: (Long?) -> Unit,
    val onImportPhotoToLog: (Long, String) -> Unit,
    val timelineActions: ProjectDetailTimelineActions
)

@Composable
private fun TimelineItemWrapper(params: TimelineWrapperParams) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(FotoRaporMotion.NAV_DURATION_STANDARD, easing = FotoRaporMotion.EasingEmphasized)) +
                slideInVertically(animationSpec = tween(FotoRaporMotion.NAV_DURATION_STANDARD, easing = FotoRaporMotion.EasingEmphasized), initialOffsetY = { 40 })
    ) {
        TimelineBlock(
            state = TimelineViewState(
                log = params.logWithPhotos.log,
                photos = params.logWithPhotos.photos.sortedByDescending { it.id },
                projectColor = params.color,
                isSelectionMode = false,
                selectedPhotoIds = emptyList(),
                language = params.language
            ),
            actions = TimelineActions(
                onPhotoClick = params.timelineActions.onPhotoClick,
                onMorePhotosClick = { params.timelineActions.onMorePhotosClick(params.logWithPhotos.log.id) },
                onNoteChange = { params.onNoteChange(params.logWithPhotos.log.id, it) },
                onAddPhotoClick = { params.onAddPhoto(params.logWithPhotos.log.id) },
                onImportPhotoClick = { params.onImportPhotoToLog(params.logWithPhotos.log.id, it.toString()) }
            )
        )
    }
}
