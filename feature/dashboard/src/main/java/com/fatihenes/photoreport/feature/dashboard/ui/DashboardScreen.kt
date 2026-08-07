package com.fatihenes.photoreport.feature.dashboard.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.fatihenes.photoreport.core.model.Project
import java.time.LocalDate

enum class DashboardViewMode { PROJECTS, CALENDAR }

// Refactored to reduce complexity
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
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(DashboardViewMode.PROJECTS) }

    DashboardScaffold(
        projects = projects,
        onProjectClick = onProjectClick,
        onAddProject = { showAddDialog = true },
        onSettingsClick = onSettingsClick,
        onTrashClick = onTrashClick,
        onRefresh = onRefresh,
        language = language,
        isRefreshing = isRefreshing,
        activityDots = activityDots,
        isTrashNotEmpty = isTrashNotEmpty,
        selectedDate = selectedDate,
        onDateSelected = { selectedDate = it },
        viewMode = viewMode,
        onViewModeChange = { viewMode = it }
    )

    if (showAddDialog) {
        AddProjectDialog(onDismiss = { showAddDialog = false }, onConfirm = onAddProject)
    }
}
