package com.fatihenes.photoreport.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.CachePolicy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.input.pointer.positionChanged
import androidx.core.net.toUri
import com.fatihenes.photoreport.R
import com.fatihenes.photoreport.data.PhotoEntity
import com.fatihenes.photoreport.util.MediaShareUtils

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenPhotoDialog(
    photoList: List<PhotoEntity>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    onDelete: (PhotoEntity) -> Unit,
    onUpdateRotation: (Long, Float) -> Unit
) {
    val currentPhotoList by rememberUpdatedState(photoList)
    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { currentPhotoList.size })
    var showDeleteConfirm by remember { mutableStateOf(false) }

    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    fun triggerDismiss() {
        onDismiss()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        androidx.activity.compose.BackHandler(enabled = true) {
            triggerDismiss()
        }
            Scaffold(
                containerColor = Color.Black,
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text("${pagerState.currentPage + 1} / ${photoList.size}", color = Color.White, style = MaterialTheme.typography.bodyMedium) },
                        navigationIcon = {
                            IconButton(onClick = { triggerDismiss() }) { Icon(Icons.Default.Close, contentDescription = null, tint = Color.White) }
                        },
                    actions = {
                        val context = LocalContext.current
                        IconButton(onClick = {
                            val currentPhoto = if (pagerState.currentPage < photoList.size) photoList[pagerState.currentPage] else null
                            currentPhoto?.let { onUpdateRotation(it.id, (it.rotation + 90f) % 360f) }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = stringResource(R.string.rotate_label), tint = Color.White)
                        }
                        IconButton(onClick = {
                            val currentPhoto = if (pagerState.currentPage < photoList.size) photoList[pagerState.currentPage] else null
                            currentPhoto?.let { MediaShareUtils.shareSingleMedia(context, it.filePath) }
                        }) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share_label), tint = Color.White)
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_label), tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    pageSpacing = 16.dp,
                    beyondViewportPageCount = 1,
                    userScrollEnabled = true,
                    key = { page -> if (page < photoList.size) photoList[page].id else page }
                ) { page ->
                    if (page >= photoList.size) return@HorizontalPager
                    val photo = photoList[page]
                    val isVideo = photo.filePath.endsWith(".mp4", ignoreCase = true)
                    if (isVideo) {
                        val isPageActive = pagerState.currentPage == page
                        val context = LocalContext.current
                        val exoPlayer = remember {
                            androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
                                setMediaItem(androidx.media3.common.MediaItem.fromUri(photo.filePath.toUri()))
                                repeatMode = androidx.media3.common.Player.REPEAT_MODE_ALL
                                prepare()
                                playWhenReady = false
                            }
                        }

                        val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                        DisposableEffect(exoPlayer, lifecycleOwner) {
                            val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                                when (event) {
                                    androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> {
                                        exoPlayer.pause()
                                    }
                                    androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                                        if (pagerState.currentPage == page) {
                                            exoPlayer.play()
                                        }
                                    }
                                    androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> {
                                        exoPlayer.release()
                                    }
                                    else -> {}
                                }
                            }
                            lifecycleOwner.lifecycle.addObserver(observer)
                            onDispose {
                                lifecycleOwner.lifecycle.removeObserver(observer)
                                exoPlayer.release()
                            }
                        }

                        LaunchedEffect(isPageActive) {
                            if (isPageActive) {
                                exoPlayer.play()
                            } else {
                                exoPlayer.pause()
                            }
                        }

                        if (isPageActive) {
                            AndroidView(
                                factory = { ctx ->
                                    androidx.media3.ui.PlayerView(ctx).apply {
                                        player = exoPlayer
                                        useController = true
                                        setShowNextButton(false)
                                        setShowPreviousButton(false)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize().background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .size(80.dp)
                                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                                        .padding(16.dp)
                                )
                            }
                        }
                    } else {
                        var scale by remember(photo.id) { mutableFloatStateOf(1f) }
                        var offset by remember(photo.id) { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(photo.id) {
                                    awaitEachGesture {
                                        awaitFirstDown()
                                        do {
                                            val event = awaitPointerEvent()
                                            val zoom = event.calculateZoom()
                                            val pan = event.calculatePan()

                                            var shouldConsume = false
                                            val newScale = (scale * zoom).coerceIn(1f, 5f)

                                            if (newScale > 1f || scale > 1f) {
                                                scale = newScale
                                                offset += pan
                                                shouldConsume = true
                                            } else {
                                                scale = 1f
                                                offset = androidx.compose.ui.geometry.Offset.Zero
                                            }

                                            if (shouldConsume) {
                                                event.changes.forEach {
                                                    if (it.positionChanged()) it.consume()
                                                }
                                            }
                                        } while (event.changes.any { it.pressed })
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val context = LocalContext.current
                            val request = remember(photo.filePath) {
                                ImageRequest.Builder(context)
                                    .data(photo.filePath)
                                    .size(2400)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .build()
                            }

                            AsyncImage(
                                model = request,
                                contentDescription = null,
                                placeholder = androidx.compose.ui.graphics.painter.ColorPainter(Color.LightGray),
                                error = androidx.compose.ui.graphics.painter.ColorPainter(Color.DarkGray),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offset.x,
                                        translationY = offset.y,
                                        rotationZ = photo.rotation
                                    ),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_photo_title)) },
            text = { Text(stringResource(R.string.delete_photo_desc)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        val currentPhoto = if (pagerState.currentPage < photoList.size) photoList[pagerState.currentPage] else null
                        currentPhoto?.let {
                            onDelete(it)
                            if (photoList.size <= 1) onDismiss()
                        }
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
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.cancel_btn)) }
            }
        )
    }
}
