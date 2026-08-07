package com.fatihenes.photoreport.feature.dashboard.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import kotlinx.coroutines.launch
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporMotion
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporTokens
import com.fatihenes.photoreport.core.model.Project
import com.fatihenes.photoreport.core.ui.R
import com.fatihenes.photoreport.core.ui.components.AppEmptyState
import com.fatihenes.photoreport.core.ui.components.HeroStatChip
import com.fatihenes.photoreport.core.ui.components.PillSegmentedControl
import com.fatihenes.photoreport.feature.dashboard.components.MonthlyCalendar
import java.time.LocalDate

@Composable
fun DashboardFab(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = RoundedCornerShape(FotoRaporTokens.RadiusM),
        modifier = Modifier.padding(bottom = FotoRaporTokens.SpacingS).size(FotoRaporTokens.FabSize),
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = FotoRaporTokens.ElevationM,
            pressedElevation = FotoRaporTokens.ElevationL,
        ),
    ) {
        Icon(Icons.Default.Add, stringResource(R.string.acc_add_project), modifier = Modifier.size(FotoRaporTokens.IconSizeM))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScaffold(
    projects: List<Project>?,
    onProjectClick: (Project) -> Unit,
    onAddProject: () -> Unit,
    onSettingsClick: () -> Unit,
    onTrashClick: () -> Unit,
    onRefresh: () -> Unit,
    language: String,
    isRefreshing: Boolean,
    activityDots: Map<LocalDate, List<Color>>,
    isTrashNotEmpty: Boolean,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    viewMode: DashboardViewMode,
    onViewModeChange: (DashboardViewMode) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = { DashboardFab(onAddProject) }
    ) { padding ->
        DashboardContent(
            padding = padding,
            projects = projects,
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            viewMode = viewMode,
            onViewModeChange = onViewModeChange,
            onProjectClick = onProjectClick,
            onAddClick = onAddProject,
            onTrashClick = onTrashClick,
            onSettingsClick = onSettingsClick,
            isTrashNotEmpty = isTrashNotEmpty,
            selectedDate = selectedDate,
            onDateSelected = onDateSelected,
            activityDots = activityDots,
            language = language,
            haptic = haptic
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    padding: PaddingValues,
    projects: List<Project>?,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    viewMode: DashboardViewMode,
    onViewModeChange: (DashboardViewMode) -> Unit,
    onProjectClick: (Project) -> Unit,
    onAddClick: () -> Unit,
    onTrashClick: () -> Unit,
    onSettingsClick: () -> Unit,
    isTrashNotEmpty: Boolean,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    activityDots: Map<LocalDate, List<Color>>,
    language: String,
    haptic: HapticFeedback
) {
    val pullState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = pullState,
        modifier = Modifier.fillMaxSize(),
        indicator = {
            if (isRefreshing) {
                PullToRefreshDefaults.Indicator(
                    state = pullState,
                    isRefreshing = true,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(),
            contentPadding = PaddingValues(bottom = FotoRaporTokens.Spacing3XL)
        ) {
            item(key = "dashboard_hero_header") {
                DashboardHeroHeader(
                    projectsCount = projects?.size ?: 0,
                    activityCount = activityDots.size,
                    viewMode = viewMode,
                    onViewModeChange = onViewModeChange,
                    onTrashClick = onTrashClick,
                    onSettingsClick = onSettingsClick,
                    isTrashNotEmpty = isTrashNotEmpty,
                    haptic = haptic
                )
            }

            if (viewMode == DashboardViewMode.PROJECTS) {
                projectsView(projects, onProjectClick, onAddClick)
            } else {
                calendarView(selectedDate, onDateSelected, activityDots, language)
            }

            item { Spacer(modifier = Modifier.height(96.dp)) }
        }
    }
}

@Composable
fun DashboardHeroHeader(
    projectsCount: Int,
    activityCount: Int,
    viewMode: DashboardViewMode,
    onViewModeChange: (DashboardViewMode) -> Unit,
    onTrashClick: () -> Unit,
    onSettingsClick: () -> Unit,
    isTrashNotEmpty: Boolean,
    haptic: HapticFeedback
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = FotoRaporTokens.ScreenPaddingHorizontal).padding(top = FotoRaporTokens.SpacingL, bottom = FotoRaporTokens.SpacingM)) {
        DashboardTopRow(onTrashClick, onSettingsClick, isTrashNotEmpty, haptic)
        Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingL))
        DashboardStatsRow(projectsCount, activityCount)
        Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingL))
        PillSegmentedControl(
            items = DashboardViewMode.entries,
            selectedItem = viewMode,
            onItemSelected = onViewModeChange,
            itemTitle = { mode ->
                when (mode) {
                    DashboardViewMode.PROJECTS -> stringResource(R.string.my_projects_title)
                    DashboardViewMode.CALENDAR -> "Saha Takvimi"
                }
            }
        )
    }
}

