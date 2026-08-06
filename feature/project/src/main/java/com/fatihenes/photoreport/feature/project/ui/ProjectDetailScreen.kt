@file:Suppress("LocalContextGetResourceValueCall")
package com.fatihenes.photoreport.feature.project.ui

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporMotion
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporTokens
import com.fatihenes.photoreport.core.model.DailyLogWithPhotos
import com.fatihenes.photoreport.core.model.Photo
import com.fatihenes.photoreport.core.model.Project
import com.fatihenes.photoreport.core.ui.R
import com.fatihenes.photoreport.core.ui.components.AppEmptyState
import com.fatihenes.photoreport.core.ui.navigation.LocalSnackbarHostState
import com.fatihenes.photoreport.feature.project.viewmodel.ProjectDetailViewModel
import com.fatihenes.photoreport.feature.export.ui.ExportDialog
import kotlinx.coroutines.launch
import java.time.LocalDate

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
    language: String = "tr"
) {
    if (project == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier.size(FotoRaporTokens.IconSizeL),
                strokeWidth = 2.5.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }
        return
    }

    val projectColor = remember(project.colorHex) { Color(project.colorHex.toColorInt()) }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHost = LocalSnackbarHostState.current
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var selectedPhotoForFullView by remember { mutableStateOf<Long?>(null) }
    var showFullGalleryByLogId by remember { mutableStateOf<Long?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            coroutineScope.launch { snackbarHost.showSnackbar(context.getString(R.string.project_detail_notif_permission_denied)) }
        }
        showExportDialog = true
    }

    val allProjectPhotos by viewModel.allProjectPhotos.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(projectColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    project.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    stringResource(R.string.project_detail_stat_summary, logs.size, allProjectPhotos.size),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back_label),
                                modifier = Modifier.size(FotoRaporTokens.IconSizeS)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val hasPermission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                if (hasPermission) showExportDialog = true else notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                showExportDialog = true
                            }
                        }) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = stringResource(R.string.share_as_file),
                                tint = projectColor,
                                modifier = Modifier.size(FotoRaporTokens.IconSizeS)
                            )
                        }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.acc_more_options),
                                modifier = Modifier.size(FotoRaporTokens.IconSizeS)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            shape = RoundedCornerShape(FotoRaporTokens.RadiusM)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(R.string.delete_project_menu),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showMenu = false
                                    showDeleteConfirm = true
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.acc_delete),
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(FotoRaporTokens.IconSizeS)
                                    )
                                }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
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
            val existingLogDates = remember(logs) {
                logs.map { it.log.date }.toSet()
            }

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
                // ── Hero Banner ──────────────────────────────────
                item(key = "project_hero_banner") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = FotoRaporTokens.SpacingL),
                        shape = RoundedCornerShape(FotoRaporTokens.RadiusL),
                        color = projectColor.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, projectColor.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(FotoRaporTokens.SpacingL),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = project.name.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = projectColor,
                                    letterSpacing = 1.5.sp
                                )
                                Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingXXS))
                                Text(
                                    text = stringResource(R.string.project_detail_stat_summary, logs.size, allProjectPhotos.size),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Button(
                                onClick = { showExportDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = projectColor,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(FotoRaporTokens.RadiusS),
                                modifier = Modifier.height(36.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Icon(
                                    Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    stringResource(R.string.project_detail_get_report),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                item(key = "week_calendar") {
                    WeekCalendar(
                        projectColor = projectColor,
                        existingLogDates = existingLogDates,
                        onDateSelected = { date ->
                            coroutineScope.launch {
                                val targetMillis = DateUtils.getStartOfDayEpochMillis(date)
                                val index = logs.indexOfFirst { it.log.date == targetMillis }
                                if (index != -1) {
                                    listState.animateScrollToItem(index + 3)
                                }
                            }
                        }
                    )
                }
                item(key = "add_date_btn") {
                    var showDatePicker by remember { mutableStateOf(false) }
                    if (showDatePicker) {
                        val datePickerState = rememberDatePickerState()
                        DatePickerDialog(
                            onDismissRequest = { showDatePicker = false },
                            confirmButton = {
                                TextButton(onClick = {
                                    datePickerState.selectedDateMillis?.let { selectedDate ->
                                        val cal = java.util.Calendar.getInstance()
                                        cal.timeInMillis = selectedDate
                                        val startOfSelectedDay = DateUtils.getStartOfDay(cal)
                                        onAddLogForDate(startOfSelectedDay)
                                    }
                                    showDatePicker = false
                                }) {
                                    Text(stringResource(R.string.add_btn))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDatePicker = false }) {
                                    Text(stringResource(R.string.cancel_btn))
                                }
                            }
                        ) {
                            DatePicker(state = datePickerState)
                        }
                    }

                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(FotoRaporTokens.ButtonHeightL),
                        shape = RoundedCornerShape(FotoRaporTokens.RadiusM),
                        border = BorderStroke(1.dp, projectColor.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = projectColor)
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = stringResource(R.string.acc_calendar),
                            modifier = Modifier.size(FotoRaporTokens.IconSizeS)
                        )
                        Spacer(modifier = Modifier.width(FotoRaporTokens.SpacingS))
                        Text(
                            stringResource(R.string.add_date_card),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                item(key = "timeline_header") {
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
                if (logs.isEmpty()) {
                    item(key = "timeline_empty_state") {
                        AppEmptyState(
                            icon = Icons.Default.CameraAlt,
                            title = stringResource(R.string.empty_photos_title),
                            description = stringResource(R.string.empty_photos_desc),
                            actionLabel = stringResource(R.string.empty_photos_action),
                            onActionClick = { onAddPhoto(null) }
                        )
                    }
                }
                items(logs, key = { it.log.id }, contentType = { "timeline_block" }) { logWithPhotos ->
                    val sortedPhotos = remember(logWithPhotos.photos) {
                        logWithPhotos.photos.sortedByDescending { it.id }
                    }
                    TimelineBlock(
                        log = logWithPhotos.log,
                        photos = sortedPhotos,
                        projectColor = projectColor,
                        isSelectionMode = false,
                        selectedPhotoIds = emptyList(),
                        language = language,
                        onPhotoClick = { photo ->
                            selectedPhotoForFullView = photo.id
                        },
                        onMorePhotosClick = { showFullGalleryByLogId = logWithPhotos.log.id },
                        onNoteChange = { onNoteChange(logWithPhotos.log.id, it) },
                        onAddPhotoClick = { onAddPhoto(logWithPhotos.log.id) },
                        onImportPhotoClick = { localUri ->
                            onImportPhotoToLog(logWithPhotos.log.id, localUri.toString())
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(96.dp)) }
            }
        }

        showFullGalleryByLogId?.let { logId ->
            val logData = logs.find { it.log.id == logId }
            logData?.let { logWithPhotos ->
                FullGalleryDialog(
                    log = logWithPhotos.log,
                    photos = logWithPhotos.photos,
                    onDismiss = { showFullGalleryByLogId = null },
                    onPhotoClick = { photo ->
                        selectedPhotoForFullView = photo.id
                    },
                    onDeletePhotos = { ids ->
                        onDeletePhotos(ids)
                    }
                )
            }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                shape = RoundedCornerShape(FotoRaporTokens.RadiusL),
                title = {
                    Text(
                        stringResource(R.string.delete_project_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = {
                    Text(
                        stringResource(R.string.delete_project_desc, project.name),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showDeleteConfirm = false; onDeleteProject() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(FotoRaporTokens.RadiusS),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = FotoRaporTokens.ElevationNone
                        )
                    ) {
                        Text(
                            stringResource(R.string.delete_confirm_btn),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text(
                            stringResource(R.string.cancel_btn),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            )
        }

        if (showExportDialog) {
            ExportDialog(
                project = project,
                logs = logs,
                onDismiss = { showExportDialog = false },
                onExportPdf = { quality ->
                    showExportDialog = false
                    onExportProject("PDF", quality, language)
                    coroutineScope.launch { snackbarHost.showSnackbar(context.getString(R.string.pdf_preparing) + " " + context.getString(R.string.project_detail_background_tag)) }
                },
                onExportZip = { quality ->
                    showExportDialog = false
                    onExportProject("ZIP", quality, language)
                    coroutineScope.launch { snackbarHost.showSnackbar(context.getString(R.string.project_detail_zip_started)) }
                }
            )
        }

        selectedPhotoForFullView?.let { photoId ->
            val index = allProjectPhotos.indexOfFirst { it.id == photoId }.coerceAtLeast(0)

            AnimatedVisibility(
                visible = selectedPhotoForFullView != null,
                enter = fadeIn(tween(250)) + scaleIn(initialScale = 0.92f, animationSpec = tween(250)),
                exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.92f, animationSpec = tween(200)),
                modifier = Modifier.fillMaxSize()
            ) {
                FullScreenPhotoDialog(
                    photoList = allProjectPhotos,
                    initialIndex = index,
                    onDismiss = {
                        selectedPhotoForFullView = null
                    },
                    onDelete = { photo ->
                        onDeletePhoto(photo)
                    },
                    onUpdateRotation = onUpdateRotation
                )
            }
        }
    }
}
