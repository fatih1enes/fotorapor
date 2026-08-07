@file:Suppress("LocalContextGetResourceValueCall")
package com.fatihenes.photoreport.feature.project.ui

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporTokens
import com.fatihenes.photoreport.core.model.DailyLogWithPhotos
import com.fatihenes.photoreport.core.model.Photo
import com.fatihenes.photoreport.core.model.Project
import com.fatihenes.photoreport.core.ui.R
import com.fatihenes.photoreport.core.ui.components.AppEmptyState
import com.fatihenes.photoreport.core.ui.navigation.LocalSnackbarHostState
import com.fatihenes.photoreport.feature.project.viewmodel.ProjectDetailViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    project: Project?,
    logs: List<DailyLogWithPhotos>,
    onBack: () -> Unit,
    onDeleteProject: () -> Unit,
    onAddPhoto: (Long?) -> Unit,
    onDeletePhoto: (Photo) -> Unit,
    onDeletePhotos: (List<Long>) -> Unit,
    onNoteChange: (Long, String) -> Unit,
    onUpdateRotation: (Long, Float) -> Unit,
    onAddLogForDate: (Long) -> Unit,
    onImportPhotoToLog: (Long, String) -> Unit,
    onExportProject: (String, Int, String) -> Unit,
    viewModel: ProjectDetailViewModel,
    language: String = "tr",
) {
    val displayProject = project ?: Project(id = -1L, name = "", colorHex = "#808080")
    val projectColor = remember(displayProject.colorHex) { runCatching { Color(displayProject.colorHex.toColorInt()) }.getOrDefault(Color.Gray) }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHost = LocalSnackbarHostState.current

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var selectedPhotoForFullView by remember { mutableStateOf<Long?>(null) }
    var showFullGalleryByLogId by remember { mutableStateOf<Long?>(null) }
    var isTransitionFinished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Wait for the NavGraph transition (300ms) to completely finish
        // before doing heavy composition of the timeline. This eliminates tearing/jank.
        kotlinx.coroutines.delay(350)
        isTransitionFinished = true
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            coroutineScope.launch { snackbarHost.showSnackbar("Bildirim izni verilmediği için arkaplan işlemi durumu gösterilemeyecek.") }
        }
        showExportDialog = true
    }

    val allProjectPhotos by viewModel.allProjectPhotos.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ProjectDetailTopBar(
                project = displayProject,
                photoCount = allProjectPhotos.size,
                logCount = logs.size,
                projectColor = projectColor,
                onBack = onBack,
                onShareClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val hasPermission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        if (hasPermission) showExportDialog = true else notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        showExportDialog = true
                    }
                },
                onDeleteClick = { showDeleteConfirm = true },
                haptic = haptic
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onAddPhoto(null)
                },
                containerColor = projectColor,
                contentColor = Color.White,
                shape = RoundedCornerShape(FotoRaporTokens.RadiusM),
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = FotoRaporTokens.ElevationM,
                    pressedElevation = FotoRaporTokens.ElevationL
                )
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = stringResource(R.string.acc_shutter),
                    modifier = Modifier.size(FotoRaporTokens.IconSizeM)
                )
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = FotoRaporTokens.ScreenPaddingHorizontal)
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding(),
            contentPadding = PaddingValues(top = FotoRaporTokens.SpacingS)
        ) {
            projectHeroSection(displayProject, logs.size, allProjectPhotos.size, projectColor, onExportClick = { showExportDialog = true })

            calendarAndAddSection(projectColor, onAddLogForDate) { date ->
                coroutineScope.launch {
                    val targetMillis = DateUtils.getStartOfDayEpochMillis(date)
                    val index = logs.indexOfFirst { it.log.date == targetMillis }
                    if (index != -1) listState.animateScrollToItem(index + 3)
                }
            }

            if (isTransitionFinished) {
                timelineSection(logs, projectColor, language, onPhotoClick = { selectedPhotoForFullView = it.id }, onMorePhotosClick = { showFullGalleryByLogId = it }, onNoteChange, onAddPhoto, onImportPhotoToLog)
            }

            item { Spacer(modifier = Modifier.height(96.dp)) }
        }
    }

    ProjectDetailDialogs(
        project = displayProject,
        logs = logs,
        allProjectPhotos = allProjectPhotos,
        showFullGalleryByLogId = showFullGalleryByLogId,
        onGalleryDismiss = { showFullGalleryByLogId = null },
        onPhotoClick = { selectedPhotoForFullView = it.id },
        onDeletePhotos = onDeletePhotos,
        showDeleteConfirm = showDeleteConfirm,
        onDeleteConfirmDismiss = { showDeleteConfirm = false },
        onDeleteProject = { showDeleteConfirm = false; onDeleteProject() },
        showExportDialog = showExportDialog,
        onExportDismiss = { showExportDialog = false },
        onExportProject = onExportProject,
        selectedPhotoForFullView = selectedPhotoForFullView,
        onPhotoViewDismiss = { selectedPhotoForFullView = null },
        onDeletePhoto = onDeletePhoto,
        onUpdateRotation = onUpdateRotation,
        language = language
    )
}

