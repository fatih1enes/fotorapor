@file:Suppress("TooManyFunctions")
package com.fatihenes.photoreport.feature.project.ui

import android.content.Context
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
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
import coil3.request.crossfade
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporMotion
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporTokens
import com.fatihenes.photoreport.core.ui.R
import com.fatihenes.photoreport.core.model.Photo
import com.fatihenes.photoreport.core.ui.util.MediaShareUtils
import com.fatihenes.photoreport.core.ui.navigation.LocalSnackbarHostState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private const val DISMISS_DELAY_MS = 200
private const val TRANSITION_SCALE = 0.96f
private const val PHOTO_VIEWER_OVERLAY_ALPHA = 0.8f
private const val ZOOM_MIN = 1f
private const val ZOOM_MAX = 5f
private const val VIDEO_PLACEHOLDER_ICON_SIZE = 72
private const val VIDEO_PLACEHOLDER_ICON_ALPHA = 0.6f
private const val VIDEO_PLACEHOLDER_BG_ALPHA = 0.1f
private const val IMAGE_FULL_RES_SIZE = 2400

data class FullScreenPhotoViewerCallbacks(
    val onDismiss: () -> Unit,
    val onDelete: (Photo) -> Unit,
    val onUpdateRotation: (Long, Float) -> Unit,
    val onSaveSuccess: (String) -> Unit = { },
    val onSaveError: () -> Unit = { },
    val onContentRefresh: () -> Unit = { },
)

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("FunctionName", "LongMethod")
@Composable
fun FullScreenPhotoDialog(
    photoList: List<Photo>,
    initialIndex: Int,
    callbacks: FullScreenPhotoViewerCallbacks,
) {
    val onDismiss = callbacks.onDismiss
    val onDelete = callbacks.onDelete
    val onUpdateRotation = callbacks.onUpdateRotation
    val onSaveSuccess = callbacks.onSaveSuccess
    val onSaveError = callbacks.onSaveError
    val onContentRefresh = callbacks.onContentRefresh

    val currentPhotoList by rememberUpdatedState(photoList)
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { currentPhotoList.size },
    )
    var showDeleteConfirm by remember { mutableStateOf(value = false) }
    var isVisible by remember { mutableStateOf(value = false) }
    var activeMarkupPhoto by remember { mutableStateOf<Photo?>(null) }

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            androidx.activity.compose.BackHandler(enabled = true) { triggerDismiss() }
            ViewerContent(
                isVisible = isVisible,
                photoList = photoList,
                pagerState = pagerState,
                onDismiss = { triggerDismiss() },
                onRotate = { rotatePhoto(photoList, pagerState, onUpdateRotation) },
                onShowDelete = { showDeleteConfirm = true },
                onOpenMarkup = {
                    if (pagerState.currentPage < photoList.size) {
                        activeMarkupPhoto = photoList[pagerState.currentPage]
                    }
                },
            )
        }
    }

    if (showDeleteConfirm) {
        DeleteConfirmWrapper(
            photoList = photoList,
            pagerState = pagerState,
            onDelete = onDelete,
            onDismissConfirm = { showDeleteConfirm = false },
            onTriggerDismissViewer = { triggerDismiss() },
        )
    }

    activeMarkupPhoto?.let { targetPhoto ->
        com.fatihenes.photoreport.feature.project.ui.markup.PhotoMarkupScreen(
            photo = targetPhoto,
            onDismiss = { activeMarkupPhoto = null },
            onSaveComplete = { filePath ->
                activeMarkupPhoto = null
                onSaveSuccess(filePath)
            },
            onSaveError = {
                activeMarkupPhoto = null
                onSaveError()
            },
            onContentRefresh = onContentRefresh,
        )
    }
}

@Suppress("FunctionName")
@Composable
private fun ViewerContent(
    isVisible: Boolean,
    photoList: List<Photo>,
    pagerState: PagerState,
    onDismiss: () -> Unit,
    onRotate: () -> Unit,
    onShowDelete: () -> Unit,
    onOpenMarkup: () -> Unit,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(FotoRaporMotion.DURATION_MEDIUM)) +
                scaleIn(initialScale = TRANSITION_SCALE),
        exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(FotoRaporMotion.DURATION_SHORT)) +
                scaleOut(targetScale = TRANSITION_SCALE),
    ) {
        Scaffold(
            containerColor = Color.Black,
            topBar = {
                PhotoViewerTopAppBar(
                    currentPage = pagerState.currentPage,
                    totalCount = photoList.size,
                    onDismiss = onDismiss,
                    onRotate = onRotate,
                    onShowDelete = onShowDelete,
                    onOpenMarkup = onOpenMarkup,
                    photoList = photoList
                )
            }
        ) { padding ->
            PhotoPager(photoList, pagerState, padding)
        }
    }
}

