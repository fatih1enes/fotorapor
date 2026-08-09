@file:Suppress("LocalContextGetResourceValueCall")
package com.fatihenes.photoreport.feature.project.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatihenes.photoreport.core.common.util.DateUtils
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporTokens
import com.fatihenes.photoreport.core.model.DailyLogWithPhotos
import com.fatihenes.photoreport.core.model.Photo
import com.fatihenes.photoreport.core.model.Project
import com.fatihenes.photoreport.core.ui.R
import com.fatihenes.photoreport.feature.export.ui.ExportDialog
import com.fatihenes.photoreport.core.ui.navigation.LocalSnackbarHostState
import kotlinx.coroutines.launch

data class ProjectDetailViewState(
    val project: Project,
    val logs: List<DailyLogWithPhotos>,
    val allProjectPhotos: List<Photo>,
    val showFullGalleryByLogId: Long?,
    val showDeleteConfirm: Boolean,
    val showExportDialog: Boolean,
    val selectedPhotoForFullView: Long?,
    val language: String,
)

data class ProjectDetailActions(
    val onGalleryDismiss: () -> Unit,
    val onPhotoClick: (Photo) -> Unit,
    val onDeletePhotos: (List<Long>) -> Unit,
    val onDeleteConfirmDismiss: () -> Unit,
    val onDeleteProject: () -> Unit,
    val onExportDismiss: () -> Unit,
    val onExportProject: (String, Int, String) -> Unit,
    val onPhotoViewDismiss: () -> Unit,
    val onDeletePhoto: (Photo) -> Unit,
    val onUpdateRotation: (Long, Float) -> Unit,
)

data class TopBarParams(
    val state: ProjectDetailViewState,
    val projectColor: Color,
    val onBack: () -> Unit,
    val onShareClick: () -> Unit,
    val haptic: HapticFeedback,
    val actions: ProjectDetailActions,
)

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("FunctionName")
@Composable
fun ProjectDetailTopBar(params: TopBarParams) {
    TopAppBar(
        title = { ProjectDetailTopBarTitle(params.state, params.projectColor) },
        navigationIcon = {
            IconButton(onClick = params.onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back_label),
                    modifier = Modifier.size(FotoRaporTokens.IconSizeS),
                )
            }
        },
        actions = {
            ProjectDetailTopBarActions(
                projectColor = params.projectColor,
                onShareClick = params.onShareClick,
                haptic = params.haptic,
                actions = params.actions,
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

@Suppress("FunctionName")
@Composable
private fun ProjectDetailTopBarTitle(state: ProjectDetailViewState, projectColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(FotoRaporTokens.SpacingM)
                .background(projectColor, CircleShape),
        )
        Spacer(modifier = Modifier.width(FotoRaporTokens.SpacingS + 2.dp))
        Column {
            Text(
                state.project.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "${state.logs.size} Saha Günlüğü · ${state.allProjectPhotos.size} Fotoğraf",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Suppress("FunctionName")
@Composable
private fun ProjectDetailTopBarActions(
    projectColor: Color,
    onShareClick: () -> Unit,
    haptic: HapticFeedback,
    actions: ProjectDetailActions,
) {
    var showMenu by remember { mutableStateOf(value = false) }

    IconButton(onClick = onShareClick) {
        Icon(
            Icons.Default.Share,
            contentDescription = stringResource(R.string.share_as_file),
            tint = projectColor,
            modifier = Modifier.size(FotoRaporTokens.IconSizeS),
        )
    }
    IconButton(onClick = { showMenu = true }) {
        Icon(
            Icons.Default.MoreVert,
            contentDescription = stringResource(R.string.acc_more_options),
            modifier = Modifier.size(FotoRaporTokens.IconSizeS),
        )
    }
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false },
        shape = RoundedCornerShape(FotoRaporTokens.RadiusM),
    ) {
        DropdownMenuItem(
            text = {
                Text(
                    stringResource(R.string.delete_project_menu),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            },
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showMenu = false
                actions.onDeleteProject()
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.acc_delete),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(FotoRaporTokens.IconSizeS),
                )
            },
        )
    }
}

