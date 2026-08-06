package com.fatihenes.photoreport.feature.dashboard.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporMotion
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporTokens
import com.fatihenes.photoreport.core.model.Project
import com.fatihenes.photoreport.core.ui.R
import com.fatihenes.photoreport.core.ui.components.AppEmptyState
import com.fatihenes.photoreport.core.ui.components.HeroStatChip
import com.fatihenes.photoreport.core.ui.components.PillSegmentedControl
import com.fatihenes.photoreport.feature.dashboard.components.MonthlyCalendar
import java.time.LocalDate

enum class DashboardViewMode { PROJECTS, CALENDAR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    projects: List<Project>,
    onProjectClick: (Project) -> Unit,
    onAddProject: (String, Color) -> Unit,
    onSettingsClick: () -> Unit,
    onTrashClick: () -> Unit,
    onRefresh: () -> Unit,
    language: String = "tr",
    isRefreshing: Boolean = false,
    activityDots: Map<LocalDate, List<Color>> = emptyMap(),
    isTrashNotEmpty: Boolean = false,
) {
    val haptic = LocalHapticFeedback.current
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(DashboardViewMode.PROJECTS) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(FotoRaporTokens.RadiusM),
                modifier = Modifier
                    .padding(bottom = FotoRaporTokens.SpacingS)
                    .size(FotoRaporTokens.FabSize),
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = FotoRaporTokens.ElevationM,
                    pressedElevation = FotoRaporTokens.ElevationL
                )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.acc_add_project),
                    modifier = Modifier.size(FotoRaporTokens.IconSizeM)
                )
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .imePadding(),
                contentPadding = PaddingValues(bottom = FotoRaporTokens.Spacing3XL)
            ) {
                // ── Hero Header ──────────────────────────────
                item(key = "dashboard_hero_header") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = FotoRaporTokens.ScreenPaddingHorizontal)
                            .padding(top = FotoRaporTokens.SpacingL, bottom = FotoRaporTokens.SpacingM)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "FOTORAPOR",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.5.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.width(FotoRaporTokens.SpacingS))
                                    // Status Badge Pill
                                    Surface(
                                        shape = RoundedCornerShape(100.dp),
                                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                                        modifier = Modifier.height(20.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "SAHA MODU",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "Saha Raporlama & Fotoğraf Yönetimi",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(FotoRaporTokens.SpacingS)) {
                                // Trash button
                                IconActionButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onTrashClick()
                                    },
                                    icon = if (isTrashNotEmpty) Icons.Default.Delete else Icons.Default.DeleteOutline,
                                    contentDescription = stringResource(R.string.acc_open_trash),
                                    tint = if (isTrashNotEmpty) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                    showBadge = isTrashNotEmpty
                                )
                                // Settings button
                                IconActionButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSettingsClick()
                                    },
                                    icon = Icons.Default.Settings,
                                    contentDescription = stringResource(R.string.acc_open_settings),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingL))

                        // Hero Stat Chips Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(FotoRaporTokens.SpacingM)
                        ) {
                            HeroStatChip(
                                icon = Icons.Default.Folder,
                                label = stringResource(R.string.active_projects),
                                value = "${projects.size}",
                                accentColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            HeroStatChip(
                                icon = Icons.Default.CalendarMonth,
                                label = "Saha Takvimi",
                                value = "${activityDots.size} Gün",
                                accentColor = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingL))

                        // Segmented Control Switcher
                        PillSegmentedControl(
                            items = DashboardViewMode.entries,
                            selectedItem = viewMode,
                            onItemSelected = { viewMode = it },
                            itemTitle = { mode ->
                                when (mode) {
                                    DashboardViewMode.PROJECTS -> stringResource(R.string.my_projects_title)
                                    DashboardViewMode.CALENDAR -> "Saha Takvimi"
                                }
                            }
                        )
                    }
                }

                // ── Projects View ────────────────────────────
                if (viewMode == DashboardViewMode.PROJECTS) {
                    item(key = "projects_section_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = FotoRaporTokens.ScreenPaddingHorizontal,
                                    vertical = FotoRaporTokens.SpacingS
                                ),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.active_projects).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                text = pluralStringResource(R.plurals.project_files, projects.size, projects.size),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (projects.isEmpty()) {
                        item(key = "projects_empty_state") {
                            AppEmptyState(
                                icon = Icons.Default.FolderOpen,
                                title = stringResource(R.string.empty_projects_title),
                                description = stringResource(R.string.empty_projects_desc),
                                actionLabel = stringResource(R.string.empty_projects_action),
                                onActionClick = { showAddDialog = true }
                            )
                        }
                    }

                    items(
                        projects,
                        key = { project -> project.id },
                        contentType = { "project_folder_item" }
                    ) { project ->
                        ProjectFolderItem(
                            project = project,
                            onClick = { onProjectClick(project) },
                            modifier = Modifier.animateItem()
                        )
                    }
                } else {
                    // ── Calendar View Mode ────────────────────
                    item(key = "calendar_view_card") {
                        Surface(
                            modifier = Modifier
                                .padding(horizontal = FotoRaporTokens.ScreenPaddingHorizontal)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(FotoRaporTokens.RadiusL),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                FotoRaporTokens.CardBorderWidth,
                                MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            MonthlyCalendar(
                                selectedDate = selectedDate,
                                onDateSelected = { selectedDate = it },
                                activityDots = activityDots,
                                locale = remember(language) { java.util.Locale.forLanguageTag(language) }
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(96.dp)) }
            }
        }
    }

    if (showAddDialog) {
        AddProjectDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, color ->
                onAddProject(name, color)
                showAddDialog = false
            }
        )
    }
}

