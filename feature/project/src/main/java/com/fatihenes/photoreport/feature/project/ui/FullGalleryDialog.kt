@file:Suppress("TooManyFunctions")
package com.fatihenes.photoreport.feature.project.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporMotion
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporTokens
import com.fatihenes.photoreport.core.ui.R
import com.fatihenes.photoreport.core.model.DailyLog
import com.fatihenes.photoreport.core.model.Photo
import com.fatihenes.photoreport.core.common.util.DateUtils
import com.fatihenes.photoreport.core.ui.util.MediaShareUtils
import com.fatihenes.photoreport.core.ui.navigation.LocalSnackbarHostState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private const val GALLERY_COLUMNS = 3
private const val TRANSITION_SCALE = 0.96f
private const val THUMB_SIZE = 256
private const val OVERLAY_ALPHA = 0.15f
private const val VIDEO_ICON_ALPHA = 0.9f
private const val DISMISS_DELAY_MS = 200

data class GalleryState(
    val log: DailyLog,
    val photos: List<Photo>,
    val isSelectionMode: Boolean,
    val selectedIds: List<Long>,
)

data class GalleryActions(
    val onToggleSelectionMode: () -> Unit,
    val onClearSelection: () -> Unit,
    val onShowDeleteConfirm: () -> Unit,
    val onDismiss: () -> Unit,
    val onTogglePhotoSelection: (Photo) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("FunctionName")
@Composable
fun FullGalleryDialog(
    log: DailyLog,
    photos: List<Photo>,
    onDismiss: () -> Unit,
    onPhotoClick: (Photo) -> Unit,
    onDeletePhotos: (List<Long>) -> Unit,
) {
    var isSelectionMode by remember { mutableStateOf(value = false) }
    val selectedIds = remember { mutableStateListOf<Long>() }
    var showBulkDeleteConfirm by remember { mutableStateOf(value = false) }
    var isVisible by remember { mutableStateOf(value = false) }

    LaunchedEffect(Unit) { isVisible = true }

    val scope = rememberCoroutineScope()
    val triggerDismiss = {
        isVisible = false
        scope.launch {
            delay(DISMISS_DELAY_MS.milliseconds)
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = { triggerDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        FullGalleryContent(
            isVisible = isVisible,
            state = GalleryState(log, photos, isSelectionMode, selectedIds.toList()),
            actions = GalleryActions(
                onToggleSelectionMode = { isSelectionMode = !isSelectionMode },
                onClearSelection = {
                    isSelectionMode = false
                    selectedIds.clear()
                },
                onShowDeleteConfirm = { showBulkDeleteConfirm = true },
                onDismiss = { triggerDismiss() },
            ) { photo ->
                if (selectedIds.contains(photo.id)) {
                    selectedIds.remove(photo.id)
                } else {
                    selectedIds.add(photo.id)
                }
            },
            onPhotoClick = onPhotoClick,
        )
    }

    if (showBulkDeleteConfirm) {
        BulkDeleteConfirmDialog(
            count = selectedIds.size,
            onDismiss = { showBulkDeleteConfirm = false },
        ) {
            onDeletePhotos(selectedIds.toList())
            selectedIds.clear()
            isSelectionMode = false
            showBulkDeleteConfirm = false
        }
    }
}

@Suppress("FunctionName")
@Composable
private fun FullGalleryContent(
    isVisible: Boolean,
    state: GalleryState,
    actions: GalleryActions,
    onPhotoClick: (Photo) -> Unit,
) {
    androidx.activity.compose.BackHandler(enabled = isVisible) { actions.onDismiss() }

    GalleryTransition(isVisible) {
        Scaffold(
            topBar = { GalleryTopAppBar(state = state, actions = actions) },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            GalleryGrid(
                photos = state.photos,
                selectedIds = state.selectedIds,
                isSelectionMode = state.isSelectionMode,
                padding = padding,
                onItemClick = { photo ->
                    if (state.isSelectionMode) actions.onTogglePhotoSelection(photo)
                    else onPhotoClick(photo)
                }
            )
        }
    }
}

@Suppress("FunctionName")
@Composable
private fun GalleryTransition(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.fadeIn(
            animationSpec = androidx.compose.animation.core.tween(FotoRaporMotion.DURATION_MEDIUM),
        ) + androidx.compose.animation.scaleIn(
            initialScale = TRANSITION_SCALE,
            animationSpec = androidx.compose.animation.core.tween(FotoRaporMotion.DURATION_MEDIUM),
        ),
        exit = androidx.compose.animation.fadeOut(
            animationSpec = androidx.compose.animation.core.tween(FotoRaporMotion.DURATION_SHORT),
        ) + androidx.compose.animation.scaleOut(
            targetScale = TRANSITION_SCALE,
            animationSpec = androidx.compose.animation.core.tween(FotoRaporMotion.DURATION_SHORT),
        ),
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("FunctionName")
@Composable
private fun GalleryTopAppBar(
    state: GalleryState,
    actions: GalleryActions,
) {
    TopAppBar(
        title = { GalleryTitle(state) },
        navigationIcon = {
            IconButton(
                onClick = if (state.isSelectionMode) actions.onClearSelection else actions.onDismiss,
            ) {
                Icon(
                    Icons.Default.Close,
                    stringResource(R.string.acc_close),
                    modifier = Modifier.size(FotoRaporTokens.IconSizeS),
                )
            }
        },
        actions = { GalleryTopAppBarActions(state = state, actions = actions) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
    )
}

@Suppress("FunctionName")
@Composable
private fun GalleryTitle(state: GalleryState) {
    val title = if (state.isSelectionMode) {
        pluralStringResource(R.plurals.selected_count, state.selectedIds.size, state.selectedIds.size)
    } else {
        DateUtils.formatDate(state.log.date)
    }
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun GalleryTopAppBarActions(
    state: GalleryState,
    actions: GalleryActions,
) {
    if (state.isSelectionMode) {
        SelectionModeActions(state = state, actions = actions)
    } else {
        TextButton(onClick = actions.onToggleSelectionMode) {
            Text(
                stringResource(R.string.bulk_select_delete),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SelectionModeActions(
    state: GalleryState,
    actions: GalleryActions
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHost = LocalSnackbarHostState.current

    IconButton(
        onClick = {
            if (state.selectedIds.isNotEmpty()) {
                val paths = state.photos
                    .asSequence()
                    .filter { state.selectedIds.contains(it.id) }
                    .map { it.filePath }
                    .toList()
                MediaShareUtils.shareMultipleMedia(context, paths) { message ->
                    scope.launch { snackbarHost.showSnackbar(message) }
                }
            }
        },
    ) {
        Icon(
            Icons.Default.Share,
            stringResource(R.string.acc_share),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(FotoRaporTokens.IconSizeS),
        )
    }
    IconButton(onClick = { if (state.selectedIds.isNotEmpty()) actions.onShowDeleteConfirm() }) {
        Icon(
            Icons.Default.Delete,
            stringResource(R.string.acc_delete),
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(FotoRaporTokens.IconSizeS),
        )
    }
}

@Suppress("FunctionName")
@Composable
private fun GalleryGrid(
    photos: List<Photo>,
    selectedIds: List<Long>,
    isSelectionMode: Boolean,
    padding: PaddingValues,
    onItemClick: (Photo) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    LazyVerticalGrid(
        columns = GridCells.Fixed(GALLERY_COLUMNS),
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(FotoRaporTokens.SpacingM),
        horizontalArrangement = Arrangement.spacedBy(FotoRaporTokens.SpacingXS + 2.dp),
        verticalArrangement = Arrangement.spacedBy(FotoRaporTokens.SpacingXS + 2.dp),
    ) {
        items(photos, key = { it.id }, contentType = { "photo_grid_item" }) { photo ->
            GalleryGridItem(
                photo = photo,
                isSelected = selectedIds.contains(photo.id),
                isSelectionMode = isSelectionMode,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onItemClick(photo)
                },
            )
        }
    }
}

@Suppress("FunctionName")
@Composable
private fun GalleryGridItem(
    photo: Photo,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val isVideo = photo.filePath.endsWith(".mp4", ignoreCase = true)
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick)
            .then(
                if (isSelectionMode && isSelected) {
                    Modifier.border(
                        2.5.dp,
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(FotoRaporTokens.RadiusS),
                    )
                } else Modifier,
            ),
        shape = RoundedCornerShape(FotoRaporTokens.RadiusS),
        elevation = CardDefaults.cardElevation(defaultElevation = FotoRaporTokens.ElevationNone),
    ) {
        Box {
            val request = remember(photo.filePath) {
                ImageRequest.Builder(context)
                    .data(photo.filePath)
                    .apply { if (isVideo) decoderFactory(VideoFrameDecoder.Factory()) }
                    .size(THUMB_SIZE)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .crossfade(FotoRaporMotion.DURATION_SHORT)
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = androidx.compose.ui.graphics.painter.ColorPainter(
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                error = androidx.compose.ui.graphics.painter.ColorPainter(
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            )
            if (isVideo) VideoPlayOverlay()
            if (isSelectionMode) SelectionOverlay(isSelected)
        }
    }
}

@Suppress("FunctionName")
@Composable
private fun VideoPlayOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = OVERLAY_ALPHA)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.PlayCircleFilled,
            null,
            tint = Color.White.copy(alpha = VIDEO_ICON_ALPHA),
            modifier = Modifier.size(FotoRaporTokens.IconSizeL),
        )
    }
}

@Suppress("FunctionName")
@Composable
private fun BoxScope.SelectionOverlay(isSelected: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = OVERLAY_ALPHA)
                } else {
                    Color.Transparent
                },
            ),
    )
    Checkbox(
        checked = isSelected,
        onCheckedChange = null,
        modifier = Modifier.align(Alignment.TopEnd),
    )
}

@Suppress("FunctionName")
@Composable
private fun BulkDeleteConfirmDialog(
    count: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(FotoRaporTokens.RadiusL),
        title = {
            Text(
                stringResource(R.string.delete_photo_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Text(
                pluralStringResource(R.plurals.delete_bulk_desc, count, count),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                shape = RoundedCornerShape(FotoRaporTokens.RadiusS),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = FotoRaporTokens.ElevationNone),
            ) {
                Text(
                    stringResource(R.string.delete_confirm_btn),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.cancel_btn),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
    )
}