@Suppress("FunctionName")
@Composable
fun ProjectHeroBanner(
    projectName: String,
    logCount: Int,
    photoCount: Int,
    projectColor: Color,
    onExportClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = FotoRaporTokens.SpacingL),
        shape = RoundedCornerShape(FotoRaporTokens.RadiusL),
        color = projectColor.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, projectColor.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(FotoRaporTokens.SpacingL),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = projectName.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = projectColor,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingXXS))
                Text(
                    text = "$logCount Saha Kaydı · $photoCount Fotoğraf",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Button(
                onClick = onExportClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = projectColor,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(FotoRaporTokens.RadiusS),
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = FotoRaporTokens.SpacingM),
            ) {
                Icon(
                    Icons.Default.PictureAsPdf,
                    null,
                    modifier = Modifier.size(FotoRaporTokens.IconSizeS),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(FotoRaporTokens.SpacingXS))
                Text(
                    "Rapor Al",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("FunctionName")
@Composable
fun AddDateCard(
    projectColor: Color,
    onAddLogForDate: (Long) -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(value = false) }
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneId.of("UTC")).toLocalDate()
                            onAddLogForDate(DateUtils.getStartOfDayEpochMillis(date))
                        }
                        showDatePicker = false
                    },
                ) {
                    Text(stringResource(R.string.add_btn))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false },
                ) {
                    Text(stringResource(R.string.cancel_btn))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    OutlinedButton(
        onClick = { showDatePicker = true },
        modifier = Modifier.fillMaxWidth().height(FotoRaporTokens.ButtonHeightL),
        shape = RoundedCornerShape(FotoRaporTokens.RadiusM),
        border = BorderStroke(1.dp, projectColor.copy(alpha = 0.5f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = projectColor)
    ) {
        Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(FotoRaporTokens.IconSizeS))
        Spacer(modifier = Modifier.width(FotoRaporTokens.SpacingS))
        Text(
            stringResource(R.string.add_date_card),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Suppress("FunctionName")
@Composable
fun ProjectDetailDialogs(
    state: ProjectDetailViewState,
    actions: ProjectDetailActions,
) {
    GalleryDialog(state, actions)

    if (state.showDeleteConfirm) {
        ProjectDeleteConfirmDialog(
            projectName = state.project.name,
            onDismiss = actions.onDeleteConfirmDismiss,
            onConfirm = actions.onDeleteProject,
        )
    }

    ExportDialogWrapper(state, actions)

    FullScreenPhotoWrapper(state, actions)
}

@Suppress("FunctionName")
@Composable
private fun GalleryDialog(state: ProjectDetailViewState, actions: ProjectDetailActions) {
    state.showFullGalleryByLogId?.let { logId ->
        state.logs.find { it.log.id == logId }?.let { logWithPhotos ->
            FullGalleryDialog(
                log = logWithPhotos.log,
                photos = logWithPhotos.photos,
                onDismiss = actions.onGalleryDismiss,
                onPhotoClick = actions.onPhotoClick,
                onDeletePhotos = actions.onDeletePhotos,
            )
        }
    }
}

@Suppress("FunctionName")
@Composable
private fun ExportDialogWrapper(state: ProjectDetailViewState, actions: ProjectDetailActions) {
    val context = LocalContext.current
    val snackbarHost = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()

    if (state.showExportDialog) {
        ExportDialog(
            project = state.project,
            logs = state.logs,
            onDismiss = actions.onExportDismiss,
            onExportPdf = { quality ->
                actions.onExportDismiss()
                actions.onExportProject("PDF", quality, state.language)
                coroutineScope.launch {
                    snackbarHost.showSnackbar(
                        context.getString(R.string.pdf_preparing) + " (Arka plan)",
                    )
                }
            },
            onExportZip = { quality ->
                actions.onExportDismiss()
                actions.onExportProject("ZIP", quality, state.language)
                coroutineScope.launch {
                    snackbarHost.showSnackbar(
                        "ZIP dışa aktarımı arka planda başlatıldı. Bildirimleri kontrol edin.",
                    )
                }
            },
        )
    }
}

@Suppress("FunctionName")
@Composable
private fun FullScreenPhotoWrapper(state: ProjectDetailViewState, actions: ProjectDetailActions) {
    state.selectedPhotoForFullView?.let { photoId ->
        val index = state.allProjectPhotos.indexOfFirst { it.id == photoId }
        if ((index != -1) && state.allProjectPhotos.isNotEmpty()) {
            val motion = com.fatihenes.photoreport.core.designsystem.theme.FotoRaporMotion
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(motion.DURATION_MEDIUM)) +
                        scaleIn(
                            initialScale = 0.92f,
                            animationSpec = tween(motion.DURATION_MEDIUM),
                        ),
                exit = fadeOut(tween(motion.DURATION_SHORT)) +
                        scaleOut(
                            targetScale = 0.92f,
                            animationSpec = tween(motion.DURATION_SHORT),
                        ),
                modifier = Modifier.fillMaxSize(),
            ) {
                FullScreenPhotoDialog(
                    photoList = state.allProjectPhotos,
                    initialIndex = index,
                    onDismiss = actions.onPhotoViewDismiss,
                    onDelete = actions.onDeletePhoto,
                    onUpdateRotation = actions.onUpdateRotation,
                )
            }
        }
    }
}

@Suppress("FunctionName")
@Composable
private fun ProjectDeleteConfirmDialog(
    projectName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
                stringResource(R.string.delete_project_desc, projectName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = RoundedCornerShape(FotoRaporTokens.RadiusS),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = FotoRaporTokens.ElevationNone)
            ) {
                Text(
                    stringResource(R.string.delete_confirm_btn),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.cancel_btn),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    )
}

