package com.fatihenes.photoreport.feature.project.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporTokens
import com.fatihenes.photoreport.core.ui.R
import com.fatihenes.photoreport.core.model.Photo
import com.fatihenes.photoreport.core.ui.util.MediaShareUtils
import com.fatihenes.photoreport.core.ui.navigation.LocalSnackbarHostState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenPhotoDialog(
    photoList: List<Photo>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    onDelete: (Photo) -> Unit,
    onUpdateRotation: (Long, Float) -> Unit
) {
    val currentPhotoList by rememberUpdatedState(photoList)
    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { currentPhotoList.size })
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { isVisible = true }

    val scope = rememberCoroutineScope()
    val triggerDismiss = {
        isVisible = false
        scope.launch { delay(200.milliseconds); onDismiss() }
        Unit
    }

    Dialog(
        onDismissRequest = { triggerDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            androidx.activity.compose.BackHandler(enabled = true) { triggerDismiss() }

            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(200)) +
                        scaleIn(initialScale = 0.96f),
                exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(150)) +
                        scaleOut(targetScale = 0.96f)
            ) {
                Scaffold(
                    containerColor = Color.Black,
                    topBar = {
                        PhotoViewerTopAppBar(
                            currentPage = pagerState.currentPage,
                            totalCount = photoList.size,
                            onDismiss = { triggerDismiss() },
                            onRotate = {
                                if (pagerState.currentPage < photoList.size) {
                                    val photo = photoList[pagerState.currentPage]
                                    onUpdateRotation(photo.id, (photo.rotation + 90f) % 360f)
                                }
                            },
                            onShowDelete = { showDeleteConfirm = true },
                            photoList = photoList
                        )
                    }
                ) { padding ->
                    Box(modifier = Modifier.fillMaxSize().padding(padding).background(Color.Black), contentAlignment = Alignment.Center) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            pageSpacing = FotoRaporTokens.SpacingL,
                            beyondViewportPageCount = 1,
                            key = { page -> if (page < photoList.size) photoList[page].id else page }
                        ) { page ->
                            if (page >= photoList.size) return@HorizontalPager
                            val photo = photoList[page]
                            if (photo.filePath.endsWith(".mp4", ignoreCase = true)) {
                                VideoPlayerItem(photo = photo, isPageActive = pagerState.currentPage == page)
                            } else {
                                ImageZoomItem(photo = photo)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        PhotoDeleteConfirmDialog(
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                showDeleteConfirm = false
                if (pagerState.currentPage < photoList.size) {
                    onDelete(photoList[pagerState.currentPage])
                    if (photoList.size <= 1) triggerDismiss()
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoViewerTopAppBar(
    currentPage: Int,
    totalCount: Int,
    onDismiss: () -> Unit,
    onRotate: () -> Unit,
    onShowDelete: () -> Unit,
    photoList: List<Photo>
) {
    val context = LocalContext.current
    val snackbarHost = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()

    CenterAlignedTopAppBar(
        title = {
            Text(
                "${currentPage + 1} / $totalCount",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        },
        navigationIcon = {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, stringResource(R.string.acc_close), tint = Color.White, modifier = Modifier.size(FotoRaporTokens.IconSizeS))
            }
        },
        actions = {
            IconButton(onClick = onRotate) {
                Icon(Icons.AutoMirrored.Filled.RotateRight, stringResource(R.string.acc_rotate), tint = Color.White, modifier = Modifier.size(FotoRaporTokens.IconSizeS))
            }
            IconButton(onClick = {
                if (currentPage < photoList.size) {
                    MediaShareUtils.shareSingleMedia(context, photoList[currentPage].filePath) { msg ->
                        scope.launch { snackbarHost.showSnackbar(msg) }
                    }
                }
            }) {
                Icon(Icons.Default.Share, stringResource(R.string.acc_share), tint = Color.White, modifier = Modifier.size(FotoRaporTokens.IconSizeS))
            }
            IconButton(onClick = onShowDelete) {
                Icon(Icons.Default.Delete, stringResource(R.string.acc_delete), tint = Color.White, modifier = Modifier.size(FotoRaporTokens.IconSizeS))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun VideoPlayerItem(photo: Photo, isPageActive: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var exoPlayer by remember { mutableStateOf<androidx.media3.exoplayer.ExoPlayer?>(null) }

    DisposableEffect(photo.filePath) {
        val player = androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            setMediaItem(androidx.media3.common.MediaItem.fromUri(photo.filePath.toUri()))
            repeatMode = androidx.media3.common.Player.REPEAT_MODE_ALL
            prepare()
            playWhenReady = false
        }
        exoPlayer = player

        onDispose {
            player.release()
            exoPlayer = null
        }
    }

    DisposableEffect(exoPlayer, lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> exoPlayer?.pause()
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> if (isPageActive) exoPlayer?.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(isPageActive, exoPlayer) {
        if (isPageActive) exoPlayer?.play() else exoPlayer?.pause()
    }

    if (isPageActive && exoPlayer != null) {
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
        VideoPlaceholder()
    }
}

@Composable
private fun VideoPlaceholder() {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Icon(
            imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(72.dp).background(Color.White.copy(alpha = 0.1f), CircleShape).padding(FotoRaporTokens.SpacingL)
        )
    }
}

@Composable
private fun ImageZoomItem(photo: Photo) {
    var scale by remember(photo.id) { mutableFloatStateOf(1f) }
    var offset by remember(photo.id) { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    Box(
        modifier = Modifier.fillMaxSize().pointerInput(photo.id) {
            awaitEachGesture {
                awaitFirstDown()
                do {
                    val event = awaitPointerEvent()
                    val zoom = event.calculateZoom()
                    val pan = event.calculatePan()
                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                    if (newScale > 1f || scale > 1f) {
                        scale = newScale
                        offset += pan
                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                    } else {
                        scale = 1f
                        offset = androidx.compose.ui.geometry.Offset.Zero
                    }
                } while (event.changes.any { it.pressed })
            }
        },
        contentAlignment = Alignment.Center
    ) {
        val context = LocalContext.current
        val request = remember(photo.filePath) {
            ImageRequest.Builder(context).data(photo.filePath).size(2400).memoryCachePolicy(CachePolicy.ENABLED).diskCachePolicy(CachePolicy.ENABLED).build()
        }
        AsyncImage(
            model = request, contentDescription = null, contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y, rotationZ = photo.rotation),
            placeholder = androidx.compose.ui.graphics.painter.ColorPainter(MaterialTheme.colorScheme.surfaceContainerHigh),
            error = androidx.compose.ui.graphics.painter.ColorPainter(MaterialTheme.colorScheme.surfaceContainerHigh)
        )
    }
}

@Composable
private fun PhotoDeleteConfirmDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(FotoRaporTokens.RadiusL),
        title = { Text(stringResource(R.string.delete_photo_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) },
        text = { Text(stringResource(R.string.delete_photo_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
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