// ─── Icon Action Button ──────────────────────────────────────

@Composable
private fun IconActionButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color,
    showBadge: Boolean = false
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(FotoRaporTokens.RadiusS),
        modifier = Modifier.size(42.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(FotoRaporTokens.IconSizeS)
            )
            if (showBadge) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(MaterialTheme.colorScheme.error, CircleShape)
                        .align(Alignment.TopEnd)
                        .offset(x = (-6).dp, y = 6.dp)
                )
            }
        }
    }
}

// ─── Project Folder Item (Card) ──────────────────────────────

@Composable
private fun ProjectFolderItem(
    project: Project,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val projectColor = remember(project.colorHex) {
        runCatching { Color(project.colorHex.toColorInt()) }.getOrDefault(Color.Gray)
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = FotoRaporMotion.pressSpring(),
        label = "card_scale"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = FotoRaporTokens.ScreenPaddingHorizontal,
                vertical = FotoRaporTokens.SpacingS
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(FotoRaporTokens.RadiusM))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            ),
        shape = RoundedCornerShape(FotoRaporTokens.RadiusM),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            FotoRaporTokens.CardBorderWidth,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // Left Accent Edge Bar
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(projectColor)
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(FotoRaporTokens.SpacingL),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color Folder Icon Badge
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = RoundedCornerShape(FotoRaporTokens.RadiusS),
                    color = projectColor.copy(alpha = 0.12f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            tint = projectColor,
                            modifier = Modifier.size(FotoRaporTokens.IconSizeM)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(FotoRaporTokens.SpacingL))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingXXS))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = "Saha Klasörü",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Icon(
                    Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = stringResource(R.string.my_projects_title),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(FotoRaporTokens.IconSizeXS)
                )
            }
        }
    }
}

// ─── Add Project Dialog ──────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddProjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Color) -> Unit
) {
    var projectName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(FotoRaporTokens.ProjectColors.first()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = FotoRaporTokens.RadiusXL, topEnd = FotoRaporTokens.RadiusXL)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FotoRaporTokens.ScreenPaddingHorizontal)
                .padding(bottom = FotoRaporTokens.Spacing3XL)
        ) {
            Text(
                text = stringResource(R.string.start_new_project),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingL))

            OutlinedTextField(
                value = projectName,
                onValueChange = { projectName = it },
                label = { Text(stringResource(R.string.project_name_label)) },
                placeholder = { Text("Örn: Kadıköy Şantiyesi El. Raporu") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(FotoRaporTokens.RadiusM)
            )

            Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingXXL))

            Text(
                text = "PROJE RENK KODU",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingM))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FotoRaporTokens.ProjectColors.forEach { color ->
                    val isSelected = color == selectedColor
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.2f else 1.0f,
                        animationSpec = FotoRaporMotion.pressSpring(),
                        label = "color_scale"
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .background(color, CircleShape)
                            .clickable { selectedColor = color }
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        3.dp,
                                        MaterialTheme.colorScheme.onSurface,
                                        CircleShape
                                    )
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(FotoRaporTokens.Spacing3XL))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FotoRaporTokens.SpacingM)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(FotoRaporTokens.ButtonHeightL),
                    shape = RoundedCornerShape(FotoRaporTokens.RadiusM)
                ) {
                    Text(stringResource(R.string.cancel_btn))
                }

                Button(
                    onClick = {
                        if (projectName.isNotBlank()) {
                            onConfirm(projectName.trim(), selectedColor)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(FotoRaporTokens.ButtonHeightL),
                    enabled = projectName.isNotBlank(),
                    shape = RoundedCornerShape(FotoRaporTokens.RadiusM)
                ) {
                    Text(
                        stringResource(R.string.add_btn),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