@Composable
private fun LoadingIndicator() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier.size(FotoRaporTokens.IconSizeL),
            strokeWidth = 2.5.dp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun LazyListScope.projectHeroSection(project: Project, logCount: Int, photoCount: Int, color: Color, onExportClick: () -> Unit) {
    item(key = "project_hero_banner") {
        ProjectHeroBanner(project.name, logCount, photoCount, color, onExportClick)
    }
}

private fun LazyListScope.calendarAndAddSection(color: Color, onAddLogForDate: (Long) -> Unit, onDateSelected: (java.time.LocalDate) -> Unit) {
    item(key = "week_calendar") {
        WeekCalendar(projectColor = color, onDateSelected = onDateSelected)
    }
    item(key = "add_date_btn") {
        AddDateCard(projectColor = color, onAddLogForDate = onAddLogForDate)
    }
}

private fun LazyListScope.timelineSection(
    logs: List<DailyLogWithPhotos>,
    color: Color,
    language: String,
    onPhotoClick: (Photo) -> Unit,
    onMorePhotosClick: (Long) -> Unit,
    onNoteChange: (Long, String) -> Unit,
    onAddPhoto: (Long?) -> Unit,
    onImportPhotoToLog: (Long, String) -> Unit
) {
    item(key = "timeline_header") {
        var isVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { isVisible = true }
        
        androidx.compose.animation.AnimatedVisibility(
            visible = isVisible,
            enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(400))
        ) {
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
    }
    if (logs.isEmpty()) {
        item(key = "timeline_empty_state") {
            var isVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { isVisible = true }
            
            androidx.compose.animation.AnimatedVisibility(
                visible = isVisible,
                enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(500)) + 
                        androidx.compose.animation.slideInVertically(initialOffsetY = { 30 }, animationSpec = androidx.compose.animation.core.tween(500))
            ) {
                AppEmptyState(
                    icon = Icons.Default.CameraAlt,
                    title = stringResource(R.string.empty_photos_title),
                    description = stringResource(R.string.empty_photos_desc),
                    actionLabel = stringResource(R.string.empty_photos_action),
                    onActionClick = { onAddPhoto(null) }
                )
            }
        }
    }
    items(logs, key = { it.log.id }, contentType = { "timeline_block" }) { logWithPhotos ->
        var isVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { isVisible = true }
        
        androidx.compose.animation.AnimatedVisibility(
            visible = isVisible,
            enter = androidx.compose.animation.fadeIn(
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = 500, 
                    easing = com.fatihenes.photoreport.core.designsystem.theme.FotoRaporMotion.EasingEmphasized
                )
            ) + androidx.compose.animation.slideInVertically(
                initialOffsetY = { 40 },
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = 500, 
                    easing = com.fatihenes.photoreport.core.designsystem.theme.FotoRaporMotion.EasingEmphasized
                )
            )
        ) {
            val sortedPhotos = remember(logWithPhotos.photos) { logWithPhotos.photos.sortedByDescending { it.id } }
            TimelineBlock(
                log = logWithPhotos.log,
                photos = sortedPhotos,
                projectColor = color,
                isSelectionMode = false,
                selectedPhotoIds = emptyList(),
                language = language,
                onPhotoClick = onPhotoClick,
                onMorePhotosClick = { onMorePhotosClick(logWithPhotos.log.id) },
                onNoteChange = { onNoteChange(logWithPhotos.log.id, it) },
                onAddPhotoClick = { onAddPhoto(logWithPhotos.log.id) },
                onImportPhotoClick = { localUri -> onImportPhotoToLog(logWithPhotos.log.id, localUri.toString()) }
            )
        }
    }
}
