package com.fatihenes.photoreport.feature.project.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullGalleryDialog(
    log: DailyLog,
    photos: List<Photo>,
    onDismiss: () -> Unit,
    onPhotoClick: (Photo) -> Unit,
    onDeletePhotos: (List<Long>) -> Unit,
) {
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { isVisible = true }

    val scope = rememberCoroutineScope()
    val triggerDismiss = {
        isVisible = false
        scope.launch { delay(200.milliseconds); onDismiss() }
    }

    Dialog(
        onDismissRequest = { triggerDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        androidx.activity.compose.BackHandler(enabled = isVisible) { triggerDismiss() }

        AnimatedVisibility(
            visible = isVisible,
            enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(FotoRaporMotion.DurationMedium)) +
                    androidx.compose.animation.scaleIn(initialScale = 0.96f, animationSpec = androidx.compose.animation.core.tween(FotoRaporMotion.DurationMedium)),
            exit = androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(FotoRaporMotion.DurationShort)) +
                    androidx.compose.animation.scaleOut(targetScale = 0.96f, animationSpec = androidx.compose.animation.core.tween(FotoRaporMotion.DurationShort))
        ) {
            Scaffold(
                topBar = {
                    GalleryTopAppBar(
                        log = log,
                        photos = photos,
                        isSelectionMode = isSelectionMode,
                        selectedIds = selectedIds,
                        onToggleSelectionMode = { isSelectionMode = !isSelectionMode },
                        onClearSelection = { isSelectionMode = false; selectedIds.clear() },
                        onShowDeleteConfirm = { showBulkDeleteConfirm = true },
                        onDismiss = { triggerDismiss() }
                    )
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { padding ->
                GalleryGrid(
                    photos = photos,
                    selectedIds = selectedIds,
                    isSelectionMode = isSelectionMode,
                    padding = padding,
                    onItemClick = { photo ->
                        if (isSelectionMode) {
                            if (selectedIds.contains(photo.id)) selectedIds.remove(photo.id) else selectedIds.add(photo.id)
                        } else onPhotoClick(photo)
                    }
                )
            }
        }
    }

    if (showBulkDeleteConfirm) {
        BulkDeleteConfirmDialog(
            count = selectedIds.size,
            onDismiss = { showBulkDeleteConfirm = false },
            onConfirm = {
                onDeletePhotos(selectedIds.toList())
                selectedIds.clear()
                isSelectionMode = false
                showBulkDeleteConfirm = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GalleryTopAppBar(
    log: DailyLog,
    photos: List<Photo>,
    isSelectionMode: Boolean,
    selectedIds: List<Long>,
    onToggleSelectionMode: () -> Unit,
    onClearSelection: () -> Unit,
    onShowDeleteConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHost = LocalSnackbarHostState.current

    TopAppBar(
        title = {
            Text(
                if (isSelectionMode) pluralStringResource(R.plurals.selected_count, selectedIds.size, selectedIds.size)
                else DateUtils.formatDate(log.date),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold
            )
        },
        navigationIcon = {
            IconButton(onClick = if (isSelectionMode) onClearSelection else onDismiss) {
                Icon(Icons.Default.Close, stringResource(R.string.acc_close), modifier = Modifier.size(FotoRaporTokens.IconSizeS))
            }
        },
        actions = {
            if (isSelectionMode) {
                IconButton(onClick = {
                    if (selectedIds.isNotEmpty()) {
                        val paths = photos.filter { selectedIds.contains(it.id) }.map { it.filePath }
                        MediaShareUtils.shareMultipleMedia(context, paths) { scope.launch { snackbarHost.showSnackbar(it) } }
                    }
                }) {
                    Icon(Icons.Default.Share, stringResource(R.string.acc_share), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(FotoRaporTokens.IconSizeS))
                }
                IconButton(onClick = { if (selectedIds.isNotEmpty()) onShowDeleteConfirm() }) {
                    Icon(Icons.Default.Delete, stringResource(R.string.acc_delete), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(FotoRaporTokens.IconSizeS))
                }
            } else {
                TextButton(onClick = onToggleSelectionMode) {
                    Text(stringResource(R.string.bulk_select_delete), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
    )
}

@Composable
private fun GalleryGrid(
    photos: List<Photo>,
    selectedIds: List<Long>,
    isSelectionMode: Boolean,
    padding: PaddingValues,
    onItemClick: (Photo) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(FotoRaporTokens.SpacingM),
        horizontalArrangement = Arrangement.spacedBy(FotoRaporTokens.SpacingXS + 2.dp),
        verticalArrangement = Arrangement.spacedBy(FotoRaporTokens.SpacingXS + 2.dp)
    ) {
        items(photos, key = { it.id }, contentType = { "photo_grid_item" }) { photo ->
            GalleryGridItem(
                photo = photo,
                isSelected = selectedIds.contains(photo.id),
                isSelectionMode = isSelectionMode,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onItemClick(photo)
                }
            )
        }
    }
}

@Composable
private fun GalleryGridItem(
    photo: Photo,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val isVideo = photo.filePath.endsWith(".mp4", ignoreCase = true)
    Card(
        modifier = Modifier.aspectRatio(1f).clickable(onClick = onClick)
            .then(if (isSelectionMode && isSelected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(FotoRaporTokens.RadiusS)) else Modifier),
        shape = RoundedCornerShape(FotoRaporTokens.RadiusS),
        elevation = CardDefaults.cardElevation(defaultElevation = FotoRaporTokens.ElevationNone)
    ) {
        Box {
            val request = remember(photo.filePath) {
                ImageRequest.Builder(context).data(photo.filePath).apply { if (isVideo) decoderFactory(VideoFrameDecoder.Factory()) }
                    .size(256).memoryCachePolicy(CachePolicy.ENABLED).diskCachePolicy(CachePolicy.ENABLED).crossfade(150).build()
            }
            AsyncImage(
                model = request, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                placeholder = androidx.compose.ui.graphics.painter.ColorPainter(MaterialTheme.colorScheme.surfaceContainerHigh),
                error = androidx.compose.ui.graphics.painter.ColorPainter(MaterialTheme.colorScheme.surfaceContainerHigh)
            )
            if (isVideo) VideoPlayOverlay()
            if (isSelectionMode) SelectionOverlay(isSelected)
        }
    }
}

@Composable
private fun VideoPlayOverlay() {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
        Icon(imageVector = Icons.Default.PlayCircleFilled, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(FotoRaporTokens.IconSizeL))
    }
}

@Composable
private fun BoxScope.SelectionOverlay(isSelected: Boolean) {
    Box(modifier = Modifier.fillMaxSize().background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent))
    Checkbox(checked = isSelected, onCheckedChange = null, modifier = Modifier.align(Alignment.TopEnd))
}

@Composable
private fun BulkDeleteConfirmDialog(
    count: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(FotoRaporTokens.RadiusL),
        title = { Text(stringResource(R.string.delete_photo_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) },
        text = { Text(pluralStringResource(R.plurals.delete_bulk_desc, count, count), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
                shape = RoundedCornerShape(FotoRaporTokens.RadiusS),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = FotoRaporTokens.ElevationNone)
            ) {
                Text(stringResource(R.string.delete_confirm_btn), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_btn), style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}