@Composable
private fun PhotoPager(photoList: List<Photo>, pagerState: PagerState, padding: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize().padding(padding).background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageSpacing = FotoRaporTokens.SpacingL,
            beyondViewportPageCount = 1,
            key = { page -> if (page < photoList.size) photoList[page].id else page }
        ) { page ->
            if (page < photoList.size) {
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

private const val ROTATION_STEP = 90f
private const val FULL_ROTATION = 360f

private fun rotatePhoto(
    photoList: List<Photo>,
    pagerState: PagerState,
    onUpdateRotation: (Long, Float) -> Unit,
) {
    if (pagerState.currentPage < photoList.size) {
        val photo = photoList[pagerState.currentPage]
        onUpdateRotation(photo.id, (photo.rotation + ROTATION_STEP) % FULL_ROTATION)
    }
}

@Composable
private fun DeleteConfirmWrapper(
    photoList: List<Photo>,
    pagerState: PagerState,
    onDelete: (Photo) -> Unit,
    onDismissConfirm: () -> Unit,
    onTriggerDismissViewer: () -> Unit
) {
    PhotoDeleteConfirmDialog(
        onDismiss = onDismissConfirm,
        onConfirm = {
            onDismissConfirm()
            if (pagerState.currentPage < photoList.size) {
                onDelete(photoList[pagerState.currentPage])
                if (photoList.size <= 1) onTriggerDismissViewer()
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongParameterList", "FunctionName")
@Composable
private fun PhotoViewerTopAppBar(
    currentPage: Int,
    totalCount: Int,
    onDismiss: () -> Unit,
    onRotate: () -> Unit,
    onShowDelete: () -> Unit,
    onOpenMarkup: () -> Unit,
    photoList: List<Photo>,
) {
    val context = LocalContext.current
    val snackbarHost = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()

    CenterAlignedTopAppBar(
        title = {
            Text(
                "${currentPage + 1} / $totalCount",
                color = Color.White.copy(alpha = PHOTO_VIEWER_OVERLAY_ALPHA),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
            )
        },
        navigationIcon = {
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    stringResource(R.string.acc_close),
                    tint = Color.White,
                    modifier = Modifier.size(FotoRaporTokens.IconSizeS),
                )
            }
        },
        actions = {
            PhotoViewerActions(
                params = PhotoViewerActionParams(
                    photoList = photoList,
                    currentPage = currentPage,
                    onRotate = onRotate,
                    onShowDelete = onShowDelete,
                    onOpenMarkup = onOpenMarkup,
                    context = context,
                    snackbarHost = snackbarHost,
                    scope = scope,
                ),
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
    )
}

private data class PhotoViewerActionParams(
    val photoList: List<Photo>,
    val currentPage: Int,
    val onRotate: () -> Unit,
    val onShowDelete: () -> Unit,
    val onOpenMarkup: () -> Unit,
    val context: android.content.Context,
    val snackbarHost: androidx.compose.material3.SnackbarHostState,
    val scope: kotlinx.coroutines.CoroutineScope,
)

@Composable
private fun PhotoViewerActions(params: PhotoViewerActionParams) {
    val isCurrentVideo = if (params.currentPage < params.photoList.size) {
        params.photoList[params.currentPage].filePath.endsWith(".mp4", ignoreCase = true)
    } else false

    if (!isCurrentVideo) {
        IconButton(onClick = params.onOpenMarkup) {
            Icon(
                Icons.Default.Brush,
                stringResource(R.string.markup_btn),
                tint = Color.White,
                modifier = Modifier.size(FotoRaporTokens.IconSizeS),
            )
        }
    }
    IconButton(onClick = params.onRotate) {
        Icon(
            Icons.AutoMirrored.Filled.RotateRight,
            stringResource(R.string.acc_rotate),
            tint = Color.White,
            modifier = Modifier.size(FotoRaporTokens.IconSizeS),
        )
    }
    IconButton(
        onClick = {
            if (params.currentPage < params.photoList.size) {
                MediaShareUtils.shareSingleMedia(
                    params.context,
                    params.photoList[params.currentPage].filePath,
                ) { msg ->
                    params.scope.launch { params.snackbarHost.showSnackbar(msg) }
                }
            }
        },
    ) {
        Icon(
            Icons.Default.Share,
            stringResource(R.string.acc_share),
            tint = Color.White,
            modifier = Modifier.size(FotoRaporTokens.IconSizeS),
        )
    }
    IconButton(onClick = params.onShowDelete) {
        Icon(
            Icons.Default.Delete,
            stringResource(R.string.acc_delete),
            tint = Color.White,
            modifier = Modifier.size(FotoRaporTokens.IconSizeS),
        )
    }
}

@Suppress("FunctionName", "LongMethod", "CyclomaticComplexMethod", "TooGenericExceptionCaught", "SwallowedException")
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun VideoPlayerItem(photo: Photo, isPageActive: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var exoPlayer by remember { mutableStateOf<androidx.media3.exoplayer.ExoPlayer?>(null) }
    var hasPlaybackError by remember { mutableStateOf(false) }

    DisposableEffect(photo.filePath) {
        val mediaUri = resolveVideoMediaUri(context, photo.filePath)
        val player = buildExoPlayer(context, mediaUri, isPageActive) {
            hasPlaybackError = true
        }
        exoPlayer = player
        onDispose { player.release(); exoPlayer = null }
    }

    DisposableEffect(exoPlayer, lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> exoPlayer?.pause()
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> if (isPageActive && !hasPlaybackError) exoPlayer?.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(isPageActive, exoPlayer, hasPlaybackError) {
        if (isPageActive && !hasPlaybackError) exoPlayer?.play() else exoPlayer?.pause()
    }

    if (isPageActive && exoPlayer != null && !hasPlaybackError) {
        AndroidView(
            factory = { ctx ->
                androidx.media3.ui.PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                }
            },
            update = { view ->
                if (view.player != exoPlayer) {
                    view.player = exoPlayer
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    } else {
        VideoPlaceholder()
    }
}

private fun resolveVideoMediaUri(context: Context, filePath: String): Uri {
    return try {
        MediaShareUtils.resolveMediaUri(context, filePath).first
    } catch (_: Exception) {
        if (filePath.startsWith("content://") || filePath.startsWith("file://")) {
            filePath.toUri()
        } else {
            java.io.File(filePath).toUri()
        }
    }
}

private fun buildExoPlayer(
    context: Context,
    mediaUri: Uri,
    isPageActive: Boolean,
    onError: () -> Unit
): androidx.media3.exoplayer.ExoPlayer {
    return androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
        setMediaItem(androidx.media3.common.MediaItem.fromUri(mediaUri))
        repeatMode = androidx.media3.common.Player.REPEAT_MODE_ALL
        addListener(object : androidx.media3.common.Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("VideoPlayerItem", "ExoPlayer error on $mediaUri", error)
                onError()
            }
        })
        prepare()
        playWhenReady = isPageActive
    }
}

@Suppress("FunctionName")
@Composable
private fun VideoPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.PlayArrow,
            null,
            tint = Color.White.copy(alpha = VIDEO_PLACEHOLDER_ICON_ALPHA),
            modifier = Modifier
                .size(VIDEO_PLACEHOLDER_ICON_SIZE.dp)
                .background(Color.White.copy(alpha = VIDEO_PLACEHOLDER_BG_ALPHA), CircleShape)
                .padding(FotoRaporTokens.SpacingL),
        )
    }
}

@Suppress("FunctionName")
@Composable
private fun ImageZoomItem(photo: Photo) {
    var scale by remember(photo.id) { mutableFloatStateOf(1f) }
    var offset by remember(photo.id) { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(photo.id) {
                handleZoomGestures(onGesture = { zoom, pan ->
                    val newScale = (scale * zoom).coerceIn(ZOOM_MIN, ZOOM_MAX)
                    if (newScale > ZOOM_MIN || scale > ZOOM_MIN) {
                        scale = newScale
                        offset += pan
                        true
                    } else {
                        scale = ZOOM_MIN
                        offset = androidx.compose.ui.geometry.Offset.Zero
                        false
                    }
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        val context = LocalContext.current
        val request = remember(photo.filePath) {
            ImageRequest.Builder(context)
                .data(photo.filePath)
                .size(IMAGE_FULL_RES_SIZE)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .crossfade(FotoRaporMotion.DURATION_MEDIUM)
                .build()
        }
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                    rotationZ = photo.rotation,
                ),
            placeholder = androidx.compose.ui.graphics.painter.ColorPainter(
                MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            error = androidx.compose.ui.graphics.painter.ColorPainter(
                MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        )
    }
}

private suspend fun PointerInputScope.handleZoomGestures(
    onGesture: (Float, androidx.compose.ui.geometry.Offset) -> Boolean,
) {
    awaitPointerEventScope {
        while (true) {
            awaitFirstDown()
            do {
                val event = awaitPointerEvent()
                val zoom = event.calculateZoom()
                val pan = event.calculatePan()
                val consumed = onGesture(zoom, pan)
                if (consumed) {
                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                }
            } while (event.changes.any { it.pressed })
        }
    }
}

@Suppress("FunctionName")
@Composable
private fun PhotoDeleteConfirmDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
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
                stringResource(R.string.delete_photo_desc),
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
