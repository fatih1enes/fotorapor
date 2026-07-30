package com.fatihenes.photoreport.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.core.graphics.toColorInt
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fatihenes.photoreport.R
import com.fatihenes.photoreport.data.ProjectEntity
import com.fatihenes.photoreport.ui.components.MonthlyCalendar
import java.time.LocalDate

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    projects: List<ProjectEntity>,
    onProjectClick: (ProjectEntity) -> Unit,
    onAddProject: (String, Color) -> Unit,
    onSettingsClick: () -> Unit,
    onTrashClick: () -> Unit,
    activityDots: Map<LocalDate, List<Color>> = emptyMap(),
    isTrashNotEmpty: Boolean = false,
) {
    val haptic = LocalHapticFeedback.current
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showAddDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .size(60.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(28.dp))
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                scope.launch {
                    // Simüle edilmiş yenileme efekti
                    delay(1200.milliseconds)
                    isRefreshing = false
                }
            },
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
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.my_projects_title),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row {
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTrashClick()
                            },
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(14.dp),
                            tonalElevation = 2.dp,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isTrashNotEmpty) Icons.Default.Delete else Icons.Default.DeleteOutline,
                                    contentDescription = "Çöp Kutusu",
                                    tint = if (isTrashNotEmpty) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                                if (isTrashNotEmpty) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 4.dp, y = (-4).dp)
                                            .size(8.dp)
                                            .background(MaterialTheme.colorScheme.error,
                                                CircleShape
                                            )
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSettingsClick()
                            },
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(14.dp),
                            tonalElevation = 2.dp,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = stringResource(R.string.settings_title),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.extraLarge),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp
                ) {
                    MonthlyCalendar(
                        selectedDate = selectedDate,
                        onDateSelected = { selectedDate = it },
                        activityDots = activityDots
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = stringResource(R.string.active_projects),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = pluralStringResource(R.plurals.project_files, projects.size, projects.size),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (projects.isEmpty()) {
                item {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(800)) + androidx.compose.animation.scaleIn(initialScale = 0.9f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 80.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(80.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.FolderOpen,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = stringResource(R.string.empty_state_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Yeni bir rapor klasörü oluşturun",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(projects, key = { project -> project.id }) { project ->
                ProjectFolderItem(
                    project = project,
                    onClick = { onProjectClick(project) },
                    modifier = Modifier.animateItem()
                )
            }
            item { Spacer(modifier = Modifier.height(100.dp)) }
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

@Composable
private fun ProjectFolderItem(project: ProjectEntity, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val projectColor = Color(project.colorHex.toColorInt())
    val haptic = LocalHapticFeedback.current
    
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.6f, stiffness = 400f)
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(4.dp, MaterialTheme.shapes.extraLarge, spotColor = Color.Black.copy(alpha = 0.1f))
            .clickable(interactionSource = interactionSource, indication = ripple()) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(16.dp),
                color = projectColor.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = projectColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.folder_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Default.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProjectDialog(onDismiss: () -> Unit, onConfirm: (String, Color) -> Unit) {
    var name by remember { mutableStateOf("") }
    val colors = listOf(
        Color(0xFFFF3B30), Color(0xFF007AFF), Color(0xFF34C759),
        Color(0xFFFFCC00), Color(0xFFAF52DE), Color(0xFFFF2D55)
    )
    var selectedColor by remember { mutableStateOf(colors[1]) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.start_new_project),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(32.dp))

            TextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text(stringResource(R.string.project_name_label)) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                colors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { selectedColor = color }
                            .border(
                                width = if (selectedColor == color) 3.dp else 0.dp,
                                color = MaterialTheme.colorScheme.onSurface,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedColor == color) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, selectedColor) },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(18.dp),
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.start_project_btn), style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
        }
    }
}