@Composable
private fun DashboardTopRow(onTrashClick: () -> Unit, onSettingsClick: () -> Unit, isTrashNotEmpty: Boolean, haptic: HapticFeedback) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(text = "FOTORAPOR", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp, color = MaterialTheme.colorScheme.onBackground)
            Text(text = "Saha Raporlama & Fotoğraf Yönetimi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(FotoRaporTokens.SpacingS)) {
            IconActionButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onTrashClick() }, icon = if (isTrashNotEmpty) Icons.Default.Delete else Icons.Default.DeleteOutline, contentDescription = stringResource(R.string.acc_open_trash), tint = if (isTrashNotEmpty) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant, showBadge = isTrashNotEmpty)
            IconActionButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSettingsClick() }, icon = Icons.Default.Settings, contentDescription = stringResource(R.string.acc_open_settings), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DashboardStatsRow(projectsCount: Int, activityCount: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FotoRaporTokens.SpacingM)) {
        HeroStatChip(icon = Icons.Default.Folder, label = stringResource(R.string.active_projects), value = projectsCount.toString(), accentColor = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
        HeroStatChip(icon = Icons.Default.CalendarMonth, label = "Saha Takvimi", value = "$activityCount Gün", accentColor = MaterialTheme.colorScheme.tertiary, modifier = Modifier.weight(1f))
    }
}

fun LazyListScope.projectsView(projects: List<Project>?, onProjectClick: (Project) -> Unit, onAddClick: () -> Unit) {
    val projectCount = projects?.size ?: 0
    item(key = "projects_section_header") {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = FotoRaporTokens.ScreenPaddingHorizontal, vertical = FotoRaporTokens.SpacingS), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = stringResource(R.string.active_projects).uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.5.sp)
            Text(text = pluralStringResource(R.plurals.project_files, projectCount, projectCount), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (projects == null) {
        // Do not render empty state while loading
    } else if (projects.isEmpty()) {
        item(key = "projects_empty_state") {
            AppEmptyState(icon = Icons.Default.FolderOpen, title = stringResource(R.string.empty_projects_title), description = stringResource(R.string.empty_projects_desc), actionLabel = stringResource(R.string.empty_projects_action), onActionClick = onAddClick)
        }
    } else {
        items(projects, key = { it.id }, contentType = { "project_folder_item" }) { project ->
            ProjectFolderItem(project = project, onClick = { onProjectClick(project) }, modifier = Modifier.animateItem())
        }
    }
}

fun LazyListScope.calendarView(selectedDate: LocalDate, onDateSelected: (LocalDate) -> Unit, activityDots: Map<LocalDate, List<Color>>, language: String) {
    item(key = "calendar_view_card") {
        Surface(modifier = Modifier.padding(horizontal = FotoRaporTokens.ScreenPaddingHorizontal).fillMaxWidth(), shape = RoundedCornerShape(FotoRaporTokens.RadiusL), color = MaterialTheme.colorScheme.surface, border = BorderStroke(FotoRaporTokens.CardBorderWidth, MaterialTheme.colorScheme.outlineVariant)) {
            MonthlyCalendar(selectedDate = selectedDate, onDateSelected = onDateSelected, activityDots = activityDots, locale = remember(language) { java.util.Locale.forLanguageTag(language) })
        }
    }
}

@Composable
fun IconActionButton(onClick: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String, tint: Color, showBadge: Boolean = false) {
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(FotoRaporTokens.RadiusS), modifier = Modifier.size(42.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(FotoRaporTokens.IconSizeS))
            if (showBadge) {
                Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.error, CircleShape).align(Alignment.TopEnd).offset(x = (-6).dp, y = 6.dp))
            }
        }
    }
}

