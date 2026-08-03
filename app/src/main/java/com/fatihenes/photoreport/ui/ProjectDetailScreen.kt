@file:Suppress("LocalContextGetResourceValueCall")
package com.fatihenes.photoreport.ui

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.core.content.ContextCompat
import android.os.Build
import kotlinx.coroutines.launch
import com.fatihenes.photoreport.ui.navigation.LocalSnackbarHostState
import java.time.LocalDate
import com.fatihenes.photoreport.R
import com.fatihenes.photoreport.data.PhotoEntity
import com.fatihenes.photoreport.data.ProjectEntity
import com.fatihenes.photoreport.data.LogWithPhotos
import com.fatihenes.photoreport.ui.components.ExportDialog
import com.fatihenes.photoreport.util.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    project: ProjectEntity?,
    logs: List<LogWithPhotos>,
    onBack: () -> Unit,
    onDeleteProject: () -> Unit,
    onAddPhoto: (Long?) -> Unit,
    onDeletePhoto: (PhotoEntity) -> Unit,
    onDeletePhotos: (List<Long>) -> Unit,
    onNoteChange: (Long, String) -> Unit,
    onUpdateRotation: (Long, Float) -> Unit,
    onAddLogForDate: (Long) -> Unit,
    onImportPhotoToLog: (Long, String) -> Unit,
    onExportProject: (String, Int, String) -> Unit,
    viewModel: com.fatihenes.photoreport.ui.viewmodel.ProjectDetailViewModel,
    language: String = "tr"
) {
    if (project == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val projectColor = remember(project.colorHex) { Color(project.colorHex.toColorInt()) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
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
            coroutineScope.launch { snackbarHost.showSnackbar("Bildirim izni verilmediği için arkaplan işlemi durumu gösterilemeyecek.") }
        }
        showExportDialog = true
    }

    // Compute allProjectPhotos from logs, memoized properly
    val allProjectPhotos by viewModel.allProjectPhotos.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(project.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.project_details_label), style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_label))
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.acc_more_options))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.share_as_file)) },
                            onClick = {
                                showMenu = false
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val hasPermission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    if (hasPermission) {
                                        showExportDialog = true
                                    } else {
                                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                } else {
                                    showExportDialog = true
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = stringResource(R.string.acc_share), tint = projectColor) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete_project_menu), color = MaterialTheme.colorScheme.error) },
                            onClick = { 
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                showMenu = false
                                showDeleteConfirm = true 
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.acc_delete), tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onAddPhoto(null)
                },
                containerColor = projectColor,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = stringResource(R.string.acc_shutter))
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
                .padding(horizontal = 16.dp)
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding(),
            contentPadding = PaddingValues(top = 16.dp)
        ) {
            item(key = "week_calendar") {
                WeekCalendar(
                    projectColor = projectColor,
                    existingLogDates = existingLogDates,
                    onDateSelected = { date ->
                        coroutineScope.launch {
                            val index = logs.indexOfFirst { it.log.date == date }
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
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, projectColor),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = projectColor)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = stringResource(R.string.acc_calendar), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(stringResource(R.string.add_date_card), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
            item(key = "timeline_header") {
                Spacer(modifier = Modifier.height(24.dp))
                Text(stringResource(R.string.timeline_label), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(16.dp))
            }
            items(logs, key = { it.log.id }) { logWithPhotos ->
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
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }

    // Dialog overlays
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
            title = { Text(stringResource(R.string.delete_project_title)) },
            text = { Text(stringResource(R.string.delete_project_desc, project.name)) },
            confirmButton = {
                Button(
                    onClick = { showDeleteConfirm = false; onDeleteProject() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(R.string.delete_confirm_btn), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.cancel_btn)) }
            }
        )
    }

    if (showExportDialog) {
        ExportDialog(
            project = project,
            logs = logs,
            viewModel = viewModel,
            onDismiss = { showExportDialog = false },
            onExportPdf = { quality ->
                showExportDialog = false
                onExportProject("PDF", quality, language)
                coroutineScope.launch { snackbarHost.showSnackbar(context.getString(R.string.pdf_preparing) + " (Arka plan)") }
            },
            onExportZip = { quality ->
                showExportDialog = false
                onExportProject("ZIP", quality, language)
                coroutineScope.launch { snackbarHost.showSnackbar("ZIP dışa aktarımı arka planda başlatıldı. Bildirimleri kontrol edin.") }
            }
        )
    }

    selectedPhotoForFullView?.let { photoId ->
        val index = allProjectPhotos.indexOfFirst { it.id == photoId }.coerceAtLeast(0)

        AnimatedVisibility(
            visible = selectedPhotoForFullView != null,
            enter = fadeIn(tween(300)) + scaleIn(initialScale = 0.9f, animationSpec = tween(300)),
            exit = fadeOut(tween(300)) + scaleOut(targetScale = 0.9f, animationSpec = tween(300)),
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
            } // Box end
}

@Composable
fun WeekCalendar(
    projectColor: Color,
    existingLogDates: Set<Long>,
    onDateSelected: (Long) -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val initialDate = LocalDate.now()
    val pagerState = rememberPagerState(initialPage = 100, pageCount = { 200 })

    val currentMonday = remember(pagerState.currentPage) {
        initialDate
            .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
            .plusWeeks((pagerState.currentPage - 100).toLong())
    }

    val turkishLocale = remember { java.util.Locale.Builder().setLanguage("tr").setRegion("TR").build() }
    val monthYearText = remember(currentMonday) {
        val month = currentMonday.month.getDisplayName(java.time.format.TextStyle.FULL, turkishLocale)
            .replaceFirstChar { it.uppercase() }
        "$month ${currentMonday.year}"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = monthYearText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                beyondViewportPageCount = 0
            ) { page ->
                val weekOffset = page - 100
                val mondayOfThisWeek = remember(page) {
                    initialDate
                        .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                        .plusWeeks(weekOffset.toLong())
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (i in 0 until 7) {
                        val date = remember(mondayOfThisWeek, i) { mondayOfThisWeek.plusDays(i.toLong()) }
                        val startOfDay = remember(date) { DateUtils.getStartOfDayEpochMillis(date) }
                        val isLogged = existingLogDates.contains(startOfDay)
                        val isToday = remember(date) { date == LocalDate.now() }

                        val dayName = when (date.dayOfWeek) {
                            java.time.DayOfWeek.MONDAY -> stringResource(R.string.day_mon)
                            java.time.DayOfWeek.TUESDAY -> stringResource(R.string.day_tue)
                            java.time.DayOfWeek.WEDNESDAY -> stringResource(R.string.day_wed)
                            java.time.DayOfWeek.THURSDAY -> stringResource(R.string.day_thu)
                            java.time.DayOfWeek.FRIDAY -> stringResource(R.string.day_fri)
                            java.time.DayOfWeek.SATURDAY -> stringResource(R.string.day_sat)
                            else -> stringResource(R.string.day_sun)
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    onDateSelected(startOfDay)
                                }
                                .background(
                                    if (isToday) projectColor.copy(alpha = 0.1f)
                                    else if (isLogged) projectColor.copy(alpha = 0.05f)
                                    else Color.Transparent
                                )
                                .border(
                                    1.dp,
                                    if (isToday) projectColor else Color.Transparent,
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = dayName,
                                fontSize = 11.sp,
                                color = if (isToday) projectColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = date.dayOfMonth.toString(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isLogged) projectColor else MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        if (isLogged) projectColor else Color.Transparent,
                                        CircleShape
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}
