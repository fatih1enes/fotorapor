package com.fatihenes.photoreport.ui

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
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.CachePolicy
import coil3.video.VideoFrameDecoder
import com.fatihenes.photoreport.R
import com.fatihenes.photoreport.data.DailyLogEntity
import com.fatihenes.photoreport.data.PhotoEntity
import com.fatihenes.photoreport.util.DateUtils
import com.fatihenes.photoreport.util.MediaShareUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullGalleryDialog(
    log: DailyLogEntity,
    photos: List<PhotoEntity>,
    onDismiss: () -> Unit,
    onPhotoClick: (PhotoEntity) -> Unit,
    onDeletePhotos: (List<Long>) -> Unit
) {
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }

    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val coroutineScope = rememberCoroutineScope()
    fun triggerDismiss() {
        isVisible = false
        coroutineScope.launch {
            delay(200.milliseconds)
            onDismiss()
        }
    }

    Dialog(onDismissRequest = { triggerDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        androidx.activity.compose.BackHandler(enabled = isVisible) {
            triggerDismiss()
        }

        AnimatedVisibility(
            visible = isVisible,
            enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(200)) +
                    androidx.compose.animation.scaleIn(initialScale = 0.95f, animationSpec = androidx.compose.animation.core.tween(200)),
            exit = androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(200)) +
                   androidx.compose.animation.scaleOut(targetScale = 0.95f, animationSpec = androidx.compose.animation.core.tween(200))
        ) {
            Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            if (isSelectionMode) pluralStringResource(R.plurals.selected_count, selectedIds.size, selectedIds.size) else DateUtils.formatDate(log.date),
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (isSelectionMode) {
                                isSelectionMode = false
                                selectedIds.clear()
                            } else {
                                triggerDismiss()
                            }
                        }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    },
                    actions = {
                        if (isSelectionMode) {
                            val context = LocalContext.current
                            IconButton(onClick = {
                                if (selectedIds.isNotEmpty()) {
                                    val selectedPaths = photos.filter { selectedIds.contains(it.id) }.map { it.filePath }
                                    MediaShareUtils.shareMultipleMedia(context, selectedPaths)
                                }
                            }) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = {
                                if (selectedIds.isNotEmpty()) {
                                    showBulkDeleteConfirm = true
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            }
                        } else {
                            TextButton(onClick = { isSelectionMode = true }) {
                                Text(stringResource(R.string.bulk_select_delete), fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        ) { padding ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(photos, key = { it.id }) { photo ->
                    val isSelected = selectedIds.contains(photo.id)
                    val isVideo = photo.filePath.endsWith(".mp4", ignoreCase = true)
                    Card(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable {
                                if (isSelectionMode) {
                                    if (isSelected) selectedIds.remove(photo.id) else selectedIds.add(photo.id)
                                } else {
                                    onPhotoClick(photo)
                                }
                            }
                            .then(if (isSelectionMode && isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)) else Modifier),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box {
                            val context = LocalContext.current
                            val request = remember(photo.filePath) {
                                ImageRequest.Builder(context)
                                    .data(photo.filePath)
                                    .apply {
                                        if (isVideo) {
                                            decoderFactory(VideoFrameDecoder.Factory())
                                        }
                                    }
                                    .size(256)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .build()
                            }
                            AsyncImage(
                                model = request,
                                contentDescription = null,
                                placeholder = androidx.compose.ui.graphics.painter.ColorPainter(Color.LightGray),
                                error = androidx.compose.ui.graphics.painter.ColorPainter(Color.DarkGray),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            if (isVideo) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayCircleFilled,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                            if (isSelectionMode) {
                                Box(modifier = Modifier.fillMaxSize().background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent))
                                Checkbox(checked = isSelected, onCheckedChange = null, modifier = Modifier.align(Alignment.TopEnd))
                            }
                        }
                    }
                }
            }
        }
    }
    }

    if (showBulkDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_photo_title)) },
            text = { Text(pluralStringResource(R.plurals.delete_bulk_desc, selectedIds.size, selectedIds.size)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePhotos(selectedIds.toList())
                        selectedIds.clear()
                        isSelectionMode = false
                        showBulkDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(R.string.delete_confirm_btn), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteConfirm = false }) { Text(stringResource(R.string.cancel_btn)) }
            }
        )
    }
}