@Composable
fun ProjectFolderItem(project: Project, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    val projectColor = remember(project.colorHex) { runCatching { Color(project.colorHex.toColorInt()) }.getOrDefault(Color.Gray) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        if (isPressed) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = FotoRaporMotion.pressSpring(),
        label = "card_scale"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = FotoRaporTokens.ScreenPaddingHorizontal, vertical = FotoRaporTokens.SpacingS)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(FotoRaporTokens.RadiusM))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(FotoRaporTokens.RadiusM),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(FotoRaporTokens.CardBorderWidth, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(modifier = Modifier.width(6.dp).fillMaxHeight().background(projectColor))
            Row(modifier = Modifier.weight(1f).padding(FotoRaporTokens.SpacingL), verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(46.dp), shape = RoundedCornerShape(FotoRaporTokens.RadiusS), color = projectColor.copy(alpha = 0.12f)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Folder, null, tint = projectColor, modifier = Modifier.size(FotoRaporTokens.IconSizeM)) }
                }
                Spacer(modifier = Modifier.width(FotoRaporTokens.SpacingL))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = project.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(FotoRaporTokens.IconSizeXS))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProjectDialog(onDismiss: () -> Unit, onConfirm: (String, Color) -> Unit) {
    var projectName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(FotoRaporTokens.ProjectColors.first()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var isSubmitting by remember { mutableStateOf(false) }

    val handleDismiss = {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onDismiss()
        }
    }

    val handleConfirm = {
        if (!isSubmitting && projectName.isNotBlank()) {
            isSubmitting = true
            val name = projectName.trim()
            val color = selectedColor
            scope.launch {
                sheetState.hide()
            }.invokeOnCompletion {
                onConfirm(name, color)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = { handleDismiss() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = FotoRaporTokens.RadiusXL, topEnd = FotoRaporTokens.RadiusXL)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = FotoRaporTokens.ScreenPaddingHorizontal).padding(bottom = FotoRaporTokens.Spacing3XL)) {
            Text(text = stringResource(R.string.start_new_project), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingL))
            OutlinedTextField(value = projectName, onValueChange = { projectName = it }, label = { Text(stringResource(R.string.project_name_label)) }, placeholder = { Text("Örn: Kadıköy Şantiyesi El. Raporu") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(FotoRaporTokens.RadiusM))
            Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingXXL))
            ProjectColorPicker(selectedColor) { selectedColor = it }
            Spacer(modifier = Modifier.height(FotoRaporTokens.Spacing3XL))
            ProjectActionButtons(
                onDismiss = { handleDismiss() },
                onConfirm = { handleConfirm() },
                isEnabled = projectName.isNotBlank() && !isSubmitting
            )
        }
    }
}

@Composable
private fun ProjectColorPicker(selectedColor: Color, onColorSelect: (Color) -> Unit) {
    Text(text = "PROJE RENK KODU", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.5.sp)
    Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingM))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        FotoRaporTokens.ProjectColors.forEach { color ->
            val isSelected = color == selectedColor
            val scale by animateFloatAsState(targetValue = if (isSelected) 1.2f else 1.0f, animationSpec = FotoRaporMotion.pressSpring(), label = "color_scale")
            Box(
                modifier = Modifier.size(36.dp).graphicsLayer { scaleX = scale; scaleY = scale }.background(color, CircleShape).clickable { onColorSelect(color) }
                    .then(if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun ProjectActionButtons(onDismiss: () -> Unit, onConfirm: () -> Unit, isEnabled: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FotoRaporTokens.SpacingM)) {
        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(FotoRaporTokens.ButtonHeightL), shape = RoundedCornerShape(FotoRaporTokens.RadiusM)) {
            Text(stringResource(R.string.cancel_btn))
        }
        Button(onClick = onConfirm, modifier = Modifier.weight(1f).height(FotoRaporTokens.ButtonHeightL), enabled = isEnabled, shape = RoundedCornerShape(FotoRaporTokens.RadiusM)) {
            Text(stringResource(R.string.add_btn), fontWeight = FontWeight.Bold)
        }
    }
}
