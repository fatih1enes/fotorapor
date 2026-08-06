package com.fatihenes.photoreport.feature.trash.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporTokens
import com.fatihenes.photoreport.core.ui.R
import com.fatihenes.photoreport.core.model.Photo
import com.fatihenes.photoreport.core.model.Project
import com.fatihenes.photoreport.core.common.util.DateUtils
import com.fatihenes.photoreport.feature.trash.viewmodel.TrashViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    onBack: () -> Unit,
    language: String = "tr",
    viewModel: TrashViewModel = hiltViewModel()
) {
    val deletedProjects by viewModel.deletedProjects.collectAsState(initial = emptyList())
    val deletedPhotos by viewModel.deletedPhotos.collectAsState(initial = emptyList())
    
    var showDeleteConfirmProject by remember { mutableStateOf<Long?>(null) }
    var showDeleteConfirmPhoto by remember { mutableStateOf<Photo?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.trash_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.back_label),
                            modifier = Modifier.size(FotoRaporTokens.IconSizeS)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (deletedProjects.isEmpty() && deletedPhotos.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                com.fatihenes.photoreport.core.ui.components.AppEmptyState(
                    icon = Icons.Default.DeleteOutline,
                    title = stringResource(R.string.empty_trash_title),
                    description = stringResource(R.string.empty_trash_desc)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = FotoRaporTokens.ScreenPaddingHorizontal),
                contentPadding = PaddingValues(
                    top = FotoRaporTokens.SpacingS,
                    bottom = FotoRaporTokens.Spacing3XL
                )
            ) {
                if (deletedProjects.isNotEmpty()) {
                    item {
                        TrashSectionHeader(stringResource(R.string.trash_deleted_projects))
                    }
                    items(deletedProjects, key = { it.id }) { project ->
                        TrashProjectItem(
                            project = project,
                            language = language,
                            onRestore = { viewModel.restoreProject(project.id) },
                            onDelete = { showDeleteConfirmProject = project.id }
                        )
                        Spacer(Modifier.height(FotoRaporTokens.SpacingS))
                    }
                    item { Spacer(Modifier.height(FotoRaporTokens.SpacingL)) }
                }

                if (deletedPhotos.isNotEmpty()) {
                    item {
                        TrashSectionHeader(stringResource(R.string.trash_deleted_photos))
                    }
                    items(deletedPhotos, key = { it.id }) { photo ->
                        TrashPhotoItem(
                            photo = photo,
                            language = language,
                            onRestore = { viewModel.restorePhoto(photo.id) },
                            onDelete = { showDeleteConfirmPhoto = photo }
                        )
                        Spacer(Modifier.height(FotoRaporTokens.SpacingS))
                    }
                }
            }
        }
    }

    // ── Confirm Delete Project Dialog ────────────
    if (showDeleteConfirmProject != null) {
        AppConfirmDialog(
            title = stringResource(R.string.trash_delete_confirm_title),
            message = stringResource(R.string.trash_delete_confirm_desc),
            confirmLabel = stringResource(R.string.btn_confirm_delete),
            onConfirm = {
                showDeleteConfirmProject?.let { id ->
                    viewModel.hardDeleteProject(id)
                }
                showDeleteConfirmProject = null
            },
            onDismiss = { showDeleteConfirmProject = null }
        )
    }

    // ── Confirm Delete Photo Dialog ──────────────
    if (showDeleteConfirmPhoto != null) {
        AppConfirmDialog(
            title = stringResource(R.string.delete_photo_title),
            message = stringResource(R.string.delete_photo_desc),
            confirmLabel = stringResource(R.string.delete_confirm_btn),
            onConfirm = {
                showDeleteConfirmPhoto?.let { photo ->
                    viewModel.hardDeletePhoto(photo)
                }
                showDeleteConfirmPhoto = null
            },
            onDismiss = { showDeleteConfirmPhoto = null }
        )
    }
}

// ─── Section Header ──────────────────────────────────────────

@Composable
private fun TrashSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing * 1.5f,
        modifier = Modifier.padding(
            start = FotoRaporTokens.SpacingXS,
            bottom = FotoRaporTokens.SpacingS,
            top = FotoRaporTokens.SpacingS
        )
    )
}

// ─── Confirm Dialog ──────────────────────────────────────────

@Composable
private fun AppConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(FotoRaporTokens.RadiusL),
        title = {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                message,
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
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = FotoRaporTokens.ElevationNone
                )
            ) {
                Text(
                    confirmLabel,
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

// ─── Trash Project Item ──────────────────────────────────────

@Composable
fun TrashProjectItem(
    project: Project,
    language: String,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val dateStr = project.deletedAt?.let { DateUtils.formatDateTime(it, language) } ?: stringResource(R.string.unknown_date)
    val projectColor = remember(project.colorHex) {
        runCatching { Color(project.colorHex.toColorInt()) }.getOrDefault(Color.Gray)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FotoRaporTokens.RadiusM),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            FotoRaporTokens.CardBorderWidth,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(FotoRaporTokens.SpacingL),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color badge
            Surface(
                modifier = Modifier.size(FotoRaporTokens.AvatarSizeS),
                shape = RoundedCornerShape(FotoRaporTokens.RadiusS),
                color = projectColor.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(projectColor, CircleShape)
                    )
                }
            }

            Spacer(Modifier.width(FotoRaporTokens.SpacingM))

            Column(Modifier.weight(1f)) {
                Text(
                    project.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(FotoRaporTokens.SpacingXXS))
                Text(
                    stringResource(R.string.trash_deleted_at, dateStr),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Action buttons
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onRestore()
                },
                modifier = Modifier.size(FotoRaporTokens.TouchTarget)
            ) {
                Icon(
                    Icons.Default.Restore,
                    stringResource(R.string.restore_btn),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(FotoRaporTokens.IconSizeS)
                )
            }
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDelete()
                },
                modifier = Modifier.size(FotoRaporTokens.TouchTarget)
            ) {
                Icon(
                    Icons.Default.Delete,
                    stringResource(R.string.hard_delete_btn),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(FotoRaporTokens.IconSizeS)
                )
            }
        }
    }
}

// ─── Trash Photo Item ────────────────────────────────────────

@Composable
fun TrashPhotoItem(
    photo: Photo,
    language: String,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val dateStr = photo.deletedAt?.let { DateUtils.formatDateTime(it, language) } ?: stringResource(R.string.unknown_date)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FotoRaporTokens.RadiusM),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            FotoRaporTokens.CardBorderWidth,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(FotoRaporTokens.SpacingM),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Photo thumbnail
            AsyncImage(
                model = photo.filePath,
                contentDescription = stringResource(R.string.photo_label),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(FotoRaporTokens.RadiusS))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            )

            Spacer(Modifier.width(FotoRaporTokens.SpacingM))

            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.photo_label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(FotoRaporTokens.SpacingXXS))
                Text(
                    stringResource(R.string.trash_deleted_at, dateStr),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onRestore()
                },
                modifier = Modifier.size(FotoRaporTokens.TouchTarget)
            ) {
                Icon(
                    Icons.Default.Restore,
                    stringResource(R.string.restore_btn),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(FotoRaporTokens.IconSizeS)
                )
            }
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDelete()
                },
                modifier = Modifier.size(FotoRaporTokens.TouchTarget)
            ) {
                Icon(
                    Icons.Default.Delete,
                    stringResource(R.string.hard_delete_btn),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(FotoRaporTokens.IconSizeS)
                )
            }
        }
    }
}
