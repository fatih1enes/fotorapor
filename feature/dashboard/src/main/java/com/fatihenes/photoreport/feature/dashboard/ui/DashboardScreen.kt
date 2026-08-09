@file:Suppress("MatchingDeclarationName")
package com.fatihenes.photoreport.feature.dashboard.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporMotion
import com.fatihenes.photoreport.core.model.Project
import java.time.LocalDate

data class DashboardScreenParams(
    val projects: List<Project>?,
    val onProjectClick: (Project) -> Unit,
    val onAddProject: (String, Color) -> Unit,
    val onSettingsClick: () -> Unit,
    val onTrashClick: () -> Unit,
    val onRefresh: () -> Unit,
    val language: String = "tr",
    val isRefreshing: Boolean = false,
    val activityDots: Map<LocalDate, List<Color>> = emptyMap(),
    val isTrashNotEmpty: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("FunctionNaming")
@Composable
fun DashboardScreen(params: DashboardScreenParams) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(DashboardViewMode.PROJECTS) }
    var isInitialLoadComplete by remember { mutableStateOf(false) }

    LaunchedEffect(params.projects) {
        if (params.projects != null && !isInitialLoadComplete) {
            isInitialLoadComplete = true
        }
    }

    AnimatedVisibility(
        visible = isInitialLoadComplete,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = 600,
                easing = FotoRaporMotion.EasingEmphasized,
            ),
        ) + slideInVertically(
            initialOffsetY = { 40 },
            animationSpec = tween(
                durationMillis = 600,
                easing = FotoRaporMotion.EasingEmphasized,
            ),
        ),
        modifier = Modifier.fillMaxSize(),
    ) {
        DashboardScaffold(
            state = DashboardViewState(
                projects = params.projects,
                isRefreshing = params.isRefreshing,
                viewMode = viewMode,
                selectedDate = selectedDate,
                activityDots = params.activityDots,
                isTrashNotEmpty = params.isTrashNotEmpty,
                language = params.language,
            ),
            actions = DashboardActions(
                onProjectClick = params.onProjectClick,
                onAddProject = params.onAddProject,
                onSettingsClick = params.onSettingsClick,
                onTrashClick = params.onTrashClick,
                onRefresh = params.onRefresh,
                onDateSelected = { selectedDate = it },
                onViewModeChange = { viewMode = it },
            ),
            onAddClick = { showAddDialog = true },
        )
    }

    if (showAddDialog) {
        AddProjectDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, color ->
                showAddDialog = false
                params.onAddProject(name, color)
            },
        )
    }
}
