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
import androidx.compose.material3.CenterAlignedTopAppBar
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
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporTokens
import com.fatihenes.photoreport.core.model.DailyLogWithPhotos
import com.fatihenes.photoreport.core.model.Photo
import com.fatihenes.photoreport.core.model.Project
import com.fatihenes.photoreport.core.ui.R
import com.fatihenes.photoreport.feature.export.ui.ExportDialog
import com.fatihenes.photoreport.core.ui.navigation.LocalSnackbarHostState
import com.fatihenes.photoreport.core.ui.util.MediaShareUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongParameterList", "LongMethod", "FunctionName")
@Composable
fun ProjectDetailTopBar(
    project: Project,
    photoCount: Int,
    logCount: Int,
    projectColor: Color,
    onBack: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit,
    haptic: HapticFeedback,
) {
    var showMenu by remember { mutableStateOf(value = false) }

    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(FotoRaporTokens.SpacingM)
                        .background(projectColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(FotoRaporTokens.SpacingS + 2.dp))
                Column {
                    Text(
                        project.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "$logCount Saha Günlüğü · $photoCount Fotoğraf",
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
            IconButton(onClick = onShareClick) {
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
                        onDeleteClick()
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
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
    )
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
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Icon(
                    Icons.Default.PictureAsPdf,
                    null,
                    modifier = Modifier.size(FotoRaporTokens.IconSizeS),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
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
    onAddLogForDate: (Long) -> Unit
) {
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
                        val startOfSelectedDay = com.fatihenes.photoreport.core.common.util.DateUtils.getStartOfDay(cal)
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

@Suppress("LongParameterList", "FunctionName", "LongMethod")
@Composable
fun ProjectDetailDialogs(
    project: Project,
    logs: List<DailyLogWithPhotos>,
    allProjectPhotos: List<Photo>,
    showFullGalleryByLogId: Long?,
    onGalleryDismiss: () -> Unit,
    onPhotoClick: (Photo) -> Unit,
    onDeletePhotos: (List<Long>) -> Unit,
    showDeleteConfirm: Boolean,
    onDeleteConfirmDismiss: () -> Unit,
    onDeleteProject: () -> Unit,
    showExportDialog: Boolean,
    onExportDismiss: () -> Unit,
    onExportProject: (String, Int, String) -> Unit,
    selectedPhotoForFullView: Long?,
    onPhotoViewDismiss: () -> Unit,
    onDeletePhoto: (Photo) -> Unit,
    onUpdateRotation: (Long, Float) -> Unit,
    language: String
) {
    val context = LocalContext.current
    val snackbarHost = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()

    showFullGalleryByLogId?.let { logId ->
        logs.find { it.log.id == logId }?.let { logWithPhotos ->
            FullGalleryDialog(
                log = logWithPhotos.log,
                photos = logWithPhotos.photos,
                onDismiss = onGalleryDismiss,
                onPhotoClick = onPhotoClick,
                onDeletePhotos = onDeletePhotos
            )
        }
    }

    if (showDeleteConfirm) {
        ProjectDeleteConfirmDialog(
            projectName = project.name,
            onDismiss = onDeleteConfirmDismiss,
            onConfirm = onDeleteProject
        )
    }

    if (showExportDialog) {
        ExportDialog(
            project = project,
            logs = logs,
            onDismiss = onExportDismiss,
            onExportPdf = { quality ->
                onExportDismiss()
                onExportProject("PDF", quality, language)
                coroutineScope.launch {
                    snackbarHost.showSnackbar(
                        context.getString(R.string.pdf_preparing) + " (Arka plan)"
                    )
                }
            },
            onExportZip = { quality ->
                onExportDismiss()
                onExportProject("ZIP", quality, language)
                coroutineScope.launch {
                    snackbarHost.showSnackbar(
                        "ZIP dışa aktarımı arka planda başlatıldı. Bildirimleri kontrol edin."
                    )
                }
            }
        )
    }

    selectedPhotoForFullView?.let { photoId ->
        val index = allProjectPhotos.indexOfFirst { it.id == photoId }
        if (index != -1 && allProjectPhotos.isNotEmpty()) {
            val motion = com.fatihenes.photoreport.core.designsystem.theme.FotoRaporMotion
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(motion.DurationMedium)) +
                        scaleIn(
                            initialScale = 0.92f,
                            animationSpec = tween(motion.DurationMedium)
                        ),
                exit = fadeOut(tween(motion.DurationShort)) +
                        scaleOut(
                            targetScale = 0.92f,
                            animationSpec = tween(motion.DurationShort)
                        ),
                modifier = Modifier.fillMaxSize()
            ) {
                FullScreenPhotoDialog(
                    photoList = allProjectPhotos,
                    initialIndex = index,
                    onDismiss = onPhotoViewDismiss,
                    onDelete = onDeletePhoto,
                    onUpdateRotation = onUpdateRotation
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

