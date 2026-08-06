package com.fatihenes.photoreport.feature.dashboard.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporMotion
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporTokens
import com.fatihenes.photoreport.core.model.Project
import com.fatihenes.photoreport.core.ui.R
import com.fatihenes.photoreport.core.ui.components.AppEmptyState
import com.fatihenes.photoreport.feature.dashboard.components.MonthlyCalendar
import java.time.LocalDate

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
                contentPadding = PaddingValues(0.dp)
            ) {
                // ── Header ──────────────────────────────────
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = FotoRaporTokens.ScreenPaddingHorizontal,
                                vertical = FotoRaporTokens.SpacingXXL
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.my_projects_title),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
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
                }

                // ── Calendar Card ────────────────────────────
                item {
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = FotoRaporTokens.ScreenPaddingHorizontal)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(FotoRaporTokens.RadiusL),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            FotoRaporTokens.CardBorderWidth,
                            MaterialTheme.colorScheme.outlineVariant
                        ),
                        tonalElevation = FotoRaporTokens.ElevationXS
                    ) {
                        MonthlyCalendar(
                            selectedDate = selectedDate,
                            onDateSelected = { selectedDate = it },
                            activityDots = activityDots,
                            locale = remember(language) { java.util.Locale.forLanguageTag(language) }
                        )
                    }
                    Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingXXL))
                }

                // ── Section Header ───────────────────────────
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = FotoRaporTokens.ScreenPaddingHorizontal,
                                vertical = FotoRaporTokens.SpacingS
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = stringResource(R.string.active_projects),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = pluralStringResource(R.plurals.project_files, projects.size, projects.size),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ── Empty State ──────────────────────────────
                if (projects.isEmpty()) {
                    item {
                        AppEmptyState(
                            icon = Icons.Default.FolderOpen,
                            title = stringResource(R.string.empty_projects_title),
                            description = stringResource(R.string.empty_projects_desc),
                            actionLabel = stringResource(R.string.empty_projects_action),
                            onActionClick = { showAddDialog = true }
                        )
                    }
                }

                // ── Project List ─────────────────────────────
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

// ─── Icon Action Button (header toolbar) ──────────────────────

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
        modifier = Modifier.size(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(FotoRaporTokens.IconSizeS)
            )
            if (showBadge) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 3.dp, y = (-3).dp)
                        .size(7.dp)
                        .background(MaterialTheme.colorScheme.error, CircleShape)
                )
            }
        }
    }
}

// ─── Project Card ─────────────────────────────────────────────

@Composable
private fun ProjectFolderItem(
    project: Project,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val projectColor = Color(project.colorHex.toColorInt())
    val haptic = LocalHapticFeedback.current

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.975f else 1f,
        animationSpec = FotoRaporMotion.pressSpring(),
        label = "card_press_scale"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = FotoRaporTokens.ScreenPaddingHorizontal,
                vertical = FotoRaporTokens.SpacingXS + 2.dp
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(interactionSource = interactionSource, indication = ripple()) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(FotoRaporTokens.RadiusM),
        border = BorderStroke(FotoRaporTokens.CardBorderWidth, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = FotoRaporTokens.ElevationXS
    ) {
        Row(
            modifier = Modifier
                .padding(FotoRaporTokens.SpacingL)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Project icon badge
            Surface(
                modifier = Modifier.size(FotoRaporTokens.AvatarSizeM),
                shape = RoundedCornerShape(FotoRaporTokens.RadiusM),
                color = projectColor.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = projectColor,
                        modifier = Modifier.size(FotoRaporTokens.IconSizeM + 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(FotoRaporTokens.SpacingL))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingXXS))
                Text(
                    text = stringResource(R.string.folder_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

// ─── Add Project Dialog (Bottom Sheet) ────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProjectDialog(onDismiss: () -> Unit, onConfirm: (String, Color) -> Unit) {
    var name by remember { mutableStateOf("") }
    val colors = FotoRaporTokens.ProjectColors
    var selectedColor by remember { mutableStateOf(colors[0]) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = FotoRaporTokens.RadiusXL, topEnd = FotoRaporTokens.RadiusXL)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FotoRaporTokens.ScreenPaddingHorizontal)
                .padding(bottom = FotoRaporTokens.Spacing5XL),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.start_new_project),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(FotoRaporTokens.Spacing3XL))

            // Input field
            TextField(
                value = name,
                onValueChange = { name = it },
                placeholder = {
                    Text(
                        stringResource(R.string.project_name_label),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(FotoRaporTokens.RadiusM),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingXXL))

            // Color picker label
            Text(
                stringResource(R.string.project_name_label).takeIf { false } ?: "",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = FotoRaporTokens.SpacingS)
            )

            // Color picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                colors.forEach { color ->
                    val isSelected = selectedColor == color
                    val borderAnim by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                        animationSpec = FotoRaporMotion.enterTween(),
                        label = "color_border"
                    )
                    val scaleAnim by animateFloatAsState(
                        targetValue = if (isSelected) 1.15f else 1f,
                        animationSpec = FotoRaporMotion.pressSpring(),
                        label = "color_scale"
                    )

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .graphicsLayer {
                                scaleX = scaleAnim
                                scaleY = scaleAnim
                            }
                            .clip(CircleShape)
                            .background(color)
                            .clickable { selectedColor = color }
                            .border(
                                width = if (isSelected) 2.5.dp else 0.dp,
                                color = borderAnim,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(FotoRaporTokens.IconSizeXS)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(FotoRaporTokens.Spacing4XL))

            // Submit button
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, selectedColor) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(FotoRaporTokens.ButtonHeightL),
                shape = RoundedCornerShape(FotoRaporTokens.RadiusM),
                enabled = name.isNotBlank(),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = FotoRaporTokens.ElevationNone,
                    pressedElevation = FotoRaporTokens.ElevationNone
                )
            ) {
                Text(
                    stringResource(R.string.start_project_btn),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
